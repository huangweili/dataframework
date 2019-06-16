package com.hwlcn.dataframework.master;

import java.io.Serializable;

/**
 * master 节点信息
 *
 * @author huangweili
 */
public class MasterNode implements Serializable {
    private String host;
    private int port;

    public MasterNode() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MasterNode(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
