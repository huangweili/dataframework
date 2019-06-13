package com.hwlcn.dataframework.application;

import com.typesafe.config.Config;

import java.io.Serializable;
import java.util.List;

/**
 * 用来定义 worker执行器的配置信息
 *
 * @author huangweili
 */
public class ExecutorJVMConfig implements Serializable {

    public ExecutorJVMConfig() {
    }


    public ExecutorJVMConfig(List<String> classPath,
                             List<String> jvmArguments,
                             String mainClass,
                             List<String> arguments,
                             AppJar jar,
                             String userName,
                             Config akkaConfig
    ) {
        this.classPath = classPath;
        this.jvmArguments = jvmArguments;
        this.mainClass = mainClass;
        this.arguments = arguments;
        this.jar = jar;
        this.userName = userName;
        this.akkaConfig = akkaConfig;
    }

    private List<String> classPath;

    private List<String> jvmArguments;

    private String mainClass;

    private List<String> arguments;

    private AppJar jar;

    private String userName;

    private Config akkaConfig;


    public List<String> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    public void setJvmArguments(List<String> jvmArguments) {
        this.jvmArguments = jvmArguments;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public AppJar getJar() {
        return jar;
    }

    public void setJar(AppJar jar) {
        this.jar = jar;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Config getAkkaConfig() {
        return akkaConfig;
    }

    public void setAkkaConfig(Config akkaConfig) {
        this.akkaConfig = akkaConfig;
    }
}
