package com.base.service;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.base.app.WebViewActivity;
import com.base.constant.Constants;
import com.base.utils.JPush.ExampleUtil;
import com.base.utils.NotificationUtils;
import com.base.utils.SpUtils;
import com.base.utils.log.AFLog;
import com.igexin.sdk.GTIntentService;
import com.igexin.sdk.PushConsts;
import com.igexin.sdk.PushManager;
import com.igexin.sdk.message.BindAliasCmdMessage;
import com.igexin.sdk.message.FeedbackCmdMessage;
import com.igexin.sdk.message.GTCmdMessage;
import com.igexin.sdk.message.GTNotificationMessage;
import com.igexin.sdk.message.GTTransmitMessage;
import com.igexin.sdk.message.SetTagCmdMessage;
import com.igexin.sdk.message.UnBindAliasCmdMessage;

/**
 * 继承 GTIntentService 接收来自个推的消息, 所有消息在线程中回调, 如果注册了该服务, 则务必要在 AndroidManifest中声明, 否则无法接受消息<br>
 * onReceiveMessageData 处理透传消息<br>
 * onReceiveClientId 接收 cid <br>
 * onReceiveOnlineState cid 离线上线通知 <br>
 * onReceiveCommandResult 各种事件处理回执 <br>
 */

public class GTPushIntentService extends GTIntentService {

    private static final String TAG = "GTPushIntentService";

    /**
     * 为了观察透传数据变化.
     */
    private static int cnt;

    @Override
    public void onReceiveServicePid(Context context, int pid) {
        Log.d(TAG, "onReceiveServicePid -> " + pid);
    }

    @Override
    public void onReceiveMessageData(Context context, GTTransmitMessage msg) {
        String appid = msg.getAppid();
        String taskid = msg.getTaskId();
        String messageid = msg.getMessageId();
        byte[] payload = msg.getPayload();
        String pkg = msg.getPkgName();
        String cid = msg.getClientId();

        // 第三方回执调用接口，actionid范围为90000-90999，可根据业务场景执行
        boolean result = PushManager.getInstance().sendFeedbackMessage(context, taskid, messageid, 90001);
        Log.d(TAG, "call sendFeedbackMessage = " + (result ? "success" : "failed"));

        Log.d(TAG, "onReceiveMessageData -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nmessageid = " + messageid + "\npkg = " + pkg
                + "\ncid = " + cid);

        try{
            if (payload == null) {
                Log.e(TAG, "receiver payload = null");
            } else {
                String data = new String(payload);
                Log.d(TAG, "receiver payload = " + data);
                JSONObject dataObj = JSON.parseObject(data);
                String title = dataObj.getString("title");
                String body = dataObj.getString("body");

                Intent intent = new Intent(context, WebViewActivity.class);
                Bundle bundle = new Bundle();
                // TODO: 历史告警页面的url需要前端设置路径，存储到sharePreference中，此处去取
                //bundle.putString("url","http://www.baidu.com");
                SpUtils.setSpFileName(Constants.PREF_NAME_USERINFO);
                //基于之前IOS已经使用过极光推送，使用的key字段为historyAlarmUrl
                String historyUrl = SpUtils.getString(context,  "historyAlarmUrl");
                if (!TextUtils.isEmpty(historyUrl)){
                    bundle.putString("url", historyUrl);
                }else {
                    AFLog.e(TAG," 推送消息跳转页面url没有设置 ");
                }
                intent.putExtras(bundle);

                NotificationUtils.getInstance(context).setNotificationTitle(title);
                NotificationUtils.getInstance(context).setNotificationContent(body);
                NotificationUtils.getInstance(context).sendNotification(intent, 2020);

            }
        }catch (Exception e){

        }

    }

    @Override
    public void onReceiveClientId(Context context, String clientid) {
        Log.e(TAG, "onReceiveClientId -> " + "clientid = " + clientid);

//        sendMessage(clientid, DemoApplication.DemoHandler.RECEIVE_CLIENT_ID);
    }

