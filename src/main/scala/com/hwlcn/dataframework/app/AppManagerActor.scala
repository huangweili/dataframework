package com.hwlcn.dataframework.app

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill, Stash}
import akka.pattern.ask
import com.hwlcn.dataframework.InMemoryKVService._
import com.hwlcn.dataframework.app.AppManager.MasterState
import com.hwlcn.dataframework.application.{ApplicationMetaData, ApplicationRuntimeInfo}
import com.hwlcn.dataframework.message.ApplicationMessage.{GetAppData, SaveAppData}
import com.hwlcn.dataframework.{Constants, TimeOutScheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * app的管理进程，每个app以独立的形式发布到cluster中
  * 提供的app的生命周期管理，包括app注册，加载，卸载，启动 等功能
  *
  * @author huangweili
  */
abstract class AppManagerActor(kvService: ActorRef) extends Actor with Stash with TimeOutScheduler {

  private val logger = LoggerFactory.getLogger(getClass)

  //定义超时时间
  implicit val timeout = akka.util.Timeout(15, TimeUnit.SECONDS)
  implicit val executionContext = context.dispatcher

  private var nextAppId: Int = 1 //记录app的id信息

  private var applicationRegistry: Map[Int, ApplicationRuntimeInfo] = Map.empty[Int, ApplicationRuntimeInfo]

  kvService ! GetKV(Constants.MASTER_GROUP, Constants.MASTER_STATE)
  context.become(waitForMasterState)

  def waitForMasterState: Receive = {
    case GetKVSuccess(_, result) =>
      val masterState = result.asInstanceOf[MasterState]
      if (masterState != null) {
        this.nextAppId = masterState.maxId + 1
        this.applicationRegistry = masterState.applicationRegistry
      }
      context.become(receiveHandler)
      unstashAll()
    case GetKVFailed(ex) =>
      logger.error("Failed to get master state, shutting down master to avoid data corruption...", ex)
      context.parent ! PoisonPill
    case msg =>
      logger.info(s"Get message ${msg.getClass.getSimpleName}")
      stash()
  }

  /**
    * 处理appmanager收到的信息
    *
    * @return
    */
  def receiveHandler: Receive = {
    logger.info("app manager 准备就绪")

    clientMsgHandler orElse appMasterMessage orElse selfMsgHandler orElse workerMessage orElse
      appDataStoreService orElse terminationWatch
  }


  /**
    * 存储简单的应用信息
    *
    * @return
    */
  def appDataStoreService: Receive = {
    case SaveAppData(appId, key, value) =>
      val client = sender()
      (kvService ? PutKV(appId.toString, key, value)).asInstanceOf[Future[PutKVResult]].map {
        case PutKVSuccess =>
          client ! AppDataSaved
        case PutKVFailed(_, _) =>
          client ! SaveAppDataFailed
      }
    case GetAppData(appId, key) =>
      val client = sender()
      (kvService ? GetKV(appId.toString, key)).asInstanceOf[Future[GetKVResult]].map {
        case GetKVSuccess(_, value) =>
          client ! GetAppDataResult(key, value)
        case GetKVFailed(_) =>
          client ! GetAppDataResult(key, null)
      }
  }

  //屏蔽默认的接收函数
  override def receive: Receive = null
}


object AppManager {

  case class RecoverApplication(appMetaData: ApplicationMetaData)

  /**
    * 获取app的注册信息
    *
    * @param maxId
    * @param applicationRegistry
    */
  case class MasterState(maxId: Int, applicationRegistry: Map[Int, ApplicationRuntimeInfo])

}

