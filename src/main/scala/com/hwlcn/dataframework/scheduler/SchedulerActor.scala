package com.hwlcn.dataframework.scheduler

import akka.actor.{Actor, ActorRef}
import com.hwlcn.dataframework.message.MasterMessage.WorkerTerminated
import com.hwlcn.dataframework.message.MasterToWorker.{UpdateResourceFailed, UpdateResourceSucceed, WorkerRegistered}
import com.hwlcn.dataframework.message.WorkerToMaster.ResourceUpdate
import com.hwlcn.dataframework.scheduler.Scheduler.ApplicationFinished
import com.hwlcn.dataframework.worker.WorkerId
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  *
  * 定义资源调度类的基础功能
  *
  * @author huangweili
  */
abstract class SchedulerActor extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  //保存资源信息
  protected var resources = new mutable.HashMap[WorkerId, (ActorRef, Resource)]

  def handleScheduleMessage: Receive = {
    case WorkerRegistered(id, _) =>
      if (!resources.contains(id)) {
        logger.info(s"添加Worker【${id}】到资源调度中")
        resources.put(id, (sender, Resource.empty))
      }
    case update@ResourceUpdate(worker, workerId, resource) =>

      logger.info(s"更新资源:${update}...")

      if (resources.contains(workerId)) {
        val resourceReturned = resource > resources.get(workerId).get._2
        resources.update(workerId, (worker, resource))
        if (resourceReturned) {
          allocateResource()
        }
        sender ! UpdateResourceSucceed
      }
      else {
        sender ! UpdateResourceFailed(
          s"更新资源失败! worker【$workerId】未注册到master中。")
      }
    case WorkerTerminated(workerId) =>
      if (resources.contains(workerId)) {
        resources -= workerId
      }
    case ApplicationFinished(appId) =>
      doneApplication(appId)
  }

  def allocateResource(): Unit

  def doneApplication(appId: Int): Unit

}

object Scheduler {

  case class PendingRequest(appId: Int, appMaster: ActorRef, request: ResourceRequest, timeStamp: Long)

  case class ApplicationFinished(appId: Int)

}