package com.base.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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

import com.base.utils.SpUtils;
import com.example.ezrealplayer.ui.EZRealPlayActivity;
import com.example.ezrealplayer.util.EZUtils;
import com.base.bean.AppInfo;
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
import com.base.utils.CameraFunctionUtil;
import com.base.utils.GeneralUtils;
import com.base.utils.GsonHelper;
import com.base.utils.BaseAppUtil;
import com.base.utils.NotificationUtils;
import com.base.utils.OkGoUpdateHttpUtil;
import com.base.utils.ToastUtil;
import com.base.utils.http.ITRequestResult;
import com.base.utils.http.OkHttpUtil;
import com.base.utils.http.Param;
import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;
import com.base.app.ui.guide.PicassoPhotoViewActivity;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.UpdateCallback;
import com.videogo.constant.IntentConsts;
import com.videogo.exception.BaseException;
import com.videogo.openapi.EZOpenSDK;
import com.videogo.openapi.bean.EZCameraInfo;
import com.videogo.openapi.bean.EZDeviceInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.base.app.WebViewActivity.mQrScanCallName;

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
                    // 当address 解析为空时，也返回超时timeout
                    if (TextUtils.isEmpty(locationAddress)){
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
        {
            /**
             * sdk日志开关，正式发布需要去掉
             */
            EZOpenSDK.showSDKLog(true);

            /**
             * 设置是否支持P2P取流,详见api
             */
            EZOpenSDK.enableP2P(false);

            /**
             * APP_KEY请替换成自己申请的
             */
            EZOpenSDK.initLib(BaseWebviewApp.getInstance(),appKey,"");
//            EZOpenSDK.initLib(this, AppKey, "");
        }
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
        EZOpenSDK.getInstance().setAccessToken(accessToken);
        getCameraList();
    }

    // TODO: 如果是前端直接请求等待获取完成的话，此处是否需要做成同步，不适用线程？
    public void getCameraList(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //每次获取前先清理掉全局变量中的内容;
                    if (null != list) {
                        list.clear();
                    }
                    List<EZDeviceInfo> result = null;
//                    showLoading();
                    result = EZOpenSDK.getInstance().getDeviceList(0, 50);
                    if(result!=null){
//                        hideLoading();
                        for (EZDeviceInfo info:result) {
                            List<EZCameraInfo>  cameraInfos= info.getCameraInfoList();
                            if(cameraInfos.size()>0){
                                for (EZCameraInfo cameraInfo:cameraInfos) {
                                    list.add(new Video(cameraInfo.getDeviceSerial(),cameraInfo.getCameraName(),"", GeneralUtils.getRightNowDateString2(),"01",cameraInfo,info));
                                }
                            }else{
                                list.add(new Video(info.getDeviceSerial(),info.getDeviceName(),"", GeneralUtils.getRightNowDateString2(),"0",info));
                            }
//                            list.add(new Video(info.getDeviceSerial(),info.getDeviceName(),"", GeneralUtils.getRightNowDateString2(),"0",info));
                        }
                        // TODO: 获取完列表之后，回调前端接口，将转换成gson字符串返回给前端，
                        AFLog.d(TAG,"getCameraList list size =" + list.size());
                        Video video = list.get(0);
                        //Log.d(TAG,"=====video 0 is" + video.toString());
                        String videoInfo = GsonHelper.toJson(video);
                        //Log.d(TAG,"=====GsonHelper videoInfo=" + videoInfo);
                        //发消息到主线程
                        Message msg = new Message();
                        msg.what = Constants.MSG_FOR_GET_EZOPEN_VIDEO_LIST;
                        msg.obj = videoInfo;
                        mHandler.sendMessage(msg);
//                        EZOpenPlay(videoInfo);
                        String videoList = GsonHelper.toJson(list);
                        //Log.d(TAG,"=====GsonHelper videoList=" + videoList);
//                        Message msg = new Message();
//                        msg.what = ESO_FLAG;
//                        handler.sendMessage(msg);
                    }
                } catch (BaseException e) {
//                    hideLoading();
                    //videoPresenter.getAccessToken(EZAppKey,EZAppSecret);;
                    AFLog.e(TAG, "BaseException" );
                }
            }
        }).start();
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
        Video playVideo = GsonHelper.toType(videoInfo, Video.class);

        if(playVideo.getVideoType().equals("0")){
            // 从gson转换会的video类中先获取对象
            Object deviceObj = playVideo.getObject();
            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
            //异常，可能是直接获取的对象不符合gson格式
            String deviceStr = GsonHelper.toJson(deviceObj);
            //将gson格式后的object转换为具体类对象
            EZDeviceInfo  deviceInfo = GsonHelper.toType(deviceStr, EZDeviceInfo.class);

            EZCameraInfo  cameraInfo = EZUtils.getCameraInfoFromDevice(deviceInfo, 0);
            if (cameraInfo == null) {
                return;
            }
            Intent intent = new Intent(mContext, EZRealPlayActivity.class);
            intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
            intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, deviceInfo);
            mContext.startActivity(intent);

        }else if(playVideo.getVideoType().equals("01")){
            //EZCameraInfo  cameraInfo = (EZCameraInfo)playVideo.getObject();
            // 从gson转换会的video类中先获取对象
            Object cameraObj = playVideo.getObject();
            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
            //异常，可能是直接获取的对象不符合gson格式
            String cameraStr = GsonHelper.toJson(cameraObj);
            //将gson格式后的object转换为具体类对象
            EZCameraInfo  cameraInfo = GsonHelper.toType(cameraStr, EZCameraInfo.class);
            if (cameraInfo == null) {
                return;
            }

            // 从gson转换会的video类中先获取对象
            Object deviceObj = playVideo.getObject2();
            //将对象在此json化，否则后面转成类时会报 MalformedJsonException: Unterminated object at line
            //异常，可能是直接获取的对象不符合gson格式
            String deviceStr = GsonHelper.toJson(deviceObj);
            //将gson格式后的object转换为具体类对象
            EZDeviceInfo  deviceInfo = GsonHelper.toType(deviceStr, EZDeviceInfo.class);

            Intent intent = new Intent(mContext, EZRealPlayActivity.class);
            intent.putExtra(IntentConsts.EXTRA_CAMERA_INFO, cameraInfo);
            //intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, (EZDeviceInfo)playVideo.getObject2());
            intent.putExtra(IntentConsts.EXTRA_DEVICE_INFO, deviceInfo);
            mContext.startActivity(intent);

        }
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
}
