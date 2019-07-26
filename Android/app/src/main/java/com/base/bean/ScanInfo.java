package com.base.bean;

public class ScanInfo {
    private String scanResult;
    //和前端JS定义好的字段，来用区分是哪里调用的请求
    private String name;

    public ScanInfo(String scanResult, String name) {
        this.scanResult = scanResult;
        this.name = name;
    }

    public String getScanResult() {
        return scanResult;
    }

    public void setScanResult(String scanResult) {
        this.scanResult = scanResult;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
