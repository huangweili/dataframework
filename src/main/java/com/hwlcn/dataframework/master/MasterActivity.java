package com.hwlcn.dataframework.master;

import java.io.Serializable;

/**
 * master的活动状态
 */
public class MasterActivity implements Serializable {
    private long time;
    private String event;

    public MasterActivity() {
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public MasterActivity(long time, String event) {
        this.time = time;
        this.event = event;
    }
}
