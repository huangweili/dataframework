package com.hwlcn.dataframework.application

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Cancellable}
import com.hwlcn.dataframework.MasterProxy.{MasterRestarted, MasterStopped, WatchMaster}
import com.hwlcn.dataframework.application.MasterConnectionKeeper.AppMasterRegisterTimeout
import com.hwlcn.dataframework.application.MasterConnectionKeeper.MasterConnectionStatus.MasterConnected
import com.hwlcn.dataframework.message.AppManagerToAppMaster.AppMasterRegistered
import com.hwlcn.dataframework.message.AppMasterToAppManager.RegisterAppMaster
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

/**
  * 该类用来保持和master的连接关系
  *
  * @author huangweili
  */
class MasterConnectionKeeper(
                              register: RegisterAppMaster, masterProxy: ActorRef, masterStatusListener: ActorRef)
  extends Actor {

  import context.dispatcher

  private val logger = LoggerFactory.getLogger(getClass)
  masterProxy ! WatchMaster(self)

  def registerAppMaster: Cancellable = {
    masterProxy ! register
    context.system.scheduler.scheduleOnce(FiniteDuration(30, TimeUnit.SECONDS),
      self, AppMasterRegisterTimeout)
  }

  context.become(waitMasterToConfirm(registerAppMaster))

  def waitMasterToConfirm(cancelRegister: Cancellable): Receive = {
    case AppMasterRegistered(_) =>
      cancelRegister.cancel()
      masterStatusListener ! MasterConnected
      context.become(masterLivenessListener)
    case AppMasterRegisterTimeout =>
      cancelRegister.cancel()
      masterStatusListener ! MasterStopped
      context.stop(self)
  }

  def masterLivenessListener: Receive = {
    case MasterRestarted =>
      logger.info("Master restarted, re-registering AppMaster....")
      context.become(waitMasterToConfirm(registerAppMaster))
    case MasterStopped =>
      logger.info("Master is dead, killing this AppMaster....")
      masterStatusListener ! MasterStopped
      context.stop(self)
  }

  def receive: Receive = null
}

object MasterConnectionKeeper {

  case object AppMasterRegisterTimeout

  object MasterConnectionStatus {

    case object MasterConnected

    case object MasterStopped

  }

}

