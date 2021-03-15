package com.base.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.base.bean.DeviceIdInfo;
import com.base.bean.ScanInfo;
import com.base.constant.Constants;
import com.base.utils.CameraFunctionUtil;
import com.base.utils.GsonHelper;
import com.base.utils.BaseAppUtil;

import com.base.utils.SpUtils;
import com.base.utils.ToastUtil;
import com.base.utils.UriUtils;
import com.base.utils.config.ParseConfig;
import com.base.utils.http.AjaxHandler;
import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;
import com.base.utils.log.LogScreenShowUtil;
import com.base.zxing.CaptureActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wendu.dsbridge.CompletionHandler;
import wendu.dsbridge.DWebView;


public class WebViewActivity extends AppCompatActivity {

    private String  TAG = "WebViewActivity";
    private DWebView mWebviewPage;
    private View mErrorView;
    private Context mContext;
    private ProgressBar mProgressBar;
    //记录点击返回键的时间戳
    private long mExitTime = 0;
    //适配Input标签
    private final int REQUEST_FILE_PICKER = 101;
    private ValueCallback<Uri> mFilePathCallback;
    private ValueCallback<Uri[]> mFilePathCallbacks;
    //用于在得到存储访问授权后，进行文件选择
    private Intent mHtmlInputIntent;
    //记录加载的URL
    private String mDefaultErrorUrl = "file:///android_asset/get_data_fail.html";
    private String mLoadUrl = ParseConfig.sFirstLoadPage;

    //用来在屏幕输出log的服务,停用
//    private LogService mLogService;
//    ServiceConnection conn = new ServiceConnection() {
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            AFLog.d(TAG,"onServiceDisconnected");
//        }
//
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            //返回一个MsgService对象
//            AFLog.d(TAG,"onServiceConnected");
//            mLogService = ((LogService.MsgBinder)service).getService();
//        }
//    };
    private boolean mIsDebugVersion;
    private boolean mFirstRun = true;
    //使用对象方式来显示Log
    private LogScreenShowUtil mLogShow;
    //发送log信息给服务
    private void sendLogMsg(String logLine){
        if (mIsDebugVersion  && BaseAppUtil.checkAlertWindowsPermission(mContext)){
            Message msg = new Message();
            msg.obj = logLine;
            // 注意权限没有获取的时候很多空指针
            if (null != LogScreenShowUtil.getInstance(WebViewActivity.this) &&
                    LogScreenShowUtil.getInstance(WebViewActivity.this).mFloatShow ){
                LogScreenShowUtil.getInstance(WebViewActivity.this).handler.sendMessage(msg);
            }
        }
    }

    //消息处理, 用来处理需要在主线程中调用的方法
    // (All WebView methods must be called on the same thread)
    private Handler mHandler =  new Handler(){
        public void handleMessage(Message msg) {
            String url;
            String emitName;
            switch (msg.what) {
                case Constants.MSG_FOR_LOCATION:
                    String locationJson =(String)msg.obj;
                    emitName = "location";
                    AFLog.d(TAG,"MSG_FOR_LOCATION locationJson=" + locationJson);
                    url = "javascript:Android.emit('"+ emitName + "','" + locationJson+"')";
                    mWebviewPage.loadUrl(url);
                    AFLog.d(TAG,"MSG_FOR_LOCATION url=" + url);
                    break;
                case Constants.MSG_FOR_NO_NETWORK:
                    //如果获取到的状态码为 -1，弹出网络提示
                    int code = (int)msg.obj;
                    if(code == -1){
                        ToastUtil.makeText(WebViewActivity.this, "请检查网络");
                    }
                    break;
                case Constants.MSG_FOR_GET_EZOPEN_VIDEO_LIST:
                    String videoInfoJson =(String)msg.obj;
                    AFLog.d(TAG,"MSG_FOR_GET_EZOPEN_VIDEO_LIST videoInfoJson=" + videoInfoJson);
                    mWebviewPage.loadUrl("javascript:getEZOpenVideoFromAndroid('"+videoInfoJson+"')");
                    break;
                case Constants.MSG_FOR_GET_MD5_IMEI:
                    String imeiMd5 =(String)msg.obj;
                    DeviceIdInfo deviceId = new DeviceIdInfo(imeiMd5);
                    String deviceIdJson = GsonHelper.toJson(deviceId);
                    emitName = "deviceId";
                    url = "javascript:Android.emit('"+ emitName + "','" + deviceIdJson +"')";
                    AFLog.d(TAG,"MSG_FOR_GET_MD5_IMEI imeiMd5=" + deviceIdJson);
                    mWebviewPage.loadUrl(url);
                    break;
                case Constants.MSG_FOR_GET_SCAN_INFO:
                    String scanInfo = (String)msg.obj;
                    emitName = "qrCode";
                    url = "javascript:Android.emit('"+ emitName + "','" + scanInfo+"')";
                    mWebviewPage.loadUrl(url);
                    AFLog.d(TAG,"MSG_FOR_GET_SCAN_INFO scanInfo=" + scanInfo);
                    break;
                default:
                    break;
            }
        }
    };

