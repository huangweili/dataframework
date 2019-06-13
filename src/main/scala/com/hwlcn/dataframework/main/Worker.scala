package com.hwlcn.dataframework.main

import akka.actor.{ActorSystem, Props}
import com.hwlcn.dataframework.util.NetUtil
import com.hwlcn.dataframework.worker.MasterProxy
import com.hwlcn.dataframework.{ClusterConfig, Constants, HostPort}
import com.typesafe.config.ConfigValueFactory
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration


/**
  * worker的启动对象
  * @au
  */
object Worker {

  private val logger = LoggerFactory.getLogger(getClass)

  //获取worker的配置信息
  private val workerConfig = ClusterConfig.worker()

  private val clusterConfig = ClusterConfig.default()

  // 主函数
  def main(args: Array[String]): Unit = {

    val workerClass = Class.forName(workerConfig.getString("worker.worker-class"))

    val ip = NetUtil.getInet4Address.getHostAddress
    val port = workerConfig.getInt("worker.port")
    val config = clusterConfig.getConfig("default.worker").
      withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(port)).
      withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(ip));

    val systemName = workerConfig.getString("worker.system-name");
    //初始化对象
    val system = ActorSystem(systemName, config);

    //获取master的seed信息
    val masterAddress = workerConfig.getStringList("worker.masters").asScala.map { address =>
      val hostAndPort = address.split(":")
      HostPort(hostAndPort(0), hostAndPort(1).toInt)
    }

    logger.info(s"尝试和 " + masterAddress.mkString(",") + " 建立连接...")

    //建立 master proxy的信息
    val masterProxy = system.actorOf(MasterProxy.props(masterAddress), "masterproxy")

    //定义唯一的系统名称
    system.actorOf(Props(workerClass, masterProxy), Constants.WORKER)
    
    //阻塞主线程
    Await.result(system.whenTerminated, Duration.Inf)
  }

}
