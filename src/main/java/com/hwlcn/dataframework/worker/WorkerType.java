package com.hwlcn.dataframework.worker;

/**
 * 定义worker的类型信息
 * worker分两种，一种是为Long长事件运行的worker,这个worker
 * 在集群中注册特定的服务。master根据注册的服务为client提供对应的worker信息
 *
 * @author huangweili
 */
public enum WorkerType {

    Long(1), //长应用

    Short(2); //短应用

    private int value;

    WorkerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
