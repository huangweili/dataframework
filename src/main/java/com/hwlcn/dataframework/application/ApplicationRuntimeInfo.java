package com.hwlcn.dataframework.application;

import akka.actor.ActorRef;
import com.typesafe.config.Config;

import java.io.Serializable;

/**
 * 记录app的运行信息
 *
 * @author huangweili
 */
public class ApplicationRuntimeInfo implements Serializable {
    
    private int appId;
    private String appName;
    private ActorRef appMaster;
    private ActorRef worker;
    private String user;
    private long submissionTime;
    private long startTime;
    private long finishTime;
    private Config config;

    public ApplicationRuntimeInfo() {
    }

    public ApplicationRuntimeInfo(int appId, String appName, ActorRef appMaster, ActorRef worker, String user, long submissionTime, long startTime, long finishTime, Config config) {
        this.appId = appId;
        this.appName = appName;
        this.appMaster = appMaster;
        this.worker = worker;
        this.user = user;
        this.submissionTime = submissionTime;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.config = config;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public ActorRef getAppMaster() {
        return appMaster;
    }

    public void setAppMaster(ActorRef appMaster) {
        this.appMaster = appMaster;
    }

    public ActorRef getWorker() {
        return worker;
    }

    public void setWorker(ActorRef worker) {
        this.worker = worker;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public long getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(long submissionTime) {
        this.submissionTime = submissionTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}
