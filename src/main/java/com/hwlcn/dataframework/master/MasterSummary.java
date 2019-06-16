package com.hwlcn.dataframework.master;

import java.io.Serializable;
import java.util.List;

/**
 * master的总信息
 *
 * @author huangweili
 */
public class MasterSummary implements Serializable {

    private MasterNode leader; // 集群的leader信息
    private List<MasterNode> cluster; //集群成员节点
    private long aliveFor; //集群存活时间
    private String logFile;
    private String jarStore; //jar的存储地址
    private String masterStatus;  //集群状态
    private String homeDirectory; //主目录
    private String jvmName;   //获取jvm的类型
    private List<MasterActivity> activities;

    public MasterSummary() {
    }

    public MasterNode getLeader() {
        return leader;
    }

    public void setLeader(MasterNode leader) {
        this.leader = leader;
    }

    public List<MasterNode> getCluster() {
        return cluster;
    }

    public void setCluster(List<MasterNode> cluster) {
        this.cluster = cluster;
    }

    public long getAliveFor() {
        return aliveFor;
    }

    public void setAliveFor(long aliveFor) {
        this.aliveFor = aliveFor;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getJarStore() {
        return jarStore;
    }

    public void setJarStore(String jarStore) {
        this.jarStore = jarStore;
    }

    public String getMasterStatus() {
        return masterStatus;
    }

    public void setMasterStatus(String masterStatus) {
        this.masterStatus = masterStatus;
    }

    public String getHomeDirectory() {
        return homeDirectory;
    }

    public void setHomeDirectory(String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    public String getJvmName() {
        return jvmName;
    }

    public void setJvmName(String jvmName) {
        this.jvmName = jvmName;
    }

    public List<MasterActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<MasterActivity> activities) {
        this.activities = activities;
    }

    public MasterSummary(MasterNode leader, List<MasterNode> cluster, long aliveFor, String logFile,
                         String jarStore, String masterStatus,
                         String homeDirectory, String jvmName,
                         List<MasterActivity> activities) {
        this.leader = leader;
        this.cluster = cluster;
        this.aliveFor = aliveFor;
        this.logFile = logFile;
        this.jarStore = jarStore;
        this.masterStatus = masterStatus;
        this.homeDirectory = homeDirectory;
        this.jvmName = jvmName;
        this.activities = activities;
    }
}



