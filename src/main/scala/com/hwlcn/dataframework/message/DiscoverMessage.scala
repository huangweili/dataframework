package com.hwlcn.dataframework.message

import akka.actor.ActorRef

object DiscoverMessage {

  /**
    * 请求获取相关的worker组
    *
    * @param groupId
    * @param workerType
    */
  case class DiscoveryRequest(groupId: String, workerType: String)

  /**
    * 返回找到的指定worker信息
    *
    * @param workes
    */
  case class DiscoveryWorkers(workes: List[ActorRef])

}
