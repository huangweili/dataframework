package com.hwlcn.dataframework.application;

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

    public AppDescription() {
        userConfig = new LinkedHashMap<>();
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
