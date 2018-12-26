package com.base.bean;

import java.util.List;

/**
 * XXX on 2017/8/17.
 * 告警返回类
 */

public class AlarmResp extends BaseResp {
    private List<Alarm> obj;
    private int count;

    public List<Alarm> getObj() {
        return obj;
    }

    public void setObj(List<Alarm> obj) {
        this.obj = obj;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
