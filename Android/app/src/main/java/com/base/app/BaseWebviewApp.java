package com.base.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.base.utils.GeneralUtils;
import com.base.utils.log.AFLog;

import com.base.bean.User;
import com.base.receiver.MyNetworkReceiver;
import com.base.utils.config.ParseConfig;
import com.iflytek.cloud.SpeechUtility;
import com.igexin.sdk.IUserLoggerInterface;

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
        if (ParseConfig.sNeedJpush){
            JPushInterface.setDebugMode(true);
            JPushInterface.init(mContext);
            String registId = JPushInterface.getRegistrationID(mContext);
            AFLog.e(TAG, "极光初始化后注册 registId="+registId);
        }

        if (ParseConfig.sNeedBaiduPush){
            String baiduPushKey = GeneralUtils.getMetaValue(mContext, "BaiduPUSH_APPKEY");
            PushManager.startWork(mContext, PushConstants.LOGIN_TYPE_API_KEY, baiduPushKey);
            AFLog.e(TAG, "百度云消息推送初始化 baiduPushKey="+baiduPushKey);
        }

        if (ParseConfig.sNeedGTPush){
            Log.d(TAG, "initializing getui push sdk...");
            com.igexin.sdk.PushManager.getInstance().initialize(this);
            if (BuildConfig.DEBUG) {
                // 切勿在 release 版本上开启调试日志
                com.igexin.sdk.PushManager.getInstance().setDebugLogger(mContext, new IUserLoggerInterface() {
                    @Override
                    public void log(String s) {
                        Log.i("PUSH_LOG", s);
                    }
                });
            }
        }

        // TODO: 初始化科大讯飞
        // 应用程序入口处调用，避免手机内存过小，杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
        // 如在Application中调用初始化，需要在Mainifest中注册该Applicaiton
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用半角“,”分隔。
        // 设置你申请的应用appid,请勿在'='与appid之间添加空格及空转义符
        // 注意： appid 必须和下载的SDK保持一致，否则会出现10407错误
        // TODO: 先不更加配置来进行确认是否要初始化了，因为这个不像消息推送，初始化之后会有其他消息主动推过来的可能，只是调用接口来使用功能。
        if(true){
            String iflySpeechAppId = GeneralUtils.getMetaValue(mContext, "com.ifly.speech.APP_ID");
            Log.d(TAG, "iflySpeechAppId="+iflySpeechAppId);
            SpeechUtility.createUtility(BaseWebviewApp.this, "appid=" + iflySpeechAppId);
            // 以下语句用于设置日志开关（默认开启），设置成false时关闭语音云SDK日志打印
            //Setting.setShowLog(false);
        }


    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // 取消BroadcastReceiver注册
        MyNetworkReceiver.unregisterReceiver(mContext);
    }
}
