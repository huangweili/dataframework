package com.hwlcn.dataframework.default

import akka.actor.ActorRef
import com.hwlcn.dataframework.application.{AppManagerActor, ApplicationLauncherFactory}

/**
  * appmanger的管理类用来处理app的管理，包括资源调度以及生命周期的管理
  *
  * @param kvService
  * @param launcher
  */
class DefaultAppManager(kvService: ActorRef, launcher: ApplicationLauncherFactory) extends AppManagerActor(kvService, launcher) {
  override def clientMsgHandler(): Receive = {
    case _ => {
      logger.info("接收到未处理的信息")
    }
  }
}
