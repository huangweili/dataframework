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
    private String user;         //提交人
    private long submissionTime; //app的提交时间
    private long startTime;      //开始时间
    private long finishTime;     //结束时间
    private Config config;
    private ApplicationStatus status;       //app状态

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public ApplicationRuntimeInfo() {
        this.status = new ApplicationStatus(ApplicationStatusConstants.NONEXIST);
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


    /**
     * 注册状态的app runtime
     *
     * @return
     */
    public ApplicationRuntimeInfo onAppMasterRegistered(ActorRef appMaster, ActorRef worker) {
        this.appMaster = appMaster;
        this.worker = worker;
        this.status = new ApplicationStatus(ApplicationStatusConstants.PENDING);
        return this;
    }

    public ApplicationRuntimeInfo onAppMasterActivated(long timestamp) {
        this.startTime = timestamp;
        this.status = new ApplicationStatus(ApplicationStatusConstants.ACTIVE);
        return this;
    }

    public ApplicationRuntimeInfo onFinalStatus(long timeStamp, ApplicationStatus finalStatus) {
        this.finishTime = timeStamp;
        this.status = finalStatus;
        return this;
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
