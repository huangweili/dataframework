package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.worker.WorkerInfo

/**
  * 定义appmaster到appManager的事件定义
  *
  * @author huangweili
  */
object AppMasterToAppManager {

  case class RegisterAppMaster(appId: Int, appMaster: ActorRef, workerInfo: WorkerInfo)


}
