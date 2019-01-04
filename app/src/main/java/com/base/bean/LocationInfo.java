package com.base.bean;

public class LocationInfo{
    private String latitude;
    private String longitude;
    private String address;
    //和前端JS定义好的字段，来用区分是哪里调用的请求
    private String name;
    //定义返回的类型，确认是正常数据还是异常数据, 正常为normal, 超时为timeout, GPS未打开为 gpsClosed
    private String type;

    public LocationInfo(String latitude, String longitude, String address, String name, String type) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.name = name;
        this.type = type;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}