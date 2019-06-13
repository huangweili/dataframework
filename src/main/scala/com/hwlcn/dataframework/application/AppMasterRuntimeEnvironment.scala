package com.hwlcn.dataframework.application

import akka.actor.{Actor, ActorPath, ActorRef, Props, Stash, Terminated}
import com.hwlcn.dataframework.application.AppMasterRuntimeEnvironment.{AppId, ListenerActorRef, MasterActorRef, StartAppMaster}
import com.hwlcn.dataframework.executor.ExecutorSystemScheduler.{Session, StartExecutorSystems}
import com.hwlcn.dataframework.executor.{ExecutorSystemLauncher, ExecutorSystemScheduler}
import com.hwlcn.dataframework.message.AppMasterToAppManager.RegisterAppMaster
import com.hwlcn.dataframework.message.MasterMessage.{MasterConnected, MasterStopped}
import com.hwlcn.dataframework.worker.MasterProxy
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * Appmaster的运行环境
  * 这个类的作用类似 serverless mesh 的gate进程
  *
  * @author huangweili
  */
class AppMasterRuntimeEnvironment(
                                   appContextInput: AppMasterContext,
                                   app: AppDescription,
                                   masters: Iterable[ActorPath],
                                   //定义master的信息
                                   masterFactory: (AppId, MasterActorRef) => Props,
                                   appMasterFactory: (AppMasterContext, AppDescription) => Props,
                                   //定义连接保持的factory
                                   masterConnectionKeeperFactory: (MasterActorRef, RegisterAppMaster, ListenerActorRef) => Props)
  extends Actor {

  private val logger = LoggerFactory.getLogger(getClass)

  private val appId = appContextInput.appId

  private val master = context.actorOf(
    masterFactory(appId, context.actorOf(Props(new MasterProxy(masters, 30.seconds)))))
  private val appContext = appContextInput.copy(masterProxy = master)


  private val appMaster = context.actorOf(appMasterFactory(appContext, app))

  context.watch(appMaster)

  private val registerAppMaster = RegisterAppMaster(appId, appMaster, appContext.workerInfo)

  private val masterConnectionKeeper = context.actorOf(
    masterConnectionKeeperFactory(master, registerAppMaster, self))

  context.watch(masterConnectionKeeper)

  def receive: Receive = {
    case MasterConnected =>
      logger.info(s"Master 已经连接启动,应用:${appId}...")
      appMaster ! StartAppMaster
    case MasterStopped =>
      logger.error(s"Master is stopped, stop AppMaster $appId...")
      context.stop(self)
    case Terminated(actor) => actor match {
      case `appMaster` =>
        logger.info(s"AppMaster $appId is stopped, shutdown myself")
        context.stop(self)
      case `masterConnectionKeeper` =>
        logger.error(s"Master connection keeper is stopped, appId: $appId, shutdown myself")
        context.stop(self)
      case t => {
        logger.info(s"接收到未处理的信息，${t}")
      }
    }
  }
}

object AppMasterRuntimeEnvironment {
  def props(masters: Iterable[ActorPath], app: AppDescription, appContextInput: AppMasterContext
           ): Props = {

    val master = (appId: AppId, masterProxy: MasterActorRef) =>
      MasterWithExecutorSystemProvider.props(appId, masterProxy)

    val appMaster = (appContext: AppMasterContext, app: AppDescription) =>
      LazyStartAppMaster.props(appContext, app)

    val masterConnectionKeeper = (master: MasterActorRef, registerAppMaster:
    RegisterAppMaster, listener: ListenerActorRef) => Props(new MasterConnectionKeeper(
      registerAppMaster, master, masterStatusListener = listener))

    Props(new AppMasterRuntimeEnvironment(appContextInput, app, masters,
      master, appMaster, masterConnectionKeeper))
  }

  class LazyStartAppMaster(appMasterProps: Props) extends Actor with Stash {

    def receive: Receive = null

    context.become(startAppMaster)

    def startAppMaster: Receive = {
      case StartAppMaster =>
        val appMaster = context.actorOf(appMasterProps, "appmaster")
        context.watch(appMaster)
        context.become(terminationWatch orElse appMasterService(appMaster))
        unstashAll()
      case _ =>
        stash()
    }

    def terminationWatch: Receive = {
      case Terminated(_) =>
        context.stop(self)
    }

    def appMasterService(appMaster: ActorRef): Receive = {
      case msg => appMaster forward msg
    }
  }


  object LazyStartAppMaster {
    def props(appContext: AppMasterContext, app: AppDescription): Props = {
      val appMasterProps = Props(Class.forName(app.getAppMaster), appContext, app)
      Props(new LazyStartAppMaster(appMasterProps))
    }
  }


  case object StartAppMaster


  class MasterWithExecutorSystemProvider(master: ActorRef, executorSystemProviderProps: Props)
    extends Actor {

    val executorSystemProvider: ActorRef = context.actorOf(executorSystemProviderProps)

    override def receive: Receive = {
      case request: StartExecutorSystems =>
        executorSystemProvider forward request
      case msg =>
        master forward msg
    }
  }

  object MasterWithExecutorSystemProvider {
    def props(appId: Int, master: ActorRef): Props = {

      val executorSystemLauncher = (appId: Int, session: Session) =>
        Props(new ExecutorSystemLauncher(appId, session))
      val scheduler = Props(new ExecutorSystemScheduler(appId, master, executorSystemLauncher))

      Props(new MasterWithExecutorSystemProvider(master, scheduler))
    }
  }

  private type AppId = Int
  private type MasterActorRef = ActorRef
  private type ListenerActorRef = ActorRef
}
