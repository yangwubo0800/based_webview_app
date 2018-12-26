package com.base.bean;


/**
 * XXX on 2017/8/15.
 * 告警类
 */

public class Alarm {
    private String soeId;
    private String soeTypeName;
    private String soeTime;
    private String soeExplain;

    public Alarm(String soeId, String soeTypeName, String soeTime, String soeExplain) {
        this.soeId = soeId;
        this.soeTypeName = soeTypeName;
        this.soeTime = soeTime;
        this.soeExplain = soeExplain;
    }

    public String getSoeId() {
        return soeId;
    }

    public void setSoeId(String soeId) {
        this.soeId = soeId;
    }

    public String getSoeTypeName() {
        return soeTypeName;
    }

    public void setSoeTypeName(String soeTypeName) {
        this.soeTypeName = soeTypeName;
    }

    public String getSoeTime() {
        return soeTime;
    }

    public void setSoeTime(String soeTime) {
        this.soeTime = soeTime;
    }

    public String getSoeExplain() {
        return soeExplain;
    }

    public void setSoeExplain(String soeExplain) {
        this.soeExplain = soeExplain;
    }
}
