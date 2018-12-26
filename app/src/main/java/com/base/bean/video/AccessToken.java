package com.base.bean.video;

/**
 * XXX on 2017/9/27.
 * 视频accessToken
 */

public class AccessToken {
    private String accessToken;
    private String expireTime;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(String expireTime) {
        this.expireTime = expireTime;
    }
}
