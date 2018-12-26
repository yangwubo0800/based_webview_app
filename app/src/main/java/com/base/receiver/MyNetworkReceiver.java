package com.base.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import com.base.app.WebViewActivity;
import com.base.utils.log.AFLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 此广播接受器提供给android5.0版本使用，解决webview无网络下不回调出错问题
 */
public class MyNetworkReceiver extends BroadcastReceiver {
    private String TAG = "MyNetworkReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        //**判断当前的网络连接状态是否可用*/
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if ( info != null && info.isAvailable()){
            //当前网络状态可用
            int netType = info.getType();
            if (netType == ConnectivityManager.TYPE_WIFI){
                AFLog.d(TAG, "wifi available");
                //发送网络状态给mqttService
                notifyObservers(NetworkType.NETWORK_WIFI);
            }else if (netType == ConnectivityManager.TYPE_MOBILE ){
                AFLog.d(TAG, "data network available");
                notifyObservers(NetworkType.NETWORK_MOBILE);
            }
            WebViewActivity.mNoNetwork = false;
        }else {
            //当前网络不可用
            AFLog.d(TAG, "no network");
            notifyObservers(NetworkType.NETWORK_NO);
            WebViewActivity.mNoNetwork = true;
        }

    }

    // android7.0 之后需要动态注册才能监听此广播，监听网络变化之后，消息订阅服务是否连接。
    private static class InstanceHolder {
        private static final MyNetworkReceiver INSTANCE = new MyNetworkReceiver();
    }

    private List<NetStateChangeObserver> mObservers = new ArrayList<>();

    /**
     * 注册网络监听
     */
    public static void registerReceiver(@NonNull Context context) {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(InstanceHolder.INSTANCE, intentFilter);
    }

    /**
     * 取消网络监听
     */
    public static void unregisterReceiver(@NonNull Context context) {
        context.unregisterReceiver(InstanceHolder.INSTANCE);
    }

    /**
     * 注册网络变化Observer
     */
    public static void registerObserver(NetStateChangeObserver observer) {
        if (observer == null){
            return;
        }
        if (!InstanceHolder.INSTANCE.mObservers.contains(observer)) {
            InstanceHolder.INSTANCE.mObservers.add(observer);
        }
    }

    /**
     * 取消网络变化Observer的注册
     */
    public static void unregisterObserver(NetStateChangeObserver observer) {
        if (observer == null){
            return;
        }
        if (InstanceHolder.INSTANCE.mObservers == null){
            return;
        }
        InstanceHolder.INSTANCE.mObservers.remove(observer);
    }

    /**
     * 通知所有的Observer网络状态变化
     */
    private void notifyObservers(NetworkType networkType) {
        if (networkType == NetworkType.NETWORK_NO) {
            for(NetStateChangeObserver observer : mObservers) {
                observer.onNetDisconnected();
            }
        } else {
            for(NetStateChangeObserver observer : mObservers) {
                observer.onNetConnected(networkType);
            }
        }
    }


}
