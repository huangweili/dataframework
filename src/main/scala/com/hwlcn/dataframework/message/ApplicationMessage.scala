package com.hwlcn.dataframework.message

import akka.actor.ActorRef

import scala.util.Try

/**
  * app 相关的 message信息
  *
  * @author huangweili
  */
object ApplicationMessage {


  /**
    * 应用保存一些简单的信息在集群中
    *
    * 注意：如果是机构相对复杂和变化量大的信心通过第三方缓存处理
    *
    * @param appId
    * @param key
    * @param value
    */
  case class SaveAppData(appId: Int, key: String, value: Any)

  /**
    * AppData已保存成功的事件
    */
  case object AppDataSaved

  /**
    * AppData已保存失败的信息
    */
  case object SaveAppDataFailed

  /**
    * 获取APP的运行结果
    *
    * @param key
    * @param value
    */
  case class GetAppDataResult(key: String, value: Any)


  /**
    * 获取应用在集群中的信息
    *
    * @param appId
    * @param key
    */
  case class GetAppData(appId: Int, key: String)


  case class SubmitApplicationResult(appId: Try[Int])

  case class SubmitApplicationResultValue(appId: Int)

  case class ShutdownApplicationResult(appId: Try[Int])

  case class ReplayApplicationResult(appId: Try[Int])


  //启动app相关的消息通信

  case class RegisterActorSystem(systemPath: String)

  case class ActorSystemRegistered(bindLifeWith: ActorRef)

}
