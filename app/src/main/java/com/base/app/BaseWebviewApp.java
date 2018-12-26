package com.base.app;

import android.app.Application;
import android.content.Context;
import com.base.utils.log.AFLog;

import com.base.bean.User;
import com.base.receiver.MyNetworkReceiver;
import com.base.utils.config.ParseConfig;

public class BaseWebviewApp extends Application {
    private String TAG = "BaseWebviewApp";

    public static BaseWebviewApp mApplication = null;
    private Context mContext;
    private String session;//用户session
    private User user;//用户
    private String stationId;//站点ID


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

    public void onCreate() {
        super.onCreate();
        AFLog.d(TAG,"BaseWebviewApp onCreate");
        mApplication = this;
        mContext = getApplicationContext();
        //初始化配置信息
        ParseConfig.initAppConfig(mContext);
        //动态注册网络监听
        MyNetworkReceiver.registerReceiver(mContext);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 取消BroadcastReceiver注册
        MyNetworkReceiver.unregisterReceiver(mContext);
    }
}
