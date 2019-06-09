package com.hwlcn.dataframework.application;

import java.io.Serializable;

/**
 * appjar信息描述
 *
 * @author huangweili
 */
public class AppJar implements Serializable {

    private String name;
    private String filePath;

    public AppJar() {
    }

    public AppJar(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
