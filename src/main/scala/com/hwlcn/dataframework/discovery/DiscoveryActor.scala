package com.hwlcn.dataframework.discovery

import akka.actor.{Actor, ActorRef}
import org.slf4j.LoggerFactory

/**
  * 该类用于处理服务发现和服务路由的功能
  * 当一个worker以service类型注册到master的时候
  * 这个worker交给discovery服务，为后续的其他worker提供服务
  *
  * @author huangweili
  */
class DiscoveryActor(master: ActorRef) extends Actor {

  private val logger = LoggerFactory.getLogger(getClass);

  //注册过的服务列表
  private var registedServices = Map.empty[String, List[ActorRef]]

  override def receive: Receive = {
    workerMsgHandler orElse notHandlerMsg
  }

  /**
    * worker注册，注销相关的信息操作
    *
    * @return
    */
  def workerMsgHandler: Receive = {
    case _ => {

    }
  }


  /**
    * 处理client端对服务发现的请求
    *
    * @return
    */
  def clientMsgHandler: Receive = {
    case _ => {

    }
  }


  def notHandlerMsg: Receive = {
    case msg => {
      logger.info(s"接收到未处理的消息${msg}")
    }
  }

}

/**
  * 服务发现相关的信息
  */
object DiscoveryMessage {


}
