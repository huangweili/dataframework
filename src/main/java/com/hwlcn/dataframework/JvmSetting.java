package com.hwlcn.dataframework;

import java.io.Serializable;
import java.util.List;

public class JvmSetting implements Serializable {

    private List<String> vmargs;

    private List<String> classPath;

    public JvmSetting() {
    }

    public JvmSetting(List<String> vmargs, List<String> classPath) {
        this.vmargs = vmargs;
        this.classPath = classPath;
    }

    public List<String> getVmargs() {
        return vmargs;
    }

    public void setVmargs(List<String> vmargs) {
        this.vmargs = vmargs;
    }

    public List<String> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<String> classPath) {
        this.classPath = classPath;
    }
}
