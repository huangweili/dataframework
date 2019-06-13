package com.hwlcn.dataframework

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Stash, Terminated}
import akka.remote.DisassociatedEvent
import com.hwlcn.dataframework.InMemoryKVService.{GetKV, GetKVFailed, GetKVSuccess, PutKV}
import com.hwlcn.dataframework.message.MasterMessage.{MasterInfo, MasterListUpdated, WorkerTerminated}
import com.hwlcn.dataframework.message.MasterToWorker.WorkerRegistered
import com.hwlcn.dataframework.message.WorkerToMaster.{RegisterNewWorker, RegisterWorker, ResourceUpdate}
import com.hwlcn.dataframework.worker.WorkerMetaData
import org.slf4j.LoggerFactory

import scala.collection.immutable

/**
  * 集群的master节点
  * 1. 负责调度
  * 2. 负责work node的看管
  *
  * @author huangweili
  */
abstract class MasterActor(schedulerClass: Class[_]) extends Actor with Stash {

  private val logger = LoggerFactory.getLogger(getClass)

  //初始化缓存功能
  private val kvService = context.actorOf(Props(new InMemoryKVService()), "kvService")

  //调度器
  private var scheduler: ActorRef = null

  //记录workers对象
  private var workers = new immutable.HashMap[ActorRef, (WorkerId, WorkerMetaData)]

  //下一个worker的id信息
  private var nextWorkerId = 0

  // 记录master启动的时间
  private val birth = System.currentTimeMillis()

  //获取本master的信息
  private val hostPort = HostPort(ActorUtil.getSystemAddress(context.system).hostPort)


  //初始haul masters对象
  private var masters: List[MasterNode] = {
    List(MasterNode(hostPort.host, hostPort.port))
  }

  //获取内存中的kv值
  kvService ! GetKV(Constants.MASTER_GROUP, Constants.WORKER_ID)

  //等待worker的注册信息
  context.become(waitForNextWorkerId)


  override def preStart(): Unit = {
    /*
    appManager = context.actorOf(Props(new AppManager(kvService, AppMasterLauncher)),
      classOf[AppManager].getSimpleName)
    */

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
  def appMasterHandler(): Receive


  /**
    * 处理web相关资源的访问接口
    *
    * @return
    */
  def clientHandler(): Receive

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
    var handler = workerMsgHandler orElse onMasterListChange orElse terminationWatch orElse disassociated
    //判断有没有appMasterHandler的处理对象
    if (appMasterHandler != null) {
      handler = appMasterHandler() orElse handler
    }
    if (clientHandler != null) {
      handler = clientHandler() orElse handler;
    }

    handler orElse defaultMsgHandler(self)
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
        scheduler ! WorkerTerminated(workers(actor)._1) //根据workID 删除worker
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
      workers += (sender() -> (id, workerMetaData)) //获取worker的信息
      val workerHostname = ActorUtil.getHostname(sender())
      logger.info(s"${workerHostname}的注册ID为${id}....")
    }

    case resourceUpdate: ResourceUpdate => {
      scheduler forward resourceUpdate
    }

  }
}
