package com.base.app.speechrecognize.tencent;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.base.constant.Constants;
import com.base.utils.GeneralUtils;
import com.tencent.aai.AAIClient;
import com.tencent.aai.audio.data.AudioRecordDataSource;
import com.tencent.aai.auth.AbsCredentialProvider;
import com.tencent.aai.auth.LocalCredentialProvider;
import com.tencent.aai.config.ClientConfiguration;
import com.tencent.aai.exception.ClientException;
import com.tencent.aai.exception.ServerException;
import com.tencent.aai.listener.AudioRecognizeResultListener;
import com.tencent.aai.listener.AudioRecognizeStateListener;
import com.tencent.aai.listener.AudioRecognizeTimeoutListener;
import com.tencent.aai.log.AAILogger;
import com.tencent.aai.model.AudioRecognizeRequest;
import com.tencent.aai.model.AudioRecognizeResult;
import com.tencent.aai.model.type.AudioRecognizeConfiguration;
import com.tencent.aai.model.type.AudioRecognizeTemplate;
import com.tencent.aai.model.type.EngineModelType;
import com.tencent.aai.model.type.ServerProtocol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Tencent {

    private static String TAG = Tencent.class.getSimpleName();
    //上下文参数
    private Context mContext;
    // 消息处理，传给主界面的
    private Handler mHandler;
    // 单例模式
    private static Tencent mInstance;
    // 识别实例
    AAIClient aaiClient;
    // 秘钥签名
    AbsCredentialProvider credentialProvider;
    //记录当前请求任务ID
    int currentRequestId = 0;
    final int projectId = 0;

    //构造函数
    Tencent(Context context, Handler handler){
        mContext = context;
        mHandler = handler;

        // 从配置文件读取
        Integer appId = GeneralUtils.getMetaIntValue(mContext, "com.tencent.speech.APP_ID");
        String secretId = GeneralUtils.getMetaValue(mContext, "com.tencent.speech.SECRET_ID");
        String secretKey = GeneralUtils.getMetaValue(mContext, "com.tencent.speech.SECRET_KEY");

        Log.e(TAG, "appId="+appId+" secretId="+secretId +" secretKey="+secretKey);

        credentialProvider = new LocalCredentialProvider(secretKey);
        // 用户配置
        ClientConfiguration.setServerProtocol(ServerProtocol.ServerProtocolWSS); // 选择访问协议，默认启用HTTPS 0
        ClientConfiguration.setMaxAudioRecognizeConcurrentNumber(1); // 语音识别的请求的最大并发数
        ClientConfiguration.setMaxRecognizeSliceConcurrentNumber(1); // 单个请求的分片最大并发数

        // demo 中将所有操作都放在onCreate 中了
        if (aaiClient==null) {
            try {
//                        aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId, credentialProvider);
                //sdk crash 上传
//                if (switchToDeviceAuth) {
//                    aaiClient = new AAIClient(MainActivity.this, Integer.valueOf(DemoConfig.appIdForDeviceAuth), projectId,
//                            DemoConfig.secretIdForDeviceAuth, DemoConfig.secretKeyForDeviceAuth,
//                            DemoConfig.serialNumForDeviceAuth, DemoConfig.deviceNumForDeviceAuth,
//                            credentialProvider, MainActivity.this);
//                } else {
//                    aaiClient = new AAIClient(MainActivity.this, appid, projectId, secretId,secretKey ,credentialProvider);
//                }

                aaiClient = new AAIClient(mContext, appId, projectId, secretId,secretKey ,credentialProvider);
            } catch (ClientException e) {
                e.printStackTrace();
                //AAILogger.info(logger, e.toString());
                Log.e(TAG, e.toString());
            }
        }
    }

    // 单例模式获取
    public static Tencent getInstance(Context context, Handler handler){
        if (null == mInstance){
            synchronized (Tencent.class){
                if (null == mInstance){
                    mInstance = new Tencent(context, handler);
                }
            }
        }
        return mInstance;
    }

    // 释放单例
    public static void releaseInstance(){
        Log.d(TAG, "releaseInstance");
        if (null != mInstance){
            mInstance = null;
        }
    }

    LinkedHashMap<String, String> resMap = new LinkedHashMap<>();
    private String buildMessage(Map<String, String> msg) {

        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter = msg.entrySet().iterator();
        while (iter.hasNext()) {
            String value = iter.next().getValue();
            stringBuffer.append(value+"\r\n");
        }
        return stringBuffer.toString();
    }


    // 识别结果回调监听器
    final AudioRecognizeResultListener audioRecognizeResultlistener = new AudioRecognizeResultListener() {
        boolean dontHaveResult = true;
        /**
         * 返回分片的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         * @param seq 该分片所在语音流的序号 (0, 1, 2...)
         */
        @Override
        public void onSliceSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {

            if (dontHaveResult && !TextUtils.isEmpty(result.getText())) {
                dontHaveResult = false;
                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                String time=format.format(date);
                String message = String.format("voice flow order = %d, receive first response in %s, result is = %s", seq, time, result.getText());
                Log.i(TAG, message);
            }

            resMap.put(String.valueOf(seq), result.getText());
            final String msg = buildMessage(resMap);
            Log.d(TAG, "#####onSliceSuccess msg="+msg);

        }

        /**
         * 返回语音流的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         * @param seq 该语音流的序号 (1, 2, 3...)
         */
        @Override
        public void onSegmentSuccess(AudioRecognizeRequest request, AudioRecognizeResult result, int seq) {
            dontHaveResult = true;
            resMap.put(String.valueOf(seq), result.getText());
            final String msg = buildMessage(resMap);
            Log.d(TAG, "#####onSegmentSuccess msg="+msg);
        }

        /**
         * 识别结束回调，返回所有的识别结果
         * @param request 相应的请求
         * @param result 识别结果
         */
        @Override
        public void onSuccess(AudioRecognizeRequest request, String result) {
            Log.e(TAG, "=====onSuccess result="+result);
            // TODO: 腾讯此版本SDK会多次回调，后续会优化处理，故此处需要增加处理，只返回第一次结果，后面的丢弃。
//            // 返回最终的识别结果
//            Message msgResult = new Message();
//            msgResult.what = Constants.MSG_FOR_SPEECH_TEXT;
//            // 使用全局buffer 来存储识别后的结果
//            msgResult.obj = result;
//            if (null != mHandler){
//                mHandler.sendMessage(msgResult);
//            }
        }

        /**
         * 识别失败
         * @param request 相应的请求
         * @param clientException 客户端异常
         * @param serverException 服务端异常
         */
        @Override
        public void onFailure(AudioRecognizeRequest request, final ClientException clientException, final ServerException serverException) {
            if (clientException!=null) {
                Log.e(TAG, "onFailure..:"+clientException.toString());
            }
            if (serverException!=null) {
                Log.e(TAG, "onFailure..:"+serverException.toString());
            }

            // 状态结束 并且 返回空内容
            Message msgResult = new Message();
            msgResult.what = Constants.MSG_FOR_SPEECH_TEXT;
            // 出错或者失败时返回空字符串
            msgResult.obj = "";
            if (null != mHandler){
                mHandler.sendMessage(msgResult);
            }

            Message speechEnd = new Message();
            speechEnd.what = Constants.MSG_FOR_SPEECH_END;
            if (null != mHandler){
                mHandler.sendMessage(speechEnd);
            }

        }
    };


    /**
     * 识别状态监听器
     */
    final AudioRecognizeStateListener audioRecognizeStateListener = new AudioRecognizeStateListener() {

        /**
         * 开始录音
         * @param request
         */
        @Override
        public void onStartRecord(AudioRecognizeRequest request) {
            currentRequestId = request.getRequestId();
            // TODO: 此处为本次语音识别开始的时机
            if (null != mHandler){
                Message msg = new Message();
                msg.what = Constants.MSG_FOR_SHOW_TOAST;
                msg.obj = "腾讯语音识别，请开始说话";
                mHandler.sendMessage(msg);

                Message speechBegin = new Message();
                speechBegin.what = Constants.MSG_FOR_SPEECH_BEGIN;
                mHandler.sendMessage(speechBegin);
            }
            Log.d(TAG, "onStartRecord..");
        }

        /**
         * 结束录音
         * @param request
         */
        @Override
        public void onStopRecord(AudioRecognizeRequest request) {
            Log.d(TAG, "onStopRecord..");
            // 此次会话结束
            if (null != mHandler){
                Message msg = new Message();
                msg.what = Constants.MSG_FOR_SHOW_TOAST;
                msg.obj = "腾讯语音识别结束";
                mHandler.sendMessage(msg);

                Message speechEnd = new Message();
                speechEnd.what = Constants.MSG_FOR_SPEECH_END;
                mHandler.sendMessage(speechEnd);
            }

            // 返回最终的识别结果
            Message msgResult = new Message();
            msgResult.what = Constants.MSG_FOR_SPEECH_TEXT;
            // 使用全局buffer 来存储识别后的结果
            msgResult.obj = buildMessage(resMap);
            if (null != mHandler){
                mHandler.sendMessage(msgResult);
            }
        }

        /**
         * 第seq个语音流开始识别
         * @param request
         * @param seq
         */
        @Override
        public void onVoiceFlowStartRecognize(AudioRecognizeRequest request, int seq) {
            Log.d(TAG, "onVoiceFlowStartRecognize.. seq = {}"+seq);
        }

        /**
         * 第seq个语音流结束识别
         * @param request
         * @param seq
         */
        @Override
        public void onVoiceFlowFinishRecognize(AudioRecognizeRequest request, int seq) {
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            String time=format.format(date);
            String message = String.format("voice flow order = %d, recognize finish in %s", seq, time);
            Log.i(TAG, message);
        }

        /**
         * 第seq个语音流开始
         * @param request
         * @param seq
         */
        @Override
        public void onVoiceFlowStart(AudioRecognizeRequest request, int seq) {
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            String time=format.format(date);
            String message = String.format("voice flow order = %d, start in %s", seq, time);
            Log.i(TAG, message);
        }

        /**
         * 第seq个语音流结束
         * @param request
         * @param seq
         */
        @Override
        public void onVoiceFlowFinish(AudioRecognizeRequest request, int seq) {
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            String time=format.format(date);
            String message = String.format("voice flow order = %d, stop in %s", seq, time);
            Log.i(TAG, message);
        }

        /**
         * 语音音量回调
         * @param request
         * @param volume
         */
        @Override
        public void onVoiceVolume(AudioRecognizeRequest request, final int volume) {
            //Log.i(TAG, "onVoiceVolume..");
        }

    };

    /**
     * 识别超时监听器
     */
    final AudioRecognizeTimeoutListener audioRecognizeTimeoutListener = new AudioRecognizeTimeoutListener() {

        /**
         * 检测第一个语音流超时
         * @param request
         */
        @Override
        public void onFirstVoiceFlowTimeout(AudioRecognizeRequest request) {
            Log.i(TAG, "onFirstVoiceFlowTimeout..");
        }

        /**
         * 检测下一个语音流超时
         * @param request
         */
        @Override
        public void onNextVoiceFlowTimeout(AudioRecognizeRequest request) {
            Log.i(TAG, "onNextVoiceFlowTimeout..");
        }
    };



    public void startRecord(String jsonParam){

        // TODO: 先取消之前的任务，跟demo中保持一致
        if (aaiClient!=null) {
            boolean taskExist = aaiClient.cancelAudioRecognize(currentRequestId);
            Log.d(TAG, "taskExist=" + taskExist);
            if (!taskExist) {
                Log.e(TAG, "识别状态：不存在该任务，无法取消");
            }
        }
        // TODO: 解析参数
        //引擎模型类型。
        //电话场景：
        //• 8k_en：电话 8k 英语；
        //• 8k_zh：电话 8k 中文普通话通用；
        //• 8k_zh_finance：电话 8k 金融领域模型；
        //非电话场景：
        //• 16k_zh：16k 中文普通话通用；
        //• 16k_en：16k 英语；
        //• 16k_ca：16k 粤语；
        //• 16k_ko：16k 韩语；
        //• 16k_zh-TW：16k 中文普通话繁体；
        //• 16k_ja：16k 日语；
        //• 16k_wuu-SH：16k 上海话方言；
        //• 16k_zh_medical 医疗；
        //• 16k_en_game 英文游戏；
        //• 16k_zh_court 法庭；
        //• 16k_en_edu 英文教育；
        //• 16k_zh_edu 中文教育；
        //• 16k_th 泰语。
        // 语言引擎， 默认为中文普通话
        String language = "16k_zh";
        // 是否开启静音检测, 默认不使用，都用长语音方式
        String vad = "0";
        // 静音检测超时时间
        String vadTimeout = "5000";
        // 是否需要标点符号， 默认为都带有标点
        String punc = "0";
        if (!TextUtils.isEmpty(jsonParam)){
            com.alibaba.fastjson.JSONObject object = JSON.parseObject(jsonParam);
            if (null != object){
                language = (null == object.getString("language"))? language: object.getString("language");
                vad = (null == object.getString("vad"))? vad: object.getString("vad");
                vadTimeout = (null == object.getString("vadTimeout"))? vadTimeout: object.getString("vadTimeout");
                punc = (null == object.getString("punc"))? punc: object.getString("punc");
            }
        }
        Log.d(TAG, "=====startRecord language="+language+" vad="+vad+" vadTimeout="+vadTimeout
        +" punc="+punc);
        int puncValue = 0;
        try{
            puncValue = Integer.valueOf(punc);
        }catch (NumberFormatException e){
            e.printStackTrace();
        }
        resMap.clear();
        AudioRecognizeRequest.Builder builder = new AudioRecognizeRequest.Builder();
        //File file = new File(Environment.getExternalStorageDirectory()+"/tencent_aai____/audio", "1.pcm");
        boolean isSaveAudioRecordFiles=false;//默认是关的 false
        // 初始化识别请求
        final AudioRecognizeRequest audioRecognizeRequest = builder
//                        .pcmAudioDataSource(new AudioRecordDataSource()) // 设置数据源
                .pcmAudioDataSource(new AudioRecordDataSource(isSaveAudioRecordFiles)) // 设置数据源
                //.templateName(templateName) // 设置模板
                //.template(new AudioRecognizeTemplate(EngineModelType.EngineModelType16K.getType(),0,0)) // 设置自定义模板
                //支持动态设置语音引擎
                .template(new AudioRecognizeTemplate(language,0,0))
                .setFilterDirty(0)  // 0 ：默认状态 不过滤脏话 1：过滤脏话
                .setFilterModal(0) // 0 ：默认状态 不过滤语气词  1：过滤部分语气词 2:严格过滤
                .setFilterPunc(puncValue) // 0 ：默认状态 不过滤句末的句号 1：滤句末的句号
                .setConvert_num_mode(1) //1：默认状态 根据场景智能转换为阿拉伯数字；0：全部转为中文数字。
//                        .setVadSilenceTime(1000) // 语音断句检测阈值，静音时长超过该阈值会被认为断句（多用在智能客服场景，需配合 needvad = 1 使用） 默认不传递该参数
//                        .setNeedvad(0) //0：关闭 vad，1：默认状态 开启 vad。
//                        .setHotWordId("")//热词 id。用于调用对应的热词表，如果在调用语音识别服务时，不进行单独的热词 id 设置，自动生效默认热词；如果进行了单独的热词 id 设置，那么将生效单独设置的热词 id。
                .build();

        // 自定义识别配置
        boolean vadOn = ("1".equals(vad))? true: false;
        int vadTimeoutValue = 5000;
        try {
             vadTimeoutValue = Integer.valueOf(vadTimeout);
        }catch (NumberFormatException e){
            Log.d(TAG, "NumberFormatException json param is wrong format ");
        }
        Log.d(TAG, "vadOn="+vadOn+" vadTimeoutValue="+vadTimeoutValue);
        final AudioRecognizeConfiguration audioRecognizeConfiguration = new AudioRecognizeConfiguration.Builder()
                .setSilentDetectTimeOut(vadOn)// 是否使能静音检测，true表示不检查静音部分
                .audioFlowSilenceTimeOut(vadTimeoutValue) // 静音检测超时停止录音
                .minAudioFlowSilenceTime(200) // 语音流识别时的间隔时间
                .minVolumeCallbackTime(80) // 音量回调时间
                .sensitive(2.5f)
                .build();

        //currentRequestId = audioRecognizeRequest.getRequestId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != aaiClient){
                    aaiClient.startAudioRecognize(audioRecognizeRequest,
                            audioRecognizeResultlistener,
                            audioRecognizeStateListener,
                            audioRecognizeTimeoutListener,
                            audioRecognizeConfiguration);
                }else {
                    Log.e(TAG, "aaiClient语音识别实例为空");
                }
            }
        }).start();
    }



    public void stopRecord(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean taskExist = false;
                Log.d(TAG, "tencent stopRecord currentRequestId="+currentRequestId + " aaiClient="+aaiClient);
                if (aaiClient!=null) {
                    taskExist = aaiClient.stopAudioRecognize(currentRequestId);
                }
                if (!taskExist) {
                    Log.e(TAG, "识别状态：不存在该任务，无法停止");
                }
            }
        }).start();
    }

}
