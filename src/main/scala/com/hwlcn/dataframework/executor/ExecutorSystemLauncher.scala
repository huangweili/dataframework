package com.hwlcn.dataframework.executor

import akka.actor.{Actor, ActorRef, AddressFromURIString}
import com.hwlcn.dataframework.ActorUtil
import com.hwlcn.dataframework.application.ExecutorJVMConfig
import com.hwlcn.dataframework.executor.ExecutorSystemScheduler.{ExecutorSystemJvmConfig, Session}
import com.hwlcn.dataframework.message.AppMasterToWorker.LaunchExecutor
import com.hwlcn.dataframework.message.ApplicationMessage.{ActorSystemRegistered, RegisterActorSystem}
import com.hwlcn.dataframework.message.WorkerToAppMaster.ExecutorLaunchRejected
import com.hwlcn.dataframework.scheduler.Resource
import com.hwlcn.dataframework.worker.WorkerInfo
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * Executor的加载器
  *
  * @author huangweili
  */
class ExecutorSystemLauncher(appId: Int, session: Session) extends Actor {

  import ExecutorSystemLauncher._

  private val logger = LoggerFactory.getLogger(getClass);

  val scheduler = context.system.scheduler

  implicit val executionContext = context.dispatcher

  val timeoutSetting = 300000 //启动程序超过5分钟认为是超时

  val timeout = scheduler.scheduleOnce(timeoutSetting.milliseconds, self, LaunchExecutorSystemTimeout(session))

  def receive: Receive = waitForLaunchCommand

  def waitForLaunchCommand: Receive = {
    case LaunchExecutorSystem(worker, executorSystemId, resource) =>
      val launcherPath = ActorUtil.getFullPath(context.system, self.path)
      val jvmConfig = Option(session.executorSystemJvmConfig)
        .map(getExecutorJvmConfig(_, s"app${appId}system${executorSystemId}", launcherPath)).orNull

      val launch = LaunchExecutor(appId, executorSystemId, resource, jvmConfig)
      logger.info(s"Launching Executor ...appId: $appId, executorSystemId: $executorSystemId, " +
        s"slots: ${resource.slots} on worker $worker")

      worker.getRef ! launch
      context.become(waitForActorSystemToStart(sender, launch, worker, executorSystemId))
  }

  def waitForActorSystemToStart(
                                 replyTo: ActorRef, launch: LaunchExecutor, worker: WorkerInfo, executorSystemId: Int)
  : Receive = {
    case RegisterActorSystem(systemPath) =>
      import launch._
      timeout.cancel()
      logger.info(s"Received RegisterActorSystem $systemPath for session ${session.requestor}")
      sender ! ActorSystemRegistered(worker.getRef)
      val system =
        ExecutorSystem(executorId, AddressFromURIString(systemPath), sender, resource, worker)
      replyTo ! LaunchExecutorSystemSuccess(system, session)
      context.stop(self)
    case ExecutorLaunchRejected(reason, ex) =>
      logger.error(s"Executor Launch ${launch.resource} failed reason: $reason", ex)
      replyTo ! LaunchExecutorSystemRejected(launch.resource, reason, session)
      context.stop(self)
    case timeout: LaunchExecutorSystemTimeout =>
      logger.error(s"The Executor ActorSystem $executorSystemId has not been started in time")
      replyTo ! timeout
      context.stop(self)
  }
}

object ExecutorSystemLauncher {

  case class LaunchExecutorSystem(worker: WorkerInfo, systemId: Int, resource: Resource)

  case class LaunchExecutorSystemSuccess(system: ExecutorSystem, session: Session)

  case class LaunchExecutorSystemRejected(resource: Resource, reason: Any, session: Session)

  case class LaunchExecutorSystemTimeout(session: Session)

  private def getExecutorJvmConfig(conf: ExecutorSystemJvmConfig, systemName: String,
                                   reportBack: String): ExecutorJVMConfig = {
    Option(conf).map { conf =>
      import conf._
      new ExecutorJVMConfig(classPath, jvmArguments, classOf[ActorSystemBooter].getName,
        Array(systemName, reportBack), jar, username, executorAkkaConfig)
    }.orNull
  }
}
