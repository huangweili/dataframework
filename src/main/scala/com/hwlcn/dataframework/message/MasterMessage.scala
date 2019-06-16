package com.hwlcn.dataframework.message

import akka.actor.ActorRef
import com.hwlcn.dataframework.master.MasterNode
import com.hwlcn.dataframework.worker.WorkerId

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


  case object MasterRestarted

  //失去master的连接对象时的事件
  case object MasterStopped

  case object MasterConnected

  case object GetAllWorkers

  case object GetMasterData

}
