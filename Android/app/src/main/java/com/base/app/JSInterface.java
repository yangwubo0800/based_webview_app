package com.base.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.baidu.android.pushservice.PushManager;
import com.base.app.speechrecognize.baidu.Baidu;
import com.base.app.speechrecognize.ifly.IFly;
import com.base.app.speechrecognize.tencent.Tencent;
import com.base.app.ui.guide.AgreementDetailActivity;
import com.base.app.ui.guide.PicassoPhotoViewActivity;
import com.base.bean.AppInfo;
import com.base.bean.GPS;
import com.base.bean.LocationInfo;
import com.base.bean.User;
import com.base.bean.video.AccessTokenResp;
import com.base.bean.video.Video;
import com.base.bean.video.VideoType;
import com.base.bean.video.VideoTypeResp;
import com.base.constant.Constants;
import com.base.constant.URLUtil;
import com.base.service.AlarmNotifyService;
import com.base.service.MqttService;
import com.base.utils.BaseAppUtil;
import com.base.utils.CameraFunctionUtil;
import com.base.utils.DataCleanManager;
import com.base.utils.DownloadUtil;
import com.base.utils.GPSConverterUtils;
import com.base.utils.GeneralUtils;
import com.base.utils.GsonHelper;
import com.base.utils.MapUtil;
import com.base.utils.NotificationUtils;
import com.base.utils.OkGoUpdateHttpUtil;
import com.base.utils.SpUtils;
import com.base.utils.ToastUtil;
import com.base.utils.http.ITRequestResult;
import com.base.utils.http.OkHttpUtil;
import com.base.utils.http.Param;
import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;
import com.cpe.ijkplayer.ui.LivePlayActivityNew;
import com.igexin.sdk.Tag;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.UpdateCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.jpush.android.api.JPushInterface;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.base.app.WebViewActivity.mQrScanCallName;

//import com.example.ezrealplayer.ui.EZRealPlayActivity;
//import com.example.ezrealplayer.util.EZUtils;
//import com.videogo.constant.IntentConsts;
//import com.videogo.exception.BaseException;
//import com.videogo.openapi.EZOpenSDK;
//import com.videogo.openapi.bean.EZCameraInfo;
//import com.videogo.openapi.bean.EZDeviceInfo;

public class JSInterface {

