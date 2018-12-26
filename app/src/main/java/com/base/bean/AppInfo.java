package com.base.bean;

public class AppInfo {

    private String packageName;
    private String appVersion;
    private String appVersionCode;

    public AppInfo(String packageName, String appVersion, String appVersionCode) {
        this.packageName = packageName;
        this.appVersion = appVersion;
        this.appVersionCode = appVersionCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public String getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(String appVersionCode) {
        this.appVersionCode = appVersionCode;
    }
}
