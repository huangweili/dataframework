package com.hwlcn.dataframework.worker;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Objects;

/**
 * worker信息
 * @author huangweili
 */
public class WorkerInfo implements Serializable {

    private WorkerId workerId;
    private ActorRef ref;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerInfo workInfo = (WorkerInfo) o;
        return Objects.equals(workerId, workInfo.workerId) &&
                Objects.equals(ref, workInfo.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerId, ref);
    }

    public WorkerInfo() {
    }

    public WorkerInfo(WorkerId workerId, ActorRef ref) {
        this.workerId = workerId;
        this.ref = ref;
    }

    public WorkerId getWorkerId() {
        return workerId;
    }

    public void setWorkerId(WorkerId workerId) {
        this.workerId = workerId;
    }

    public ActorRef getRef() {
        return ref;
    }

    public void setRef(ActorRef ref) {
        this.ref = ref;
    }
}
