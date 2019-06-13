package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.scheduler.Resource
import com.hwlcn.dataframework.worker.{WorkerId, WorkerMetaData}

/**
  * work发送信息给master
  *
  * @author huangweili
  */
object WorkerToMaster {

  /**
    * 向master发起worker对象注册信息并向worker注册信息
    */
  case class RegisterNewWorker(workerMetaData: WorkerMetaData)

  /**
    * 当worker失去和master之间的连接后，重新注册到master对象上
    *
    * @param workerId
    */
  case class RegisterWorker(workerId: WorkerId, workerMetaData: WorkerMetaData)

  /**
    * 更新worker信息到 master对象上
    *
    * @param worker
    * @param workerId
    * @param resource
    */
  case class ResourceUpdate(worker: ActorRef, workerId: WorkerId, resource: Resource)

}