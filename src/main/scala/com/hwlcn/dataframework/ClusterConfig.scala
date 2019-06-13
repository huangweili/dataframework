package com.hwlcn.dataframework


import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.util.Try

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

  /**
    * 获取appmaster的通用配置信息
    *
    * @param configFile
    * @return
    */
  def appmaster(configFile: String = Constants.APPMASTER_CONFIG): Config = {
    load(configFile)
  }


  private def load(configFile: String): Config = {
    ConfigFactory.load(configFile)
  }

  def resolveJvmSetting(conf: Config): AppJvmSettings = {
    val appMasterVMArgs = Try(conf.getString(Constants.APPMASTER_ARGS).split("\\s+")
      .filter(_.nonEmpty)).toOption
    val executorVMArgs = Try(conf.getString(Constants.EXECUTOR_ARGS).split("\\s+")
      .filter(_.nonEmpty)).toOption

    val appMasterClassPath = Try(
      conf.getString(Constants.APPMASTER_EXTRA_CLASSPATH)
        .split("[;:]").filter(_.nonEmpty)).toOption

    val executorClassPath = Try(
      conf.getString(Constants.EXECUTOR_EXTRA_CLASSPATH)
        .split(File.pathSeparator).filter(_.nonEmpty)).toOption

    new AppJvmSettings(
      new JvmSetting(
        appMasterVMArgs.getOrElse(Array.empty[String]).toList.asJava,
        appMasterClassPath.getOrElse(Array.empty[String]).toList.asJava),
      new JvmSetting(executorVMArgs.getOrElse(Array.empty[String]).toList.asJava,
        executorClassPath.getOrElse(Array.empty[String]).toList.asJava)
    )
  }


  class ConfigValidationException(msg: String) extends Exception(msg: String)

}
