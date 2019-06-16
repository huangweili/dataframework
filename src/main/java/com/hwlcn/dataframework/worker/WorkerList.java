package com.hwlcn.dataframework.worker;

import java.io.Serializable;
import java.util.List;

/**
 * worker列表信息
 */
public class WorkerList implements Serializable {
    private List<WorkerData> workers;

    public WorkerList() {
    }

    public List<WorkerData> getWorkers() {
        return workers;
    }

    public void setWorkers(List<WorkerData> workers) {
        this.workers = workers;
    }

    public WorkerList(List<WorkerData> workers) {
        this.workers = workers;
    }
}
