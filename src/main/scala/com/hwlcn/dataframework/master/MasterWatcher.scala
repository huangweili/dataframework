package com.hwlcn.dataframework.master

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, Member, MemberStatus}
import com.hwlcn.dataframework.Constants
import com.hwlcn.dataframework.application.AppManagerActor
import com.hwlcn.dataframework.message.MasterMessage.MasterListUpdated
import com.hwlcn.dataframework.message.MasterWatcherMessage
import com.hwlcn.dataframework.scheduler.SchedulerActor
import com.typesafe.config.{Config, ConfigList}
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration


class MasterWatcher(
                     role: String,
                     masterClass: Class[_ >: MasterActor],
                     schedulerClass: Class[_ >: SchedulerActor],
                     appManagerClass: Class[_ >: AppManagerActor]
                   ) extends Actor {

  import context.dispatcher

  private val logger = LoggerFactory.getLogger(classOf[MasterWatcher])


  protected val system: ActorSystem = context.system

  protected val cluster: Cluster = Cluster(system); //获取集群信息

  protected val config: Config = system.settings.config

  //获取种子节点，当种子节点的个数没有达到 masters.size()/2+1的时候系统不启动
  protected val masters: ConfigList = config.getList("akka.cluster.seed-nodes")


  protected val quorum: Int = masters.size() / 2 + 1

  // 根据集群节点的加入时间排序
  private val ageOrdering = Ordering.fromLessThan[Member] { (a, b) => a.isOlderThan(b) }
  private var membersByAge: immutable.SortedSet[Member] = immutable.SortedSet.empty(ageOrdering)


  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent])

    context.become(waitForInit) // 等待系统的初始化
  }


  override def postStop(): Unit = {
    cluster.unsubscribe(self); //去除在集群服务上的监控信息
  }

  //判断对象的角色
  def matchingRole(member: Member): Boolean = member.hasRole(role)


  override def receive: Receive = null


  def waitForInit: Receive = {
    case state: CurrentClusterState => {
      membersByAge = immutable.SortedSet.empty(ageOrdering) ++ state.members.filter(m =>
        m.status == MemberStatus.Up && matchingRole(m))

      if (membersByAge.size < quorum) {
        membersByAge.iterator.mkString(",")
        logger.info(s"没有获取到足够的种子节点数: $quorum, " +
          s"系统正在关闭...${membersByAge.iterator.mkString(",")}")

        context.become(waitForShutdown)

        self ! MasterWatcherMessage.Shutdown
      } else {
        //创建master正式启动系统
        val master = context.actorOf(Props(masterClass, schedulerClass, appManagerClass), Constants.MASTER)

        //变动master的成员信息，每个master node节点的成员信息 实际上是有watcher来维护的
        notifyMasterMembersChange(master)
        //等待集群事件
        context.become(waitForClusterEvent(master))
      }
    }
  }


  def waitForClusterEvent(master: ActorRef): Receive = {
    case MemberUp(m) if matchingRole(m) => {
      membersByAge += m
      notifyMasterMembersChange(master)
    }
    case mEvent: MemberEvent if (mEvent.isInstanceOf[MemberExited] ||
      mEvent.isInstanceOf[MemberRemoved]) && matchingRole(mEvent.member) => {
      logger.info(s"成员 ${mEvent.member} 移除")
      val m = mEvent.member
      membersByAge -= m
      if (membersByAge.size < quorum) {
        logger.info(s"当前成员数为$quorum,由于小于最小成员数" +
          s"系统将关闭以下的成员...${membersByAge.iterator.mkString(",")}")
        context.become(waitForShutdown)
        self ! MasterWatcherMessage.Shutdown
      } else {
        notifyMasterMembersChange(master)
      }
    }
  }

  //通知master列表变更信息
  private def notifyMasterMembersChange(master: ActorRef): Unit = {
    val masters = membersByAge.toList.map { member =>
      new MasterNode(member.address.host.getOrElse("Unknown-Host"),
        member.address.port.getOrElse(0))
    }
    master ! MasterListUpdated(masters)
  }

  /**
    * 系统关闭处理函数
    *
    * @return
    */
  def waitForShutdown: Receive = {
    case MasterWatcherMessage.Shutdown => {
      cluster.unsubscribe(self)
      cluster.leave(cluster.selfAddress)
      context.stop(self)
      system.scheduler.scheduleOnce(Duration.Zero) {
        try {
          Await.result(system.whenTerminated, Duration(3, TimeUnit.SECONDS))
        } catch {
          case _: Exception => // Ignore
        }
        system.terminate()
      }
    }
  }
}
