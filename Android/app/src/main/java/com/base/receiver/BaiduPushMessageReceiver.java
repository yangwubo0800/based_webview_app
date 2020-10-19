package com.base.receiver;

/*
 * Push消息处理receiver。请编写您需要的回调函数， 一般来说： onBind是必须的，用来处理startWork返回值；
 * onMessage用来接收透传消息； onSetTags、onDelTags、onListTags是tag相关操作的回调；
 * onNotificationClicked在通知被点击时回调； onUnbind是stopWork接口的返回值回调
 * 返回值中的errorCode，解释如下：
 * 0 - Success
 * 10001 - Network Problem
 * 10101 - Integrate Check Error
 * 30600 - Internal Server Error
 * 30601 - Method Not Allowed
 * 30602 - Request Params Not Valid
 * 30603 - Authentication Failed
 * 30604 - Quota Use Up Payment Required
 * 30605 - Data Required Not Found
 * 30606 - Request Time Expires Timeout
 * 30607 - Channel Token Timeout
 * 30608 - Bind Relation Not Found
 * 30609 - Bind Number Too Many
 * 当您遇到以上返回错误时，如果解释不了您的问题，请用同一请求的返回值requestId和errorCode联系我们追查问题。
 *
 */

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.pushservice.PushManager;
import com.base.app.JSInterface;
import com.base.app.WebViewActivity;
import com.base.constant.Constants;
import com.base.utils.JPush.ExampleUtil;
import com.base.utils.SpUtils;
import com.base.utils.log.AFLog;

import java.util.Arrays;
import java.util.List;
import com.baidu.android.pushservice.PushMessageReceiver;


public class BaiduPushMessageReceiver extends PushMessageReceiver {
    /**
     * TAG to Log
     */
    public static final String TAG = BaiduPushMessageReceiver.class.getSimpleName();

    /**
     * 调用PushManager.startWork后，sdk将对push
     * server发起绑定请求，这个过程是异步的。绑定请求的结果通过onBind返回。 如果您需要用单播推送，需要把这里获取的channel
     * id和user id上传到应用server中，再调用server接口用channel id和user id给单个手机或者用户推送。
     *
     * @param context   BroadcastReceiver的执行Context
     * @param errorCode 绑定接口返回值，0 - 成功
     * @param appid     应用id。errorCode非0时为null
     * @param userId    应用user id。errorCode非0时为null
     * @param channelId 应用channel id。errorCode非0时为null
     * @param requestId 向服务端发起的请求id。在追查问题时有用；
     * @return none
     */
    @Override
    public void onBind(Context context, int errorCode, String appid,
                       String userId, String channelId, String requestId) {
        String responseString = "onBind errorCode=" + errorCode + " appid="
                + appid + " userId=" + userId + " channelId=" + channelId
                + " requestId=" + requestId;
        Log.d(TAG, responseString);

        if (errorCode == 0) {
            // 绑定成功
            Log.d(TAG, "绑定成功");
        }

    }

    /**
     * 接收透传消息的函数。
     *
     * @param context             上下文
     * @param message             推送的消息
     * @param customContentString 自定义内容,为空或者json字符串
     */
    @Override
    public void onMessage(Context context, String message,
                          String customContentString) {
        String messageString = "透传消息 onMessage=\"" + message
                + "\" customContentString=" + customContentString;
        Log.d(TAG, messageString);

    }

    /**
     * 接收通知到达的函数。
     *
     * @param context             上下文
     * @param title               推送的通知的标题
     * @param description         推送的通知的描述
     * @param customContentString 自定义内容，为空或者json字符串
     */

    @Override
    public void onNotificationArrived(Context context, String title,
                                      String description, String customContentString) {

        String notifyString = "通知到达 onNotificationArrived  title=\"" + title
                + "\" description=\"" + description + "\" customContent="
                + customContentString;
        Log.d(TAG, notifyString);

    }

    @Override
    public void onUnbind(Context context, int errorCode, String requestId) {
        String responseString = "onUnbind errorCode=" + errorCode
                + " requestId = " + requestId;
        Log.d(TAG, responseString);

        if (errorCode == 0) {
            // 解绑定成功
            Log.d(TAG, "解绑成功");
        }

    }

