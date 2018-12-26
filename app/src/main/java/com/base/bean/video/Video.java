package com.base.bean.video;

/**
 * XXX on 2017/9/18.
 * 视频类
 */

public class Video {
    private String videoId; //id
    private String videoName; //名称
    private String videoImageUrl; //图片地址
    private String createTime; //创建时间
    private String videoType; //0 萤石云，1公司视频，2 未知
    private Object object;
    private Object object2;

    public Video(String videoId, String videoName, String videoImageUrl, String createTime) {
        this.videoId = videoId;
        this.videoName = videoName;
        this.videoImageUrl = videoImageUrl;
        this.createTime = createTime;
    }

    public Video(String videoId, String videoName, String videoImageUrl, String createTime, String videoType, Object object) {
        this.videoId = videoId;
        this.videoName = videoName;
        this.videoImageUrl = videoImageUrl;
        this.createTime = createTime;
        this.videoType = videoType;
        this.object = object;
    }
    public Video(String videoId, String videoName, String videoImageUrl, String createTime, String videoType, Object object,Object object2) {
        this.videoId = videoId;
        this.videoName = videoName;
        this.videoImageUrl = videoImageUrl;
        this.createTime = createTime;
        this.videoType = videoType;
        this.object = object;
        this.object2 = object2;
    }

    public Object getObject2() {
        return object2;
    }

    public void setObject2(Object object2) {
        this.object2 = object2;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoImageUrl() {
        return videoImageUrl;
    }

    public void setVideoImageUrl(String videoImageUrl) {
        this.videoImageUrl = videoImageUrl;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}