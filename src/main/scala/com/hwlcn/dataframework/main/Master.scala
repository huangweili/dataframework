package com.hwlcn.dataframework.main


import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.Cluster
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.hwlcn.dataframework.master.MasterWatcher
import com.hwlcn.dataframework.util.NetUtil
import com.hwlcn.dataframework.{ClusterConfig, Constants}
import com.typesafe.config.ConfigValueFactory
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * master 启动的入口函数
  *
  * @author huangweili
  */
object Master {

  val logger = LoggerFactory.getLogger(getClass)

  // master配置信息
  val masterConfig = ClusterConfig.master()

  // 集群配置信息
  val clusterConfig = ClusterConfig.default()


  def start(): Unit = {
    try {

      //获取master的主类
      val masterClass = Class.forName(masterConfig.getString("master.master-class"))

      //获取调度类
      val schedulerClass = Class.forName(masterConfig.getString("master.scheduler-class"))


      //获取seed的列表
      val masters = masterConfig.getStringList("master.masters").asScala
      val masterList = masters.map(master => s"akka.tcp://${Constants.MASTER}@$master").toList.asJava
      val quorum = masterList.size() / 2 + 1
      val ip = NetUtil.getInet4Address.getHostAddress
      val port = masterConfig.getInt("master.port")

      val config = clusterConfig.getConfig("default.master").
        withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port)).
        withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(ip)).
        withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromAnyRef(masterList)).
        withValue(s"akka.cluster.role.${Constants.MASTER}.min-nr-of-members", ConfigValueFactory.fromAnyRef(quorum))

      logger.info(s"启动系统:$ip:$port, 种子列表: ${masters.mkString(";")}")

      //加载akka的system系统
      val system = ActorSystem(Constants.MASTER, config)

      // 启动单点管理器
      val _ = system.actorOf(ClusterSingletonManager.props(
        singletonProps = Props(classOf[MasterWatcher], Constants.MASTER, masterClass, schedulerClass),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system).withSingletonName(Constants.MASTER_WATCHER)
          .withRole(Constants.MASTER)),
        name = Constants.SINGLETON_MANAGER)


      //在集群内部实现proxy的 转换，使得远程的remote端能够访问内部的单点进程
      val masterProxy = system.actorOf(ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/${Constants.SINGLETON_MANAGER}",
        settings = ClusterSingletonProxySettings(system)
          .withSingletonName(s"${Constants.MASTER_WATCHER}/${Constants.MASTER}").withRole(Constants.MASTER)),
        name = Constants.MASTER
      )


      val mainThread = Thread.currentThread()

      Runtime.getRuntime().addShutdownHook(new Thread() {
        override def run(): Unit = {
          if (!system.whenTerminated.isCompleted) {
            logger.info("akka system 正在关闭....")
            system.stop(masterProxy)
            val cluster = Cluster(system)
            cluster.leave(cluster.selfAddress)
            cluster.down(cluster.selfAddress)
            try {
              //等待10秒
              Await.result(system.whenTerminated, Duration(10, TimeUnit.SECONDS))
            } catch {
              case _: Exception => // Ignore
            }
            system.terminate()
            //等待主线程完成工作
            mainThread.join()
          }
        }
      })
      //阻塞主进程
      Await.result(system.whenTerminated, Duration.Inf)
    } catch {
      case ex: Exception => {
        logger.error("启动master的时候发生了异常。", ex);
      }
    }

  }

  def main(args: Array[String]): Unit = {
    start()
  }
}
