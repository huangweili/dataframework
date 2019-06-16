package com.hwlcn.dataframework.worker;

import java.io.Serializable;

/**
 * 用来记录和传递Woker的信息
 */
public class WorkerData implements Serializable {

    private WorkerId workerId;

    private WorkerMetaData workerMetaData;


    public WorkerData(WorkerId workerId, WorkerMetaData workerMetaData) {
        this.workerId = workerId;
        this.workerMetaData = workerMetaData;
    }

    public WorkerId getWorkerId() {
        return workerId;
    }

    public void setWorkerId(WorkerId workerId) {
        this.workerId = workerId;
    }

    public WorkerMetaData getWorkerMetaData() {
        return workerMetaData;
    }

    public void setWorkerMetaData(WorkerMetaData workerMetaData) {
        this.workerMetaData = workerMetaData;
    }


}
