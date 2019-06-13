package com.hwlcn.dataframework.scheduler

import akka.actor.ActorRef
import com.hwlcn.dataframework.message.AppMasterToMaster.RequestResource
import com.hwlcn.dataframework.message.MasterToAppMaster.ResourceAllocated
import com.hwlcn.dataframework.scheduler.Scheduler.PendingRequest
import com.hwlcn.dataframework.worker.WorkerId

import scala.collection.mutable

/**
  * 按照优先级实现资源调度
  *
  * @author huangweili
  */
class PriorityScheduler extends SchedulerActor {
  private var resourceRequests = new mutable.PriorityQueue[PendingRequest]()(requestOrdering)

  def requestOrdering: Ordering[PendingRequest] = new Ordering[PendingRequest] {
    override def compare(x: PendingRequest, y: PendingRequest): Int = {
      var res = x.request.priority.id - y.request.priority.id
      if (res == 0) {
        res = y.timeStamp.compareTo(x.timeStamp)
      }
      res
    }
  }

  override def receive: Receive = super.handleScheduleMessage orElse resourceRequestHandler

  override def allocateResource(): Unit = {
    var scheduleLater = Array.empty[PendingRequest]
    val resourcesSnapShot = resources.clone()
    var allocated = Resource.empty
    val totalResource = Resource(resourcesSnapShot.values.map(_._2.slots).sum)

    // 当资源不足的时候的处理办法
    while (resourceRequests.nonEmpty && (allocated < totalResource)) {

      val PendingRequest(appId, appMaster, request, timeStamp) = resourceRequests.dequeue()
      request.relaxation match {
        case Relaxation.ANY =>
          val allocations = allocateFairly(resourcesSnapShot, request)
          val newAllocated = Resource(allocations.map(_.resource.slots).sum)
          if (allocations.nonEmpty) {
            appMaster ! ResourceAllocated(allocations.toArray)
          }
          if (newAllocated < request.resource) {
            val remainingRequest = request.resource - newAllocated
            val remainingExecutors = request.executorNum - allocations.length
            val newResourceRequest = request.copy(resource = remainingRequest,
              executorNum = remainingExecutors)
            scheduleLater = scheduleLater :+
              PendingRequest(appId, appMaster, newResourceRequest, timeStamp)
          }
          allocated = allocated + newAllocated
        case Relaxation.ONEWORKER =>
          val availableResource = resourcesSnapShot.find { params =>
            val (_, (_, resource)) = params
            resource > request.resource
          }
          if (availableResource.nonEmpty) {
            val (workerId, (worker, resource)) = availableResource.get
            allocated = allocated + request.resource
            appMaster ! ResourceAllocated(Array(ResourceAllocation(request.resource, worker,
              workerId)))
            resourcesSnapShot.update(workerId, (worker, resource - request.resource))
          } else {
            scheduleLater = scheduleLater :+ PendingRequest(appId, appMaster, request, timeStamp)
          }
        case Relaxation.SPECIFICWORKER =>
          val workerAndResource = resourcesSnapShot.get(request.workerId)
          if (workerAndResource.nonEmpty && workerAndResource.get._2 > request.resource) {
            val (worker, availableResource) = workerAndResource.get
            appMaster ! ResourceAllocated(Array(ResourceAllocation(request.resource, worker,
              request.workerId)))
            allocated = allocated + request.resource
            resourcesSnapShot.update(request.workerId, (worker,
              availableResource - request.resource))
          } else {
            scheduleLater = scheduleLater :+ PendingRequest(appId, appMaster, request, timeStamp)
          }
      }
    }
    for (request <- scheduleLater)
      resourceRequests.enqueue(request)
  }

  def resourceRequestHandler: Receive = {
    case RequestResource(appId, request) =>
      logger.info(s"Request resource: appId: $appId, slots: ${request.resource.slots}, " +
        s"relaxation: ${request.relaxation}, executor number: ${request.executorNum}")
      val appMaster = sender()
      resourceRequests.enqueue(new PendingRequest(appId, appMaster, request,
        System.currentTimeMillis()))
      allocateResource()
  }

  override def doneApplication(appId: Int): Unit = {
    resourceRequests = resourceRequests.filter(_.appId != appId)
  }

  private def allocateFairly(
                              resources: mutable.HashMap[WorkerId, (ActorRef, Resource)], request: ResourceRequest)
  : List[ResourceAllocation] = {
    val workerNum = resources.size
    var allocations = List.empty[ResourceAllocation]
    var totalAvailable = Resource(resources.values.map(_._2.slots).sum)
    var remainingRequest = request.resource
    var remainingExecutors = Math.min(request.executorNum, request.resource.slots)

    while (remainingExecutors > 0 && !totalAvailable.isEmpty) {
      val exeutorNum = Math.min(workerNum, remainingExecutors)
      val toRequest = Resource(remainingRequest.slots * exeutorNum / remainingExecutors)

      val sortedResources = resources.toArray.sortBy(_._2._2.slots)(Ordering[Int].reverse)
      val pickedResources = sortedResources.take(exeutorNum)

      val flattenResource = pickedResources.zipWithIndex.flatMap { workerWithIndex =>
        val ((workerId, (worker, resource)), index) = workerWithIndex
        0.until(resource.slots).map(seq => ((workerId, worker), seq * workerNum + index))
      }.sortBy(_._2).map(_._1)

      if (flattenResource.length < toRequest.slots) {
        totalAvailable = Resource.empty
      } else {
        flattenResource.take(toRequest.slots).groupBy(actor => actor).mapValues(_.length).
          toArray.foreach { params =>
          val ((workerId, worker), slots) = params
          resources.update(workerId, (worker, resources.get(workerId).get._2 - Resource(slots)))
          allocations :+= ResourceAllocation(Resource(slots), worker, workerId)
        }
        totalAvailable -= toRequest
        remainingRequest -= toRequest
        remainingExecutors -= exeutorNum
      }
    }
    allocations
  }
}
