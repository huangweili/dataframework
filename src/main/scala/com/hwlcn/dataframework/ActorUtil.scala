package com.hwlcn.dataframework

import akka.actor.{ActorPath, ActorRef, ActorSystem, Address, ExtendedActorSystem}

object ActorUtil {
  /**
    * 用来生成master的访问地址，这个master实际上是一个master proxy对象
    *
    * @param master
    * @return
    */
  def getMasterActorPath(master: HostPort): ActorPath = {
    ActorPath.fromString(s"akka.tcp://${Constants.MASTER}@${master.host}:${master.port}/user/${Constants.MASTER}")
  }

  /**
    * 定义一个Executor的名称
    * @param appId
    * @param executorId
    * @return
    */
  def actorNameForExecutor(appId: Int, executorId: Int): String = "app_" + appId + "_executor_" +
    executorId


  def getSystemAddress(system: ActorSystem): Address = {
    system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress
  }

  /**
    * 获取actor的完整路径
    *
    * @param system
    * @param path
    * @return
    */
  def getFullPath(system: ActorSystem, path: ActorPath): String = {
    path.toStringWithAddress(getSystemAddress(system))
  }

  def isChildActorPath(parent: ActorRef, child: ActorRef): Boolean = {
    if (null != child) {
      parent.path.name == child.path.parent.name
    } else {
      false
    }
  }

  /**
    * 获取actor的host信息
    *
    * @param actor
    * @return
    */
  def getHostname(actor: ActorRef): String = {
    val path = actor.path
    path.address.host.getOrElse("localhost")
  }


}
