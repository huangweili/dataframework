package com.hwlcn.dataframework.application

import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import com.hwlcn.dataframework.message.AppMasterMessage.{ActorCreated, BindLifeCycle, CreateActor, CreateActorFailed}
import com.hwlcn.dataframework.message.AppMasterToMaster.RequestResource
import com.hwlcn.dataframework.message.AppMasterToWorker.{LaunchExecutor, ShutdownExecutor}
import com.hwlcn.dataframework.message.ApplicationMessage.{ActorSystemRegistered, RegisterActorSystem, SubmitApplicationResult}
import com.hwlcn.dataframework.message.MasterToAppMaster.ResourceAllocated
import com.hwlcn.dataframework.message.WorkerToAppMaster.ExecutorLaunchRejected
import com.hwlcn.dataframework.scheduler.{Resource, ResourceAllocation, ResourceRequest}
import com.hwlcn.dataframework.system.ActorSystemBooter
import com.hwlcn.dataframework.worker.WorkerInfo
import com.hwlcn.dataframework.{ActorUtil, ClusterConfig, HostPort}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  * 该类用来实现 App的加载启动机制
  * 根据传入的Application的信息来实现 app的实例化
  * 每次做启动的时候都会有一个独立的actor来进行执行 ，相当于一个新的线程中进行
  *
  * @author huangweili
  */
class ApplicationLauncher(appId: Int, executorId: Int, app: AppDescription,
                          jar: Option[AppJar], username: String, master: ActorRef, client: Option[ActorRef])
  extends Actor {

  private val logger = LoggerFactory.getLogger(getClass);

  private val scheduler = context.system.scheduler

  //系统的配置信息
  private val systemConfig = context.system.settings.config


  private val appMasterAkkaConfig: Config = app.getClusterConfig;


  //定义交互超时时间
  private val TIMEOUT = Duration(15, TimeUnit.SECONDS)

  override def preStart(): Unit = {
    logger.info(s"向master申请资源来启动 应用:${appId}")
    //发送请求到master申请资源
    master ! RequestResource(appId, ResourceRequest(Resource(1), WorkerId.unspecified))
    context.become(waitForResourceAllocation)
  }

  /**
    * 等待资源分配结果
    *
    * @return
    */
  def waitForResourceAllocation: Receive = {
    case ResourceAllocated(allocations) => {
      val ResourceAllocation(resource, worker, workerId) = allocations(0)

      logger.info(s"APPID:${appId}已在服务器${workerId}(${worker.path})申请到了资源。")

      //定义worker信息
      val workerInfo = new WorkerInfo(workerId, worker)

      val appMasterContext = AppMasterContext(appId, username, resource, workerInfo, jar, null)

      logger.info(s"在worker:${workerId} 启动app:${appId}")

      val name = ActorUtil.actorNameForExecutor(appId, executorId)

      val selfPath = ActorUtil.getFullPath(context.system, self.path)

      val jvmSetting = ClusterConfig.resolveJvmSetting(appMasterAkkaConfig.withFallback(systemConfig)).getAppMater

      val executorJVM: ExecutorJVMConfig = new ExecutorJVMConfig(
        jvmSetting.getClassPath,
        jvmSetting.getVmargs,
        classOf[ActorSystemBooter].getName,
        Array(name, selfPath).toList.asJava,
        jar.getOrElse(null),
        username,
        appMasterAkkaConfig
      )

      worker ! LaunchExecutor(appId, executorId, resource, executorJVM)
      context.become(waitForActorSystemToStart(worker, appMasterContext, resource))
    }
  }


  def waitForActorSystemToStart(worker: ActorRef, appContext: AppMasterContext,
                                resource: Resource): Receive = {
    case ExecutorLaunchRejected(reason, ex) =>
      logger.error(s"Executor Launch failed reason: $reason", ex)
      logger.info(s"reallocate resource $resource to start appmaster")
      master ! RequestResource(appId, ResourceRequest(resource, WorkerId.unspecified))
      context.become(waitForResourceAllocation)
    case RegisterActorSystem(systemPath) =>
      logger.info(s"Received RegisterActorSystem $systemPath for AppMaster")
      sender ! ActorSystemRegistered(worker)


      //获取集群的种子
      val masterAddress = systemConfig.getStringList("master.masters").asScala.map(HostPort(_)).map(ActorUtil.getMasterActorPath)

      sender ! CreateActor(AppMasterRuntimeEnvironment.props(masterAddress, app, appContext),
        s"appdaemon$appId")
      import context.dispatcher
      val appMasterTimeout = scheduler.scheduleOnce(TIMEOUT, self,
        CreateActorFailed(app.getAppMaster, new TimeoutException))
      context.become(waitForAppMasterToStart(worker, appMasterTimeout))
  }

  /**
    * 等待app创建启动成功
    *
    * 在app创建后 本身的actor需要进行注销，以免引起内存垃圾
    *
    * @param worker
    * @param cancel
    * @return
    */
  def waitForAppMasterToStart(worker: ActorRef, cancel: Cancellable): Receive = {
    case ActorCreated(appMaster, _) =>
      cancel.cancel()
      sender ! BindLifeCycle(appMaster)
      replyToClient(SubmitApplicationResult(Success(appId)))
      context.stop(self)
    case CreateActorFailed(_, reason) =>
      cancel.cancel()
      worker ! ShutdownExecutor(appId, executorId, reason.getMessage)
      replyToClient(SubmitApplicationResult(Failure(reason)))
      context.stop(self)
  }

  def replyToClient(result: SubmitApplicationResult): Unit = {
    client.foreach(_.tell(result, master))
  }

  override def receive: Receive = ???
}


object ApplicationLauncher extends ApplicationLauncherFactory {
  def props(appId: Int, executorId: Int, app: AppDescription, jar: Option[AppJar],
            username: String, master: ActorRef, client: Option[ActorRef]): Props = {
    Props(new ApplicationLauncher(appId, executorId, app, jar, username, master, client))
  }
}

/**
  * App 加载启动器
  */
trait ApplicationLauncherFactory {
  def props(appId: Int, executorId: Int, app: AppDescription, jar: Option[AppJar],
            username: String, master: ActorRef, client: Option[ActorRef]): Props
}