package com.hwlcn.dataframework;

import java.io.Serializable;

/**
 * app的 jvm 配置信息
 *
 * @author huangweili
 */
public class AppJvmSettings implements Serializable {

    private JvmSetting appMater;

    private JvmSetting executor;

    public AppJvmSettings() {
    }

    public AppJvmSettings(JvmSetting appMater, JvmSetting executor) {
        this.appMater = appMater;
        this.executor = executor;
    }

    public JvmSetting getExecutor() {
        return executor;
    }

    public void setExecutor(JvmSetting executor) {
        this.executor = executor;
    }

    public JvmSetting getAppMater() {
        return appMater;
    }

    public void setAppMater(JvmSetting appMater) {
        this.appMater = appMater;
    }
}
