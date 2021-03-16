package com.base.app.speechrecognize.baidu.recog.listener;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.base.constant.Constants;
import com.base.app.speechrecognize.baidu.recog.RecogResult;
import com.baidu.speech.asr.SpeechConstant;

/**
 * Created by fujiayi on 2017/6/16.
 */

public class MessageStatusRecogListener extends StatusRecogListener {
    private Handler handler;

    private long speechEndTime = 0;

    private boolean needTime = true;

    private static final String TAG = "MesStatusRecogListener";

    // 为了记录长语音识别模式下，最终的返回结果
    private StringBuffer mSpeechResult = new StringBuffer();

    public MessageStatusRecogListener(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void onAsrReady() {
        super.onAsrReady();
        speechEndTime = 0;
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_WAKEUP_READY, "引擎就绪，可以开始说话。");
        // TODO: 识别开始时间发送给前端，方便界面开始显示图标 add by willow
        // 每次调用将结果清空
        mSpeechResult.setLength(0);
        if (null != handler){
            Log.d(TAG,"#####onAsrReady");
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_SHOW_TOAST;
            msg.obj = "百度语音识别，请开始说话";
            handler.sendMessage(msg);

            // 开始事件发送给前端
            Message speechBegin = new Message();
            speechBegin.what = Constants.MSG_FOR_SPEECH_BEGIN;
            handler.sendMessage(speechBegin);
        }
    }

    @Override
    public void onAsrBegin() {
        super.onAsrBegin();
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN, "检测到用户说话");
    }

    @Override
    public void onAsrEnd() {
        super.onAsrEnd();
        speechEndTime = System.currentTimeMillis();
        sendMessage("【asr.end事件】检测到用户说话结束");
        // TODO：由于使用了长语音识别模式，百度在识别过程中系统会判断断句，调用此接口，所以不能在此处返回识别结束，将其改为调用结束接口后返回
//        if (null != handler){
//            Log.d(TAG,"#####onAsrEnd");
//            Message msg = new Message();
//            msg.what = Constants.MSG_FOR_SHOW_TOAST;
//            msg.obj = "百度语音识别结束";
//            handler.sendMessage(msg);
//
//            Message speechEnd = new Message();
//            speechEnd.what = Constants.MSG_FOR_SPEECH_END;
//            handler.sendMessage(speechEnd);
//        }
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL,
                "临时识别结果，结果是“" + results[0] + "”；原始json：" + recogResult.getOrigalJson());
        super.onAsrPartialResult(results, recogResult);
    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        super.onAsrFinalResult(results, recogResult);
        String message = "识别结束，结果是”" + results[0] + "”";
        // TODO: 此处是每次识别结束的回调，并且获取到了此次最终的识别结果， 需要做处理将结果返回给前端 add by willow
        Log.d(TAG,"#####onAsrFinalResult");
        // 将每次的结果追加进来
        mSpeechResult.append(results[0]);
//        if (null != handler){
//            Message msg = new Message();
//            msg.what = Constants.MSG_FOR_SPEECH_TEXT;
//            msg.obj = results[0];
//            handler.sendMessage(msg);
//        }
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL,
                message + "；原始json：" + recogResult.getOrigalJson());
        if (speechEndTime > 0) {
            long currentTime = System.currentTimeMillis();
            long diffTime = currentTime - speechEndTime;
            message += "；说话结束到识别结束耗时【" + diffTime + "ms】" + currentTime;

        }
        speechEndTime = 0;
        sendMessage(message, status, true);
    }

    @Override
    public void onAsrFinishError(int errorCode, int subErrorCode, String descMessage,
                                 RecogResult recogResult) {
        super.onAsrFinishError(errorCode, subErrorCode, descMessage, recogResult);
        String message = "【asr.finish事件】识别错误, 错误码：" + errorCode + " ," + subErrorCode + " ; " + descMessage;
        // TODO: 此处是每次识别错误，并结束的回调， 需要做处理将结果返回为空给前端， 效果和科大讯飞保持一致 add by willow
        Log.d(TAG,"#####onAsrFinishError");
        if (null != handler){
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_SPEECH_TEXT;
            msg.obj = "";
            handler.sendMessage(msg);

            // 结束事件发送给前端
            Message speechEnd = new Message();
            speechEnd.what = Constants.MSG_FOR_SPEECH_END;
            handler.sendMessage(speechEnd);
        }
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL, message);
        if (speechEndTime > 0) {
            long diffTime = System.currentTimeMillis() - speechEndTime;
            message += "。说话结束到识别结束耗时【" + diffTime + "ms】";
        }
        speechEndTime = 0;
        sendMessage(message, status, true);
        speechEndTime = 0;
    }

    @Override
    public void onAsrOnlineNluResult(String nluResult) {
        super.onAsrOnlineNluResult(nluResult);
        if (!nluResult.isEmpty()) {
            sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL, "原始语义识别结果json：" + nluResult);
        }
    }

    @Override
    public void onAsrFinish(RecogResult recogResult) {
        super.onAsrFinish(recogResult);
        Log.d(TAG,"#####onAsrFinish");
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_FINISH, "识别一段话结束。如果是长语音的情况会继续识别下段话。");

    }

    /**
     * 长语音识别结束
     */
    @Override
    public void onAsrLongFinish() {
        super.onAsrLongFinish();
        Log.d(TAG,"#####onAsrLongFinish");
        if (null != handler){
            // 返回最终识别结果
            Message text = new Message();
            text.what = Constants.MSG_FOR_SPEECH_TEXT;
            text.obj = mSpeechResult.toString();
            handler.sendMessage(text);

            // toast提示
            Message toast = new Message();
            toast.what = Constants.MSG_FOR_SHOW_TOAST;
            toast.obj = "百度长语音识别结束";
            handler.sendMessage(toast);

            // TODO：由于使用了长语音识别模式，将结束事件改为此处为准
            Message speechEnd = new Message();
            speechEnd.what = Constants.MSG_FOR_SPEECH_END;
            handler.sendMessage(speechEnd);
        }
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_LONG_SPEECH, "长语音识别结束。");
    }


    /**
     * 使用离线命令词时，有该回调说明离线语法资源加载成功
     */
    @Override
    public void onOfflineLoaded() {
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_LOADED, "离线资源加载成功。没有此回调可能离线语法功能不能使用。");
    }

    /**
     * 使用离线命令词时，有该回调说明离线语法资源加载成功
     */
    @Override
    public void onOfflineUnLoaded() {
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_UNLOADED, "离线资源卸载成功。");
    }

    @Override
    public void onAsrExit() {
        super.onAsrExit();
        sendStatusMessage(SpeechConstant.CALLBACK_EVENT_ASR_EXIT, "识别引擎结束并空闲中");
    }

    private void sendStatusMessage(String eventName, String message) {
        message = "[" + eventName + "]" + message;
        sendMessage(message, status);
    }

    private void sendMessage(String message) {
        sendMessage(message, WHAT_MESSAGE_STATUS);
    }

    private void sendMessage(String message, int what) {
        sendMessage(message, what, false);
    }


    private void sendMessage(String message, int what, boolean highlight) {


        if (needTime && what != STATUS_FINISHED) {
            message += "  ;time=" + System.currentTimeMillis();
        }
        if (handler == null) {
            Log.i(TAG, message);
            return;
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = status;
        if (highlight) {
            msg.arg2 = 1;
        }
        msg.obj = message + "\n";
        handler.sendMessage(msg);
    }
}