    @Override
    public void onReceiveOnlineState(Context context, boolean online) {
        Log.d(TAG, "onReceiveOnlineState -> " + (online ? "online" : "offline"));
//        sendMessage(String.valueOf(online), DemoApplication.DemoHandler.RECEIVE_ONLINE_STATE);
    }

    @Override
    public void onReceiveCommandResult(Context context, GTCmdMessage cmdMessage) {
        Log.d(TAG, "onReceiveCommandResult -> " + cmdMessage);

        int action = cmdMessage.getAction();

        if (action == PushConsts.SET_TAG_RESULT) {
            setTagResult((SetTagCmdMessage) cmdMessage);
        } else if (action == PushConsts.BIND_ALIAS_RESULT) {
            bindAliasResult((BindAliasCmdMessage) cmdMessage);
        } else if (action == PushConsts.UNBIND_ALIAS_RESULT) {
            unbindAliasResult((UnBindAliasCmdMessage) cmdMessage);
        } else if ((action == PushConsts.THIRDPART_FEEDBACK)) {
            feedbackResult((FeedbackCmdMessage) cmdMessage);
        }
    }

    @Override
    public void onNotificationMessageArrived(Context context, GTNotificationMessage message) {
        Log.d(TAG, "onNotificationMessageArrived -> " + "appid = " + message.getAppid() + "\ntaskid = " + message.getTaskId() + "\nmessageid = "
                + message.getMessageId() + "\npkg = " + message.getPkgName() + "\ncid = " + message.getClientId() + "\ntitle = "
                + message.getTitle() + "\ncontent = " + message.getContent());
    }

    @Override
    public void onNotificationMessageClicked(Context context, GTNotificationMessage message) {
        Log.d(TAG, "onNotificationMessageClicked -> " + "appid = " + message.getAppid() + "\ntaskid = " + message.getTaskId() + "\nmessageid = "
                + message.getMessageId() + "\npkg = " + message.getPkgName() + "\ncid = " + message.getClientId() + "\ntitle = "
                + message.getTitle() + "\ncontent = " + message.getContent());

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

                intent.putExtras(bundle);
                context.startActivity(intent);
            }else {
                AFLog.e(TAG, "消息体为空");
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setTagResult(SetTagCmdMessage setTagCmdMsg) {
        String sn = setTagCmdMsg.getSn();
        String code = setTagCmdMsg.getCode();

        if ("0".equals(code)){
            ExampleUtil.showToast("个推设置tag成功",GTPushIntentService.this);
        }else {
            ExampleUtil.showToast("个推设置tag失败 code="+code,GTPushIntentService.this);
        }

    }

    private void bindAliasResult(BindAliasCmdMessage bindAliasCmdMessage) {
        String sn = bindAliasCmdMessage.getSn();
        String code = bindAliasCmdMessage.getCode();


    }

    private void unbindAliasResult(UnBindAliasCmdMessage unBindAliasCmdMessage) {
        String sn = unBindAliasCmdMessage.getSn();
        String code = unBindAliasCmdMessage.getCode();

    }


    private void feedbackResult(FeedbackCmdMessage feedbackCmdMsg) {
        String appid = feedbackCmdMsg.getAppid();
        String taskid = feedbackCmdMsg.getTaskId();
        String actionid = feedbackCmdMsg.getActionId();
        String result = feedbackCmdMsg.getResult();
        long timestamp = feedbackCmdMsg.getTimeStamp();
        String cid = feedbackCmdMsg.getClientId();

        Log.d(TAG, "onReceiveCommandResult -> " + "appid = " + appid + "\ntaskid = " + taskid + "\nactionid = " + actionid + "\nresult = " + result
                + "\ncid = " + cid + "\ntimestamp = " + timestamp);
    }

    private void sendMessage(String data, int what) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = data;
    }
}