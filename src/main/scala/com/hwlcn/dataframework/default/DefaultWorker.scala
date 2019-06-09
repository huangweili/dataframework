package com.hwlcn.dataframework.default

import akka.actor.ActorRef
import com.hwlcn.dataframework.WorkerActor

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
}
