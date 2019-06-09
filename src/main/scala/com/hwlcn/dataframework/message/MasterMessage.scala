package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.{MasterNode, WorkerId}

object MasterMessage {

  //master列表更新信息
  case class MasterListUpdated(masters: List[MasterNode])

  case class MasterInfo(master: ActorRef, startTime: Long = 0L)

  //默认的master信息

  object MasterInfo {
    def empty: MasterInfo = MasterInfo(null)
  }

  //worker终止事件
  case class WorkerTerminated(workerId: WorkerId)

}
