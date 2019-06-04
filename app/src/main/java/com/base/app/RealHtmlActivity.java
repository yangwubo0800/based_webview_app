package com.base.app;

import android.annotation.TargetApi;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;


public class RealHtmlActivity extends AppCompatActivity {

    private String TAG = "RealHtmlActivity";
    private String url;
    private WebView webView;
    /**
     * 返回按钮
     */
    protected LinearLayout back;

    /**
     * 标题
     */
    protected TextView title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_real_html);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);

        url = getIntent().getStringExtra("url");
        AFLog.d(TAG,"url:"+url);

        webView = (WebView)findViewById(R.id.real_html);
        back = (LinearLayout) findViewById(R.id.ll_back);
        title = (TextView) findViewById(R.id.tv_title);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //title.setText(menuName);

        // 清除浏览器缓存
        webView.clearCache(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setSupportZoom(true);
        // 设置出现缩放工具
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(true); //将图片调整到适合webview的大小
        webView.getSettings().setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient(){

            @Override
            public void onLoadResource(WebView view, String url) {
                //Log.i(TAG,"onLoadResource url="+url);
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                AFLog.i(TAG,"onPageFinished url="+url);
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                view.loadUrl(url);// 根据传入的参数再去加载新的网页LayoutAlgorithm
                view.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
                view.getSettings().setLoadWithOverviewMode(true);
                return true;//不再借用系统浏览器

            }

            // 旧版本，会在新版本中也可能被调用，所以加上一个判断，防止重复显示
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    return;
                }
                // 只有无网络的情况才显示默认出错页面
                if (errorCode == -2){
                    showErrorPage();//显示错误页面
                }
                Log.d(TAG,"onReceivedError smaller than M errorCode=" + errorCode
                        + " description=" + description);
                LogSaveUtils.e("failingUrl:" + failingUrl);
                LogSaveUtils.e("onReceivedError smaller than M errorCode=" + errorCode
                        + " description=" + description);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);

                if (null != error) {
                    CharSequence errDes = error.getDescription();
                    int errorCode = error.getErrorCode();
                    Log.e(TAG," after M version onReceivedError errDes=" + errDes
                            + " errorCode=" + errorCode);
                    String url = request.getUrl().toString();
                    LogSaveUtils.e("url:" + url);
                    LogSaveUtils.e(" after M version onReceivedError errDes=" + errDes
                            + " errorCode=" + errorCode);
//                    if((null != request) && (null!= request.getUrl())) {
//                        String url = request.getUrl().toString();
//                        //net::ERR_CONNECTION_REFUSED
//                        //http://md.1drj.com/fingerprintjs.min.js
//                        if (url.contains("fingerprintjs") &&
//                                (errorCode == -6)){
//                            Log.e(TAG," visit fingerprintjs, just ignore ERR_CONNECTION_REFUSED error");
//                            return;
//                        }
//                    }
                    // 只有无网络的情况才显示默认出错页面
                    if (errorCode == -2){
                        showErrorPage();//显示错误页面
                    }
                }

            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                //showErrorPage();//显示错误页面
                int statusCode = 0;
                String reason = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    statusCode = errorResponse.getStatusCode();
                    reason = errorResponse.getReasonPhrase();
                    String url = request.getUrl().toString();
                    LogSaveUtils.e("url:" + url);
                }
                Log.e(TAG," onReceivedHttpError statusCode=" + statusCode
                        + " reason=" + reason);
                LogSaveUtils.e(" onReceivedHttpError statusCode=" + statusCode
                        + " reason=" + reason);
            }

        });

        // 添加给遥控授权校验使用，H5画面中需要获取手机imi
        webView.addJavascriptInterface(new Object(){
//            @JavascriptInterface
//            public String getDeviceId(){
//                String imei = null;
//                String imeiMD5 = null;
//                boolean checkResult = GeneralUtils.selfPermissionGranted(HtmlActivity_h5_2.this, android.Manifest.permission.READ_PHONE_STATE);
//                if (checkResult){
//                    //get device id
//                    imei = GeneralUtils.getDeviceId(HtmlActivity_h5_2.this);
//                    imeiMD5 = GeneralUtils.md5forString(imei);
//                } else {
//                    //动态申请权限
//                    Log.d(TAG,"getDeviceId request READ_PHONE_STATE Permissions");
//                    ToastUtil.makeText(HtmlActivity_h5_2.this, "请打开读取手机状态权限");
//                    ActivityCompat.requestPermissions(HtmlActivity_h5_2.this,
//                            new String[]{android.Manifest.permission.READ_PHONE_STATE},
//                            0);
//                }
//                return imeiMD5;
//            }

            @JavascriptInterface
            public void setOrientation(String orientation){
                Log.d(TAG, "#####setOrientation orientation=" + orientation);
                if (!TextUtils.isEmpty(orientation)){
                    //和前端约定：0 随屏幕旋转，1 横屏， 2 竖屏
                    if ("0".equals(orientation)){
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    } else if("1".equals(orientation)){
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else if("2".equals(orientation)){
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }
                }
            }
        }, "android");

        webView.loadUrl(url);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int orientation = newConfig.orientation;
        // 重新加载url
        Log.d(TAG,"#####onConfigurationChanged url=" +url);
        webView.loadUrl(url);
    }



    boolean mIsErrorPage;
    private View mErrorView;
    protected void showErrorPage() {
        LinearLayout webParentView = (LinearLayout)webView.getParent();
        initErrorPage();//初始化自定义页面
        while (webParentView.getChildCount() > 1) {
            webParentView.removeViewAt(0);
        }
        @SuppressWarnings("deprecation")
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewPager.LayoutParams.FILL_PARENT, ViewPager.LayoutParams.FILL_PARENT);
        webParentView.addView(mErrorView, 0, lp);
        mIsErrorPage = true;
    }
    /****
     * 把系统自身请求失败时的网页隐藏
     */
    protected void hideErrorPage() {
        LinearLayout webParentView = (LinearLayout)webView.getParent();
        mIsErrorPage = false;
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
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideErrorPage();
                    webView.loadUrl("about:blank");
                    hideErrorPage();
                    webView.loadUrl(url);
//                    webView.reload();

                }
            });
            mErrorView.setOnClickListener(null);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        AFLog.d(TAG," onDestroy");
    }
}
