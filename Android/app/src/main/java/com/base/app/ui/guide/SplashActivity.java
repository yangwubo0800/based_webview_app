package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.base.app.ModeChooseActivity;
import com.base.constant.Constants;
import com.base.app.R;
import com.base.app.WebViewActivity;
import com.base.service.UploadErrorInfoService;
import com.base.utils.SpUtils;
import com.base.utils.config.ParseConfig;
import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;

import java.io.File;

/**
 * @desc 启动屏
 * Created by devilwwj on 16/1/23.
 */
public class SplashActivity extends Activity {

    private String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 判断是否是第一次开启应用
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        boolean isFirstOpen = SpUtils.getBoolean(this, Constants.FIRST_OPEN_KEY, true);
        // 如果是第一次启动，则先进入功能引导页
        if (isFirstOpen ) {
            //首先进入用户协议，点击同意后再判断是否要进入向导页面
            Intent intent = new Intent(this, AgreementActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 如果不是第一次启动app，则正常显示启动屏
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                enterHomeActivity();
            }
        }, 2000);

        // TODO: 检测是否有错误日志文件，如果有，启动上传服务；
        String errorFilePath = LogSaveUtils.PATH_LOG_INFO  + LogSaveUtils.ERROR_LOG_FILE_NAME;
        File file = new File(errorFilePath);
        if (file.exists()){
            AFLog.d(TAG," start service upload error");
            startService(new Intent(this, UploadErrorInfoService.class));
        }

    }

    private void enterHomeActivity() {
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
        finish();
    }
}