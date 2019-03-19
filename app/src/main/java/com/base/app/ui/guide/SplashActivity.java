package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.base.app.ModeChooseActivity;
import com.base.constant.Constants;
import com.base.app.R;
import com.base.app.WebViewActivity;
import com.base.utils.SpUtils;
import com.base.utils.config.ParseConfig;

/**
 * @desc 启动屏
 * Created by devilwwj on 16/1/23.
 */
public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 判断是否是第一次开启应用
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        boolean isFirstOpen = SpUtils.getBoolean(this, Constants.FIRST_OPEN_KEY, true);
        // 如果是第一次启动，则先进入功能引导页
        if (isFirstOpen && ParseConfig.sNeedGuidePage) {
            Intent intent = new Intent(this, WelcomeGuideActivity.class);
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
    }

    private void enterHomeActivity() {
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
        finish();
    }
}