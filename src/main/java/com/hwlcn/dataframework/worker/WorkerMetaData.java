package com.hwlcn.dataframework.worker;

import java.io.Serializable;
import java.util.UUID;

/**
 * 定义worker的基础信息，每个worker都有自己的定义信息
 * 在集群中根据worker的信息来响应client端的请求
 * <p>
 * id 用来标记worker信息，一种worker的id都是相同的
 * <p>
 * 用户端可以根据id请求 worker的信息以便获取worker的服务
 *
 * @author huangweili
 */
public class WorkerMetaData implements Serializable {


    public WorkerMetaData() {
        this.id = UUID.randomUUID().toString();
        this.type = WorkerType.Short;
    }

    public WorkerMetaData(String id, WorkerType type) {
        this.id = id;
        this.type = type;
    }

    private String id;  //worker的唯一标识用来识别worker的信息

    private WorkerType type; //worker的类型


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public WorkerType getType() {
        return type;
    }

    public void setType(WorkerType type) {
        this.type = type;
    }
}
