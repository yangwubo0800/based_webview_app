package com.base.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.base.bean.Alarm;
import com.base.bean.AlarmResp;
import com.base.bean.User;
import com.base.constant.Constants;
import com.base.constant.URLUtil;
import com.base.app.BaseWebviewApp;
import com.base.app.R;

import java.io.File;
import java.io.IOException;

import com.base.app.WebViewActivity;
import com.base.utils.GeneralUtils;
import com.base.utils.GsonHelper;
import com.base.utils.IniFile;
import com.base.utils.NotificationUtils;
import com.base.utils.log.AFLog;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class AlarmNotifyService extends Service {

    public static final  int SEND_NOTIFICATION=1;
    private final String TAG="AlarmNotifyService";
    private Handler handler;
    private Intent intent;
    private PendingIntent pi;
    private NotificationManager manager;
    private Notification notification;
    private OkHttpClient okHttpClient;
    private FormEncodingBuilder builder;
    Request request;
    Call call;
    private File basefile;
    private File basePath;
    private File basePath_unread;
    IniFile notifyNewFile;
    IniFile unreadFile;
    private long notifyNewId=0; //告警手机端最新ID
    private long unreadId=0; //手机端已读ID
    private long differentValue=0;
    private int sum;
    private String strIniPath;
    private int notifyInt;
    private String currentStationId;
    private String notifycationFlag="";
    public volatile  boolean isrun=true;

    public AlarmNotifyService(){}

    @Override
    public void onCreate() {
        super.onCreate();
        handler=new Handler();
        AFLog.d(TAG,"onCreate");

        okHttpClient=new OkHttpClient();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AFLog.v(TAG,"onStartCommand");
        currentStationId=intent.getStringExtra("stationId");
        // TODO: 拼凑ini文件地址，IP + stationId
        String userId;
        User user = BaseWebviewApp.getInstance().getUser();
        if (null == user){
            Log.e(TAG,"not set user id yet !!!!");
            userId = "defaultUserId";
        } else {
            userId = user.getId();
        }
        strIniPath="baseApp/ini/"+ URLUtil.getServerIP(this)+"/"+ userId +"/"+ currentStationId;
        notifyInt=intent.getIntExtra("notifyInt",0);

        basefile = new File(Environment.getExternalStorageDirectory(),  strIniPath);
        basePath = new File(basefile,  "notifyNew.ini");
        basePath_unread=new File(basefile,  "unread.ini");

        Runnable runnable=new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                if(isrun){
                    //要做的事情，这里再次调用此Runnable对象，以实现每5秒实现一次的定时器操作
                    getNotify();
                    handler.postDelayed(this, 5000);
                }
            }
        };
        runnable.run();

        // TODO: 启动为前台服务, 但是会在通知栏出现一个通知，用户点击清理不掉。
        startForeground(101, NotificationUtils.getInstance(this).getNotification(null));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isrun=false;
        super.onDestroy();
        AFLog.v(TAG,"onDestroy");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void getNotify(){
        builder=new FormEncodingBuilder();//传参
        builder.add("stationId", currentStationId);

        if(basePath.exists()){
            notifyNewFile = new IniFile(basePath);
            if( GeneralUtils.isNotNull(notifyNewFile.get("Config", "newId"))&&!notifyNewFile.get("Config", "newId").toString().equals(""))
            {
                builder.add("newSoeId", notifyNewFile.get("Config", "newId").toString());
                notifyNewId=Long.valueOf(notifyNewFile.get("Config", "newId").toString());
            }
            else{
                builder.add("newSoeId", "-1");
            }

        }else{
            builder.add("newSoeId", "-1");
        }

        if(basePath_unread.exists()){
            unreadFile=new IniFile(basePath_unread);
            if(!unreadFile.get("Config", "unReadId").toString().equals(""))
            {
                unreadId=Long.valueOf(unreadFile.get("Config", "unReadId").toString());
                builder.add("soeId", unreadFile.get("Config", "unReadId").toString());
            } else{
                builder.add("soeId", "-1");
            }
        } else{
            builder.add("soeId", "-1");
        }

        differentValue=notifyNewId-unreadId;

        request=new Request.Builder()
                .url(URLUtil.getHTTPServerIP(this)+URLUtil.GET_LEAST_ALART)
                .post(builder.build())
                .addHeader("cookie", BaseWebviewApp.getInstance().getSession())
                .build();
        call=okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                //startActivity(LoginActivity.class,null);
                AFLog.v(TAG,"您的网络状况不佳，请检查网络连接");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    String result = response.body().string(); //方法只能调用一次
                    AFLog.i(TAG, result);
                    final AlarmResp res = GsonHelper.toType(result, AlarmResp.class);
                    int code = -1;
                    Alarm entity;
                    if (res != null && res instanceof AlarmResp) {
                        code = res.getRetcode();
                        switch (code) {
                            case 1000: //操作成功
                                if(res.getObj()!=null&&res.getObj().size()>0){
                                    entity=res.getObj().get(0);
                                    if(res.getCount()==0){
                                        notifyNewFile = new IniFile();
                                        notifyNewFile.set("Config", "newId", entity.getSoeId());
                                        IniFile unreadFile = new IniFile();
                                        unreadFile.set("Config", "unReadId", entity.getSoeId());
                                        try {
                                            notifyNewFile.save(new File(GeneralUtils.getIniPath(strIniPath),"notifyNew.ini"));
                                            unreadFile.save(new File(GeneralUtils.getIniPath(strIniPath),"unread.ini"));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }else {
//                                    sum=differentValue+res.getCount();
                                        sum=res.getCount();
                                        notifyNewFile = new IniFile(basePath);
                                        notifyNewFile.set("Config", "newId", entity.getSoeId());
                                        notifyNewFile.save(basePath);

                                        StringBuffer alertBuffer=new StringBuffer();
                                        alertBuffer.append("[");
                                        if (sum>0){
                                            if(sum==1){
                                                alertBuffer.append(entity.getSoeTypeName());
                                            }
                                            else{
                                                if(sum>1000){
                                                    alertBuffer.append("1000+条");
                                                }else{
                                                    alertBuffer.append(sum);
                                                    alertBuffer.append("条");
                                                }
                                            }
                                            alertBuffer.append("]");
                                            startSendNotify(entity.getSoeId(),alertBuffer.toString(),entity.getSoeExplain());
                                        }
                                    }
                                }
                                break;
                            default:
                                //Log.v(TAG,res.getRetinfo());
                                break;
                        }
                    }else{
                        AFLog.e(TAG,"请求失败，请稍后再试");
                    }
                }else{
                    AFLog.e(TAG,"您的网络状况不佳，请检查网络连接");
                }
            }
        });
    }

    //发送通知
    public void startSendNotify(String soeId,String title,String content){
//        EBSharedPrefManager prefManager = BridgeFactory.getBridge(Bridges.SHARED_PREFERENCE);
//        notifycationFlag = prefManager.getKDSharedPrefSetting().getString(EBSharedPrefSetting.NOTIFYCATION, "on");
        SharedPreferences sp = getSharedPreferences(Constants.PREF_NAME_SETTING, Context.MODE_PRIVATE);
        notifycationFlag = sp.getString(Constants.NOTIFYCATION, "on");
        // TODO: 告警列表页面也需要H5来实现，所以此处使用传递url方式，让webview来加载显示，代替AlarmNotifyActivity
        if(notifycationFlag.equals("on")){
            intent=new Intent(getApplication(), WebViewActivity.class);
            intent.putExtra("soeId",soeId);
            intent.putExtra("strIniPath",strIniPath);
            intent.putExtra("currentStationId",currentStationId);
            String url = URLUtil.getHTTPServerIP(this) + "AlarmNotifyActivity.html";
            //debug
            url = "http://www.baidu.com";
            intent.putExtra("url",url);
            pi=PendingIntent.getActivity(getApplication(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            manager=(NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(notifyInt);
            notification=new NotificationCompat
                    .Builder(getApplication())
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_app_logo)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_app_logo))
                    .setContentIntent(pi)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .build();
            manager.notify(notifyInt,notification);
        }
    }

}
