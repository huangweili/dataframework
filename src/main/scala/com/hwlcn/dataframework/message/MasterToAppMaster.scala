package com.hwlcn.dataframework.message

import com.hwlcn.dataframework.scheduler.ResourceAllocation

object MasterToAppMaster {

  case class ResourceAllocated(allocations: Array[ResourceAllocation])

}
