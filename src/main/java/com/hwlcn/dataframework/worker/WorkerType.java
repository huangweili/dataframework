package com.hwlcn.dataframework.worker;

/**
 * 定义worker的类型信息
 * worker分两种，一种是为SERVICE长时间运行的worker,这个worker
 * 在集群中注册特定的服务。master根据注册的服务为client提供对应的worker信息
 *
 * @author huangweili
 */
public final class WorkerType {

    public static int SERVICE = 1; //提供服务的的worker

    public static int APP = 2; //短时间运行的 APP
}
