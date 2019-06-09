package com.hwlcn.dataframework.application;

import java.io.Serializable;

/**
 * 应用信息
 *
 * @author huangweili
 */
public class ApplicationMetaData implements Serializable {

    private int appId;
    private int attemptId;
    private AppDescription appDesc;
    private AppJar jar;
    private String username;

    public ApplicationMetaData() {
    }

    public ApplicationMetaData(int appId, int attemptId, AppDescription appDesc, AppJar jar, String username) {
        this.appId = appId;
        this.attemptId = attemptId;
        this.appDesc = appDesc;
        this.jar = jar;
        this.username = username;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public AppDescription getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(AppDescription appDesc) {
        this.appDesc = appDesc;
    }

    public AppJar getJar() {
        return jar;
    }

    public void setJar(AppJar jar) {
        this.jar = jar;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
