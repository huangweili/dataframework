package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.application.{AppDescription, AppJar}
import com.hwlcn.dataframework.worker.WorkerId
import com.typesafe.config.Config

import scala.util.Try

object ClientMessage {

  /**
    * 从客户端提交应用
    *
    * @param appDescription
    * @param appJar
    * @param username
    */
  case class SubmitApplication(
                                appDescription: AppDescription, appJar: Option[AppJar],
                                username: String = System.getProperty("user.name"))

  /**
    * 根据APPID重新启动应用
    *
    * @param appId
    */
  case class RestartApplication(appId: Int)


  /**
    * 根据APPID获取APP的信息
    *
    * @param appId
    */
  case class ResolveAppId(appId: Int)


  /**
    * 根据wokerID 获取worker的引用
    *
    * @param workerId
    */
  case class ResolveWorkerId(workerId: WorkerId)


  case class ResolveWorkerIdResult(worker: Try[ActorRef])

  /**
    * 把服务端注册到appmaster的结束监听里面
    *
    * @param appId
    */
  case class RegisterAppResultListener(appId: Int)


  /**
    * 获取指定appId的数据
    *
    * @param appId
    * @param detail
    */
  case class AppMasterDataRequest(appId: Int, detail: Boolean = false)

  /**
    * 获取所有的AppMaster的信息
    */
  case object AppMastersDataRequest

  /**
    * 根据IP查询APPMaster的配置信息
    *
    * @param appId
    */
  case class QueryAppMasterConfig(appId: Int)


  /**
    * 查询集群的状态
    */
  case object QueryMasterConfig


  case class MasterConfig(config: Config)

}

