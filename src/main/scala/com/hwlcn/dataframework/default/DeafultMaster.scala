package com.hwlcn.dataframework.default

import com.hwlcn.dataframework.master.MasterActor
import com.hwlcn.dataframework.scheduler.SchedulerActor

class DeafultMaster(schedulerClass: Class[SchedulerActor]) extends MasterActor(schedulerClass) {

  /**
    * 与appMaster的事件处理
    *
    * @return
    */
  override def appMasterHandler(): Receive = null

  /**
    * 处理web相关资源的访问接口
    *
    * @return
    */
  override def clientHandler(): Receive = null
}
