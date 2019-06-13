package com.hwlcn.dataframework

import java.util.concurrent.TimeUnit

object Constants {
  final val MASTER_WATCHER = "watcher"
  final val SINGLETON_MANAGER = "singleton"
  final val MASTER = "master"
  final val WORKER = "worker"
  final val APP_METADATA = "app_metadata"

  //基础的集群配置文件
  final val APPLICATION_CONFIG = "application.conf"
  // master相关的配置信息
  final val MASTER_CONFIG = "master.conf"
  // worker相关的配置文件信息
  final val WORK_CONFIG = "worker.conf"

  final val MASTER_GROUP = "master_group"

  final val WORKER_ID = "next_worker_id"

  final val MASTER_STATE = "master_state"

  //定义app的默认执行id
  final val APPMASTER_DEFAULT_EXECUTOR_ID = -1

  //试用feture时的 超时定义
  final val FUTURE_TIMEOUT = akka.util.Timeout(15, TimeUnit.SECONDS)

}
