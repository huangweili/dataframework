package com.hwlcn.dataframework.system

import akka.actor.{ActorSystem, Props}
import com.hwlcn.dataframework.{ClusterConfig, Constants}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * 系统启动器
  *
  * @author huangweili
  */
class ActorSystemBooter(config: Config) {

  /**
    * 定义启动的函数
    *
    * @param name
    * @param reportBackActor
    * @return
    */
  def boot(name: String, reportBackActor: String): ActorSystem = {

    val system = ActorSystem(name, config)
    system.actorOf(Props(classOf[Daemon], name, reportBackActor), "daemon")
    system
  }
}

object ActorSystemBooter {

  def apply(config: Config): ActorSystemBooter = new ActorSystemBooter(config)

  def main(args: Array[String]) {
    val logger = LoggerFactory.getLogger(getClass);
    val name = args(0)

    val reportBack = args(1)

    val config = ClusterConfig.default()
    val debugPort = Option(System.getProperty(Constants.REMOTE_DEBUG_PORT))
    debugPort.foreach { port =>
      logger.info("==========================================")
      logger.info("远程调试接口 port: " + port)
      logger.info("==========================================")
    }

    val system = apply(config).boot(name, reportBack)

    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run(): Unit = {
        logger.info("由于上级进程关闭，本进程将关闭。")
        system.terminate()
      }
    })

    Await.result(system.whenTerminated, Duration.Inf)
  }
}