    //记录app更新服务器地址
    public static String mAppUpdateServerIP;
    //记录网络状态是否改变，给安卓5.0版本使用
    public static boolean mNoNetwork;
    //用来记录前端调用扫码的接口名字
    public static String mQrScanCallName;
    //记录获取存储权限的弹框，每次进入去检测一次
    private boolean mStorageForSaveLog = true;



    //本地调试H5
    private String localFile = "file:///android_asset/main.html";
    //http, 萤石云开发测试地址，直播流
    private String liveUrl = "http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.hd.m3u8";
    //https，录制视频播放
    private String videoUrl = "http://175.6.40.67:18894/baseApp/app/VID20170430200439.mp4";
    private String debug = "http://www.baidu.com";
    private String netVideoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_web_view);

        mWebviewPage = findViewById(R.id.webview_function_page);
        mProgressBar = findViewById(R.id.progressBar);
        //debug版本启动log输出到屏幕服务
        mIsDebugVersion = isDebugVersion(mContext);
        AFLog.d(TAG,"onCreate isDebugVersion=" + mIsDebugVersion);
        if (mIsDebugVersion){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!BaseAppUtil.checkAlertWindowsPermission(mContext) && mFirstRun) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent,Constants.OPEN_ALERT_WINDOW_PERMISSION);
                    mFirstRun = false;
                } else {
                    if (!LogScreenShowUtil.getInstance(WebViewActivity.this).mFloatShow){
                        LogScreenShowUtil.getInstance(WebViewActivity.this).createFloatView();
                    }
                }
            }

            if (Build.VERSION.SDK_INT > 19) {
                //设置debug版本的webview可以通过 chrome://inspect/#devices 调试
                WebView.setWebContentsDebuggingEnabled(true);
            }
        }

        WebSettings webSettings = mWebviewPage.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //设置缓存模式，无网络时依然可以打开已经打开过的网页
        if (BaseAppUtil.isNetworkAvailable(this)) {
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        } else {
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        //在Android5.0以下，默认是采用的MIXED_CONTENT_ALWAYS_ALLOW模式，
        // 即总是允许WebView同时加载Https和Http；而从Android5.0开始，默认用MIXED_CONTENT_NEVER_ALLOW模式，
        // 即总是不允许WebView同时加载Https和Http。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        initWebview();


        mWebviewPage.loadUrl(mLoadUrl);
        AFLog.i(TAG,"webview load url:" + mLoadUrl);
        //注册JS调用的natvie接口
        mWebviewPage.addJavascriptInterface(new JSInterface(mContext, WebViewActivity.this, mHandler), "functionTag");
        mWebviewPage.addJavascriptObject(new Object(){

            /**
             * Note: This method is for Fly.js
             * In browser, Ajax requests are sent by browser, but Fly can
             * redirect requests to native, more about Fly see  https://github.com/wendux/fly
             * @param requestData passed by fly.js, more detail reference https://wendux.github.io/dist/#/doc/flyio-en/native
             * @param handler
             */
            @JavascriptInterface
            public void onAjaxRequest(Object requestData, CompletionHandler handler){
                // Handle ajax request redirected by Fly
                AjaxHandler.onAjaxRequest((JSONObject)requestData,
                        handler,
                        mContext);
            }

        },null);
    }

    // TODO: 从通知栏跳转过来的告警列表页面显示处理；
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AFLog.d(TAG, "#####onNewIntent intent="+ ((null == intent)? null: intent.toString()));
        if (null != intent) {
            Bundle data = intent.getExtras();
            AFLog.d(TAG, "#####data = "+ (null ==data ? null : data.toString()));
            if (null != data){
                String url  = data.getString("url");
                AFLog.d(TAG, "#####url = "+ url);
                if (!TextUtils.isEmpty(url)){
                    mLoadUrl = url;
                    mWebviewPage.loadUrl(mLoadUrl);
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        AFLog.d(TAG,"onResume mIsDebugVersion=" + mIsDebugVersion);
        if (mIsDebugVersion){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!BaseAppUtil.checkAlertWindowsPermission(mContext)) {
                    AFLog.d(TAG, "onResume no alert window permission");
                } else {
                    if (!LogScreenShowUtil.getInstance(WebViewActivity.this).mFloatShow){
                        LogScreenShowUtil.getInstance(WebViewActivity.this).createFloatView();
                    }
                }
            }
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO: 为了存储错误日志信息，需要请求存储权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if ( (Build.VERSION.SDK_INT >= 23) &&
                            (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
                            mStorageForSaveLog) {
                        AFLog.d(TAG,"requestPermissions storage");
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSION_REQUEST_FOR_SAVE_LOG);
                        //防止反复弹框
                        mStorageForSaveLog = false;
                    }
                }

            }
        }, 500);

    }


    private static boolean isDebugVersion(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    protected void onDestroy() {
        super.onDestroy();
        AFLog.d(TAG,"webview onDestroy");
        if (mIsDebugVersion){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!BaseAppUtil.checkAlertWindowsPermission(mContext)) {
                    AFLog.d(TAG, "onDestroy no permission alert window.");
                } else {
                    if (LogScreenShowUtil.getInstance(WebViewActivity.this).mFloatShow){
                        LogScreenShowUtil.getInstance(WebViewActivity.this).removeFloatWindow();
                    }
                }
            }
        }
        // TODO: 清理缓存, 删除相关缓存目录下的文件
