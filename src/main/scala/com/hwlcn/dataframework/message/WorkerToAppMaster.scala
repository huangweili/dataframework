package com.hwlcn.dataframework.message

/**
  * 定义Worker到appmaster的消息
  *
  * @author huangweili
  */
object WorkerToAppMaster {

  /**
    * Executor 启动失败
    *
    * @param reason
    * @param ex
    */
  case class ExecutorLaunchRejected(reason: String = null, ex: Throwable = null)

  /**
    * 关闭 Executor 成功
    *
    * @param appId
    * @param executorId
    */
  case class ShutdownExecutorSucceed(appId: Int, executorId: Int)

  /**
    * 关闭 executor 失败
    *
    * @param reason
    * @param ex
    */
  case class ShutdownExecutorFailed(reason: String = null, ex: Throwable = null)

}
