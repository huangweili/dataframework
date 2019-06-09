package com.hwlcn.dataframework

object Constants {
  final val MASTER_WATCHER = "watcher"
  final val SINGLETON_MANAGER = "singleton"
  final val MASTER = "master"
  final val WORKER = "worker"

  //基础的集群配置文件
  final val APPLICATION_CONFIG = "application.conf"
  // master相关的配置信息
  final val MASTER_CONFIG = "master.conf"
  // worker相关的配置文件信息
  final val WORK_CONFIG = "worker.conf"

  final val MASTER_GROUP = "master_group"

  final val WORKER_ID = "next_worker_id"

  final val MASTER_STATE = "master_state"
}
