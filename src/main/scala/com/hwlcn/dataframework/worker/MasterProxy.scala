package com.hwlcn.dataframework.worker

import akka.actor.{Actor, ActorIdentity, ActorPath, ActorRef, Cancellable, Identify, PoisonPill, Props, Scheduler, Stash, Terminated}
import com.hwlcn.dataframework.message.MasterMessage.{MasterRestarted, MasterStopped}
import com.hwlcn.dataframework.worker.MasterProxy.WatchMaster
import com.hwlcn.dataframework.{ActorUtil, HostPort}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

/**
  * work端访问 master的代理类
  * 1. 负责 master proxy的连接管理
  * 2. 维护 master 的连接信息
  * 3. 作为 具体的actor类的消息转发对象
  *
  * @author huangweili
  */
class MasterProxy(masters: Iterable[ActorPath], timeout: FiniteDuration) extends Actor with Stash {
  private val logger = LoggerFactory.getLogger(getClass)

  //维护与master的监听关系
  private var watchers: List[ActorRef] = List.empty[ActorRef]

  def findMaster(): Cancellable = {
    repeatActionUtil(timeout) {
      //contacts 通过seed节点循环访问节点
      contacts foreach { contact =>
        logger.info(s"向 $contact 发起认证请求")
        contact ! Identify(None)
      }
    }
  }

  private val contacts = masters.map { url =>
    logger.info(s"包含的master接入点URL : $url")
    context.actorSelection(url)
  }


  override def receive: Receive = {
    case _ => {
      logger.info("MasterProxy 接收到未知类型的消息")
    }
  }

  override def postStop(): Unit = {
    watchers.foreach(_ ! MasterStopped)
    super.postStop()
  }

  override def preStart(): Unit = {
    context.become(establishing(findMaster()))
  }

  def establishing(findMaster: Cancellable): Receive = {
    case ActorIdentity(_, Some(receptionist)) =>
      // 获取远端的响应
      findMaster.cancel() // 优先处理定时任务的处理
      context watch receptionist
      logger.info(s"对 [${receptionist.path}] 进行连接")

      watchers.foreach(_ ! MasterRestarted)
      //恢复未处理的信息
      unstashAll()
      //去除超时控制
      context.become(active orElse messageHandler(receptionist))
    case ActorIdentity(_, None) => {
      logger.info("接收到不正常的认证信息")
    }
    case msg =>
      //把未处理的消息存储起来，直到master proxy的连接恢复
      logger.info(s"存储消息 ${msg.getClass.getSimpleName}")
      stash()
  }


  def active: Receive = {
    case Terminated(master) =>
      logger.warn(s"失去了与 master [$master]的 连接 将进行重连")
      context.become(establishing(findMaster()))
    case _: ActorIdentity =>

    case WatchMaster(watcher) =>
      // 添加master对象当master对象发生变化的时候提供
      watchers = watchers :+ watcher
  }

  def messageHandler(master: ActorRef): Receive = {
    case msg =>
      logger.debug(s"proxy 收到消息: ${msg.getClass.getSimpleName}, 转发到 ${master.path}")
      master forward msg
  }


  //获取默认的调度服务
  def scheduler: Scheduler = context.system.scheduler

  import context.dispatcher

  import scala.concurrent.duration._

  private def repeatActionUtil(timeout: FiniteDuration)(action: => Unit): Cancellable = {

    val send = scheduler.schedule(0.seconds, 2.seconds)(action)

    //访问超时了的时候，关闭当前的对象。主要是正对master连接失效的情况

    val suicide = scheduler.scheduleOnce(timeout) {
      send.cancel()
      self ! PoisonPill
    }

    new Cancellable {
      def cancel(): Boolean = {
        val result1 = send.cancel()
        val result2 = suicide.cancel()
        result1 && result2
      }

      def isCancelled: Boolean = {
        send.isCancelled && suicide.isCancelled
      }
    }
  }
}


object MasterProxy {

  import scala.concurrent.duration._

  //添加可用的master对象
  case class WatchMaster(watcher: ActorRef)


  def props(masters: Iterable[HostPort], duration: FiniteDuration = 30.seconds): Props = {
    val contacts = masters.map(ActorUtil.getMasterActorPath)
    Props(new MasterProxy(contacts, duration))
  }
}