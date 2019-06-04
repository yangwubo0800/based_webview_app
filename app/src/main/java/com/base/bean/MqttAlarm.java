package com.base.bean;

public class MqttAlarm {
    private String alarmDt;
    private String alarmType;
    private int alarmTypeNum;
    private String content;
    private String stationId;
    private String stationNm;

    public MqttAlarm(String alarmDt, String alarmType, int alarmTypeNum,
                     String content, String stationId, String stationNm) {
        this.alarmDt = alarmDt;
        this.alarmType = alarmType;
        this.alarmTypeNum = alarmTypeNum;
        this.content = content;
        this.stationId = stationId;
        this.stationNm = stationNm;
    }

    public String getAlarmDt() {
        return alarmDt;
    }

    public void setAlarmDt(String alarmDt) {
        this.alarmDt = alarmDt;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public int getAlarmTypeNum() {
        return alarmTypeNum;
    }

    public void setAlarmTypeNum(int alarmTypeNum) {
        this.alarmTypeNum = alarmTypeNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getStationNm() {
        return stationNm;
    }

    public void setStationNm(String stationNm) {
        this.stationNm = stationNm;
    }
}
