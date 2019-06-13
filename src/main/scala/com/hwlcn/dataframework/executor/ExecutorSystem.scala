package com.hwlcn.dataframework.executor

import akka.actor.{ActorRef, Address, PoisonPill}
import com.hwlcn.dataframework.message.AppMasterMessage.BindLifeCycle
import com.hwlcn.dataframework.scheduler.Resource
import com.hwlcn.dataframework.worker.WorkerInfo

/** *
  * Executor 是最小的执行单元，每个worker底下可能有多个Executor来执行具体的事情
  *
  * @author huangweili
  */
case class ExecutorSystem(executorSystemId: Int, address: Address, daemon:
ActorRef, resource: Resource, worker: WorkerInfo) {
  def bindLifeCycleWith(actor: ActorRef): Unit = {
    daemon ! BindLifeCycle(actor)
  }

  def shutdown(): Unit = {
    daemon ! PoisonPill
  }
}