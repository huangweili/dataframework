package com.hwlcn.dataframework.discovery

import akka.actor.{Actor, ActorRef}
import com.hwlcn.dataframework.message.DiscoverMessage.{DiscoveryRequest, DiscoveryWorkers}
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
    case r: DiscoveryRequest => {
      val client = sender()
      val workers = registedServices.getOrElse(s"${r.workerType}_${r.groupId}", Nil)
      client ! DiscoveryWorkers(workers)
    }

    case msg => {
      logger.info(s"接收来自${sender()}的未处理消息${msg}")
    }
  }

}

