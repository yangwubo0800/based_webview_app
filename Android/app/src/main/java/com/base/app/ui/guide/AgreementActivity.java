package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.base.app.R;
import com.base.app.WebViewActivity;
import com.base.constant.Constants;
import com.base.utils.SpUtils;
import com.base.utils.config.ParseConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class AgreementActivity extends Activity {

    private String TAG = "AgreementActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        TextView tips = findViewById(R.id.tips);
        // 读取文本显示
        String tipsStr = getContentFromAsset("agreementTip.txt");
        tips.setText(tipsStr);

        TextView agreement = findViewById(R.id.agreenment_detail);
        TextView privacy = findViewById(R.id.privacy_detail);
        agreement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(AgreementActivity.this, AgreementDetailActivity.class);
                it.putExtra("infoType","agreement");
                startActivity(it);
            }
        });

        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(AgreementActivity.this, AgreementDetailActivity.class);
                it.putExtra("infoType","privacy");
                startActivity(it);
            }
        });

        Button agree = findViewById(R.id.agree);
        Button disagree = findViewById(R.id.disagree);
        agree.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //记录到sharePreference
                SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
                SpUtils.putBoolean(AgreementActivity.this, Constants.FIRST_OPEN_KEY, false);
                Intent it = null;
                if (ParseConfig.sNeedGuidePage){
                    it = new Intent(AgreementActivity.this, WelcomeGuideActivity.class);
                }else {
                    it = new Intent(AgreementActivity.this, WebViewActivity.class);
                }
                startActivity(it);
                finish();
            }
        });


        disagree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //退出程序
                finish();
            }
        });
    }



    public String getContentFromAsset(String fileName) {
        String str = null;
        try {
            InputStream inputStream = getAssets().open(fileName);

            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer("");
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            //赋值字符串，关闭资源
            str = sb.toString();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"getContentFromAsset str is:"+ str);
        return str;
    }

}