package com.hwlcn.dataframework.application

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, PoisonPill, Stash, Terminated}
import akka.pattern.ask
import com.hwlcn.dataframework.InMemoryKVService._
import com.hwlcn.dataframework.application.AppManager.{MasterState, RecoverApplication}
import com.hwlcn.dataframework.message.AppManagerToAppMaster.{AppMasterActivated, AppMasterRegistered, ShutdownApplication}
import com.hwlcn.dataframework.message.AppMasterToAppManager.{ApplicationStatusChanged, RegisterAppMaster}
import com.hwlcn.dataframework.message.ApplicationMessage._
import com.hwlcn.dataframework.{Constants, TimeOutScheduler}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * app的管理进程，每个app以独立的形式发布到cluster中
  * 提供的app的生命周期管理，包括app注册，加载，卸载，启动 等功能
  *
  * @author huangweili
  */
abstract class AppManagerActor(kvService: ActorRef, launcher: ApplicationLauncherFactory) extends Actor with Stash with TimeOutScheduler {

  private val logger = LoggerFactory.getLogger(getClass)

  //定义超时时间
  implicit val timeout = akka.util.Timeout(15, TimeUnit.SECONDS)

  implicit val executionContext = context.dispatcher

  private var nextAppId: Int = 1 //记录app的id信息

  private var applicationRegistry: Map[Int, ApplicationRuntimeInfo] = Map.empty[Int, ApplicationRuntimeInfo]

  //app 结束后的信息监听信息
  private var appResultListeners = Map.empty[Int, List[ActorRef]]

  //注册 app的重启机制
  private var appMasterRestartPolicies = Map.empty[Int, RestartPolicy]


  /** *
    * 保存APP运行后的结果信息
    */
  private var applicationResults = Map.empty[Int, ApplicationStatus]

  kvService ! GetKV(Constants.MASTER_GROUP, Constants.MASTER_STATE)

  context.become(waitForMasterState)

  def waitForMasterState: Receive = {
    case GetKVSuccess(_, result) =>
      val masterState = result.asInstanceOf[MasterState]
      if (masterState != null) {
        this.nextAppId = masterState.maxId + 1
        this.applicationRegistry = masterState.applicationRegistry
      }
      //成为正式的处理类
      context.become(receive)
      unstashAll()
    case GetKVFailed(ex) =>
      logger.error("Failed to get master state, shutting down master to avoid data corruption...", ex)
      context.parent ! PoisonPill
    case msg =>
      logger.info(s"Get message ${msg.getClass.getSimpleName}")
      stash()
  }

  def clientMsgHandler(): Receive;

  /**
    * 定义Appmaster到appmanager之间的事件处理
    *
    * @return
    */
  def appMasterMessage(): Receive = {
    // 响应APP的注册信息
    case RegisterAppMaster(appId, appMaster, workerInfo) => {
      val appInfo = applicationRegistry.get(appId)
      appInfo match {
        case Some(info) =>
          logger.info(s"APP ID: ${appId} 注册成功。")
          val updatedInfo = info.onAppMasterRegistered(appMaster, workerInfo.getRef)
          context.watch(appMaster)
          applicationRegistry += appId -> updatedInfo
          kvService ! PutKV(Constants.MASTER_GROUP, Constants.MASTER_STATE, MasterState(nextAppId, applicationRegistry))
          sender ! AppMasterRegistered(appId)
        case None =>
          logger.error(s"未找到 appId:${appId} 对应的信息")
      }
    }
    //响应App的状态变更信息
    case ApplicationStatusChanged(appId, newStatus, timeStamp) =>
      onApplicationStatusChanged(appId, newStatus, timeStamp)

  }

