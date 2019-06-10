package com.hwlcn.dataframework


import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Cancellable, PoisonPill, Terminated}
import com.hwlcn.dataframework.message.MasterMessage.MasterInfo
import com.hwlcn.dataframework.message.MasterToWorker.{UpdateResourceFailed, UpdateResourceSucceed, WorkerRegistered}
import com.hwlcn.dataframework.message.WorkerToMaster.{RegisterNewWorker, RegisterWorker, ResourceUpdate}
import com.hwlcn.dataframework.scheduler.Resource
import com.hwlcn.dataframework.worker.WorkerMetaData
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


/**
  * work的实际操作对象
  */
abstract class WorkerActor(masterProxy: ActorRef) extends Actor with TimeOutScheduler {
  private val logger = LoggerFactory.getLogger(getClass)

  //worker创建时间
  private val createdTime = System.currentTimeMillis()

  //worker的地址信息
  private val address = ActorUtil.getFullPath(context.system, self.path)

  private var id: WorkerId = WorkerId.unspecified

  private var masterInfo: MasterInfo = MasterInfo.empty

  private var resource = Resource.empty
  private val resourceUpdateTimeoutMs = 30000L // Milliseconds

  // 引入默认的线程处理对象
  import context.dispatcher

  //消息处理
  override def receive: Receive = null

  //进行worker的注册活动
  override def preStart(): Unit = {
    logger.info(s"开始注册worker端到master")
    //totalSlots = systemConfig.getInt(GEARPUMP_WORKER_SLOTS)
    //this.resource = Resource(totalSlots)
    //通过代理发送注册信息到master上
    masterProxy ! RegisterNewWorker(workerMetaInfo())
    // 等待注册信息返回
    context.become(waitForMasterConfirm(registerTimeoutTicker(seconds = 30)))
  }

  private def registerTimeoutTicker(seconds: Int): Cancellable = {
    repeatActionUtil(seconds, () => Unit, () => {
      logger.error(s"在${seconds}秒内注册失败，worker进程将关闭 ")
      self ! PoisonPill
    })
  }

  private def repeatActionUtil(seconds: Int, action: () => Unit, onTimeout: () => Unit): Cancellable = {
    val cancelTimeout = context.system.scheduler.schedule(Duration.Zero,
      Duration(2, TimeUnit.SECONDS))(action())
    val cancelSuicide = context.system.scheduler.scheduleOnce(seconds.seconds)(onTimeout())

    new Cancellable {
      def cancel(): Boolean = {
        val result1 = cancelTimeout.cancel()
        val result2 = cancelSuicide.cancel()
        result1 && result2
      }

      def isCancelled: Boolean = {
        cancelTimeout.isCancelled && cancelSuicide.isCancelled
      }
    }
  }

  /**
    * 在进行向master进行worker注册后，等待的事件处理状态
    *
    * @param timeoutTicker
    * @return
    */
  def waitForMasterConfirm(timeoutTicker: Cancellable): Receive = {
    // 返回worker的注册信息
    case WorkerRegistered(id, masterInfo) =>
      //取消超时定时器
      timeoutTicker.cancel()
      this.id = id
      /*
      if (!metricsInitialized) {
        initializeMetrics()
        metricsInitialized = true
      }
      */
      this.masterInfo = masterInfo

      //监控master信息
      context.watch(masterInfo.master)

      logger.info(s"${ActorUtil.getFullPath(context.system, self.path)}已经成功注册,master信息${masterInfo}")

      //发送资源信息到master
      sendMsgWithTimeOutCallBack(masterInfo.master, ResourceUpdate(self, id, resource),
        resourceUpdateTimeoutMs, updateResourceTimeOut())
      //注册成功后worker转换为 service 状态正式提供服务
      context.become(service)
  }

  /**
    * 定义worker的基础信息master和client根据worker的基础信息来获取worker的信息
    *
    * @return
    */
  def workerMetaInfo(): WorkerMetaData;

  /**
    * 服务功能包装
    *
    * @return
    */
  def service: Receive = {
    if (appMsgHandler == null) {
      resourceMsgHandler() orElse terminationWatch(masterInfo.master) orElse defaultMsgHandler()
    } else {
      appMsgHandler() orElse resourceMsgHandler() orElse terminationWatch(masterInfo.master) orElse defaultMsgHandler()
    }
  }


  /**
    * 处理资源相关的事件信息
    *
    * @return
    */
  def resourceMsgHandler(): Receive = {
    case UpdateResourceFailed(reason, ex) =>
      logger.error(s"更新资源失败${reason},worker将推出", ex)
      context.stop(self)
    case UpdateResourceSucceed => {
      logger.info("更新资源成功。")
    }
  }


  def defaultMsgHandler(): Receive = {
    case msg: Any =>
      logger.error(s"收到来自${self}的未定义消息: $msg")
  }


  /**
    * 对watcher对象进行监控包括master的连接和child actor的状态
    *
    * @param master
    * @return
    */
  def terminationWatch(master: ActorRef): Receive = {
    case Terminated(actor) =>
      if (actor.compareTo(master) == 0) {
        logger.info(s"master的连接丢失，尝试重连master...")
        context.become(waitForMasterConfirm(retryRegisterWorker(id, timeOutSeconds = 30)))
      }
      else if (ActorUtil.isChildActorPath(self, actor)) {
        childTermination(self, actor)
      }
  }

  /**
    * 处理子任务节点异常结束的时候的情况
    *
    * @param parentActor
    * @param child
    */
  def childTermination(parentActor: ActorRef, child: ActorRef): Unit


  /**
    * 对应的appMsg到 worker的信息，这里主要用来处理不同app和work之间的通讯
    *
    * @return
    */
  def appMsgHandler(): Receive


  /**
    * 重试注册worker到master中
    *
    * @param workerId
    * @param timeOutSeconds
    * @return
    */
  private def retryRegisterWorker(workerId: WorkerId, timeOutSeconds: Int): Cancellable = {
    repeatActionUtil(
      seconds = timeOutSeconds,
      action = () => {
        masterProxy ! RegisterWorker(workerId, workerMetaInfo())
      },
      onTimeout = () => {
        logger.error(s"在${timeOutSeconds}秒内重新注册worker:[$workerId]失败，worker将被关闭...")
        self ! PoisonPill
      })
  }


  private def updateResourceTimeOut(): Unit = {
    logger.error(s"更新worker的资源信息超时了。")
  }


  override def postStop(): Unit = {
    logger.info(s"worker 将关闭....")
    context.system.terminate()
  }

}
