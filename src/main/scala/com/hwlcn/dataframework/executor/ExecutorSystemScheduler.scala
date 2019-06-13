package com.hwlcn.dataframework.executor

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import com.hwlcn.dataframework.Constants
import com.hwlcn.dataframework.application.AppJar
import com.hwlcn.dataframework.executor.ExecutorSystemLauncher.{LaunchExecutorSystem, LaunchExecutorSystemRejected, LaunchExecutorSystemSuccess, LaunchExecutorSystemTimeout}
import com.hwlcn.dataframework.executor.ExecutorSystemScheduler._
import com.hwlcn.dataframework.message.AppMasterToMaster.RequestResource
import com.hwlcn.dataframework.message.MasterToAppMaster.ResourceAllocated
import com.hwlcn.dataframework.scheduler.{ResourceAllocation, ResourceRequest}
import com.hwlcn.dataframework.worker.WorkerInfo
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * worker端的Executor的调度系统
  *
  * @author huangweili
  */
class ExecutorSystemScheduler(appId: Int, masterProxy: ActorRef,
                              executorSystemLauncher: (Int, Session) => Props) extends Actor {

  private val logger = LoggerFactory.getLogger(getClass)
  implicit val timeout = Constants.FUTURE_TIMEOUT
  implicit val actorSystem = context.system
  var currentSystemId = 0

  var resourceAgents = Map.empty[Session, ActorRef]

  def receive: Receive = {
    clientCommands orElse resourceAllocationMessageHandler orElse executorSystemMessageHandler
  }

  def clientCommands: Receive = {
    case start: StartExecutorSystems =>
      logger.info(s"starting executor systems (ExecutorSystemConfig(${start.executorSystemConfig}), " +
        s"Resources(${start.resources.mkString(",")}))")
      val requestor = sender()
      val executorSystemConfig = start.executorSystemConfig
      val session = Session(requestor, executorSystemConfig)
      val agent = resourceAgents.getOrElse(session,
        context.actorOf(Props(new ResourceAgent(masterProxy, session))))
      resourceAgents = resourceAgents + (session -> agent)

      start.resources.foreach { resource =>
        agent ! RequestResource(appId, resource)
      }

    case StopExecutorSystem(executorSystem) =>
      executorSystem.shutdown
  }


  def resourceAllocationMessageHandler: Receive = {
    case ResourceAllocatedForSession(allocations, session) =>
      if (isSessionAlive(session)) {
        allocations.foreach { resourceAllocation =>
          val ResourceAllocation(resource, worker, workerId) = resourceAllocation

          val launcher = context.actorOf(executorSystemLauncher(appId, session))
          launcher ! LaunchExecutorSystem(new WorkerInfo(workerId, worker), currentSystemId, resource)
          currentSystemId = currentSystemId + 1
        }
      }
    case ResourceAllocationTimeOut(session) =>
      if (isSessionAlive(session)) {
        resourceAgents = resourceAgents - session
        logger.error(s"Resource allocation for ${session.requestor} timed out")
        session.requestor ! StartExecutorSystemTimeout
      }
  }

  /**
    * Executor执行器的消息通信
    *
    * @return
    */
  def executorSystemMessageHandler: Receive = {
    case LaunchExecutorSystemSuccess(system, session) =>
      if (isSessionAlive(session)) {
        logger.info("LaunchExecutorSystemSuccess, send back to " + session.requestor)
        system.bindLifeCycleWith(self)
        session.requestor ! ExecutorSystemStarted(system, session.executorSystemJvmConfig.jar)
      } else {
        logger.error("We get a ExecutorSystem back, but resource requestor is no longer valid. " +
          "Will shutdown the allocated system")
        system.shutdown
      }
    case LaunchExecutorSystemTimeout(session) =>
      if (isSessionAlive(session)) {
        logger.error(s"Failed to launch executor system for ${session.requestor} due to timeout")
        session.requestor ! StartExecutorSystemTimeout
      }

    case LaunchExecutorSystemRejected(resource, reason, session) =>
      if (isSessionAlive(session)) {
        logger.error(s"Failed to launch executor system, due to $reason, " +
          s"will ask master to allocate new resources $resource")
        resourceAgents.get(session).foreach { resourceAgent: ActorRef =>
          resourceAgent ! RequestResource(appId, ResourceRequest(resource, WorkerId.unspecified))
        }
      }
  }

  private def isSessionAlive(session: Session): Boolean = {
    Option(session).flatMap(session => resourceAgents.get(session)).nonEmpty
  }
}

object ExecutorSystemScheduler {

  case class StartExecutorSystems(
                                   resources: Array[ResourceRequest], executorSystemConfig: ExecutorSystemJvmConfig)

  case class ExecutorSystemStarted(system: ExecutorSystem, boundedJar: Option[AppJar])

  case class StopExecutorSystem(system: ExecutorSystem)

  case object StartExecutorSystemTimeout

  case class ExecutorSystemJvmConfig(classPath: Array[String], jvmArguments: Array[String],
                                     jar: Option[AppJar], username: String, executorAkkaConfig: Config = null)

  case class Session(requestor: ActorRef, executorSystemJvmConfig: ExecutorSystemJvmConfig)

  class ResourceAgent(master: ActorRef, session: Session) extends Actor {
    private var resourceRequestor: ActorRef = null

    var timeOutClock: Cancellable = null

    private var unallocatedResource: Int = 0

    import context.dispatcher

    //定义资源分配的超时时间
    private val timeout = 30

    def receive: Receive = {
      case request: RequestResource =>
        unallocatedResource += request.request.resource.slots
        Option(timeOutClock).map(_.cancel)
        timeOutClock = context.system.scheduler.scheduleOnce(
          timeout.seconds, self, ResourceAllocationTimeOut(session))
        resourceRequestor = sender
        master ! request
      case ResourceAllocated(allocations) =>
        unallocatedResource -= allocations.map(_.resource.slots).sum
        resourceRequestor forward ResourceAllocatedForSession(allocations, session)
      case timeout: ResourceAllocationTimeOut =>
        if (unallocatedResource > 0) {
          resourceRequestor ! timeout
          context.stop(self)
        }
    }
  }

  case class ResourceAllocatedForSession(resource: Array[ResourceAllocation], session: Session)

  case class ResourceAllocationTimeOut(session: Session)

}