//        BaseAppUtil.clearCacheFile(this);
//
//        // TODO: 调用常规接口清理缓存
//        CookieSyncManager.createInstance(this);
//        CookieManager cookieManager = CookieManager.getInstance();
//        cookieManager.removeAllCookie();
//
//        //webview 自身接口清理记录
//        mWebviewPage.clearCache(true);
//        mWebviewPage.clearFormData();
//        mWebviewPage.clearHistory();
//
//        //其实还是无法回收内存
//        mWebviewPage.removeAllViews();
//        mWebviewPage.destroy();

        //自杀退出
        //android.os.Process.killProcess(android.os.Process.myPid());
    }


    /**
     * 处理返回键
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //返回键处理
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebviewPage.canGoBack()) {
            AFLog.d(TAG,"onKeyDown mWebviewPage can go back");
            // TODO: 需要判断最后的网页到底是什么,会出现无法返回退出的情况。
            //判断是否在主页面，如果是，则直接退出程序
            boolean isHomeUrl = false;
            if (null != ParseConfig.sHomeUrls){
                for (int i=0; i<ParseConfig.sHomeUrls.size(); i++){
                    if (mLoadUrl.equals(ParseConfig.sHomeUrls.get(i))){
                        isHomeUrl = true;
                        break;
                    }
                }
            }
            AFLog.d(TAG,"isHomeUrl=" + isHomeUrl);
            if (isHomeUrl){
                if ((System.currentTimeMillis() - mExitTime) > 2000) {
                    //弹出提示，可以有多种方式
                    ToastUtil.makeText(WebViewActivity.this, "再按一次退出程序");
                    mExitTime = System.currentTimeMillis();
                    return true;
                } else {
                    //在主页面连续两次按返回键，直接退出了
                    return super.onKeyDown(keyCode, event);
                }
            } else {
                //浏览器内返回历史页面处理
                // TODO: 考虑出错页面url是否会占据历史url,导致直接goback 还是加载无法访问的url
//                if ("file:///android_asset/show_error.html".equals(mLoadUrl)) {
//                    boolean backTwoSteps = mWebviewPage.canGoBackOrForward(-2);
//                    boolean backOneSteps = mWebviewPage.canGoBackOrForward(-1);
//                    mWebviewPage.goBack();
//                } else {
//                    mWebviewPage.goBack();
//                }
                //和前端定义好的事件，用来解决按返回键时，页面显示无动画效果问题
                String emitName = "back";
                String url = "javascript:Android.emit('"+ emitName + "')";
                mWebviewPage.loadUrl(url);
                mWebviewPage.goBack();
                return true;
            }
        } else {
            AFLog.d(TAG,"onKeyDown mWebviewPage can not go back, maybe not back key");
        }

        return super.onKeyDown(keyCode, event);
    }


    /**
     * 处理启动本地其他activity功能返回值处理
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        AFLog.d(TAG,"onActivityResult requestCode="+requestCode
                +" resultCode="+resultCode + " intent="+intent);

        // 处理各种返回值
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                //扫码
                case Constants.SCAN_QRCODE_REQUEST:
                    //返回给JS
                    String scanResult = null;
                    if (null != intent) {
                        //扫码结果
                        scanResult = intent.getStringExtra(CaptureActivity.KEY_DATA);
                    }
                    // TODO: 前端定义接口给native调用返回扫码结果
//                    mWebviewPage.loadUrl("javascript:getScanInfoFromAndroid('" + scanResult + "')");
                    ScanInfo  si = new ScanInfo(scanResult, mQrScanCallName);
                    String scanResultJson = GsonHelper.toJson(si);
                    Message msg = new Message();
                    msg.what = Constants.MSG_FOR_GET_SCAN_INFO;
                    msg.obj = scanResultJson;
                    mHandler.sendMessage(msg);
//                    ToastUtil.makeText(mContext, "扫码结果：" + scanResult);
                    break;
                //照相
                case Constants.TAKE_PHOTO_REQUEST:
                    AFLog.d(TAG,"onActivityResult TAKE_PHOTO_REQUEST ");
                    if (TextUtils.isEmpty(CameraFunctionUtil.fileFullName)) {
//                        ToastUtil.makeText(mContext, "拍照失败了");
                    } else {
//                        ToastUtil.makeText(mContext, "照片生成路径：" + CameraFunctionUtil.fileFullName);

                        // TODO: 扫描新生成的文件到媒体库
                        MediaScannerConnection.scanFile(this, new String[] { CameraFunctionUtil.fileFullName },
                                null, new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        AFLog.i(TAG, "Scanned " + path + ":");
                                        AFLog.i(TAG, "-> uri=" + uri);
                                    }
                                });
                    }
                    break;
                //录像
                case Constants.RECORD_VIDEO_REQUEST:
                    AFLog.d(TAG,"onActivityResult RECORD_VIDEO_REQUEST");
                    if (TextUtils.isEmpty(CameraFunctionUtil.fileFullName)) {
//                        ToastUtil.makeText(mContext, "录像失败了");
                    } else {
//                        ToastUtil.makeText(mContext, "录像生成路径：" + CameraFunctionUtil.fileFullName);
                        // TODO: 扫描新生成的文件到媒体库
                        MediaScannerConnection.scanFile(this, new String[] { CameraFunctionUtil.fileFullName },
                                null, new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        AFLog.i(TAG, "Scanned " + path + ":");
                                        AFLog.i(TAG, "-> uri=" + uri);
                                    }
                                });
                    }
                    break;
                //照相压缩并选择该文件
                case Constants.TAKE_PHOTO_AND_COMPRESS_UPLOAD_REQUEST:
                    AFLog.d(TAG,"onActivityResult TAKE_PHOTO_AND_COMPRESS_UPLOAD_REQUEST ");
                    String finalPhotoPath;
                    if (TextUtils.isEmpty(CameraFunctionUtil.fileFullName)) {
//                    ToastUtil.makeText(mContext, "拍照失败了");
                        finalPhotoPath = CameraFunctionUtil.fileFullName;
                    } else {
//                    ToastUtil.makeText(mContext, "照片生成路径：" + CameraFunctionUtil.fileFullName);
                        // 扫描新生成的文件到媒体库
                        MediaScannerConnection.scanFile(this, new String[] { CameraFunctionUtil.fileFullName },
                                null, new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        AFLog.i(TAG, "Scanned " + path + ":");
                                        AFLog.i(TAG, "-> uri=" + uri);
                                    }
                                });

                        // TODO: 压缩图片处理，然后以带有compressed 前缀存储
                        finalPhotoPath = CameraFunctionUtil.fileFullName;
                        Bitmap bm = BaseAppUtil.convertToBitmap(finalPhotoPath);
                        File file = new File(finalPhotoPath);
                        String originalName = file.getName();
                        int slashIndex = finalPhotoPath.lastIndexOf("/");
                        String newFileName = finalPhotoPath.substring(0, slashIndex+1) + "Compressed_"+ originalName;
                        finalPhotoPath = BaseAppUtil.saveBitToJpg(bm, newFileName);
                        AFLog.i(TAG, "compress image finalPhotoPath=" + finalPhotoPath);
                    }
                    //对 input 标签中设置  拍照路径回调，不论成败
                    setFilePathCallback(null, finalPhotoPath);
                    break;
                default:
                    break;
            }
        } else {
            // TODO: 清理全局变量文件路径
            CameraFunctionUtil.fileFullName = null;
            AFLog.d(TAG,"resultCode is not OK");
        }

        // TODO: resultCode OK or KO should process it for H5 input tag.
        switch (requestCode) {
            case REQUEST_FILE_PICKER:
                AFLog.d(TAG,"onActivityResult REQUEST_FILE_PICKER" +
                        " mFilePathCallback=" + mFilePathCallback +
                        " mFilePathCallbacks=" + mFilePathCallbacks);
                Uri result = (intent == null || resultCode != Activity.RESULT_OK) ? null : intent.getData();
                setFilePathCallback(result, null);
                break;
            //照相并选择该文件
            case Constants.TAKE_PHOTO_AND_UPLOAD_REQUEST:
                AFLog.d(TAG,"onActivityResult TAKE_PHOTO_AND_UPLOAD_REQUEST ");
                if (TextUtils.isEmpty(CameraFunctionUtil.fileFullName)) {
//                    ToastUtil.makeText(mContext, "拍照失败了");
                } else {
//                    ToastUtil.makeText(mContext, "照片生成路径：" + CameraFunctionUtil.fileFullName);
                    // TODO: 扫描新生成的文件到媒体库
                    MediaScannerConnection.scanFile(this, new String[] { CameraFunctionUtil.fileFullName },
                            null, new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    AFLog.i(TAG, "Scanned " + path + ":");
                                    AFLog.i(TAG, "-> uri=" + uri);
                                }
                            });
                }
                //对 input 标签中设置  拍照路径回调，不论成败
                setFilePathCallback(null, CameraFunctionUtil.fileFullName);
                break;
            //录像并选择该文件
            case Constants.RECORD_VIDEO_AND_UPLOAD_REQUEST:
                AFLog.d(TAG,"onActivityResult RECORD_VIDEO_AND_UPLOAD_REQUEST ");
                if (TextUtils.isEmpty(CameraFunctionUtil.fileFullName)) {
//                    ToastUtil.makeText(mContext, "录像失败了");
                } else {
//                    ToastUtil.makeText(mContext, "录像生成路径：" + CameraFunctionUtil.fileFullName);
                    // TODO: 扫描新生成的文件到媒体库
                    MediaScannerConnection.scanFile(this, new String[] { CameraFunctionUtil.fileFullName },
                            null, new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    AFLog.i(TAG, "Scanned " + path + ":");
                                    AFLog.i(TAG, "-> uri=" + uri);
                                }
                            });
                }
                //对 input 标签中设置  拍照路径回调，不论成败
                setFilePathCallback(null, CameraFunctionUtil.fileFullName);
                break;
            default:
                break;
        }
    }

    /**
     * 设置input 标签出发的回调选择文件路径，优先使用path参数，
     * 其次使用uri参数
     * @param uriParam
     * @param pathParam
     */
    private void setFilePathCallback(Uri uriParam, String pathParam) {
        //都为空，则设置null
        if (uriParam == null && pathParam == null) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            if (mFilePathCallbacks != null) {
                mFilePathCallbacks.onReceiveValue(null);
            }
        } else if (null != pathParam) { // 优先使用path
            if (mFilePathCallback != null) {
                Uri uri = Uri.fromFile(new File(pathParam));
                mFilePathCallback.onReceiveValue(uri);
            }
            if (mFilePathCallbacks != null) {
                Uri uri = Uri.fromFile(new File(pathParam));
                //将文件路径直接传给拦截接口使用；
                if (null != AjaxHandler.mFilePaths){
                    AjaxHandler.mFilePaths.clear();
                    AjaxHandler.mFilePaths.add(pathParam);
                }
                mFilePathCallbacks.onReceiveValue(new Uri[] { uri });
            }
        } else if (null != uriParam) { //其次使用uri
            if (mFilePathCallback != null) {
                String path = UriUtils.getPath(getApplicationContext(), uriParam);
                Uri uri = Uri.fromFile(new File(path));
                mFilePathCallback.onReceiveValue(uri);
            }
            if (mFilePathCallbacks != null) {
                String path = UriUtils.getPath(getApplicationContext(), uriParam);
                //将文件路径直接传给拦截接口使用；
                if (null != AjaxHandler.mFilePaths){
                    AjaxHandler.mFilePaths.clear();
                    AjaxHandler.mFilePaths.add(path);
                }
                Uri uri = Uri.fromFile(new File(path));
                mFilePathCallbacks.onReceiveValue(new Uri[] { uri });
            }
        }

        mFilePathCallback = null;
        mFilePathCallbacks = null;

    }


    /**
     * 权限请求结果处理
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults );
        AFLog.d(TAG,"onRequestPermissionsResult requestCode="+requestCode);
        switch (requestCode) {
            case Constants.PERMISSION_REQUEST_CAMERA_FOR_PHOTO:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        Intent it = CameraFunctionUtil.takePhoto(mContext);
                        startActivityForResult(it, Constants.TAKE_PHOTO_REQUEST);
                        AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_PHOTO permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机 或者 文件读写权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_PHOTO no grant result");
                    ToastUtil.makeText(mContext, "请打开相机 或者 文件读写权限");
                }
                break;

            case Constants.PERMISSION_REQUEST_CAMERA_FOR_VIDEO:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        Intent it = CameraFunctionUtil.recordVideo(mContext);
                        startActivityForResult(it, Constants.RECORD_VIDEO_REQUEST);
                        AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_VIDEO permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机  或者 文件读写权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_VIDEO no grant result");
                    ToastUtil.makeText(mContext, "请打开相机  或者 文件读写权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_CAMERA_FOR_SCAN:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        Intent it = new Intent();
                        it.setClass(mContext, com.base.zxing.CaptureActivity.class);
                        startActivityForResult(it, Constants.SCAN_QRCODE_REQUEST);
                        AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_SCAN permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开相机权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_CAMERA_FOR_SCAN no grant result");
                    ToastUtil.makeText(mContext, "请打开相机权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_LOCATION:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 8, mLocationListener);
                        new JSInterface(mContext, WebViewActivity.this, mHandler).requestLocation();
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_LOCATION permission get succeed");
                    } else {
                        ToastUtil.makeText(mContext, "请打开GPS定位权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_LOCATION no grant result");
                    ToastUtil.makeText(mContext, "请打开GPS定位权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_STORAGE:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_STORAGE permission get succeed");
                        //updateVersion(mAppUpdateServerIP);
                        new JSInterface(mContext, WebViewActivity.this, mHandler).updateVersion(mAppUpdateServerIP);
                    } else {
                        ToastUtil.makeText(mContext, "请打开存储权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_STORAGE no grant result");
                    ToastUtil.makeText(mContext, "请打开存储权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_EZOPENPLAY:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_EZOPENPLAY permission get succeed");
                        String stationId = BaseWebviewApp.getInstance().getStationId();
                        // TODO: 注意授权后后续播放流程
                        new JSInterface(mContext, WebViewActivity.this, mHandler).getVideoType(stationId);
                    } else {
                        ToastUtil.makeText(mContext, "请打开读取手机状态权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_EZOPENPLAY no grant result");
                    ToastUtil.makeText(mContext, "请打开读取手机状态权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_IMEI:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_EZOPENPLAY permission get succeed");
                        // TODO: 需要回调JS接口，将返回值传给H5
                        String imeiMD5 = new JSInterface(mContext, WebViewActivity.this, mHandler).getDeviceId();
                        mWebviewPage.loadUrl("javascript:getDeviceIdFromAndroid('"+imeiMD5+"')");
                    } else {
                        ToastUtil.makeText(mContext, "请打开读取手机状态权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_EZOPENPLAY no grant result");
                    ToastUtil.makeText(mContext, "请打开读取手机状态权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE permission get succeed");
                        // 授权后访问文件
                        WebViewActivity.this.startActivityForResult(Intent.createChooser(mHtmlInputIntent, "File Chooser"),
                                REQUEST_FILE_PICKER);
                    } else {
                        setFilePathCallback(null, null);
                        ToastUtil.makeText(mContext, "请打开文件存储权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE no grant result");
                    ToastUtil.makeText(mContext, "请打开文件存储权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO permission get succeed");
                        // 授权后拍照然后获取文件
                        Intent it = CameraFunctionUtil.takePhoto(mContext);
                        startActivityForResult(it, Constants.TAKE_PHOTO_AND_UPLOAD_REQUEST);
                    } else {
                        setFilePathCallback(null, null);
                        ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE no grant result");
                    ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_RECORD_VIDEO:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_RECORD_VIDEO permission get succeed");
                        // 授权后录像然后获取文件
                        Intent it = CameraFunctionUtil.recordVideo(mContext);
                        startActivityForResult(it, Constants.RECORD_VIDEO_AND_UPLOAD_REQUEST);
                    } else {
                        setFilePathCallback(null, null);
                        ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_RECORD_VIDEO no grant result");
                    ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                }
                break;
            case Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO_COMPRESS:
                if (null != grantResults && grantResults.length > 0) {
                    if (BaseAppUtil.permissionGrantedCheck(grantResults)){
                        AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO_COMPRESS permission get succeed");
                        // 授权后继续
                        Intent it = CameraFunctionUtil.takePhoto(mContext);
                        startActivityForResult(it, Constants.TAKE_PHOTO_AND_COMPRESS_UPLOAD_REQUEST);
                    } else {
                        setFilePathCallback(null, null);
                        ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                    }
                } else {
                    AFLog.d(TAG,"PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO_COMPRESS no grant result");
                    ToastUtil.makeText(mContext, "请打开摄像头和文件存储权限");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 显示出错页面
     */
    protected void showErrorPage() {
//        LinearLayout webParentView = (LinearLayout)mWebviewPage.getParent();
//        initErrorPage();//初始化自定义页面
//        while (webParentView.getChildCount() > 1) {
//            webParentView.removeViewAt(0);
//        }
//        @SuppressWarnings("deprecation")
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewPager.LayoutParams.FILL_PARENT, ViewPager.LayoutParams.FILL_PARENT);
//        webParentView.addView(mErrorView, 0, lp);
        // TODO: 需要添加h5页面来显示默认出错异常页面
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        boolean offlineMode = SpUtils.getBoolean(mContext, Constants.OFFLINE_MODE, false);
        if (!offlineMode){
            mWebviewPage.loadUrl(mDefaultErrorUrl);
        }
    }
    /****
     * 把系统自身请求失败时的网页隐藏
     */
    protected void hideErrorPage() {
        LinearLayout webParentView = (LinearLayout)mWebviewPage.getParent();
        while (webParentView.getChildCount() > 1) {
            webParentView.removeViewAt(0);
        }
    }
    /***
     * 显示加载失败时自定义的网页
     */
    protected void initErrorPage() {
        if (mErrorView == null) {
            mErrorView = View.inflate(this, R.layout.activity_error, null);
            RelativeLayout layout = (RelativeLayout)mErrorView.findViewById(R.id.online_error_btn_retry);
            // TODO: 众多页面在一个webview里面跑，需要管理 url 才知道加载哪个，所以不做重新加载。
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideErrorPage();
                    mWebviewPage.loadUrl("about:blank");
                    hideErrorPage();
                    mWebviewPage.loadUrl(localFile);
                    //mWebviewPage.reload();
                }
            });
            mErrorView.setOnClickListener(null);
        }
    }
    /**
     * 获取请求状态码
     *
     * @param url
     * @return 请求状态码
     */
    private int getResponseCode(String url) {
        try {
            URL getUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            return connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void  initWebview(){

        //设置下载回调
        mWebviewPage.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                //处理下载事件
                AFLog.d(TAG,"onDownloadStart url="+url);
//                BaseAppUtil.downloadByBrowser(mContext, url);
            }
        });

        //设置webview client
        mWebviewPage.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                AFLog.v(TAG," shouldOverrideUrlLoading url=" + url);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                AFLog.i(TAG,"onPageFinished url="+url);
                //记录当前URL，判断是否在主业面，处理返回键
                mLoadUrl = url;
                super.onPageFinished(view, url);
                String logLine = "I " + " url=" + url;
                sendLogMsg(logLine);
                // TODO: set orientation
                // 根据url 强制设置横竖屏模式
