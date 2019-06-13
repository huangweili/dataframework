package com.hwlcn.dataframework.message

import com.hwlcn.dataframework.message.MasterMessage.MasterInfo
import com.hwlcn.dataframework.worker.WorkerId

/**
  * master 到 worker的通信
  *
  * @author huangweili
  */
object MasterToWorker {

  /**
    * 返回master和master的信息
    *
    * @param workerId
    * @param masterInfo
    */
  case class WorkerRegistered(workerId: WorkerId, masterInfo: MasterInfo)

  /**
    * 资源更新失败的事件
    *
    * @param reason
    * @param ex
    */
  case class UpdateResourceFailed(reason: String = null, ex: Throwable = null)

  /**
    * 资源更新成功的信息
    */
  case object UpdateResourceSucceed

}


