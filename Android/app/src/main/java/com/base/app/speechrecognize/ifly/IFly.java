package com.base.app.speechrecognize.ifly;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.base.constant.Constants;
import com.base.utils.SpUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;


public class IFly {

    private static String TAG = IFly.class.getSimpleName();
    //上下文参数
    private Context mContext;
    // 消息处理
    private Handler mHandler;
    // 语音听写对象
    private SpeechRecognizer mIat;
    // 单例模式
    private static IFly mInstance;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private StringBuffer buffer = new StringBuffer();
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    //默认语言类型
    private String language="zh_cn";
    //返回数据格式
    private String resultType = "json";

    // 获取单例
    public static IFly  getInstance(Context context, Handler handler){
        //单例模式
        if (null == mInstance){
            synchronized (IFly.class){
                if(null == mInstance){
                    mInstance = new IFly(context, handler);
                }
            }
        }
        return mInstance;
    }

    // 释放单例
    public static void releaseInstance(){
        if (null != mInstance){
            mInstance = null;
        }
    }

    // 开始录音识别
    public void startRecord(String jsonParam){
        // 不显示听写对话框
        Message msg = new Message();
        msg.what = Constants.MSG_FOR_SHOW_TOAST;
        String toast;

        if (null != mIat){
            //清空变量
            mIatResults.clear();
            buffer.setLength(0);
            // 设置参数
            setParam(jsonParam);
            int ret = mIat.startListening(mRecognizerListener);
            if (ret != ErrorCode.SUCCESS) {
                toast = "听写失败,错误码：" + ret+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案";
            } else {
                toast = "请开始说话…";
            }
        }else {
            toast = "设备未初始化";
        }

        // 显示toast 信息
        msg.obj = toast;
        if (null != mHandler && !TextUtils.isEmpty(toast)){
            mHandler.sendMessage(msg);
        }
    }

    // 停止录音识别
    public void stopRecord(){

        Message msg = new Message();
        msg.what = Constants.MSG_FOR_SHOW_TOAST;
        String toast = "设备未初始化";
        Log.d(TAG, "ifly stopRecord mIat="+mIat);
        if (null != mIat){
            // 多次停止会报错，但是不影响功能
            mIat.stopListening();
            // TODO: 发送结束识别事件, 直接调用此接口时，系统的回调不会运行 onEndOfSpeech，所以此处添加此事件返回， 配合变相长语音模式识别。
            if (null != mHandler){
                Message speechEnd = new Message();
                speechEnd.what = Constants.MSG_FOR_SPEECH_END;
                mHandler.sendMessage(speechEnd);

                toast = "讯飞语音识别停止";
                msg.obj = toast;
                mHandler.sendMessage(msg);
            }
        }else {
            // 显示toast 信息
            msg.obj = toast;
            if (null != mHandler && !TextUtils.isEmpty(toast)){
                mHandler.sendMessage(msg);
            }
        }
    }


    //构造函数
    private IFly(Context context, Handler handler){
        //构造一次，记录上下文，并创建麦克风录音识别对象
        mContext = context;
        //handler 对象赋值
        mHandler = handler;
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = SpeechRecognizer.createRecognizer(context, mInitListener);
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Message msg = new Message();
                msg.what = Constants.MSG_FOR_SHOW_TOAST;
                String toast = "初始化失败，错误码：" + code+",请点击网址https://www.xfyun.cn/document/error-code查询解决方案";
                msg.obj = toast;
                if (null != mHandler && !TextUtils.isEmpty(toast)){
                    mHandler.sendMessage(msg);
                }
            }
        }
    };


    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_SHOW_TOAST;
            msg.obj = "讯飞语音识别，请开始说话";
            if (null != mHandler){
                mHandler.sendMessage(msg);
            }
            // TODO: 需要将事件回传给前端，用于界面绘画，显示录音过程开始
            Log.d(TAG, "=====onBeginOfSpeech=====");
            Message speechBegin = new Message();
            speechBegin.what = Constants.MSG_FOR_SPEECH_BEGIN;
            if (null != mHandler){
                mHandler.sendMessage(speechBegin);
            }
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            String toast = error.getPlainDescription(true);
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_SHOW_TOAST;
            msg.obj = toast;
            if (null != mHandler){
                mHandler.sendMessage(msg);
            }
            Log.e(TAG, "=====onError=====");
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_SHOW_TOAST;
            msg.obj = "讯飞语音识别结束";
            if (null != mHandler){
                mHandler.sendMessage(msg);
            }
            // TODO: 需要将事件回传给前端，用于界面绘画，显示录音过程结束
            Log.d(TAG, "=====onEndOfSpeech=====");
            Message speechEnd = new Message();
            speechEnd.what = Constants.MSG_FOR_SPEECH_END;
            if (null != mHandler){
                mHandler.sendMessage(speechEnd);
            }
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if (resultType.equals("json")) {
                printResult(results);
            } else if(resultType.equals("plain")) {
                buffer.append(results.getResultString());
                Log.d(TAG, "=====plain mSpeechText="+buffer.toString());
                Message msg = new Message();
                msg.what = Constants.MSG_FOR_SPEECH_TEXT;
                msg.obj = buffer.toString();
                //mHandler.sendMessage(msg);
            }
            // TODO: 将最后的识别结果返回给前端
            if (isLast){
                Log.d(TAG, "isLast onResult");
                // TODO; 如果需要减少出发前端事件次数，可以将每次录音结果的最终结果在此处返回
                Message msgResult = new Message();
                msgResult.what = Constants.MSG_FOR_SPEECH_TEXT;
                // 使用全局buffer 来存储识别后的结果
                msgResult.obj = buffer.toString();
                mHandler.sendMessage(msgResult);
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            //Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };


    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        // 赋值给类全局变量
        buffer.setLength(0);
        buffer.append(resultBuffer);

        Log.d(TAG, "=====json mSpeechText="+buffer.toString());
        Message msg = new Message();
        msg.what = Constants.MSG_FOR_SPEECH_TEXT;
        msg.obj = buffer.toString();;
        //mHandler.sendMessage(msg);
    }


    /**
     * 参数设置
     *
     * @return
     */
    public void setParam(String jsonParam) {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, resultType);

        // 如果参数不为空，解析json 参数进行设置
        // TODO: 默认三个厂商为长语音识别方式，讯飞无此模式，故将其静音间隔设置尽量打，默认为中文普通话， 超时时间都为8秒，带有标点符号
        String language = "zh_cn";
        String accent = "mandarin";
        String vadBos = "8000";
        String vadEos = "8000";
        String ptt = "1";
        if (TextUtils.isEmpty(jsonParam)){
            // 使用默认参数
        }else {
            com.alibaba.fastjson.JSONObject object = JSON.parseObject(jsonParam);
            if (null != object){
                language = (null == object.getString("language"))? language: object.getString("language");
                accent = (null == object.getString("accent"))? accent: object.getString("accent");
                vadBos = (null == object.getString("vadBos"))? vadBos: object.getString("vadBos");
                vadEos = (null == object.getString("vadEos"))? vadEos: object.getString("vadEos");
                ptt = (null == object.getString("ptt"))? ptt: object.getString("ptt");
            }
        }

        Log.d(TAG,"set param: language="+language + " accent="+accent
        +" vadBos="+vadBos+" vadEos="+vadEos+" ptt="+ptt);

        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        mIat.setParameter(SpeechConstant.ACCENT, accent);

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, vadBos);
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, vadEos);
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, ptt);
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }


}
