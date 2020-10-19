package com.base.app;

import android.app.Application;
import android.content.Context;
import com.base.utils.log.AFLog;

import com.base.bean.User;
import com.base.receiver.MyNetworkReceiver;
import com.base.utils.config.ParseConfig;

import cn.jpush.android.api.JPushInterface;
import io.github.skyhacker2.sqliteonweb.SQLiteOnWeb;

public class BaseWebviewApp extends Application {
    private String TAG = "BaseWebviewApp";

    public static BaseWebviewApp mApplication = null;
    private Context mContext;
    private String session;//用户session
    private User user;//用户
    private String stationId;//站点ID
    //消息订阅主题由前端设置，因为主题有可能后台那边改变，通过前端设置灵活。
    //并且可以根据站点设置多个主题，使用逗号分隔字符串，形成字符串数组
    private String[] topics;
    //由于clientId要满足多系统多端区分需求,提供给前端来拼接设置
    private String clientId;


    public static  BaseWebviewApp getInstance(){
        return mApplication;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public void setTopic(String[] topics){
        this.topics = topics;
    }

    public String[] getTopics(){
        return  this.topics;
    }

    public void setClientId(String clientId){
        this.clientId = clientId;
    }

    public String getClientId(){
        return  this.clientId;
    }

    public void onCreate() {
        super.onCreate();
        AFLog.d(TAG,"BaseWebviewApp onCreate");
        mApplication = this;
        mContext = getApplicationContext();
        //初始化配置信息
        ParseConfig.initAppConfig(mContext);
        //动态注册网络监听
        MyNetworkReceiver.registerReceiver(mContext);
        //将手机连接PC的热点，查看log中的如下信息,可以获取PC端查看数据库db的地址信息
        // I/SQLiteOnWeb: SQLiteOnWeb running on: http://192.168.137.224:9000
        SQLiteOnWeb.init(this).start();
        //处理未捕获异常
        new MyCrashHandler(this);
        //极光推送初始化,  TODO:是否要做成可配置
        JPushInterface.setDebugMode(true);
        JPushInterface.init(mContext);
        String registId = JPushInterface.getRegistrationID(mContext);
        AFLog.e(TAG, "极光初始化后注册 registId="+registId);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 取消BroadcastReceiver注册
        MyNetworkReceiver.unregisterReceiver(mContext);
    }
}
