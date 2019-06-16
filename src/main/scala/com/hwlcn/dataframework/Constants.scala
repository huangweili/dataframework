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

  final val APPMASTER_CONFIG = "appmaster.conf"

  final val APPMASTER_ARGS = "appmaster.vmargs"

  final val EXECUTOR_ARGS = "executor.vmargs"


  final val APPMASTER_EXTRA_CLASSPATH = "appmaster.extraClasspath"

  final val EXECUTOR_EXTRA_CLASSPATH = "executor.extraClasspath"

  final val MASTER_GROUP = "master_group"

  final val WORKER_ID = "next_worker_id"

  final val MASTER_STATE = "master_state"

  //定义app的默认执行id
  final val APPMASTER_DEFAULT_EXECUTOR_ID = -1

  //试用feture时的 超时定义
  final val FUTURE_TIMEOUT = akka.util.Timeout(15, TimeUnit.SECONDS)

  //应用的jar存储地址
  final val APP_JAR_STORE_ROOT_PATH = "master.app.jarstore.rootpath"


  //TODO 这里的前缀最好加上系统名称
  final val SYSTEM_USERNAME = "system.username"
  final val REMOTE_DEBUG_PORT = "remote-debug-port"
}
