package com.base.receiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.base.app.WebViewActivity;
import com.base.constant.Constants;
import com.base.utils.JPush.ExampleUtil;
import com.base.utils.SpUtils;
import com.base.utils.log.AFLog;

import cn.jpush.android.api.CmdMessage;
import cn.jpush.android.api.JPushMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

public class JiGuangMessageReceiver extends JPushMessageReceiver {
    private static final String TAG = "JiGuangMessageReceiver";

    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage message) {
        Log.e(TAG,"[onNotifyMessageOpened] "+message);
//        try{
//            //打开自定义的Activity
//            Intent i = new Intent(context, WebViewActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putString(JPushInterface.EXTRA_NOTIFICATION_TITLE,message.notificationTitle);
//            bundle.putString(JPushInterface.EXTRA_ALERT,message.notificationContent);
//            i.putExtras(bundle);
//            //i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
//            context.startActivity(i);
//        }catch (Throwable throwable){
//
//        }

        try{
            if (null != message){
                Intent intent = new Intent(context, WebViewActivity.class);
                Bundle bundle = new Bundle();
                // TODO: 历史告警页面的url需要前端设置路径，存储到sharePreference中，此处去取
                //bundle.putString("url","http://www.baidu.com");
                SpUtils.setSpFileName(Constants.PREF_NAME_USERINFO);
                //基于之前IOS已经使用过极光推送，使用的key字段为historyAlarmUrl
                String historyUrl = SpUtils.getString(context,  Constants.PUSH_MESSAGE_JUMP_URL_KEY);
                if (!TextUtils.isEmpty(historyUrl)){
                    bundle.putString("url", historyUrl);
                }else {
                    AFLog.e(TAG," 推送消息跳转页面url没有设置 ");
                }

//                String extra = message.notificationExtras;
//                AFLog.e(TAG," 安卓消息中的extra="+extra);

                intent.putExtras(bundle);
                context.startActivity(intent);
            }else {
                AFLog.e(TAG, "消息体为空");
            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }


    // TODO:收到极光推送消息，目前先直接使用极光内部自己的通知展示，因为后端发送的是带有alert消息的通知，
    //  如果需要实现自己的通知展示，后台推送消息需要alert为空或者发送消息体。
    @Override
    public void onNotifyMessageArrived(Context context, NotificationMessage message) {
        Log.e(TAG,"[onNotifyMessageArrived] "+message);
//        if(null != message){
//
//            Intent intent = new Intent(context, WebViewActivity.class);
//            Bundle bundle = new Bundle();
//            // TODO: 历史告警页面的url需要前端设置路径，存储到sharePreference中，此处去取
//            //bundle.putString("url","http://www.baidu.com");
//            SpUtils.setSpFileName(Constants.PREF_NAME_USERINFO);
//            //基于之前IOS已经使用过极光推送，使用的key字段为historyAlarmUrl
//            String historyUrl = SpUtils.getString(context,  "historyAlarmUrl");
//            if (!TextUtils.isEmpty(historyUrl)){
//                bundle.putString("url", historyUrl);
//            }else {
//                AFLog.e(TAG," 推送消息跳转页面url没有设置 ");
//            }
//            intent.putExtras(bundle);
//
//            String title = message.notificationTitle;
//            String content = message.notificationContent;
//            NotificationUtils.getInstance(context).setNotificationTitle(title);
//            NotificationUtils.getInstance(context).setNotificationContent(content);
//            NotificationUtils.getInstance(context).sendNotification(intent, 2020);
//
//        }else {
//            Log.e(TAG, "消息为空");
//        }

    }

    @Override
    public void onNotifyMessageDismiss(Context context, NotificationMessage message) {
        Log.e(TAG,"[onNotifyMessageDismiss] "+message);
    }

    @Override
    public void onRegister(Context context, String registrationId) {
        Log.e(TAG,"[onRegister] "+registrationId);
    }

    @Override
    public void onConnected(Context context, boolean isConnected) {
        Log.e(TAG,"[onConnected] "+isConnected);
    }

    @Override
    public void onCommandResult(Context context, CmdMessage cmdMessage) {
        Log.e(TAG,"[onCommandResult] "+cmdMessage);
    }



    @Override
    public void onTagOperatorResult(Context context, JPushMessage jPushMessage) {
        Log.d(TAG,"[onTagOperatorResult] "+jPushMessage);
        int sequence = jPushMessage.getSequence();
        if(jPushMessage.getErrorCode() == 0){
            if (sequence == 2020){
                ExampleUtil.showToast("设置极光推送tag成功", context);
            }else if (sequence == 2021){
                ExampleUtil.showToast("清除极光推送tag成功", context);
            }
        }else{
            if (sequence == 2020){
                ExampleUtil.showToast("设置极光推送tag失败", context);
            }else if (sequence == 2021){
                ExampleUtil.showToast("清除极光推送tag失败", context);
            }
        }
        super.onTagOperatorResult(context, jPushMessage);
    }
//    @Override
//    public void onCheckTagOperatorResult(Context context,JPushMessage jPushMessage){
//        //TagAliasOperatorHelper.getInstance().onCheckTagOperatorResult(context,jPushMessage);
//        super.onCheckTagOperatorResult(context, jPushMessage);
//    }
//    @Override
//    public void onAliasOperatorResult(Context context, JPushMessage jPushMessage) {
//        //TagAliasOperatorHelper.getInstance().onAliasOperatorResult(context,jPushMessage);
//        super.onAliasOperatorResult(context, jPushMessage);
//    }
//
//    @Override
//    public void onMobileNumberOperatorResult(Context context, JPushMessage jPushMessage) {
//        //TagAliasOperatorHelper.getInstance().onMobileNumberOperatorResult(context,jPushMessage);
//        super.onMobileNumberOperatorResult(context, jPushMessage);
//    }



}