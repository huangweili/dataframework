package com.hwlcn.dataframework.application;


import jdk.internal.joptsimple.internal.Strings;

import java.io.Serializable;

/**
 * 定义APP的运行状态
 * 由于这个涉及到序列化的需求所以不建议用enum来实现
 *
 * @author huangweili
 */
public class ApplicationStatus implements Serializable {


    public ApplicationStatus() {
    }

    public ApplicationStatus(String status) {
        if (Strings.isNullOrEmpty(status)) {
            throw new NullPointerException("状态不能为空。");
        }
        this.status = status;
    }

    private String status;

    /**
     * 判断是否是终止状态
     *
     * @return
     */
    public boolean isTerminal() {
        return this.status.equals(ApplicationStatusConstants.PENDING) || this.status.equals(ApplicationStatusConstants.FAILED)
                || this.status.equals(ApplicationStatusConstants.TERMINATED);

    }


    /**
     * 记录状态的信息
     */
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }


    public void setStatus(String status) {
        this.status = status;
    }


    /**
     * 定义状态之间的转化逻辑
     *
     * @param newStatus
     * @return
     */
    public boolean canTransitTo(ApplicationStatus newStatus) {
        return false;
    }



}


