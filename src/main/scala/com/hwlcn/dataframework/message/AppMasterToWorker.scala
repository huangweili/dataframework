package com.hwlcn.dataframework.message

import com.hwlcn.dataframework.application.ExecutorJVMConfig
import com.hwlcn.dataframework.scheduler.Resource

/**
  * 定义 appmaster和 worker之间的通信协议
  *
  * @author huangweili
  */
object AppMasterToWorker {

  /**
    * 启动 执行器 用来启动一个 APP
    *
    * @param appId
    * @param executorId
    * @param resource
    * @param executorJvmConfig
    */
  case class LaunchExecutor(appId: Int, executorId: Int, resource: Resource, executorJvmConfig: ExecutorJVMConfig)

  /**
    * 关闭执行器
    *
    * @param appId
    * @param executorId
    * @param reason
    */
  case class ShutdownExecutor(appId: Int, executorId: Int, reason: String)

  /**
    * 修改执行器的资源信息
    *
    * @param appId
    * @param executorId
    * @param resource
    */
  case class ChangeExecutorResource(appId: Int, executorId: Int, resource: Resource)

}
