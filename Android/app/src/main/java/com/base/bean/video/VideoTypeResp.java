package com.base.bean.video;

import com.base.bean.BaseResp;

import java.util.List;

/**
 * XXX on 2017/10/18.视频类型返回
 */

public class VideoTypeResp extends BaseResp {
    private List<VideoType> obj;

    public List<VideoType> getObj() {
        return obj;
    }

    public void setObj(List<VideoType> obj) {
        this.obj = obj;
    }
}