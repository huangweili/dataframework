package com.hwlcn.dataframework.default

import akka.actor.ActorRef
import com.hwlcn.dataframework.WorkerActor
import com.hwlcn.dataframework.worker.WorkerMetaData

class DefaultWorker(proxy: ActorRef) extends WorkerActor(proxy) {


  /**
    * 处理子任务节点异常结束的时候的情况
    *
    * @param parentActor
    * @param child
    */
  override def childTermination(parentActor: ActorRef, child: ActorRef): Unit = {

  }

  /**
    * 对应的appMsg到 worker的信息，这里主要用来处理不同app和work之间的通讯
    *
    * @return
    */
  override def appMsgHandler(): Receive = null

  /**
    * 定义worker的基础信息master和client根据worker的基础信息来获取worker的信息
    *
    * @return
    */
  override def workerMetaInfo(): WorkerMetaData = {
    new WorkerMetaData();
  }
}
