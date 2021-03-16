package com.base.app.speechrecognize.baidu;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.base.constant.Constants;
import com.base.app.speechrecognize.baidu.recog.listener.IRecogListener;
import com.base.app.speechrecognize.baidu.recog.listener.MessageStatusRecogListener;
import com.base.app.speechrecognize.baidu.recog.listener.RecogEventAdapter;



public class Baidu {
    private static String TAG = Baidu.class.getSimpleName();
    //上下文参数
    private Context mContext;
    // 消息处理，传给主界面的
    private Handler mHandler;
    // 单例模式
    private static Baidu mInstance;

    /**
     * SDK 内部核心 EventManager 类
     */
    private EventManager asr;

    // SDK 内部核心 事件回调类， 用于开发者写自己的识别回调逻辑
    private EventListener eventListener;


    //构造函数
    Baidu(Context context, Handler handler){
        mContext = context;
        mHandler = handler;

        //百度语音识别实例
        // 基于DEMO集成第1.1, 1.2, 1.3 步骤 初始化EventManager类并注册自定义输出事件
        // DEMO集成步骤 1.2 新建一个回调类，识别引擎会回调这个类告知重要状态和识别结果
        IRecogListener listener = new MessageStatusRecogListener(mHandler);
        //使用RecogEventAdapter 适配EventListener
        eventListener = new RecogEventAdapter(listener);
        // SDK集成步骤 初始化asr的EventManager示例，多次得到的类，只能选一个使用
        asr = EventManagerFactory.create(context, "asr");
        // SDK集成步骤 设置回调event， 识别引擎会回调这个类告知重要状态和识别结果
        asr.registerListener(eventListener);

    }


    public static Baidu getInstance(Context context, Handler handler){
        if (null == mInstance){
            synchronized (Baidu.class){
                if (null == mInstance){
                    mInstance =  new Baidu(context, handler);
                }
            }
        }
        return  mInstance;
    }


    // 释放单例对象
    public static void releaseInstance (){
        if (null != mInstance){
            mInstance = null;
        }
    }


    // 开始录音识别
    public void startRecord(String jsonParam){
        //检查参数
        //默认使用中文普通话识别， 长语音模式
        String json ="{\"pid\":15372,\"vad.endpoint-timeout\":0}";
        if (TextUtils.isEmpty(jsonParam)){
            Log.d(TAG, "参数设置为空，使用默认值");
        }else {
            // 直接使用json 字符串进行设置，由于百度需要训练集才支持标点符号识别，静音超时设置也还不支持，故此处指需要设置语音pid
            json = jsonParam;
        }

        // TODO: 确认百度设置参数是否有效
//        String json ="{\"accept-audio-data\":false,\"DISABLE_PUNCTUATION\":false,\"accept-audio-volume\":true," +
//                "\"pid\":15372,\"vad.endpoint-timeout\":5000,\"PUNCTUATION_MODE\":2}";
        // 长语音参数
        //String json ="{\"pid\":15372,\"vad.endpoint-timeout\":0}";
        // 短语音参数
        //String json ="{\"pid\":15372}";

        Log.e(TAG, "startRecord json="+json);
        if (null != asr){
            Log.d(TAG, "ASR_START 发送启动百度语音识别事件");
            asr.send(SpeechConstant.ASR_START, json, null, 0, 0);
        }else {
            Log.e(TAG, "开始百度识别，对象未初始化");
        }
    }

    // 停止录音识别
    public void stopRecord(){
        if (null != asr){
            Log.d(TAG, "ASR_STOP 发送停止百度语音识别事件");
            asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
        }else {
            Log.e(TAG, "停止百度识别，对象未初始化");
        }
    }


}
