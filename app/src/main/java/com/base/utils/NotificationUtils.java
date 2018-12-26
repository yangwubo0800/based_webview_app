package com.base.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.base.app.WebViewActivity;
import com.base.app.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtils {

    private static final String TAG = "NotificationUtils";
    private static NotificationUtils mInstance;
    private Context mContext;
    private NotificationCompat.Builder mBuilder;
    //Android O 需要先创建通知渠道
    private String NOTIFICATION_CHANNEL = "Notification_channel";
    private String NOTIFICATION_NAME = "Notification_name";
    // 通知ID
    private int NOTIFICATION_ID = 110;

    //通知标题
//    private String NOTIFICATION_TITLE = "Notification Title";
//    //通知内容
//    private String NOTIFICATION_CONTENT = "Notification Content";
    //通知到达时显示在状态栏的文字
    private String NOTIFICATION_TICKER = "Notification Ticker";
    // 动态改变的通知标题和内容
    private String mNotificationTitle;
    private String mNotificationContent;




    // 构造函数，创建通知构造者对象
    private NotificationUtils(Context context){
        this.mContext = context;
        //创建notification
        // TODO: 考虑兼容低版本安卓，使用 NotificationCompat, 以及8.0之后的 channel概念
        if (Build.VERSION.SDK_INT >= 26) {
            Log.d(TAG,"========NotificationUtils NotificationChannel");
            // 创建通知渠道
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(
                    NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            // 创建该渠道下的builder对象
            mBuilder = new NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL);
        } else {
            Log.d(TAG,"========NotificationUtils api < 26");
            mBuilder = new NotificationCompat.Builder(mContext);
        }

    }

     //使用饿汉模式来实例化类，以及其中的成员变量
    public static NotificationUtils getInstance(Context context) {
        Log.d(TAG,"getInstance mInstance="+mInstance);
        if (null == mInstance) {
            synchronized (NotificationUtils.class) {
                if (null == mInstance) {
                    mInstance = new NotificationUtils(context);
                }
            }
        }

        return mInstance;
    }

    /**
     * 提供外部使用发送通知
     */
    public void sendNotification(Intent intent, int notificationId) {

        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= getInstance(mContext).mBuilder;
        //震动时间配置[不震，震，不震 ,震，...]
        long[] vibrate = new long[]{0, 1500, 500, 1500, 500};
        // TODO: 需要定义通知跳转后的界面
        //点击通知后的动作
        if (null == intent) {
            intent = new Intent(mContext, WebViewActivity.class);
        }
        PendingIntent pdIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //系统默认通知音
        Uri defaultRingtone = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION);
        //大图标
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.notification);

        builder.setContentTitle(getNotificationTitle())
                .setContentText(getNotificationContent())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentIntent(pdIntent)
                .setAutoCancel(true)
                .setTicker(NOTIFICATION_TICKER)
                //.setWhen(System.currentTimeMillis())
                //.setDefaults(Notification.DEFAULT_ALL);
                .setVibrate(vibrate)
                .setLights(0xFF0000,2000, 2000)
                .setSound(defaultRingtone);

        notifyManager.notify(notificationId, builder.build());
    }

    /**
     * 提供给告警service使用，使其变为前台服务
     */
    public Notification getNotification(Intent intent) {

        NotificationCompat.Builder builder= getInstance(mContext).mBuilder;
        //震动时间配置[不震，震，不震 ,震，...]
        long[] vibrate = new long[]{0, 1500, 500, 1500, 500};
        // TODO: 需要定义通知跳转后的界面
        //点击通知后的动作
        if (null == intent) {
            intent = new Intent(mContext, WebViewActivity.class);
        }
        PendingIntent pdIntent = PendingIntent.getActivity(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        //系统默认通知音
        Uri defaultRingtone = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION);
        //大图标
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_app_logo);

        builder.setContentTitle("BaseWebview 云平台")
                .setContentText("正在获取后台通知")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIcon)
                .setContentIntent(null)
                .setAutoCancel(true)
                .setTicker(NOTIFICATION_TICKER);

        return builder.build();
    }

    /**
     * 清除通知显示
     *
     */
    public void clearNotification() {
        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        //只能清除自己应用的
        notifyManager.cancelAll();
    }

    /**
     * 设置和获取通知 标题 内容
     * @param title
     */
    public void setNotificationTitle(String title) {
        this.mNotificationTitle = title;
    }

    public String getNotificationTitle() {
        return this.mNotificationTitle;
    }

    public void setNotificationContent(String content) {
        this.mNotificationContent = content;
    }

    public String getNotificationContent() {
        return this.mNotificationContent;
    }


    /**
     * 使用自定义布局发送通知
     */
    public void sendCustomNotification() {
        NotificationManager notifyManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= getInstance(mContext).mBuilder;
        //震动时间配置[不震，震，不震 ,震，...]
        long[] vibrate = new long[]{0, 1500, 500, 1500, 500};

        //系统默认通知音
        Uri defaultRingtone = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION);
        //大图标
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher_background);

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.customer_notitfication_layout);
        remoteViews.setTextViewText(R.id.title, "custom notification title");
        remoteViews.setTextViewText(R.id.text, "custom notification text");
        //set pending intent for image icon
        Intent intent = new Intent(mContext, WebViewActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.icon, pendingIntent);
        builder.setCustomContentView(remoteViews);

        builder.setSound(defaultRingtone)
                .setVibrate(vibrate)
                //自定义通知一定要设置这个，不然会报异常
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis());

        notifyManager.notify(NOTIFICATION_ID, builder.build());

    }


}