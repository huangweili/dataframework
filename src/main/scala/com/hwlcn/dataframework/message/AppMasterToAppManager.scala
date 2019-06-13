package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.application.ApplicationStatus
import com.hwlcn.dataframework.worker.WorkerInfo

/**
  * 定义appmaster到appManager的事件定义
  *
  * @author huangweili
  */
object AppMasterToAppManager {

  case class RegisterAppMaster(appId: Int, appMaster: ActorRef, workerInfo: WorkerInfo)

  //app状态改变事件
  case class ApplicationStatusChanged(appId: Int, newStatus: ApplicationStatus,
                                      timeStamp: Long)

}
