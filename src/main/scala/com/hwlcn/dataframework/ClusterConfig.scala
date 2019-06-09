package com.hwlcn.dataframework


import com.typesafe.config.{Config, ConfigFactory}

object ClusterConfig {

  def defaultConfig: Config = {
    default(Constants.APPLICATION_CONFIG)
  }

  def default(configFile: String = Constants.APPLICATION_CONFIG): Config = {
    load(configFile);
  }

  /**
    * 获取master的配置信息
    *
    * @param configFile
    * @return
    */
  def master(configFile: String = Constants.MASTER_CONFIG): Config = {
    load(configFile);
  }


  /**
    * 获取work的配置信息
    *
    * @param configFile
    * @return
    */
  def worker(configFile: String = Constants.WORK_CONFIG): Config = {
    load(configFile)
  }


  private def load(configFile: String): Config = {
    ConfigFactory.load(configFile)
  }

  class ConfigValidationException(msg: String) extends Exception(msg: String)
  
}
