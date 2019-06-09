package com.hwlcn.dataframework.scheduler

import akka.actor.ActorRef
import com.hwlcn.dataframework.WorkerId
import com.hwlcn.dataframework.scheduler.Priority.Priority
import com.hwlcn.dataframework.scheduler.Relaxation.Relaxation

/**
  * 定义资源对象，通过slot表示一个worker的资源的多少
  *
  * @author huangweili
  * @param slots
  */
case class Resource(slots: Int) {

  // scalastyle:off spaces.after.plus
  def +(other: Resource): Resource = Resource(slots + other.slots)

  // scalastyle:on spaces.after.plus

  def -(other: Resource): Resource = Resource(slots - other.slots)

  def >(other: Resource): Boolean = slots > other.slots

  def >=(other: Resource): Boolean = !(this < other)

  def <(other: Resource): Boolean = slots < other.slots

  def <=(other: Resource): Boolean = !(this > other)

  def isEmpty: Boolean = {
    slots == 0
  }
}

object Priority extends Enumeration {
  type Priority = Value
  val LOW, NORMAL, HIGH = Value
}

object Resource {
  def empty: Resource = new Resource(0)

  def min(res1: Resource, res2: Resource): Resource = if (res1.slots < res2.slots) res1 else res2
}


object Relaxation extends Enumeration {
  type Relaxation = Value
  val ANY, ONEWORKER, SPECIFICWORKER = Value
}


/**
  * 资源分配信息
  *
  * @param resource
  * @param worker
  * @param workerId
  */
case class ResourceAllocation(resource: Resource, worker: ActorRef, workerId: WorkerId)

/**
  * 资源请求事件
  *
  * @param resource
  * @param workerId
  * @param priority
  * @param relaxation
  * @param executorNum
  */
case class ResourceRequest(
                            resource: Resource, workerId: WorkerId, priority: Priority = Priority.NORMAL,
                            relaxation: Relaxation = Relaxation.ANY, executorNum: Int = 1)