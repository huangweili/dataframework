package com.hwlcn.dataframework.application;

/**
 * 重启APP的策略类
 *
 * @author huangweili
 */
public class RestartPolicy {

    private int totalRetries;

    private int historyRetries = 0;

    public RestartPolicy() {
        this.totalRetries = 0;
    }

    public RestartPolicy(int totalRetries) {
        this.totalRetries = totalRetries;
    }

    //判断是否可以重启
    public boolean allowRestart() {
        this.historyRetries++;
        return this.totalRetries < 0 || this.historyRetries <= this.totalRetries;
    }
}
