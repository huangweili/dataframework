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
        this.type = WorkerType.APP;
    }

    public WorkerMetaData(String id, int type) {
        this.id = id;
        this.type = type;
    }

    private String id;  //worker的唯一标识用来识别worker的信息

    private int type; //worker的类型


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
