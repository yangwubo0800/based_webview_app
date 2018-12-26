package com.base.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.base.app.BaseWebviewApp;
import com.base.receiver.MyNetworkReceiver;
import com.base.receiver.NetStateChangeObserver;
import com.base.receiver.NetworkType;
import com.base.utils.NotificationUtils;
import com.base.utils.log.AFLog;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Arrays;

public class MqttService extends Service implements MqttCallback, NetStateChangeObserver {

    private String TAG = "MqttService";
    // TODO: these variable should be set when login
//    private String mBroke = "tcp://175.6.40.67:8883";
    private String mBrokeDomain = "tcp://mq.dev.hnaccloud.com:8883";
//    private String mTopic = "ST/65086508";
//    private String mClientId = "Android_user";
//    private int mQos = 2;
    //订阅参数
    private String[] mTopics;
    private int[] mQoSs;
    private String mClientId;
    private String mUserName = "admin";
    private String mPassWord = "password";
    private MqttClient mClient;
    private MqttConnectOptions mConnectOptions;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        AFLog.d(TAG,"onCreate");
        //注册监听网络状态变化
        MyNetworkReceiver.registerObserver(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //启动为前台服务, 但是会在通知栏出现一个通知，用户点击清理不掉。
        startForeground(102, NotificationUtils.getInstance(this).getNotification(null));
        AFLog.d(TAG,"onStartCommand");
        //get params from front end and set topics and clientId
        String clientId = BaseWebviewApp.getInstance().getUser().getName();
        String stationId = BaseWebviewApp.getInstance().getStationId();
        String[] topics = {"ST/"+ stationId};
        AFLog.d(TAG,"clientId="+ clientId +  " stationId=" + stationId);
        if (!TextUtils.isEmpty(clientId)){
            if (clientId.length() > 23){
                mClientId = clientId.substring(0,23);
            } else {
                mClientId = clientId;
            }
        }

        if (null != topics){
            mTopics = topics;
            int count = mTopics.length;
            mQoSs = new int[count];
            for (int i=0; i<count; i++){
                mQoSs[i] = 2;
            }
        }

        //create client and connect and subscribe
        initClient();
        connectBroke();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // TODO: unsubscribe and disconnect
        AFLog.d(TAG,"onDestroy");
        if (mClient != null){
            try {
                mClient.unsubscribe(mTopics);
                mClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            mClient = null;
        }
        //取消监听观察者
        MyNetworkReceiver.unregisterObserver(this);
    }


    public void initClient(){
        AFLog.d(TAG,"initClient");
        try {
            //create
            mClient = new MqttClient(mBrokeDomain, mClientId, new MemoryPersistence());
            AFLog.d(TAG,"mBrokeDomain=" + mBrokeDomain);
            //connection params
            mConnectOptions = new MqttConnectOptions();
            mConnectOptions.setUserName(mUserName);
            mConnectOptions.setPassword(mPassWord.toCharArray());
            mConnectOptions.setConnectionTimeout(10);
            mConnectOptions.setKeepAliveInterval(10);
            mConnectOptions.setCleanSession(false);
            //set callback
            mClient.setCallback(this);
        } catch (MqttException e) {
            AFLog.e(TAG,"initClient MqttException");
            e.printStackTrace();
        }catch (Exception e){
            AFLog.e(TAG,"initClient Exception");
            e.printStackTrace();
        }

    }

    public void connectBroke(){
        AFLog.d(TAG,"connectBroke mClient=" + mClient);
        if (null != mClient){
            if (mClient.isConnected()){
                AFLog.d(TAG,"client has connected clientId:" + mClientId);
            } else {
                // TODO: network operation, do not exec it in main thread.
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mClient.connect(mConnectOptions);
                            mClient.subscribe(mTopics, mQoSs);
                            AFLog.d(TAG, "connectBroke succeed");
                        } catch (MqttException e) {
                            AFLog.e(TAG, "connectBroke MqttException");
                            e.printStackTrace();
                        }catch (Exception e){
                            AFLog.e(TAG, "connectBroke Exception");
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }else {
            AFLog.e(TAG,"client is null, please create it firstly");
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        String causeMsg = "";
        if (null != cause){
            causeMsg = cause.getMessage();
        }
        AFLog.e(TAG,"connectionLost cause:" + causeMsg);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msgPayLoad = "";
        if ((null != message) && (message.getPayload() != null)){
            msgPayLoad = new String(message.getPayload(), "UTF-8");
        }
        AFLog.d(TAG,"messageArrived UTF-8 msgPayLoad:" + msgPayLoad);
        // TODO: 发送消息通知，记录soeId, 跳转界面到历史未读信息列表，由webview加载html实现
        NotificationUtils.getInstance(this).setNotificationTitle("推送消息");
        NotificationUtils.getInstance(this).setNotificationContent(msgPayLoad);
        NotificationUtils.getInstance(this).sendNotification(null, 2018);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        boolean deliveryComplete = false;
        if (null != token){
            deliveryComplete = token.isComplete();
        }
        AFLog.d(TAG,"deliveryComplete deliveryComplete:" + deliveryComplete);
    }

    @Override
    public void onNetDisconnected() {
        AFLog.d(TAG,"onNetDisconnected");
    }

    @Override
    public void onNetConnected(NetworkType networkType) {
        AFLog.d(TAG,"onNetConnected");
        //在service还活着的情况下，当检测到网络由断开变成连接时，重新连接。
        connectBroke();
    }
}
