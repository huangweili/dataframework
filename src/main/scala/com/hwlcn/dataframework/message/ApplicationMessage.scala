package com.hwlcn.dataframework.message

/**
  * app 相关的 message信息
  *
  * @author huangweili
  */
object ApplicationMessage {


  /**
    * 应用保存一些简单的信息在集群中
    *
    * 注意：如果是机构相对复杂和变化量大的信心通过第三方缓存处理
    *
    * @param appId
    * @param key
    * @param value
    */
  case class SaveAppData(appId: Int, key: String, value: Any)


  /**
    * 获取应用在集群中的信息
    *
    * @param appId
    * @param key
    */
  case class GetAppData(appId: Int, key: String)

}