  /**
    * 处理指定的APP的状态变化事件
    *
    * @param appId
    * @param newStatus
    * @param timeStamp
    */
  private def onApplicationStatusChanged(appId: Int, newStatus: ApplicationStatus,
                                         timeStamp: Long): Unit = {
    applicationRegistry.get(appId) match {
      case Some(appRuntimeInfo) =>
        if (appRuntimeInfo.getStatus.canTransitTo(newStatus)) {
          var updatedStatus: ApplicationRuntimeInfo = null
          logger.info(s"Application${appId}在${timeStamp}时变更到新的状态${newStatus.toString}")
          newStatus match {
            case s: ApplicationStatus if ApplicationStatusConstants.ACTIVE.equals(s.getStatus) => {
              updatedStatus = appRuntimeInfo.onAppMasterActivated(timeStamp)
              sender ! AppMasterActivated(appId)
            }
            case status: ApplicationStatus if status.isTerminal => {
              shutdownApplication(appRuntimeInfo)
              updatedStatus = appRuntimeInfo.onFinalStatus(timeStamp, status)
              //保存日志信息
              applicationResults += appId -> status
            }
            case status =>
              logger.error(s"应用:${appId}不能变更到状态${status}")
          }

          if (newStatus.isTerminal) {
            kvService ! DeleteKVGroup(appId.toString)
          }
          applicationRegistry += appId -> updatedStatus
          kvService ! PutKV(Constants.MASTER_GROUP, Constants.MASTER_STATE, MasterState(nextAppId, applicationRegistry))
        } else {
          logger.error(s"Application $appId tries to switch status ${appRuntimeInfo.getStatus} " +
            s"to $newStatus")
        }
      case None =>
        logger.error(s"未找到ID:${appId}的应用的信息")
    }
  }

  /**
    * 向appmaster发送应用暂停的信息
    *
    * @param info
    */
  private def shutdownApplication(info: ApplicationRuntimeInfo): Unit = {
    info.getAppMaster ! ShutdownApplication(info.getAppId)
  }


  /**
    * 当app与服务中断后的请求
    *
    * @return
    */
  def terminationWatch: Receive = {
    case terminate: Terminated =>
      logger.info(s"AppMaster(${terminate.actor.path}) 已经终止, " +
        s"network: ${terminate.getAddressTerminated}")

      applicationRegistry.find(_._2.getAppMaster.equals(terminate.actor)).foreach {
        case (appId, info) =>
          info.getStatus match {
            //为终止状态的时候，把运行信息同步给APP的linstener
            case s: ApplicationStatus if s.isTerminal =>
              sendAppResultToListeners(appId, applicationResults(appId))
            case _ =>
              //获取应用的信息
              (kvService ? GetKV(appId.toString, Constants.APP_METADATA))
                .asInstanceOf[Future[GetKVResult]].map {
                case GetKVSuccess(_, result) =>
                  val appMetadata = result.asInstanceOf[ApplicationMetaData]
                  if (appMetadata != null) {
                    logger.info(s"恢复应用:${appId}的信息")
                    val updatedInfo = new ApplicationStatus(ApplicationStatusConstants.PENDING)
                    applicationRegistry += appId -> updatedInfo
                    self ! RecoverApplication(appMetadata)
                  } else {
                    logger.error(s"没有找到${appId}对应的应用数据。")
                  }
                case GetKVFailed(ex) =>
                  logger.error(s"未能获取到AppMaster的信息", ex)
              }
          }
      }
  }

  private def sendAppResultToListeners(appId: Int, result: ApplicationStatus): Unit = {
    appResultListeners.get(appId).foreach {
      _.foreach { client =>
        client ! result
      }
    }
  }

  /**
    * 自己和自己通信，完成APP的通信机制,用于处理app的自恢复机制
    *
    * @return
    */
  def selfMsgHandler: Receive = {
    //App重启的逻辑处理
    case RecoverApplication(state) =>
      val appId = state.getAppId
      if (appMasterRestartPolicies(appId).allowRestart) {
        logger.info(s"Application 正在恢复 $appId...")
        kvService ! PutKV(Constants.MASTER_GROUP, Constants.MASTER_STATE,
          MasterState(this.nextAppId, applicationRegistry))
        val launcherID = UUID.randomUUID().toString;
        //重新启动一个Appmaster信息
        context.actorOf(launcher.props(appId, Constants.APPMASTER_DEFAULT_EXECUTOR_ID, state.getAppDesc,
          Option(state.getJar), state.getUsername, context.parent, None), s"launcher${appId}_${launcherID}")

      } else {
        logger.error(s"Application $appId 的重启次数超过了限制次数。")
      }
  }


  /**
    * 该方法用来响应app的数据请求，根据存储的key更新和获取对应的APP ID的数据信息
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
  override def receive: Receive = {
    clientMsgHandler orElse appMasterMessage orElse selfMsgHandler orElse
      appDataStoreService orElse terminationWatch
  }
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

