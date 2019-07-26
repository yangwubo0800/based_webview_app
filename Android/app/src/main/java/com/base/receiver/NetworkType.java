package com.base.receiver;

public enum NetworkType {

    NETWORK_WIFI("WiFi"),
    NETWORK_MOBILE("MOBILE"),
    NETWORK_UNKNOWN("Unknown"),
    NETWORK_NO("No network");

    private String desc;
    NetworkType(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return desc;
    }
}
