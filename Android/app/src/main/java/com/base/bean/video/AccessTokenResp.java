package com.base.bean.video;

import com.base.bean.ESOBaseResp;

/**
 * XXX on 2017/9/27.
 * AccessToken返回类
 */

public class AccessTokenResp extends ESOBaseResp {
    private AccessToken data;

    public AccessToken getData() {
        return data;
    }

    public void setData(AccessToken data) {
        this.data = data;
    }
}