    private String TAG = "JSInterface";
    //主界面传过来的上下文信息
    private Context mContext;
    private Activity mActivity;
    //定位功能
    private LocationManager mLocationManager = null;
    private LocationListener mLocationListener = null;
    private Location mCurrentLocation;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    //主界面传过来的handler
    private Handler mHandler;
    // 萤石云 key 和 secret 以及 视频列表变量
    private String EZAppKey;
    private String EZAppSecret;
    private List<Video> list = new ArrayList<>();
    //用来记录调用定位请求的对象, 前端需要根据调用者名称来处理界面，当授权点击时，此处是new的对象没有传值，
    // 需要将此改为静态变量
    private static String mLocationCallName;
    //GPS定位异常处理
    private Handler GpsTimeoutHandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //以json格式返回经纬度信息
            LocationInfo locationInfo = new LocationInfo("", "", "", mLocationCallName, "timeout");
            String locationJson = GsonHelper.toJson(locationInfo);
            AFLog.d(TAG," GPS timeout name=" + mLocationCallName);
            //发消息到主线程
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_LOCATION;
            msg.obj = locationJson;
            mHandler.sendMessage(msg);
            //超时后停止定位获取
            cancelMonitorLocation();
        }
    };

    //用来记录每次定位请求是否返回过给前端
    private boolean mHasPostLocation;

    //记录获取后台实时视频配置信息的接口路径
    private String mVideoCfgInfoUrl;
    //获取后台实时视频配置信息数据的对象
    private static OkHttpClient getVideoInfoClient = new OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
    // 定时通过webview调用js 获取后台实时视频配置信息
    Runnable mUpdateVideoInfoRunnable = new Runnable() {
        @Override
        public void run() {
            // TODO: 视频界面正在则更新，不在则取消定时器
            if (null != LivePlayActivityNew.mHandler &&
                    !TextUtils.isEmpty(mVideoCfgInfoUrl)){
                //调用后台接口获取数据
                // Create and send HTTP requests
                Request rb = new Request.Builder().url(mVideoCfgInfoUrl).build();
                Call call = getVideoInfoClient.newCall(rb);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        AFLog.e(TAG,"getVideoInfoClient fail");
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //防止两次调用后流关闭，先暂存数据
                        String responseStr = response.body().string();
                        AFLog.d(TAG,"getVideoInfoClient onResponse response is "+ responseStr);
                        // TODO: 解析获取的视频配置信息，注意换行字符设置格式
                        com.alibaba.fastjson.JSONObject jsonObject = JSON.parseObject(responseStr);
                        //{"status":"1","code":"","msg":"","data":{"实时水位":"376.5米","警戒水位":"400米"}}
                        com.alibaba.fastjson.JSONObject data = jsonObject.getJSONObject("data");
                        Set<String> keys = data.keySet();
                        String showInfo = "";
                        for(String key: keys){
                            showInfo = showInfo + key + "：" + data.get(key) + "\n";
                        }
                        AFLog.d(TAG," onResponse showInfo=" + showInfo);
                        Message msg = new Message();
                        msg.obj = showInfo;
                        AFLog.d(TAG,"updateVideoConfigInfo LivePlayActivityNew.mHandler=" + LivePlayActivityNew.mHandler);
                        if (null != LivePlayActivityNew.mHandler){
                            LivePlayActivityNew.mHandler.sendMessage(msg);
                        }
                    }
                });

                mHandler.postDelayed(mUpdateVideoInfoRunnable, 10000);
            }else {
                //取消定时器，不处理首次进入界面，耗时很长的情况。
                AFLog.d(TAG,"mUpdateVideoInfoRunnable remove callback");
                mHandler.removeCallbacks(mUpdateVideoInfoRunnable);
            }
        }
    };

    //跳转三方地图当行选择框
    private AlertDialog mapItemDialog;
    //区分百度云消息推送是设置还是清理标签 "set" "clean"
    public static String mBaiduPushOperate;
    //需要设置的百度推送标签
    public static String mBaiduPushTags;

    //语音识别全局参数
    public static String mSpeechJsonParam;
    //当前调用的是哪个平台的语音识别，在请求权限成功之后需要决定调用哪个接口
    public static String mSpeechPlatform;
    //语音识别调用60秒后超时处理
    private Handler SpeechRecTimeoutHandler = new Handler();
    Runnable speechRecStop = new Runnable() {
        @Override
        public void run() {
            AFLog.e(TAG, "speechRecStop  run ");
            ToastUtil.makeText(mContext, "单次语音识别超过60秒，自动停止识别" );
            if (!TextUtils.isEmpty(mSpeechJsonParam)){
                com.alibaba.fastjson.JSONObject param = (com.alibaba.fastjson.JSONObject) JSON.parse(mSpeechJsonParam);
                String speechPlatform = param.getString("speechPlatform");
                if (Constants.SPEECH_IFLY.equals(speechPlatform)){
                    IFly iflySpeech = IFly.getInstance(mContext, mHandler);
                    iflySpeech.stopRecord();
                }else if (Constants.SPEECH_BAIDU.equals(speechPlatform)){
                    Baidu baiduSpeech = Baidu.getInstance(mContext, mHandler);
                    baiduSpeech.stopRecord();
                }else if (Constants.SPEECH_TENCENT.equals(speechPlatform)){
                    Tencent tencentSpeech = Tencent.getInstance(mContext, mHandler);
                    tencentSpeech.stopRecord();
                }else {
                    AFLog.e(TAG, "speechRecStop  run param error");
                }
            }
        }
    };


    /**
     *
     * @param context
     * @param activity
     * @param handler
     */
    public JSInterface(Context context, Activity activity, Handler handler) {
        mContext = context;
        mActivity = activity;
        mHandler = handler;

        //初始化，创建位置管理服务和监听器
        mLocationManager = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);
        mLocationListener =  new LocationListener() {
            @Override
            /*当地理位置发生改变的时候调用*/
            public void onLocationChanged(Location location) {
                String latitude = location.getLatitude() + "";
                String longitude = location.getLongitude() + "";
                String locationAddress = "";
                //移除超时返回处理，不管是谁请求的，都移除
                GpsTimeoutHandler.removeCallbacks(runnable);
                AFLog.d(TAG,"onLocationChanged GpsTimeoutHandler removeCallbacks");

                //选择GPS和network中更好的定位
                boolean flag = isBetterLocation(location, mCurrentLocation);
                if (flag) {
                    //定位更优，更新当前定位
                    mCurrentLocation = location;
                } else {
                    //如果没有优于之前的定位，则使用之前记录的定位信息。
                    latitude = mCurrentLocation.getLatitude() + "";
                    longitude = mCurrentLocation.getLongitude() + "";
                }

                AFLog.d(TAG, "onLocationChanged latitude=" + latitude + " longitude=" + longitude);
                Geocoder gc = new Geocoder(mContext);
                List<Address> addresses = null;
                try {
                    addresses = gc.getFromLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), 1);
                    AFLog.d(TAG, "Geocoder get addresses size=" + (addresses==null? null: addresses.size()));
                    if (addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        AFLog.d(TAG, "address0 =" + (address == null ? null : address.toString()));
                        // TODO: 区分安卓版本处理
                        // 7.0版本作为分界点
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N){
                            // Address[addressLines=[0:"中国",1:"湖南省长沙市岳麓区",2:"佳园路"],feature=佳园路,admin=湖南省
                            int size = address.getMaxAddressLineIndex();
                            for (int i=0; i<=size; i++){
                                locationAddress = locationAddress + address.getAddressLine(i);
                            }
                        } else {
                            //Address[addressLines=[0:"湖南省长沙市岳麓区佳园路",1:"华自科技股份有限公司",2:"明德麓谷幼儿园",3:"明德麓谷学校"
                            int size = address.getMaxAddressLineIndex();
                            if (size >= 1){
                                for (int i=0; i<=1; i++){
                                    locationAddress = locationAddress + address.getAddressLine(i);
                                }
                            } else {
                                locationAddress =  address.getAddressLine(0);
                            }
                        }

                        AFLog.d(TAG, "locationAddress=" + locationAddress);
                    }
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }




                //如果还没有返回过定位信息，则返回，确保两个设备只返回一次。
                if (!mHasPostLocation) {
                    String provider =location.getProvider();
                    AFLog.d(TAG,"onLocationChanged provider=" + provider);
                    //以json格式返回经纬度信息
                    LocationInfo locationInfo = null;
                    // 由于地理位置解析接口依赖于google service,不同手机厂商的效果不同，因此根据经纬度来判断是否定位成功
                    if (TextUtils.isEmpty(latitude) || TextUtils.isEmpty(longitude)){
                        locationInfo = new LocationInfo(latitude, longitude, locationAddress, mLocationCallName, "timeout");
                    } else {
                        locationInfo = new LocationInfo(latitude, longitude, locationAddress, mLocationCallName, "normal");
                    }

                    String locationJson = GsonHelper.toJson(locationInfo);
                    AFLog.d(TAG," onLocationChanged latitude=" + latitude + " longitude=" + longitude + " name=" + mLocationCallName);
                    //发消息到主线程
                    Message msg = new Message();
                    msg.what = Constants.MSG_FOR_LOCATION;
                    msg.obj = locationJson;
                    mHandler.sendMessage(msg);
                    mHasPostLocation = true;
                    // 定位成功或者超时之后，都取消定位
                    cancelMonitorLocation();
                }
            }

            /* 当状态发生改变的时候调用*/
            @Override
            public void onStatusChanged(String provider, int status, Bundle bundle) {
                AFLog.d(TAG, "onStatusChanged provider=" + provider +" status=" + status);
                if (LocationManager.GPS_PROVIDER.equals(provider)
                        && LocationProvider.OUT_OF_SERVICE == status) {
                    ToastUtil.makeText(mContext, "GPS服务丢失" );
                }
            }

            /*当定位者启用的时候调用*/
            @Override
            public void onProviderEnabled(String provider) {
                AFLog.d(TAG, "onProviderEnabled: provider=" + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                AFLog.d(TAG, "onProviderDisabled: provider=" + provider);
                if (LocationManager.GPS_PROVIDER.equals(provider)
                        && BaseAppUtil.isGPSOpen(mContext)){
                    ToastUtil.makeTextShowLong(mContext, "为提高定位精度，请选择带GPS选项的定位");
                }
            }
        };
    }
    /**
     * 功能：供JS使用，提供拍照功能，生成照片路径无法直接返回，
     * 需要等相机界面返回，JS可提供接口供native调用返回照片路径或者文件流
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.nativeTakePhoto()
     */
    @JavascriptInterface
    public void nativeTakePhoto() {
        AFLog.d(TAG,"nativeTakePhoto");
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AFLog.d(TAG,"nativeTakePhoto already has camera permission");
            Intent it = CameraFunctionUtil.takePhoto(mContext);
            mActivity.startActivityForResult(it, Constants.TAKE_PHOTO_REQUEST);
        } else {
            //动态申请权限
            AFLog.d(TAG,"nativeTakePhoto request CAMERA Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
        }
    }


    /**
     * 功能：供JS使用，提供录像功能，生成录像路径无法直接返回，
     * 需要等相机界面返回，JS可提供接口供native调用返回录像路径或者文件流
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.nativeRecordVideo()
     */
    @JavascriptInterface
    public void nativeRecordVideo() {
        AFLog.d(TAG,"nativeRecordVideo");
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AFLog.d(TAG,"nativeRecordVideo already has camera permission");
            Intent it = CameraFunctionUtil.recordVideo(mContext);
            mActivity.startActivityForResult(it, Constants.RECORD_VIDEO_REQUEST);
        } else {
            //动态申请权限
            AFLog.d(TAG,"nativeRecordVideo request CAMERA Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.PERMISSION_REQUEST_CAMERA_FOR_VIDEO);
        }
    }

    /**
     * 功能：供JS使用，提供扫码功能，生成扫码信息无法直接返回，
     * 需要等相机界面返回，JS可提供接口供native调用返回扫码信息
     * 参数：callerName, 用来区分调用者
     * 返回值：无
     * 使用方式：window.functionTag.scanQRCode(callerName)
     */
    @JavascriptInterface
    public void scanQRCode(String callerName) {
        AFLog.d(TAG,"scanQRCode callerName=" + callerName);
        mQrScanCallName = callerName;
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA)) {
            AFLog.d(TAG,"scanQRCode already has camera permission");
            Intent it = new Intent();
            it.setClass(mContext, com.base.zxing.CaptureActivity.class);
            mActivity.startActivityForResult(it, Constants.SCAN_QRCODE_REQUEST);
        } else {
            //动态申请权限
            AFLog.d(TAG,"scanQRCode request CAMERA Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.CAMERA}, Constants.PERMISSION_REQUEST_CAMERA_FOR_SCAN);
        }
    }


    /**
     * 功能：由JS调用，传入直播源后可以进入直播界面
     * 此处需要根据各个站点的配置来进行处理，鉴于目前萤石云并没有提供可以直接通过播放源url方式
     * 进行播放的接口，所以后台配置不会有配置类型为ezo ,然后罗列各种url记录的情况。
     * 此接口预留为提供url 配置的站点使用。
     * 参数：
     * @param liveUrl 直播流地址，字符串类型
     * @param liveTitle 直播界面显示的标题信息，字符串类型
     * @param videoType 直播流类型， 萤石云：ezopen, 其他标准流: standard
     * 返回值：无
     * 使用方式：window.functionTag.livePlay(videoType, liveUrl，liveTitle)
     */
    @JavascriptInterface
    public void livePlay(String videoType, String liveUrl, String liveTitle) {
        AFLog.d(TAG,"livePlay liveUrl==" + liveUrl + " liveTitle=" + liveTitle + " videoType=" + videoType);
        if ("ezopen".equals(videoType)) {
            // TODO: 萤石云接口
        } else if ("standard".equals(videoType)) {
            com.cpe.ijkplayer.ui.LivePlayActivityNew.startActivityForLPAN(mContext, liveUrl, liveTitle);
        } else {
            AFLog.d(TAG,"please give the right type of the video");
        }
    }



    /**
     * 功能：由JS调用，传入视频源后可以进入视频播放界面
     * 参数：
     * @param videoUrl：视频播放源地址，字符串类型。
     * @param videoTitle：视频播放界面标题，字符串类型。
     * 返回值：无
     * 使用方式：window.functionTag.videoPlay(videoUrl，videoTitle)
     */
    @JavascriptInterface
    public void videoPlay(String videoUrl, String videoTitle) {
        AFLog.d(TAG,"videoPlay videoUrl=" + videoUrl + " videoTitle=" + videoTitle);
        com.cpe.ijkplayer.ui.IjkVideoPlayActivity.startActivityForIJKPlayer(mContext, videoUrl, videoTitle);
    }



    /**
     * 功能：提供清理缓存功能，将webview产生的缓存目录文件全部删除
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.CleanWebCache()
     */
    @JavascriptInterface
    public void CleanWebCache() {
        BaseAppUtil.clearCacheFile(mContext);
    }


    /**
     * 功能：发送通知给状态栏，目前此接口还知识测试功能，
     * 关于点击通知跳转的页面还没有确认，后续还要和服务器配合做推送功能
     * 参数：
     * @param title：通知标题，字符串类型。
     * @param content，通知内容，字符串类型
     * 返回值：无
     * 使用方式：window.functionTag.SendNotification(title，content)
     */
    @JavascriptInterface
    public void SendNotification(String title, String content) {
        NotificationUtils.getInstance(mContext).setNotificationTitle(title);
        NotificationUtils.getInstance(mContext).setNotificationContent(content);
        NotificationUtils.getInstance(mContext).sendNotification(null, 1001);
        //自定义通知
        //NotificationUtils.getInstance(WebViewTagFunctionActivity.this).sendCustomNotification();
    }

    /**
     * 功能：拨号,通过传入号码可以实现直接跳转到拨号盘界面，拨打客服号码
     * 参数：
     * @param number：电话号码，字符串类型
     * 返回值：无
     * 使用方式：window.functionTag.CallNumber(number)
     */
    @JavascriptInterface
    public void CallNumber(String number) {
        BaseAppUtil.callNumber(mActivity, number);
    }



    /**
     * 功能：检测app版本是否需要更新，如果有则会更新下载安装
     * 参数：updateIPAddress， 更新app服务器地址
     * 返回值：无
     * 使用方式：window.functionTag.checkNewVersion(updateIPAddress)
     */
    @JavascriptInterface
    public void checkNewVersion(String updateIPAddress) {
        AFLog.d(TAG,"checkNewVersion updateIPAddress="+updateIPAddress);
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            updateVersion(updateIPAddress);
        } else {
            WebViewActivity.mAppUpdateServerIP = updateIPAddress;
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.PERMISSION_REQUEST_FOR_STORAGE);
        }
    }

    /**
     * 检查完权限之后用来进行更新app
     * @param updateIPAddress
     */
    public void updateVersion(String updateIPAddress){

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        Map<String, String> params = new HashMap<String, String>();
        // TODO: compose url
        //String updateVersionUrl = updateIPAddress + "mobile/sys/getVersion.do";
        String updateVersionUrl = "http://" + updateIPAddress + "/" + URLUtil.VERSION_UPDATE;
        //final String downloadUrl = updateIPAddress;
        final String downloadUrl = "http://" + updateIPAddress + "/";
        AFLog.d(TAG,"checkNewVersion updateVersionUrl="+updateVersionUrl);

        ApplicationInfo appInfo = null;
        String version ="";
        String versioncode ="";
        String packageName = "";
        try {
            appInfo = mContext.getPackageManager().
                    getApplicationInfo(mActivity.getPackageName(), PackageManager.GET_META_DATA);
            PackageManager manager = mContext.getPackageManager();
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);
            version = info.versionName;
            versioncode = String.valueOf(info.versionCode);
            packageName = mContext.getPackageName();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String appKey = appInfo.metaData.getString("UPDATE_APP_KEY");
        params.put("appKey", appKey);
        params.put("appVersion", version);
        params.put("appVersionCode", versioncode);
        params.put("packageName",packageName);
        AFLog.d(TAG,"checkNewVersion versioncode=" + versioncode);


        new UpdateAppManager
                .Builder()
                //必须设置，当前Activity
                .setActivity(mActivity)
                //必须设置，实现httpManager接口的对象
                .setHttpManager(new OkGoUpdateHttpUtil())
                //必须设置，更新地址
                .setUpdateUrl(updateVersionUrl)

                //以下设置，都是可选
                //设置请求方式，默认get
                .setPost(false)
                //不显示通知栏进度条
                //.dismissNotificationProgress()
                //是否忽略版本
//                .showIgnoreVersion()
                //添加自定义参数，默认version=1.0.0（app的versionName）；apkKey=唯一表示（在AndroidManifest.xml配置）
                .setParams(params)
                //设置点击升级后，消失对话框，默认点击升级后，对话框显示下载进度
                //.hideDialogOnDownloading()
                //设置头部，不设置显示默认的图片，设置图片后自动识别主色调，然后为按钮，进度条设置颜色
                .setTopPic(R.drawable.image_version_update)
                //为按钮，进度条设置颜色，默认从顶部图片自动识别。
//                .setThemeColor(ColorUtil.getRandomColor())
                //设置apk下砸路径，默认是在下载到sd卡下/Download/1.0.0/test.apk
                .setTargetPath(path)
                //设置appKey，默认从AndroidManifest.xml获取，如果，使用自定义参数，则此项无效
//                .setAppKey("ab55ce55Ac4bcP408cPb8c1Aaeac179c5f6f")

                .build()
                //检测是否有新版本
                .checkNewApp(new UpdateCallback() {
                    /**
                     * 解析json,自定义协议
                     *
                     * @param json 服务器返回的json
                     * @return UpdateAppBean
                     */
                    @Override
                    protected UpdateAppBean parseJson(String json) {
                        UpdateAppBean updateAppBean = new UpdateAppBean();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            updateAppBean
                                    //（必须）是否更新Yes,No
                                    .setUpdate(jsonObject.optString("update"))
                                    //（必须）新版本号，
                                    .setNewVersion(jsonObject.optString("new_version"))
                                    //（必须）下载地址
                                    .setApkFileUrl(downloadUrl + jsonObject.optString("apk_file_url"))
                                    //（必须）更新内容
                                    .setUpdateLog(jsonObject.optString("update_log"))
                                    //大小，不设置不显示大小，可以不设置
                                    .setTargetSize(jsonObject.optString("target_size"))
                                    //是否强制更新，可以不设置
                                    .setConstraint(false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return updateAppBean;
                    }

                    @Override
                    protected void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager) {
                        updateAppManager.showDialogFragment();
                    }

                    /**
                     * 网络请求之前
                     */
                    @Override
                    public void onBefore() {
                        //CProgressDialogUtils.showProgressDialog(mActivity);
                    }

                    /**
                     * 网路请求之后
                     */
                    @Override
                    public void onAfter() {
                        //CProgressDialogUtils.cancelProgressDialog(mActivity);
                    }

                    /**
                     * 没有新版本
                     */
                    @Override
                    public void noNewApp(String error) {
                        ToastUtil.makeText(mActivity, "当前已是最新版本");
                    }
                });
    }

    /**
     *
     * 功能：设置站点ID，在登录成功后调用，开启告警服务前必须要做此动作
     * 参数：stationId
     * 返回值：无
     * 使用方式：window.functionTag.setStaionId(stationId)
     */
    @JavascriptInterface
    public void setStationId(String stationId){
        BaseWebviewApp.getInstance().setStationId(stationId);
    }

    /**
     *
     *功能：设置用户，开启告警服务前必须要做此动作
     * 参数：user
     * 返回值：无
     * 使用方式：window.functionTag.setUser(user)
     */
    @JavascriptInterface
    public void setUser(String user){
        if (!TextUtils.isEmpty(user)){
            AFLog.d(TAG,"setUser json user=" + user);
            User clazzUser = GsonHelper.toType(user, User.class);
            BaseWebviewApp.getInstance().setUser(clazzUser);
        }
    }

    /**
     *
     * 功能：设置会话，在登录成功后调用，开启告警服务前必须要做此动作
     * 参数：session
     * 返回值：无
     * 使用方式：window.functionTag.setSession(session)
     */
    @JavascriptInterface
    public void setSession(String session){
        BaseWebviewApp.getInstance().setSession(session);
    }

    /**
     *
     * 功能：设置服务器地址，在登录成功后调用，开启告警服务前必须要做此动作
     * 注意：设置地址不要带协议，如http;// 字段，只需要IP 或者 域名即可
     * 参数：serverAddress
     * 返回值：无
     * 使用方式：window.functionTag.setServerAddress(serverAddress)
     */
    @JavascriptInterface
    public void setServerAddress(String serverAddress){
        URLUtil.setServerIP(mContext, serverAddress);
    }

    /**
     * 功能：启动告警服务
     * 注意：在调用此方法前，请确保已经调用设置站点、用户、会话、服务器地址接口，
     * 即文档中接口 16 17 19 20
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.startAlarmService()
     */
    @JavascriptInterface
    public void startAlarmService(){
        AFLog.d(TAG,"startAlarmService");
        Intent intent = new Intent(mActivity, AlarmNotifyService.class);
        // TODO: 需要先设置好站点ID
        intent.putExtra("stationId",BaseWebviewApp.getInstance().getStationId());
        intent.putExtra("notifyInt",1000);
        mContext.stopService(intent);
        mContext.startService(intent);
    }


    /**
     * 功能：获取位置经纬度信息
     * 参数：callerName, 用来区分调用者
     * 返回值：无,
     * 异步返回数据格式： {"address":"中国湖南省长沙市岳麓区佳园路","latitude":
     "28.229733","longitude":"112.864245","name":"test1"}
     * 使用方式：window.functionTag.getLocationInfo(callerName)
     */
    @SuppressLint("MissingPermission")
    @JavascriptInterface
    public void getLocationInfo(String callerName) {
        AFLog.d(TAG,"getLocationInfo callerName=" + callerName);
        mLocationCallName = callerName;
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)){
            requestLocation();
        } else {
            //动态申请权限
            AFLog.d(TAG,"getLocationInfo request CAMERA Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    Constants.PERMISSION_REQUEST_FOR_LOCATION);
        }
    }

    /**
     * 功能：取消位置监听，当退出定位页面时，取消监听。
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.cancelMonitorLocation()
     */
    @JavascriptInterface
    public void cancelMonitorLocation() {
        AFLog.d(TAG,"cancelMonitorLocation");
        if (null != mLocationManager) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    /**
     * 将获取位置接口单独出来，便于获取到权限后直接可以调用
     */
    @SuppressLint("MissingPermission")
    public void requestLocation(){
        AFLog.d(TAG,"requestLocation GpsTimeoutHandler postDelayed");
        GpsTimeoutHandler.postDelayed(runnable, 10000);
        mHasPostLocation = false;
        //先检测GPS是否打开，不管结果如何，都去请求位置信息。
        if(!BaseAppUtil.isGPSOpen(mContext)){
            //返回给前端，GPS关闭信息
            LocationInfo locationInfo = new LocationInfo("", "", "", mLocationCallName, "gpsClosed");
            String locationJson = GsonHelper.toJson(locationInfo);
            AFLog.d(TAG," GPS closed name=" + mLocationCallName);
            //发消息到主线程
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_LOCATION;
            msg.obj = locationJson;
            mHandler.sendMessage(msg);

            //跳转GPS设置界面
            ToastUtil.makeText(mContext, "请打开GPS定位");
            BaseAppUtil.openGPS(mContext);
        }

        //获取时时更新，第一个是Provider,第二个参数是更新时间1000ms，第三个参数是更新半径，第四个是监听器
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, mLocationListener);
        // 只使用GPS定位来返回结果
        //mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, mLocationListener);
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     *
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new
     *            one
     */
    protected boolean isBetterLocation(Location location,
                                       Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use
        // the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
                .getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate
                && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * 功能：请求站点视频列表
     * 基于目前站点中都是配置的萤石云播放，所以此请求初始化是必须的，而且完成之后通过回调前端接口，将获取信息返回。
     * 参数：stationId
     * 返回值：无
     * 使用方式：window.functionTag.EZOpenVideoRequest(stationId)
     */
    @JavascriptInterface
    public void EZOpenVideoRequest(String stationId){
        // 安卓6.0之后动态权限申请检查，萤石云需要获取手机deviceId，读取手机状态权限
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.READ_PHONE_STATE)) {
            AFLog.d(TAG,"EZOpenVideoRequest already has READ_PHONE_STATE permission");
            // continue;
            getVideoType(stationId);
        } else {
            //动态申请权限
            AFLog.d(TAG,"EZOpenVideoRequest request READ_PHONE_STATE Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.READ_PHONE_STATE},
                    Constants.PERMISSION_REQUEST_FOR_EZOPENPLAY);
        }
    }

    // TODO: 1、从服务器获取视频列表类型信息
    public void getVideoType(String stationId){
        String url = URLUtil.getHTTPServerIP(mContext)+ URLUtil.GET_VIDEO_TYPE_LIST;
        String cookie = BaseWebviewApp.getInstance().getSession();
        String activityName = mActivity.getClass().getSimpleName();
        AFLog.d(TAG,"getVideoType url="+url
                +" cookie="+cookie
                +" activityName="+activityName);

        OkHttpUtil.getInstance().requestAsyncPostByTagHeader(url, cookie, activityName,
                new ITRequestResult<VideoTypeResp>() {
                    @Override
                    public void onCompleted() {
                        //mvpView.hideLoading();
                        AFLog.d(TAG,"getVideoType onCompleted");
                    }

                    @Override
                    public void onSuccessful(VideoTypeResp entity,String session) {
                        //mvpView.getVideoTypeSuccess(entity.getObj());
                        AFLog.d(TAG,"getVideoType onSuccessful");
                        // TODO: 2、获取成功之后判断类型，然后决定是否要初始化萤石云SDK
                        getVideoTypeSuccess(entity.getObj());
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        //mvpView.onError(errorMsg, "");
                        AFLog.e(TAG,"getVideoType onFailure");
                        //记录到文件日志中
                        LogSaveUtils.e(errorMsg);
                    }

                    @Override
                    public void sessionLose() {
                        //mvpView.sessionLose();
                        AFLog.d(TAG,"getVideoType sessionLose");
                    }
                },
                VideoTypeResp.class,
                new Param("stationId", /*BaseWebviewApp.getInstance().getStationId()*/stationId));
    }

    /**
     * 初始化萤石云SDK 接口
     * @param appKey
     */
    private void initSDK(String appKey) {
//        {
//            /**
//             * sdk日志开关，正式发布需要去掉
//             */
//            EZOpenSDK.showSDKLog(true);
//
//            /**
//             * 设置是否支持P2P取流,详见api
//             */
//            EZOpenSDK.enableP2P(false);
//
//            /**
//             * APP_KEY请替换成自己申请的
//             */
//            EZOpenSDK.initLib(BaseWebviewApp.getInstance(),appKey,"");
////            EZOpenSDK.initLib(this, AppKey, "");
//        }
    }

    /**
     * 从服务器获取视频类型成功后，决定是否初始化萤石云
     * @param list
     */
    public void getVideoTypeSuccess(List<VideoType> list) {
        if(list!=null){
            for(int i=0;i<list.size();i++){
                if(list.get(i).getType()==0){//type=0 ,萤石云
                    EZAppKey=list.get(i).getAppKey();
                    EZAppSecret=list.get(i).getSecret();
                    // TODO: 3、初始化萤石云SDK
                    try {
                        initSDK(EZAppKey);//初始化萤石云
                    }catch (Exception ex){
                        AFLog.e("V",ex.toString());
                    }
                    getAccessToken(EZAppKey,EZAppSecret);//异步获取
                    break;
                }
            }

        }
    }

    //异步获取AccessToken
    public void getAccessToken(String appKey,String appSecret){
        // TODO: 从服务器获取token
        String url = URLUtil.GetAccessToken;
        String activityName = mActivity.getClass().getSimpleName();
        OkHttpUtil.getInstance().requestAsyncPostByTag_EZO(url,
                activityName,
                "ezo",
                new ITRequestResult<AccessTokenResp>() {
                    @Override
                    public void onCompleted() {
                        //mvpView.hideLoading();
                        AFLog.d(TAG,"getAccessToken onCompleted");
                    }

                    @Override
                    public void onSuccessful(AccessTokenResp entity, String session) {
                       // mvpView.setAccessToken(entity.getData().getAccessToken());
                        AFLog.d(TAG,"getAccessToken onSuccessful");
                        // TODO: 4、获取token成功后，设置，并且获取视频列表；
                        setAccessToken(entity.getData().getAccessToken());
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        //mvpView.onError(errorMsg, "");
                        AFLog.e(TAG,"getAccessToken onFailure");
                        //记录到文件日志中
                        LogSaveUtils.e(errorMsg);
                    }

                    @Override
                    public void sessionLose() {
                        //mvpView.sessionLose();
                        AFLog.d(TAG,"getAccessToken sessionLose");
                    }
                },
                AccessTokenResp.class,
                new Param("appKey", appKey),
                new Param("appSecret", appSecret));
    }

    /**
     * 设置token，并且获取摄像头视频列表信息
     * @param accessToken
     */
    public void setAccessToken(String accessToken) {
//        EZOpenSDK.getInstance().setAccessToken(accessToken);
//        getCameraList();
    }

    // TODO: 如果是前端直接请求等待获取完成的话，此处是否需要做成同步，不适用线程？
    public void getCameraList(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    //每次获取前先清理掉全局变量中的内容;
//                    if (null != list) {
//                        list.clear();
//                    }
//                    List<EZDeviceInfo> result = null;
////                    showLoading();
//                    result = EZOpenSDK.getInstance().getDeviceList(0, 50);
//                    if(result!=null){
////                        hideLoading();
//                        for (EZDeviceInfo info:result) {
//                            List<EZCameraInfo>  cameraInfos= info.getCameraInfoList();
//                            if(cameraInfos.size()>0){
//                                for (EZCameraInfo cameraInfo:cameraInfos) {
//                                    list.add(new Video(cameraInfo.getDeviceSerial(),cameraInfo.getCameraName(),"", GeneralUtils.getRightNowDateString2(),"01",cameraInfo,info));
//                                }
//                            }else{
//                                list.add(new Video(info.getDeviceSerial(),info.getDeviceName(),"", GeneralUtils.getRightNowDateString2(),"0",info));
//                            }
////                            list.add(new Video(info.getDeviceSerial(),info.getDeviceName(),"", GeneralUtils.getRightNowDateString2(),"0",info));
//                        }
//                        // TODO: 获取完列表之后，回调前端接口，将转换成gson字符串返回给前端，
//                        AFLog.d(TAG,"getCameraList list size =" + list.size());
//                        Video video = list.get(0);
//                        //Log.d(TAG,"=====video 0 is" + video.toString());
//                        String videoInfo = GsonHelper.toJson(video);
//                        //Log.d(TAG,"=====GsonHelper videoInfo=" + videoInfo);
//                        //发消息到主线程
//                        Message msg = new Message();
//                        msg.what = Constants.MSG_FOR_GET_EZOPEN_VIDEO_LIST;
//                        msg.obj = videoInfo;
//                        mHandler.sendMessage(msg);
////                        EZOpenPlay(videoInfo);
//                        String videoList = GsonHelper.toJson(list);
//                        //Log.d(TAG,"=====GsonHelper videoList=" + videoList);
////                        Message msg = new Message();
////                        msg.what = ESO_FLAG;
////                        handler.sendMessage(msg);
//                    }
//                } catch (BaseException e) {
////                    hideLoading();
//                    //videoPresenter.getAccessToken(EZAppKey,EZAppSecret);;
//                    AFLog.e(TAG, "BaseException" );
//                }
//            }
//        }).start();
    }


    /**
     * 功能：萤石云播放接口，前端设置响应点击播放动作，将对应的Video 对象gson化之后传递给安卓
     * 参数：videoInfo
     * 返回值：无
     * 使用方式：window.functionTag.EZOpenPlay(videoInfo)
     */
    @JavascriptInterface
    public void EZOpenPlay(String videoInfo){
        // TODO: 需要将参数变为string，供给前端使用，然后gson化成类，再在安卓层面使用。
//        Video playVideo = GsonHelper.toType(videoInfo, Video.class);
//
//        if(playVideo.getVideoType().equals("0")){
//            // 从gson转换会的video类中先获取对象
//            Object deviceObj = playVideo.getObject();
//            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
//            //异常，可能是直接获取的对象不符合gson格式
//            String deviceStr = GsonHelper.toJson(deviceObj);
//            //将gson格式后的object转换为具体类对象
//            EZDeviceInfo  deviceInfo = GsonHelper.toType(deviceStr, EZDeviceInfo.class);
//
//            EZCameraInfo  cameraInfo = EZUtils.getCameraInfoFromDevice(deviceInfo, 0);
//            if (cameraInfo == null) {
//                return;
//            }
//            Intent intent = new Intent(mContext, EZRealPlayActivity.class);
//            intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
//            intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, deviceInfo);
//            mContext.startActivity(intent);
//
//        }else if(playVideo.getVideoType().equals("01")){
//            //EZCameraInfo  cameraInfo = (EZCameraInfo)playVideo.getObject();
//            // 从gson转换会的video类中先获取对象
//            Object cameraObj = playVideo.getObject();
//            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
//            //异常，可能是直接获取的对象不符合gson格式
//            String cameraStr = GsonHelper.toJson(cameraObj);
//            //将gson格式后的object转换为具体类对象
//            EZCameraInfo  cameraInfo = GsonHelper.toType(cameraStr, EZCameraInfo.class);
//            if (cameraInfo == null) {
//                return;
//            }
//
//            // 从gson转换会的video类中先获取对象
//            Object deviceObj = playVideo.getObject2();
//            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
//            //异常，可能是直接获取的对象不符合gson格式
//            String deviceStr = GsonHelper.toJson(deviceObj);
//            //将gson格式后的object转换为具体类对象
//            EZDeviceInfo  deviceInfo = GsonHelper.toType(deviceStr, EZDeviceInfo.class);
//
//            Intent intent = new Intent(mContext, EZRealPlayActivity.class);
//            intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
//            //intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, (EZDeviceInfo)playVideo.getObject2());
//            intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, deviceInfo);
//            mContext.startActivity(intent);
//
//        }
    }

    /**
     * 功能：获取手机MD5加密之后的IMEI码，提供给JS使用
     * 参数：无
     * 返回值：加密后的IMEI码
     * 使用方式：window.functionTag.getDeviceId()
     */
    @JavascriptInterface
    public String getDeviceId(){
        String imei = null;
        String imeiMD5 = null;
        boolean checkResult = BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.READ_PHONE_STATE);
        if (checkResult){
            //get device id
            imei = GeneralUtils.getDeviceId(mContext);
            imeiMD5 = GeneralUtils.md5forString(imei);
            Message msg = new Message();
            msg.what = Constants.MSG_FOR_GET_MD5_IMEI;
            msg.obj =imeiMD5;
            mHandler.sendMessage(msg);
        } else {
            //动态申请权限
            AFLog.d(TAG,"getDeviceId request READ_PHONE_STATE Permissions");
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.READ_PHONE_STATE},
                    Constants.PERMISSION_REQUEST_FOR_IMEI);
        }

        return imeiMD5;
    }

    /**
     * 功能：获取手机app 基本信息
     * 参数：无
     * 返回值：以json格式返回app信息，例如：
     *  appInfoJson={"appVersion":"3.0.0.0","appVersionCode":"3","packageName":"com.base.app.debug"}
     * 使用方式：window.functionTag.getAppInfo()
     */
    @JavascriptInterface
    public String getAppInfo(){

        ApplicationInfo appInfo = null;
        String version ="";
        String versioncode ="";
        String packageName = "";
        try {
            appInfo = mContext.getPackageManager().
                    getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            PackageManager manager = mContext.getPackageManager();
            PackageInfo info = manager.getPackageInfo(mContext.getPackageName(), 0);

            version = info.versionName;
            versioncode=String.valueOf(info.versionCode);
            packageName = mContext.getPackageName();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        AppInfo appInformation = new AppInfo(packageName, version, versioncode);
        String appInfoJson = GsonHelper.toJson(appInformation);
        AFLog.d(TAG, "getAppInfo appInfoJson=" + appInfoJson);
        return appInfoJson;
    }

    /**
     * 功能：启动APP 消息订阅服务
     * 参数：无
     * 返回值：无
     * 注意事项：服务启动一般在登录之后进行，
     * 在启动服务之前请确保设置好用户user和站点stationId信息，
     * 这两个信息将用于clientId 和 topic
     * 使用方式：window.functionTag.startMqttService()
     */
    @JavascriptInterface
    public void startMqttService(){
        AFLog.d(TAG, "startMqttService " );
        Intent intent = new Intent(mActivity, MqttService.class);
        mContext.stopService(intent);
        mContext.startService(intent);
    }

    /**
     * 功能：停止APP 消息订阅服务，一般在退出站点或者账号后使用，避免退出之后任然收到告警消息。
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.stopMqttService()
     */
    @JavascriptInterface
    public void stopMqttService(){
        AFLog.d(TAG, "stopMqttService " );
        Intent intent = new Intent(mActivity, MqttService.class);
        mContext.stopService(intent);
    }


    /**
     * 功能：提供通过key value操作文件存储数据接口给前端。
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.setKeyValue(key， value)
     */
    @JavascriptInterface
    public void setKeyValue(String key, String value){
        if (!TextUtils.isEmpty(key)){
            SpUtils.setSpFileName(Constants.PREF_NAME_USERINFO);
            SpUtils.putString(mContext, key, value);
            AFLog.d(TAG,"setKeyValue key="+ key + " value=" + value);
        }
    }

    /**
     * 功能：提供通过key value操作文件读取数据接口给前端。
     * 参数：无
     * 返回值：value
     * 使用方式：window.functionTag.getValueByKey(key)
     */
    @JavascriptInterface
    public String getValueByKey(String key){
        String value = null;
        if (!TextUtils.isEmpty(key)){
            SpUtils.setSpFileName(Constants.PREF_NAME_USERINFO);
            value = SpUtils.getString(mContext, key);
            AFLog.d(TAG,"getValueByKey key="+ key + " value=" + value);
        }
        return value;
    }

    /**
     * 功能：预览图片
     * 参数：无
     * 返回值：value
     * 使用方式：window.functionTag.previewPhoto(url)
     */
    @JavascriptInterface
    public void previewPhoto(String url){
        Intent it = new Intent(mContext, PicassoPhotoViewActivity.class);
        it.putExtra("url", url);
        mContext.startActivity(it);
    }

    /**
     * 功能：设置横竖屏显示功能
     * 参数：orientation
     * 返回值：无
     * 使用方式：window.functionTag.setOrientation(orientation)
     */
    @JavascriptInterface
    public void setOrientation(String orientation){
        AFLog.d(TAG, "#####setOrientation orientation=" + orientation);
        if (!TextUtils.isEmpty(orientation)){
            //和前端约定：0 随屏幕旋转，1 横屏， 2 竖屏
            if ("0".equals(orientation)){
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else if("1".equals(orientation)){
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else if("2".equals(orientation)){
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }


    /**
     * 功能：进入实时画面显示
     * 参数：url
     * 返回值：无
     * 使用方式：window.functionTag.startRealHtmlActivity(url)
     */
    @JavascriptInterface
    public  void startRealHtmlActivity(String url){
        Intent it = new Intent(mContext, RealHtmlActivity.class);
        it.putExtra("url", url);
        mContext.startActivity(it);
    }

    /**
     * 功能：设置消息订阅的主题
     * 参数：topics，订阅的主题，可以设置多个，多个主题之间以逗号分隔，例如"Project/ST/0001,Project/ST/0002"
     * 返回值：无
     * 使用方式：window.functionTag.setTopics(topics)
     */
    @JavascriptInterface
    public  void setTopics(String topics){
        AFLog.d(TAG, "topics is "+ topics);
        if (!TextUtils.isEmpty(topics)){
            String[] topic = topics.split(",");
//            for (int i=0; i<topic.length; i++) {
//                AFLog.d(TAG, "topic split by comma is " + topic[i]);
//            }
            BaseWebviewApp.getInstance().setTopic(topic);
        }
    }

    /**
     * 功能：获取设置的消息订阅的主题
     * 参数：无
     * 返回值：topic, 主题，如果是多个，则主题以逗号分隔
     * 使用方式：window.functionTag.getTopics()
     */
    @JavascriptInterface
    public  String  getTopics(){
        String [] topics = BaseWebviewApp.getInstance().getTopics();
        String topic = "";
        if (null != topics){
            for (int i=0; i<topics.length; i++){
                topic = topic +  topics[i] + ",";
            }

            if (!TextUtils.isEmpty(topic)){
                //remove the last comma
                topic = topic.substring(0, topic.length() -1);
            }
        }
        return topic;
    }

    /**
     * 功能：设置消息订阅的客户ID
     * 参数：clientId，客户ID，由账号名+端标识+系统编号 例如"admin_app03"
     * 返回值：无
     * 使用方式：window.functionTag.setClientId(clientId)
     */
    @JavascriptInterface
    public  void setClientId(String clientId){
        AFLog.d(TAG, "clientId is "+ clientId);
        if (!TextUtils.isEmpty(clientId)){
            BaseWebviewApp.getInstance().setClientId(clientId);
        }else {
            AFLog.e(TAG,"wrong clientId");
        }
    }


    /**
     * 功能：获取应用缓存大小
     * 参数：无
     * 返回值：带有单位的缓存大小字符串, 单位最小为KB
     * 使用方式：window.functionTag.getAppCacheSize()
     */
    @JavascriptInterface
    public String getAppCacheSize(){
        String size = "";
        try {
            size = DataCleanManager.getTotalCacheSize(mContext);
        } catch (Exception e) {
            AFLog.e(TAG,"getAppCacheSize Exception");
            e.printStackTrace();
        }
        AFLog.w(TAG,"getAppCacheSize is " + size);
        return size;
    }



    /**
     * 功能：设置压缩图片的固定宽和高
     * 参数：w 宽度 h 高度，注意都要大于0
     * 返回值：无
     * 使用方式：window.functionTag.setCompressImageWidthAndHeight(int w, int h)
     */
    @JavascriptInterface
    public void setCompressImageWidthAndHeight(int w, int h){
        AFLog.d(TAG,"setCompressImageWidthAndHeight w=" + w +" h="+h);
        if (w > 0  && h >0){
            BaseAppUtil.mCompressImageWidth = w;
            BaseAppUtil.mCompressImageHeight = h;
        }
    }

    /**
     * 功能：下载文件
     * 参数： url 文件下载地址, fileName 文件名称
     * 返回值：无
     * 使用方式：window.functionTag.DownloadFileByName(url, fileName)
     */
    @JavascriptInterface
    public void DownloadFileByName( String url, String fileName){
        AFLog.d(TAG,"setDownloadFileName url=" + url);
        AFLog.d(TAG,"setDownloadFileName fileName=" + fileName);
        DownloadUtil.downloadBySystem(mContext, url, null, null, fileName);
    }


    /**
     * 功能：设置更新视频配置信息后台接口地址
     * 参数：url 视频配置信息后台接口地址
     * 返回值：无
     * 使用方式：window.functionTag.setVideoConfigInfoUrl(url)
     */
    @JavascriptInterface
    public void setVideoConfigInfoUrl(String url){
        AFLog.d(TAG,"setVideoConfigInfoUrl url=" + url);
        mVideoCfgInfoUrl = url;
        mHandler.postDelayed(mUpdateVideoInfoRunnable, 1000);
    }

    /**
     * 功能：退出应用
     * 参数：
     * 返回值：无
     * 使用方式：window.functionTag.finish()
     */
    @JavascriptInterface
    public void finish(){
        AFLog.d(TAG,"finish");
        mActivity.finish();
    }


    /**
     * 提供前端界面进入用户协议或者隐私政策的入口
     * @param infoType 用户协议传入字符串agreement， 隐私政策传入字符串privacy
     */
    @JavascriptInterface
    public void enterAgreementDetail(String infoType){
        AFLog.d(TAG,"enterAgreementDetail infoType="+infoType);
        Intent it = new Intent(mActivity, AgreementDetailActivity.class);
        it.putExtra("infoType",infoType);
        mActivity.startActivity(it);
    }

    /**
     *
     * @param navigationLocation 出发地和目的地参数json字符串
     */
    @JavascriptInterface
    public void startMapNavigation(String navigationLocation){

        //先根据手机上安装的地图应用，显示列表框，如果没有安装任何地图，提示用户安装
        boolean gaodeInstalled =  MapUtil.isGdMapInstalled();
        boolean baiduInstalled =  MapUtil.isBaiduMapInstalled();
        boolean tencentMapInstalled =  MapUtil.isTencentMapInstalled();

        if (!gaodeInstalled && !baiduInstalled && !tencentMapInstalled){
            Toast.makeText(mContext, "请在手机上安装百度、高德、腾讯地图中的一种地图", Toast.LENGTH_SHORT).show();
            return;
        }

        //解析json字符参数
        com.alibaba.fastjson.JSONObject locations =  JSON.parseObject(navigationLocation);
        final String slatStr = locations.getString("slat");
        String slonStr = locations.getString("slon");
        final String sname = locations.getString("sname");
        String dlatStr = locations.getString("dlat");
        String dlonStr = locations.getString("dlon");
        final String dname = locations.getString("dname");


        ArrayList<String> mapItems = new ArrayList<>();
        if (gaodeInstalled){
            mapItems.add("高德地图");
        }

        if (baiduInstalled){
            mapItems.add("百度地图");
        }

        if (tencentMapInstalled){
            mapItems.add("腾讯地图");
        }

        AFLog.d(TAG,"原始GPS坐标数据出发地 slatStr="+slatStr+" slonStr="+slonStr);
        AFLog.d(TAG,"原始GPS坐标数据目的地 dlatStr="+dlatStr+" dlonStr="+dlonStr);

        try{
            final double slat = Double.parseDouble(slatStr);
            final double slon = Double.parseDouble(slonStr);
            final double dlat = Double.parseDouble(dlatStr);
            final double dlon = Double.parseDouble(dlonStr);


            //将GS84转换为火星坐标系
            GPS gcj02Destination = GPSConverterUtils.gps84_To_Gcj02(dlat, dlon);
            final double gcj02LatD = (null == gcj02Destination) ?0:gcj02Destination.getLat();
            final double gcj02LonD = (null == gcj02Destination) ?0:gcj02Destination.getLon();
            AFLog.d(TAG,"目的地转换为火星坐标后 gcj02LatD="+gcj02LatD+" gcj02LonD="+gcj02LonD);

            GPS gcj02Start = GPSConverterUtils.gps84_To_Gcj02(slat, slon);
            final double gcj02LatS = (null == gcj02Start) ?0:gcj02Start.getLat();
            final double gcj02LonS = (null == gcj02Start) ?0:gcj02Start.getLon();
            AFLog.d(TAG,"出发地转换为火星坐标后 gcj02LatS="+gcj02LatS+" gcj02LonS="+gcj02LonS);


            final String[] items = (String[]) mapItems.toArray(new String[mapItems.size()]);
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);
            alertBuilder.setTitle("地图选择");
            alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Toast.makeText(mContext, items[i], Toast.LENGTH_SHORT).show();
                    //根据选择的应用来确认坐标系转换
                    mapItemDialog.dismiss();
                    if ("高德地图".equals(items[i])){
                        if ("0".equals(slatStr)){
                           MapUtil.openGaoDeNavi(mContext, 0, 0, "我的位置" , gcj02LatD, gcj02LonD, dname);
                        }else {
                            MapUtil.openGaoDeNavi(mContext, gcj02LatS, gcj02LonS, sname, gcj02LatD, gcj02LonD, dname);
                        }

                    }

                    if ("腾讯地图".equals(items[i])){
                        if ("0".equals(slatStr)){
                            MapUtil.openTencentMap(mContext, 0, 0, "我的位置" , gcj02LatD, gcj02LonD, dname);
                        }else {
                            MapUtil.openTencentMap(mContext, gcj02LatS, gcj02LonS, sname, gcj02LatD, gcj02LonD, dname);
                        }

                    }

                    if ("百度地图".equals(items[i])){

                        //将火星坐标转换为百度坐标
//                        GPS bd09Destination = GPSConverterUtils.gcj02_To_Bd09(gcj02LatD, gcj02LonD);
//                        double bd09LatD = (null == bd09Destination) ?0:bd09Destination.getLat();
//                        double bd09LonD = (null == bd09Destination) ?0:bd09Destination.getLon();
//                        AFLog.d(TAG,"目的地转换为百度坐标后 bd09LatD="+bd09LatD+" bd09LonD="+bd09LonD);
//
//                        GPS bd09Start = GPSConverterUtils.gcj02_To_Bd09(gcj02LatS, gcj02LonS);
//                        double bd09LatS = (null == bd09Start) ?0:bd09Start.getLat();
//                        double bd09LonS = (null == bd09Start) ?0:bd09Start.getLon();
//                        AFLog.d(TAG,"出发地转换为百度坐标后 bd09LatS="+bd09LatS+" bd09LonS="+bd09LonS);
//
//
//                        if ("0".equals(slatStr)){
//                            MapUtil.openBaiDuNavi(mContext, 0, 0, "我的位置" , bd09LatD, bd09LonD, dname);
//                        }else {
//                            MapUtil.openBaiDuNavi(mContext, bd09LatS, bd09LonS, sname, bd09LatD, bd09LonD, dname);
//                        }


                        if ("0".equals(slatStr)){
                            MapUtil.openBaiDuNavi(mContext, 0, 0, "我的位置" , gcj02LatD, gcj02LonD, dname);
                        }else {
                            MapUtil.openBaiDuNavi(mContext, gcj02LatS, gcj02LonS, sname, gcj02LatD, gcj02LonD, dname);
                        }

                    }
                }
            });
            mapItemDialog = alertBuilder.create();
            mapItemDialog.show();


        }catch (NumberFormatException e){
            e.printStackTrace();
            AFLog.d(TAG,"传入经纬度参数有误");
        }
    }



    /**
     * 功能：设置极光推送tag
     * 参数：tagWithUrl 由标签和跳转页面组成的json字符串
     * tags 标签，如果有多个，以逗号分隔
     * jumpUrl 点击消息跳转页面路径
     * 返回值：无
     * 使用方式：window.functionTag.setJPushTagAndJumpUrl(tagWithUrl)
     */
    @JavascriptInterface
    public void setJPushTagAndJumpUrl(String tagWithUrl){

        String jumpUrl = null;
        String tags = null;
        com.alibaba.fastjson.JSONObject tagWithUrlObj = JSON.parseObject(tagWithUrl);
        if (null != tagWithUrlObj){
            jumpUrl = tagWithUrlObj.getString("jumpUrl");
            tags = tagWithUrlObj.getString("tags");
        }

        //设置历史告警页面查看地址, 将此动作和推送tag关联起来，便于前端使用,其也不必要关注使用哪个key来设置了
        if (!TextUtils.isEmpty(jumpUrl)){
            setKeyValue(Constants.PUSH_MESSAGE_JUMP_URL_KEY, jumpUrl);
        }

        AFLog.d(TAG,"setJPushTag tags="+tags);
        if (!TextUtils.isEmpty(tags)){
            String[] tagStr = tags.split(",");

            List<String> list = Arrays.asList(tagStr);
            Set<String> tagSet = new HashSet<>(list);
            // 设置是覆盖逻辑
            JPushInterface.setTags(mContext, 2020, tagSet);
            AFLog.d(TAG,"设置tag接口已经调用,等待结果...");
        }else {
            AFLog.e(TAG,"setJPushTag tags为空");
        }
    }


    /**
     * 功能：清除极光推送tag
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.cleanJPushTag()
     */
    @JavascriptInterface
    public void cleanJPushTag(){
        AFLog.d(TAG,"开始清除极光推送tag，等待结果...");
        JPushInterface.cleanTags(mContext, 2021);
    }


    public static List<String> getTagsList(String originalText) {
        if (TextUtils.isEmpty(originalText)) {
            return null;
        }
        List<String> tags = new ArrayList<String>();
        int indexOfComma = originalText.indexOf(',');
        String tag;
        while (indexOfComma != -1) {
            tag = originalText.substring(0, indexOfComma);
            tags.add(tag);

            originalText = originalText.substring(indexOfComma + 1);
            indexOfComma = originalText.indexOf(',');
        }

        tags.add(originalText);
        return tags;
    }

    /**
     * 功能：设置百度云消息推送tag
     * 参数：tagWithUrl 由标签和跳转页面组成的json字符串
     * tags 标签，如果有多个，以逗号分隔
     * jumpUrl 点击消息跳转页面路径
     * 返回值：无
     * 使用方式：window.functionTag.setBaiduPushTagAndJumpUrl(tagWithUrl)
     */
    @JavascriptInterface
    public void setBaiduPushTagAndJumpUrl(String tagWithUrl){

        String jumpUrl = null;
        String tags = null;
        com.alibaba.fastjson.JSONObject tagWithUrlObj = JSON.parseObject(tagWithUrl);
        if (null != tagWithUrlObj){
            jumpUrl = tagWithUrlObj.getString("jumpUrl");
            tags = tagWithUrlObj.getString("tags");
        }

        //设置历史告警页面查看地址, 将此动作和推送tag关联起来，便于前端使用,其也不必要关注使用哪个key来设置了
        if (!TextUtils.isEmpty(jumpUrl)){
            setKeyValue(Constants.PUSH_MESSAGE_JUMP_URL_KEY, jumpUrl);
        }

        AFLog.d(TAG,"setBaiduPushTagAndJumpUrl tags="+tags);
        if (!TextUtils.isEmpty(tags)){
            mBaiduPushOperate = "set";
            mBaiduPushTags = tags;
            // TODO: 先都获取，然后删除，最后设置
            PushManager.listTags(mContext);
            AFLog.d(TAG,"设置百度云消息推送tag接口已经调用,等待结果...");
        }else {
            AFLog.e(TAG,"setBaiduPushTagAndJumpUrl tags为空");
        }
    }


    /**
     * 功能：清除百度云消息推送tag
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.cleanBaiduPushTag()
     */
    @JavascriptInterface
    public void cleanBaiduPushTag(){
        AFLog.d(TAG,"开始清除百度云消息推送tag，等待结果...");
        mBaiduPushOperate = "clean";
        // TODO: 先都获取，然后删除
        PushManager.listTags(mContext);
    }


    /**
     * 功能：清除百度云消息推送tag
     * 参数：无
     * 返回值：无
     * 使用方式：window.functionTag.ShowBaiduPushTag()
     */
    @JavascriptInterface
    public void ShowBaiduPushTag(){
        AFLog.d(TAG,"开始获取百度云消息推送tag，等待结果...");
        mBaiduPushOperate = null;
        PushManager.listTags(mContext);
    }

    /**
     * 功能：设置个推消息推送tag
     * 参数：tagWithUrl 由标签和跳转页面组成的json字符串
     * tags 标签，如果有多个，以逗号分隔
     * jumpUrl 点击消息跳转页面路径
     * 返回值：无
     * 使用方式：window.functionTag.setGTPushTagAndJumpUrl(tagWithUrl)
     */
    @JavascriptInterface
    public void setGTPushTagAndJumpUrl(String tagWithUrl){

        String jumpUrl = null;
        String tags = null;
        com.alibaba.fastjson.JSONObject tagWithUrlObj = JSON.parseObject(tagWithUrl);
        if (null != tagWithUrlObj){
            jumpUrl = tagWithUrlObj.getString("jumpUrl");
            tags = tagWithUrlObj.getString("tags");
        }

        //设置历史告警页面查看地址, 将此动作和推送tag关联起来，便于前端使用,其也不必要关注使用哪个key来设置了
        if (!TextUtils.isEmpty(jumpUrl)){
            setKeyValue(Constants.PUSH_MESSAGE_JUMP_URL_KEY, jumpUrl);
        }

        AFLog.d(TAG,"setGTPushTagAndJumpUrl tags="+tags);
        if (!TextUtils.isEmpty(tags)){
            String[] tagArr = tags.split(",");
            Tag[] tagParam = new Tag[tagArr.length];
            for (int i=0; i<tagArr.length; i++){
                Tag temp = new Tag();
                temp.setName(tagArr[i]);
                tagParam[i] = temp;
            }

            com.igexin.sdk.PushManager.getInstance().setTag(mContext, tagParam,
                    String.valueOf(System.currentTimeMillis()));

        }else {
            AFLog.e(TAG,"setGTPushTagAndJumpUrl tags为空");
        }
    }


    /**
     * 功能：科大讯飞开始录音识别
     * 参数：jsonParam 语音识别参数 json字符串
     *             "language":"zh_cn",
     *             "accent":"changshanese",
     *             "vadBos":"50000",
     *             "vadEos":"50000",
     *             "ptt":"1"
     * 返回值：无
     * 使用方式：window.functionTag.iflyStartRecord(jsonParam)
     */
    @JavascriptInterface
    public void  iflyStartRecord(String jsonParam){
        mSpeechJsonParam = jsonParam;
        AFLog.d(TAG, "iflyStartRecord jsonParam="+jsonParam);
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.RECORD_AUDIO)) {
            IFly iflySpeech = IFly.getInstance(mContext, mHandler);
            iflySpeech.startRecord(jsonParam);
        } else {
            mSpeechPlatform = Constants.SPEECH_IFLY;
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO},
                    Constants.PERMISSION_REQUEST_FOR_SPEECH_RECOGNIZE);
        }
    }

    /**
     * 功能：科大讯飞停止语音识别
     * 参数： 无
     * 返回值：无
     * 使用方式：window.functionTag.iflyStopRecord()
     */
    @JavascriptInterface
    public void   iflyStopRecord(){
        IFly iflySpeech = IFly.getInstance(mContext, mHandler);
        iflySpeech.stopRecord();
    }


    /**
     * 功能：百度开始录音识别
     * 参数：jsonParam 语音识别参数 json字符串
     *             "pid":"15372",  语言参数 https://ai.baidu.com/ai-doc/SPEECH/5khq3i39w
     *             "vad.endpoint-timeout":"0"  设置为0为长语音，此时开启和结束识别需要调用接口自己控制，否则默认为短语音识别，两句话之间超过800ms则会中断。
     * 返回值：无
     * 使用方式：window.functionTag.baiduStartRecord(jsonParam)
     */
    @JavascriptInterface
    public void  baiduStartRecord(String jsonParam){
        mSpeechJsonParam = jsonParam;
        AFLog.d(TAG, "baiduStartRecord jsonParam="+jsonParam);
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.RECORD_AUDIO)) {
            Baidu baiduSpeech = Baidu.getInstance(mContext, mHandler);
            baiduSpeech.startRecord(jsonParam);
        } else {
            // TODO： 授予权限之后要调用对应的接口开启语音识别
            mSpeechPlatform = Constants.SPEECH_BAIDU;
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO},
                    Constants.PERMISSION_REQUEST_FOR_SPEECH_RECOGNIZE);
        }
    }

    /**
     *  结束百度语音识别
     * 参数： 无
     * 返回值：无
     * 使用方式：window.functionTag.baiduStopRecord()
     */
    @JavascriptInterface
    public void  baiduStopRecord(){
        AFLog.d(TAG, "baiduStopRecord ");
        Baidu baiduSpeech = Baidu.getInstance(mContext, mHandler);
        baiduSpeech.stopRecord();
    }


    /**
     * 功能：腾讯开始录音识别
     * 参数：jsonParam 语音识别参数 json字符串
     * *https://cloud.tencent.com/document/product/1093/35799
     * language  语言引擎
     * vad 是否开启静音检测
     * vadTimeoout 静音检测超时时间
     * punc 识别内容中是否需要标点符号
     * 返回值：无
     * 使用方式：window.functionTag.tencentStartRecord(jsonParam)
     */
    @JavascriptInterface
    public void  tencentStartRecord(String jsonParam){
        mSpeechJsonParam = jsonParam;
        AFLog.d(TAG, "tencentStartRecord jsonParam="+jsonParam);
        if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.RECORD_AUDIO)) {
            Tencent tencentSpeech = Tencent.getInstance(mContext, mHandler);
            tencentSpeech.startRecord(jsonParam);
        } else {
            // TODO： 授予权限之后要调用对应的接口开启语音识别
            mSpeechPlatform = Constants.SPEECH_TENCENT;
            ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.RECORD_AUDIO},
                    Constants.PERMISSION_REQUEST_FOR_SPEECH_RECOGNIZE);
        }
    }


    /**
     * 结束腾讯语音识别
     * 参数： 无
     * 返回值：无
     * 使用方式：window.functionTag.tencentStopRecord()
     */
    @JavascriptInterface
    public void  tencentStopRecord(){
        AFLog.d(TAG, "tencentStopRecord ");
        Tencent tencentSpeech = Tencent.getInstance(mContext, mHandler);
        tencentSpeech.stopRecord();
    }


    /**
     * 对外统一接口，调用开启语音识别，并且60秒超时后自动结束本地识别
     * 参数   识别平台 ifly baidu tencent ，
     * 例如 params = {
     *             "speechPlatform":"tencent"
     *             }
     * 返回值  无
     * @param jsonParam
     */
    @JavascriptInterface
    public void  speechRecStartTimeout60S(String jsonParam){
        mSpeechJsonParam = jsonParam;
        AFLog.d(TAG, "speechRecStartTimeout60S jsonParam="+jsonParam);
        if (TextUtils.isEmpty(jsonParam)){
            AFLog.e(TAG,"speechRecStartTimeout60S param error");
            return;
        }else {
            // 先移除掉之前的超时回调
            SpeechRecTimeoutHandler.removeCallbacks(speechRecStop);
            com.alibaba.fastjson.JSONObject param = (com.alibaba.fastjson.JSONObject) JSON.parse(jsonParam);
            String speechPlatform = param.getString("speechPlatform");
            // TODO: 先都暂时使用默认配置，后续如果有定制需求，可以通过前端增加此参数来设置
            boolean useDefaultSetting = param.getBoolean("useDefaultSetting");

            if (BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                    BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.RECORD_AUDIO)) {
                if (Constants.SPEECH_IFLY.equals(speechPlatform)){
                    IFly iflySpeech = IFly.getInstance(mContext, mHandler);
                    iflySpeech.startRecord(null);
                }else if (Constants.SPEECH_BAIDU.equals(speechPlatform)){
                    Baidu baiduSpeech = Baidu.getInstance(mContext, mHandler);
                    baiduSpeech.startRecord(null);
                }else if (Constants.SPEECH_TENCENT.equals(speechPlatform)){
                    Tencent tencentSpeech = Tencent.getInstance(mContext, mHandler);
                    tencentSpeech.startRecord(null);
                }else {
                    AFLog.e(TAG, "speechRecStartTimeout60S param error");
                }
            } else {
                // TODO： 授予权限之后要调用对应的接口开启语音识别
                mSpeechPlatform = speechPlatform;
                ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO},
                        Constants.PERMISSION_REQUEST_FOR_SPEECH_RECOGNIZE);
            }
            // 调用超时时间后自动结束
            SpeechRecTimeoutHandler.postDelayed(speechRecStop, 60000);
        }
    }


    /**
     * 关闭语音识别统一接口，同时移除超时60秒自动关闭识别功能
     * 参数 识别平台 ifly baidu tencent ，直接传参数字符串即可，不用json格式
     * 返回值  无
     * @param speechPlatform
     */
    @JavascriptInterface
    public void  speechRecStopTimeout60S(String speechPlatform){
        AFLog.d(TAG, "speechRecStopTimeout60S speechPlatform="+speechPlatform);
        if (TextUtils.isEmpty(speechPlatform)){
            AFLog.e(TAG,"speechRecStopTimeout60S speechPlatform error");
            return;
        }else {
            if (Constants.SPEECH_IFLY.equals(speechPlatform)){
                // TODO: 鉴于科大讯飞是有在识别过程中是有反馈校正机制的，此处前端调用结束是否需要延迟处理
                IFly iflySpeech = IFly.getInstance(mContext, mHandler);
                iflySpeech.stopRecord();
            }else if (Constants.SPEECH_BAIDU.equals(speechPlatform)){
                Baidu baiduSpeech = Baidu.getInstance(mContext, mHandler);
                baiduSpeech.stopRecord();
            }else if (Constants.SPEECH_TENCENT.equals(speechPlatform)){
                Tencent tencentSpeech = Tencent.getInstance(mContext, mHandler);
                tencentSpeech.stopRecord();
            }else {
                AFLog.e(TAG, "speechRecStopTimeout60S param error");
            }
            // 移动超时后自动结束回调事件， 只需要remove即可，不关心run 里面执行的具体接口
            SpeechRecTimeoutHandler.removeCallbacks(speechRecStop);
        }
    }

}
