package com.hwlcn.dataframework.message

/**
  * appmanager和appmaster之间的交互信息
  *
  * @author huangweili
  */
object AppManagerToAppMaster {

  /**
    * appmaster注册完成的信息
    *
    * @param appId
    */
  case class AppMasterRegistered(appId: Int)

  /**
    * 发送APP激活信息
    *
    * @param appId
    */
  case class AppMasterActivated(appId: Int)


  /**
    * 发送应用暂停信息
    *
    * @param appId
    */
  case class ShutdownApplication(appId: Int)

}
