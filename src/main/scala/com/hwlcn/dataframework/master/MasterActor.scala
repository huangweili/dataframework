package com.hwlcn.dataframework.master

import java.lang.management.ManagementFactory

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Stash, Terminated}
import akka.remote.DisassociatedEvent
import com.hwlcn.dataframework.InMemoryKVService.{GetKV, GetKVFailed, GetKVSuccess, PutKV}
import com.hwlcn.dataframework._
import com.hwlcn.dataframework.application.ApplicationLauncher
import com.hwlcn.dataframework.discovery.DiscoveryActor
import com.hwlcn.dataframework.message.AppManagerToAppMaster.ShutdownApplication
import com.hwlcn.dataframework.message.AppMasterToAppManager.{ApplicationStatusChanged, RegisterAppMaster}
import com.hwlcn.dataframework.message.AppMasterToMaster.{InvalidAppMaster, RequestResource}
import com.hwlcn.dataframework.message.ApplicationMessage.{GetAppData, SaveAppData}
import com.hwlcn.dataframework.message.ClientMessage._
import com.hwlcn.dataframework.message.DiscoverMessage.DiscoveryRequest
import com.hwlcn.dataframework.message.MasterMessage._
import com.hwlcn.dataframework.message.MasterToWorker.WorkerRegistered
import com.hwlcn.dataframework.message.WorkerToMaster.{RegisterNewWorker, RegisterWorker, ResourceUpdate}
import com.hwlcn.dataframework.scheduler.Scheduler.ApplicationFinished
import com.hwlcn.dataframework.worker.{WorkerData, WorkerId, WorkerList, WorkerMetaData}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.util.{Failure, Success}

/**
  * 集群的master节点
  * 1. 负责调度
  * 2. 负责work node的看管
  *
  * @author huangweili
  */
abstract class MasterActor(schedulerClass: Class[_], appManagerClass: Class[_]) extends Actor with Stash {

  protected val logger = LoggerFactory.getLogger(getClass)

  //初始化缓存功能
  private val kvService = context.actorOf(Props(new InMemoryKVService()), "kvService")

  //调度器
  private var scheduler: ActorRef = null

  //记录workers对象
  private var workers = new immutable.HashMap[ActorRef, WorkerData]

  //下一个worker的id信息
  private var nextWorkerId = 0

  // 记录master启动的时间
  private val birth = System.currentTimeMillis()

  //获取本master的信息
  private val hostPort = HostPort(ActorUtil.getSystemAddress(context.system).hostPort)
  private val systemConfig: Config = context.system.settings.config
  val jarStoreRootPath = systemConfig.getString(Constants.APP_JAR_STORE_ROOT_PATH)

  val discoveryRef = context.actorOf(Props(classOf[DiscoveryActor], self))
  /**
    * appManager的代理对象
    */
  private var appManager: ActorRef = null

  //初始haul masters对象
  private var masters: List[MasterNode] = {
    List(new MasterNode(hostPort.host, hostPort.port))
  }

  //获取内存中的kv值
  kvService ! GetKV(Constants.MASTER_GROUP, Constants.WORKER_ID)

  //等待worker的注册信息
  context.become(waitForNextWorkerId)


  override def preStart(): Unit = {
    //根据传入的appMananger的class管理结构
    appManager = context.actorOf(Props(appManagerClass, kvService, ApplicationLauncher), "appManager")
    //初始化资源调度器
    scheduler = context.actorOf(Props(schedulerClass))
    //监听事件丢失的情况
    context.system.eventStream.subscribe(self, classOf[DisassociatedEvent])
  }


  def disassociated: Receive = {
    case disassociated: DisassociatedEvent =>
      logger.info(s"${disassociated.remoteAddress}的连接中断")
  }

  def defaultMsgHandler(actor: ActorRef): Receive = {
    case msg: Any =>
      logger.error(s"检测到来自:$actor 的未处理信息:$msg")
  }


  /**
    * 与appMaster的事件处理
    *
    * @return
    */
  def appMasterHandler(): Receive = {
    case request: RequestResource =>
      scheduler forward request
    case registerAppMaster: RegisterAppMaster =>
      appManager forward registerAppMaster

    case save: SaveAppData =>
      appManager forward save

    case get: GetAppData =>
      appManager forward get
    //获取全部的work信息
    case GetAllWorkers =>
      sender ! new WorkerList(workers.values.toList.asJava)

    //获取master的信息
    case GetMasterData =>

      val aliveFor = System.currentTimeMillis() - birth

      val userDir = System.getProperty("user.dir")
      //master的描述信息 用来获取master的信息
      val masterDescription =
        new MasterSummary(
          new MasterNode(hostPort.host, hostPort.port),
          masters.asJava,
          aliveFor,
          null,
          jarStoreRootPath,
          MasterStatus.Synced,
          userDir,
          ManagementFactory.getRuntimeMXBean().getName(),
          List.empty[MasterActivity].asJava,
        )
      sender ! masterDescription

    case invalidAppMaster: InvalidAppMaster =>
      appManager forward invalidAppMaster
    case statusChanged: ApplicationStatusChanged =>
      appManager forward statusChanged

  }


