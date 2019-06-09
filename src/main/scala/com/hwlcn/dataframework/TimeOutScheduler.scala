package com.hwlcn.dataframework

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * 该类用于控制可能存在的超时调度
  *
  * @author huangweili
  */
trait TimeOutScheduler {
  this: Actor =>

  import context.dispatcher

  def sendMsgWithTimeOutCallBack(
                                  target: ActorRef, msg: AnyRef, duration: Long, timeOutHandler: => Unit): Unit = {
    val result = target.ask(msg)(FiniteDuration(duration, TimeUnit.MILLISECONDS))
    result.onComplete {
      case Success(r) =>
        self ! r
      case Failure(_) =>
        timeOutHandler
    }
  }
}
