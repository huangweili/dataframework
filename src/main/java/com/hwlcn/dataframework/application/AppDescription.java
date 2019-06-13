package com.hwlcn.dataframework.application;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * app描述信息
 * 包括App启动时的配置信息
 *
 * @author huangweili
 */
public class AppDescription implements Serializable {

    private String name;

    private String appMaster;

    private Map<String, String> userConfig; //用户的配置信息


    // appmaster相关的配置信息
    private Config clusterConfig;

    public AppDescription(String name, String appMaster, Map<String, String> userConfig, Config clusterConfig) {
        this.name = name;
        this.appMaster = appMaster;
        this.userConfig = userConfig;
        this.clusterConfig = clusterConfig;
    }

    public Config getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(Config clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public AppDescription() {
        userConfig = new LinkedHashMap<>();
        clusterConfig = ConfigFactory.empty();
    }

    public AppDescription(String name, String appMaster, Map<String, String> userConfig) {
        this.name = name;
        this.appMaster = appMaster;
        this.userConfig = userConfig;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppMaster() {
        return appMaster;
    }

    public void setAppMaster(String appMaster) {
        this.appMaster = appMaster;
    }

    public Map<String, String> getUserConfig() {
        return userConfig;
    }

    public void setUserConfig(Map<String, String> userConfig) {
        this.userConfig = userConfig;
    }
}
