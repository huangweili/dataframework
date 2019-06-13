package com.hwlcn.dataframework.message

import akka.actor.{ActorRef, Props}

object AppMasterMessage {

  /**
    * 对actor的生命周期进行监控
    *
    * @param actor
    */
  case class BindLifeCycle(actor: ActorRef)

  /**
    * 创建 Actor对象
    *
    * @param prop
    * @param name
    */
  case class CreateActor(prop: Props, name: String)

  /**
    * 创建Actor对象成功
    *
    * @param actor
    * @param name
    */
  case class ActorCreated(actor: ActorRef, name: String)

  /**
    * 创建Actor对象失败
    *
    * @param name
    * @param reason
    */
  case class CreateActorFailed(name: String, reason: Throwable)

}