//                if (url.contains("XXXXX")){
//                    //设置横屏显示
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//                } else {
//                    //设置竖屏显示
//                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                AFLog.i(TAG,"onPageStarted url=" + url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onLoadResource(WebView view, String url){
                super.onLoadResource(view, url);
                // TODO: android5.0 版本，且无网络状态时，发出无网络提示
                if((Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
                        && mNoNetwork){
                    final String getCodeUrl = url;
                    AFLog.d(TAG,"onLoadResource getCodeUrl="+ getCodeUrl);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // FIXME 此处是否还有必要请求
                            int responseCode = getResponseCode(getCodeUrl);
                            AFLog.d(TAG," onLoadResource responseCode=" + responseCode);
                            Message message = mHandler.obtainMessage();
                            message.what = Constants.MSG_FOR_NO_NETWORK;
                            message.obj = responseCode;
                            mHandler.sendMessage(message);
                        }
                    }).start();
                }

            }

            // 旧版本，会在新版本中也可能被调用，所以加上一个判断，防止重复显示
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    return;
                }
                super.onReceivedError(view, errorCode, description, failingUrl);
                AFLog.e(TAG," before M version onReceivedError description=" + description
                +" errorCode=" + errorCode);
                //记录到文件日志中
                LogSaveUtils.e(" before M version onReceivedError description=" + description
                        +" errorCode=" + errorCode);
                String logLine = "E " + " description=" + description +" errorCode=" + errorCode;
                sendLogMsg(logLine);
                // 在这里显示自定义错误页,只处理没有连接网络的情况
                //规避出现非断网情况下，errorCode也为-2，但是错误描述为ERR_NAME_NOT_RESOLVE时显示默认无网络页面的问题
                if ((errorCode == -2) && ("net::ERR_INTERNET_DISCONNECTED".equals(description))){
                    showErrorPage();//显示错误页面
                }
            }


            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (null != error) {
                    CharSequence errDes = error.getDescription();
                    int errorCode = error.getErrorCode();
                    AFLog.e(TAG," after M version onReceivedError errDes=" + errDes
                    + " errorCode=" + errorCode);
                    //记录到文件日志中
                    LogSaveUtils.e(" after M version onReceivedError errDes=" + errDes
                            + " errorCode=" + errorCode);
                    String logLine = "E " + " errDes=" + errDes +" errorCode=" + errorCode;
                    sendLogMsg(logLine);
                    // 在这里显示自定义错误页,只处理没有连接网络的情况
                    if ((errorCode == -2) && ("net::ERR_INTERNET_DISCONNECTED".equals(errDes))){
                        showErrorPage();//显示错误页面
                    }
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                int statusCode = 0;
                String reason = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    statusCode = errorResponse.getStatusCode();
                    reason = errorResponse.getReasonPhrase();
                }
                AFLog.e(TAG," onReceivedHttpError statusCode=" + statusCode
                + " reason=" + reason);
                //记录到文件日志中
                LogSaveUtils.e(" onReceivedHttpError statusCode=" + statusCode
                        + " reason=" + reason);
                String logLine = "E " + " HttpError statusCode=" + statusCode +" reason=" + reason;
                sendLogMsg(logLine);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //解决无信任证书访问的https I/X509Util: Failed to validate the certificate chain
                //handler.cancel(); 默认的处理方式，WebView变成空白页
                //接受证书
                handler.proceed();
                super.onReceivedSslError(view, handler, error);
                AFLog.v(TAG,"onReceivedSslError error=" + error);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                AFLog.v(TAG,"version lower than L shouldInterceptRequest url=" + url);
                //加载指定.js时 引导服务端加载本地js
                try{
                    if (!TextUtils.isEmpty(url)){
                        int urlLen = url.length();
                        int lastSlashIndex = url.lastIndexOf("/");
                        String lastPartUrl = "";
                        if (lastSlashIndex != -1  && urlLen > 0){
                            lastPartUrl =  url.substring(lastSlashIndex+1, urlLen);
                            AFLog.v(TAG,"version lower than L lastPartUrl=" + lastPartUrl);
                        }
                        //匹配url中最后部分是否符合vendor.d76e879c7a94a196e743.js 这种格式。
//                        String pattern = "vendor\\.[A-Za-z0-9]+\\.js";
//                        Pattern r = Pattern.compile(pattern);
//                        Matcher m = r.matcher(lastPartUrl);
//
//                        if ( m.matches()){
//                            return new WebResourceResponse("application/x-javascript","utf-8",
//                                    getBaseContext().getAssets().open("js/vendor.js"));
//                        }
                        // 前端将js版本号去除了，所以进行等于匹配查找
                        if (!TextUtils.isEmpty(lastPartUrl)){
                            for (int i=0; i<ParseConfig.sLocalJsFiles.size(); i++){
                                if (lastPartUrl.equals(ParseConfig.sLocalJsFiles.get(i))){
                                    AFLog.d(TAG,"version lower than L use local js file name:" + lastPartUrl);
                                    return new WebResourceResponse("application/x-javascript","utf-8",
                                            getBaseContext().getAssets().open("js/"+lastPartUrl));
                                }
                            }
                        }
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    e.printStackTrace();
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (null != request && null != request.getUrl()){
                        String url = request.getUrl().toString();
                        AFLog.v(TAG,"version higher than L shouldInterceptRequest url=" + url);
                        try{
                            if (!TextUtils.isEmpty(url)){
                                int urlLen = url.length();
                                int lastSlashIndex = url.lastIndexOf("/");
                                String lastPartUrl = "";
                                if (lastSlashIndex != -1  && urlLen > 0){
                                    lastPartUrl =  url.substring(lastSlashIndex+1, urlLen);
                                    AFLog.v(TAG,"version higher than L lastPartUrl=" + lastPartUrl);
                                }
                                //匹配url中最后部分是否符合vendor.d76e879c7a94a196e743.js 这种格式。
//                                String pattern = "vendor\\.[A-Za-z0-9]+\\.js";
//                                Pattern r = Pattern.compile(pattern);
//                                Matcher m = r.matcher(lastPartUrl);
//
//                                if ( m.matches()){
//                                    AFLog.d(TAG,"version higher than L url match vendor.js");
//                                    return new WebResourceResponse("application/x-javascript","utf-8",
//                                            getBaseContext().getAssets().open("js/vendor.js"));
//                                }
                                // 前端将js版本号去除了，所以进行等于匹配查找
                                if (!TextUtils.isEmpty(lastPartUrl)){
                                    for (int i=0; i<ParseConfig.sLocalJsFiles.size(); i++){
                                        if (lastPartUrl.equals(ParseConfig.sLocalJsFiles.get(i))){
                                            AFLog.d(TAG,"version higher than L use local js file name:" + lastPartUrl);
                                            return new WebResourceResponse("application/x-javascript","utf-8",
                                                    getBaseContext().getAssets().open("js/"+lastPartUrl));
                                        }
                                    }
                                }
                            }
                        }catch (IOException e) {
                            e.printStackTrace();
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        //设置加载过程 进度条
        mWebviewPage.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgressBar.setProgress(newProgress);
                if (newProgress ==  100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                super.onProgressChanged(view, newProgress);
            }

            // Android < 3.0 调用这个方法
            public void openFileChooser(ValueCallback<Uri> filePathCallback) {
                mFilePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                WebViewActivity.this.startActivityForResult(Intent.createChooser(intent, "File Chooser"),
                        REQUEST_FILE_PICKER);
            }
            // 3.0 + 调用这个方法
            public void openFileChooser(ValueCallback filePathCallback,
                                        String acceptType) {
                mFilePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                WebViewActivity.this.startActivityForResult(Intent.createChooser(intent, "File Chooser"),
                        REQUEST_FILE_PICKER);
            }
            // js上传文件的<input type="file" name="fileField" id="fileField" />事件捕获
            // Android > 4.1.1 调用这个方法
            public void openFileChooser(ValueCallback<Uri> filePathCallback,
                                        String acceptType, String capture) {
                mFilePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                WebViewActivity.this.startActivityForResult(Intent.createChooser(intent, "File Chooser"),
                        REQUEST_FILE_PICKER);
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onShowFileChooser(WebView webView,
                                             ValueCallback<Uri[]> filePathCallback,
                                             WebChromeClient.FileChooserParams fileChooserParams) {
                mFilePathCallbacks = filePathCallback;
                // TODO: 根据标签中得接收类型，启动对应的文件类型选择器
                String[] acceptTypes = fileChooserParams.getAcceptTypes();
                for (String type : acceptTypes) {
                    Log.d(TAG, "acceptTypes=" + type);
                }
                boolean cameraAndStorage = BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        && BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        && BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.CAMERA);

                // 针对拍照后马上进入上传状态处理
                if ((acceptTypes.length > 0) && acceptTypes[0].equals("image/example")) {
                    AFLog.d(TAG, "onShowFileChooser takePhoto permission cameraAndStorage=" + cameraAndStorage);
                    if (cameraAndStorage){
                        Intent it = CameraFunctionUtil.takePhoto(mContext);
                        startActivityForResult(it, Constants.TAKE_PHOTO_AND_UPLOAD_REQUEST);
                    } else {
                        ActivityCompat.requestPermissions(WebViewActivity.this,
                                new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        android.Manifest.permission.CAMERA },
                                Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO);
                    }
                    return true;
                }

                // 针对录像后马上进入上传状态处理
                if ((acceptTypes.length > 0) && acceptTypes[0].equals("video/example")) {
                    AFLog.d(TAG, "onShowFileChooser record video  permission cameraAndStorage=" + cameraAndStorage);
                    if (cameraAndStorage) {
                        Intent it = CameraFunctionUtil.recordVideo(mContext);
                        startActivityForResult(it, Constants.RECORD_VIDEO_AND_UPLOAD_REQUEST);
                    } else {
                        ActivityCompat.requestPermissions(WebViewActivity.this,
                                new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        android.Manifest.permission.CAMERA },
                                Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_RECORD_VIDEO);
                    }
                    return true;
                }

                // TODO: 拍照后进行图片压缩，然后将压缩图片填充到input标签中，等待上传
                if ((acceptTypes.length > 0) && acceptTypes[0].equals("image/compress")) {
                    AFLog.d(TAG, "onShowFileChooser take photo permission cameraAndStorage=" + cameraAndStorage);
                    if (cameraAndStorage) {
                        Intent it = CameraFunctionUtil.takePhoto(mContext);
                        startActivityForResult(it, Constants.TAKE_PHOTO_AND_COMPRESS_UPLOAD_REQUEST);
                    } else {
                        ActivityCompat.requestPermissions(WebViewActivity.this,
                                new String[]{ android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        android.Manifest.permission.CAMERA },
                                Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO_COMPRESS);
                    }
                    return true;
                }

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if (acceptTypes.length > 0) {
                    if (acceptTypes[0].contains("image")) {
                        intent.setType("image/*");
                    } else if (acceptTypes[0].contains("video")) {
                        intent.setType("video/*");
                    } else {
                        intent.setType("*/*");
                    }
                } else {
                    intent.setType("*/*");
                }

                // TODO: 需要申请文件读取权限
                //用于授权后执行文件访问
                mHtmlInputIntent = intent;
                boolean permissionResult = BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                            && BaseAppUtil.selfPermissionGranted(mContext, android.Manifest.permission.READ_EXTERNAL_STORAGE);
                if (permissionResult){
                    WebViewActivity.this.startActivityForResult(Intent.createChooser(intent, "File Chooser"),
                            REQUEST_FILE_PICKER);
                } else {
                    ActivityCompat.requestPermissions(WebViewActivity.this,
                            new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE);
                }

                return true;
            }
        });
    }


}
