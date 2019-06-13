package com.hwlcn.dataframework.system

import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor.{Actor, PoisonPill, Props, Terminated}
import com.hwlcn.dataframework.message.AppMasterMessage.{ActorCreated, BindLifeCycle, CreateActor, CreateActorFailed}
import com.hwlcn.dataframework.message.ApplicationMessage.{ActorSystemRegistered, RegisterActorSystem, RegisterActorSystemFailed}
import com.hwlcn.dataframework.{ActorUtil, Constants}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * 资源运行的守护进程
  *
  * @author huangweili
  */
class Daemon(val name: String, reportBack: String) extends Actor {
  val logger = LoggerFactory.getLogger(getClass)

  val username = Option(System.getProperty(Constants.SYSTEM_USERNAME)).getOrElse("not_defined")

  logger.info(s"RegisterActorSystem to ${reportBack}, current user: $username")

  val reportBackActor = context.actorSelection(reportBack)

  reportBackActor ! RegisterActorSystem(ActorUtil.getSystemAddress(context.system).toString)

  implicit val executionContext = context.dispatcher

  val timeout = context.system.scheduler.scheduleOnce(Duration(25, TimeUnit.SECONDS),
    self, RegisterActorSystemFailed(new TimeoutException))


  context.become(waitForRegisterResult)

  def receive: Receive = null

  def waitForRegisterResult: Receive = {
    case ActorSystemRegistered(parent) =>
      timeout.cancel()
      context.watch(parent)
      context.become(waitCommand)
    case RegisterActorSystemFailed(ex) =>
      logger.error("RegisterActorSystemFailed", ex)
      timeout.cancel()
      context.stop(self)
  }

  def waitCommand: Receive = {
    case BindLifeCycle(actor) =>
      logger.info(s"ActorSystem $name Binding life cycle with actor: $actor")
      context.watch(actor)
    case CreateActor(props: Props, name: String) =>
      logger.info(s"creating actor $name")
      val actor = Try(context.actorOf(props, name))
      actor match {
        case Success(actor) =>
          sender ! ActorCreated(actor, name)
        case Failure(e) =>
          sender ! CreateActorFailed(props.clazz.getName, e)
      }
    case PoisonPill =>
      context.stop(self)
    case Terminated(actor) =>
      logger.info(s"System $name Watched actor is terminated $actor")
      context.stop(self)
  }

  override def postStop(): Unit = {
    logger.info(s"ActorSystem $name is shutting down...")
    context.system.terminate()
  }
}