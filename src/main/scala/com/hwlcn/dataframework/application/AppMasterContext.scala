package com.hwlcn.dataframework.application

import akka.actor.ActorRef
import com.hwlcn.dataframework.scheduler.Resource
import com.hwlcn.dataframework.worker.WorkerInfo

/**
  * 定义 AppMasterContext的信息
  *
  * @param appId
  * @param username
  * @param resource
  * @param workerInfo
  * @param appJar
  * @param masterProxy
  * @author huangweili
  */
case class AppMasterContext(
                             appId: Int,
                             username: String,
                             resource: Resource,
                             workerInfo: WorkerInfo,
                             appJar: Option[AppJar],
                             masterProxy: ActorRef)

