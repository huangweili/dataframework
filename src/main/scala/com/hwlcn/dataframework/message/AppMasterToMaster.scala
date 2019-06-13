package com.hwlcn.dataframework.message

import com.hwlcn.dataframework.scheduler.ResourceRequest

/**
  * appmaster 到 master的通信
  *
  * @author
  */
object AppMasterToMaster {

  case class RequestResource(appId: Int, request: ResourceRequest)

}