  /**
    * 处理客户端请求的事件
    *
    * @return
    */
  def clientHandler(): Receive = {
    case app: SubmitApplication =>
      logger.debug(s"从客户端提交了应用:${app}")
      appManager.forward(app)

    case app: RestartApplication =>
      logger.debug(s"从客户端请求重启应用:${app.appId}")
      appManager.forward(app)


    case app: ShutdownApplication =>
      logger.debug(s"从客户端请求关闭程序:${app.appId}")
      scheduler ! ApplicationFinished(app.appId)
      appManager.forward(app)


    case app: ResolveAppId =>
      logger.debug(s"根据APPID:${app.appId}获取 APP的引用。")
      appManager.forward(app)

    //web端和appmaster端的请求
    case resolve: ResolveWorkerId =>
      logger.debug(s"根据worker id:${resolve.workerId}，获取worker的引用信息")
      val worker = workers.find(_._2.getWorkerId == resolve.workerId)
      worker match {
        case Some(w) => sender ! ResolveWorkerIdResult(Success(w._1))
        case None => sender ! ResolveWorkerIdResult(Failure(
          new Exception(s"未找到ID:${resolve.workerId} 的 worker对象")))
      }

    //查询所有对象的数据
    case AppMastersDataRequest =>
      appManager forward AppMastersDataRequest

    //查询指定对象的数据
    case appMasterDataRequest: AppMasterDataRequest =>
      logger.debug(s"获取指定的appmaster的信息${appMasterDataRequest}")
      appManager forward appMasterDataRequest
    case query: QueryAppMasterConfig =>
      logger.debug("获取")
      appManager forward query

    case QueryMasterConfig =>
      sender ! MasterConfig(systemConfig) //把systemConfig的信息返回
    case register: RegisterAppResultListener =>
      appManager forward register

  }

  /**
    * 路由相关的事件处理器
    *
    * @return
    */
  def discoveryHandler: Receive = {
    case request: DiscoveryRequest => {
      logger.info("转发worker资源寻找请求")
      discoveryRef forward request
    }

  }


  /**
    * 获取集群中的NextWorkerId
    *
    * @return
    */
  def waitForNextWorkerId: Receive = {
    case GetKVSuccess(_, result) =>
      if (result != null) {
        this.nextWorkerId = result.asInstanceOf[Int]
      } else {
        logger.warn("在集群中没有找到相应的资源信息")
      }
      context.become(receive)
      unstashAll()
    case GetKVFailed(ex) =>
      logger.error("数据分片出现问题..系统将关闭", ex)
      context.parent ! PoisonPill
    case msg =>
      logger.info(s"未处理信息 ${msg.getClass.getSimpleName}")
      stash() //先保存接收到的数据
  }

  /**
    * 消息处理类
    *
    * @return
    */
  def receive: Receive = {
    clientHandler orElse appMasterHandler orElse discoveryHandler orElse workerMsgHandler orElse onMasterListChange orElse terminationWatch orElse disassociated orElse defaultMsgHandler(sender())
  }

  /**
    * 监视worker对象消除时的信息
    *
    * @return
    */
  def terminationWatch: Receive = {
    case t: Terminated => {
      val actor = t.actor
      logger.info(s"worker${actor.path}连接丢失!${t.getAddressTerminated}")
      if (workers.keySet.contains(actor)) {
        //在资源调度中去除worker信息
        scheduler ! WorkerTerminated(workers(actor).getWorkerId) //根据workID 删除worker
        workers -= actor
      }
    }
  }

  //监听masters对象发生变化的信息
  def onMasterListChange: Receive = {
    case MasterListUpdated(masters: List[MasterNode]) =>
      this.masters = masters
  }


  /**
    * 定义worker向master发起资源注册的信息
    *
    * @return
    */
  def workerMsgHandler: Receive = {
    case RegisterNewWorker(workerMetaData: WorkerMetaData) => {
      val workerId = WorkerId(nextWorkerId, System.currentTimeMillis())
      nextWorkerId += 1
      kvService ! PutKV(Constants.MASTER_GROUP, Constants.WORKER_ID, nextWorkerId)
      val workerHostname = ActorUtil.getHostname(sender())
      logger.info(s"来自${workerHostname}的worker注册....")
      self forward RegisterWorker(workerId, workerMetaData)
    }

    case RegisterWorker(id, workerMetaData: WorkerMetaData) => {
      context.watch(sender())
      sender ! WorkerRegistered(id, MasterInfo(self, birth))
      scheduler forward WorkerRegistered(id, MasterInfo(self, birth))
      workers += (sender() -> new WorkerData(id, workerMetaData)) //获取worker的信息
      val workerHostname = ActorUtil.getHostname(sender())
      //TODO 把注册完成的 service的 work 添加到服务发现路由中
      logger.info(s"${workerHostname}的注册ID为${id}....")
    }

    case resourceUpdate: ResourceUpdate => {
      scheduler forward resourceUpdate
    }

  }
}
