package com.hwlcn.dataframework.default

import com.hwlcn.dataframework.application.AppManagerActor
import com.hwlcn.dataframework.master.MasterActor
import com.hwlcn.dataframework.scheduler.SchedulerActor

class DeafultMaster(schedulerClass: Class[SchedulerActor], appManagerClass: Class[AppManagerActor]) extends MasterActor(schedulerClass, appManagerClass) {

  /**
    * 消息处理类
    *
    * @return
    */
  override def receive: Receive = super.receive orElse otherHandler;


  /** *
    * 扩展定义其他要处理的事件
    *
    * @return
    */
  def otherHandler: Receive = {
    case msg => {
      logger.info(s"otherHandler 接收到的信息${msg}")
    }
  }

}
