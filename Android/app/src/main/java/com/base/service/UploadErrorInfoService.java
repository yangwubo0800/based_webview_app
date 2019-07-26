package com.base.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.base.app.BaseWebviewApp;
import com.base.constant.URLUtil;
import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 进入应用时检测是否有错误日志文件，使用服务上传
 */
public class UploadErrorInfoService extends IntentService {

    private String TAG = "UploadErrorInfoService";
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public UploadErrorInfoService(String name) {
        super(name);
    }

    public UploadErrorInfoService() {
        super("UploadErrorInfoService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        AFLog.d(TAG, "onHandleIntent");
        // TODO: 上传文件和设备信息
        String uploadApi = "http://hydro.hnaccloud.com/hznet/app/error/submit";
        if (TextUtils.isEmpty(URLUtil.getServerIP(this))){
            //使用默认域名
            uploadApi = "http://xxxxxxx/";
        }else {
            //使用前端设置域名
            uploadApi = "http://" + URLUtil.getServerIP(this) + "/" + "xxxxx";
        }
        OkHttpClient okHttpClient = new OkHttpClient();
        String errorFilePath = LogSaveUtils.PATH_LOG_INFO  + LogSaveUtils.ERROR_LOG_FILE_NAME;
        final File file = new File(errorFilePath);
        String phoneBrand = "";
        String phoneVersion = "";
        String versionName = "";
        String versionCode = "";
        String packageName = "";

        try {
            Context context = BaseWebviewApp.getInstance().getApplicationContext();
            PackageManager pm = context.getPackageManager();
            //得到该应用的信息
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),PackageManager.GET_ACTIVITIES);
            if(pi != null)
            {
                versionName = pi.versionName == null ? "null" : pi.versionName;
                versionCode = pi.versionCode + "";
                packageName = pi.packageName;
                phoneBrand = android.os.Build.BRAND;
                phoneVersion = android.os.Build.VERSION.RELEASE;
                AFLog.d(TAG,"app  info versionName=" + versionName +
                        " versionCode=" + versionCode + " phoneBrand=" + phoneBrand
                        +" phoneVersion= "+ phoneVersion);

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            AFLog.d(TAG,"获取信息失败");
        }

        //同时上传参数和文件
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.addFormDataPart("deviceBrand", phoneBrand);
        builder.addFormDataPart("platformVersion ", phoneVersion);
        builder.addFormDataPart("appPackageName", packageName);
        builder.addFormDataPart("appVersionName", versionName);
        builder.addFormDataPart("appVersionCode", versionCode);
        String TYPE = "application/octet-stream";
        RequestBody fileBody = RequestBody.create(MediaType.parse(TYPE), file);

        RequestBody requestBody = builder
                .setType(MultipartBody.FORM)
                .addFormDataPart("files",file.getName(),fileBody)
                .build();

        Request request = new Request.Builder()
                .url(uploadApi)
                .post(requestBody)
                .build();
        AFLog.d(TAG,"post request url is " + uploadApi );
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                AFLog.d(TAG, "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                AFLog.d(TAG, "onResponse: " + response.protocol() + " " + response.code() + " " + response.message());
                Headers headers = response.headers();
                for (int i = 0; i < headers.size(); i++) {
                    AFLog.d(TAG, headers.name(i) + ":" + headers.value(i));
                }
                Log.d(TAG, "onResponse: " + response.body().string());
                // TODO: 上传成功，删除日志
                if (file.exists()){
                    file.delete();
                    AFLog.d(TAG,"delete the error info file");
                }
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        AFLog.d(TAG,"onDestroy");
    }
}