    /**
     * setTags() 的回调函数。
     *
     * @param context     上下文
     * @param errorCode   错误码。0表示某些tag已经设置成功；非0表示所有tag的设置均失败。
     * @param successTags 设置成功的tag
     * @param failTags    设置失败的tag
     * @param requestId   分配给对云推送的请求的id
     */
    @Override
    public void onSetTags(Context context, int errorCode,
                          List<String> successTags, List<String> failTags, String requestId) {
        String responseString = "onSetTags errorCode=" + errorCode
                + " successTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId;
        Log.d(TAG, responseString);
        if (errorCode == 0){
            ExampleUtil.showToast("设置百度云消息推送tag成功 successTags="+successTags, context);
        }else {
            ExampleUtil.showToast("设置百度云消息推送tag失败 failTags="+failTags, context);
        }


    }

    /**
     * delTags() 的回调函数。
     *
     * @param context     上下文
     * @param errorCode   错误码。0表示某些tag已经删除成功；非0表示所有tag均删除失败。
     * @param successTags 成功删除的tag
     * @param failTags    删除失败的tag
     * @param requestId   分配给对云推送的请求的id
     */
    @Override
    public void onDelTags(Context context, int errorCode,
                          List<String> successTags, List<String> failTags, String requestId) {
        String responseString = "onDelTags errorCode=" + errorCode
                + " successTags=" + successTags + " failTags=" + failTags
                + " requestId=" + requestId;
        Log.d(TAG, responseString);
        if (errorCode == 0){
            ExampleUtil.showToast("清除百度云消息推送tag成功 successTags="+successTags, context);
            if ("set".equals(JSInterface.mBaiduPushOperate)){
                if (!TextUtils.isEmpty(JSInterface.mBaiduPushTags)){
                    String[] tagStr = JSInterface.mBaiduPushTags.split(",");
                    List<String> list = Arrays.asList(tagStr);
                    PushManager.setTags(context, list);
                }
            }else if ("clean".equals(JSInterface.mBaiduPushOperate)){
                //do nothing
            }
        }else {
            ExampleUtil.showToast("清除百度云消息推送tag失败 failTags="+failTags, context);
        }

    }

    /**
     * listTags() 的回调函数。
     *
     * @param context   上下文
     * @param errorCode 错误码。0表示列举tag成功；非0表示失败。
     * @param tags      当前应用设置的所有tag。
     * @param requestId 分配给对云推送的请求的id
     */
    @Override
    public void onListTags(Context context, int errorCode, List<String> tags,
                           String requestId) {
        String responseString = "onListTags errorCode=" + errorCode + " tags="
                + tags;
        Log.d(TAG, responseString);
        // TODO: 前端调用清理或者设置tags实际上是启动了获取tag集合，然后此处再调用删除，最后设置
        if (errorCode == 0){
            ExampleUtil.showToast("获取百度云消息推送tag成功 tags="+tags, context);
            if (null != tags){
                if ("set".equals(JSInterface.mBaiduPushOperate) ||
                        "clean".equals(JSInterface.mBaiduPushOperate)){
                    PushManager.delTags(context, tags);
                }
            }else {
                //如果没有要清理的tag则直接设置
                if ("set".equals(JSInterface.mBaiduPushOperate)){
                    if (!TextUtils.isEmpty(JSInterface.mBaiduPushTags)){
                        String[] tagStr = JSInterface.mBaiduPushTags.split(",");
                        List<String> list = Arrays.asList(tagStr);
                        PushManager.setTags(context, list);
                    }
                }
            }
        }else {
            ExampleUtil.showToast("获取百度云消息推送tag失败 tags="+tags, context);
        }

    }


    /**
     * 接收通知点击的函数。
     *
     * @param context             上下文
     * @param title               推送的通知的标题
     * @param description         推送的通知的描述
     * @param customContentString 自定义内容，为空或者json字符串
     */
    @Override
    public void onNotificationClicked(Context context, String title,
                                      String description, String customContentString) {
        String notifyString = "通知点击 onNotificationClicked title=\"" + title + "\" description=\""
                + description + "\" customContent=" + customContentString;
        Log.d(TAG, notifyString);


        try{
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
        }catch (Exception e){
            e.printStackTrace();
        }

    }


}