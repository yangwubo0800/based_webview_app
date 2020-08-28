package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.base.app.R;
import com.base.utils.GeneralUtils;


public class AgreementDetailActivity extends Activity {

    private String agreementFileName = "agreement.txt";
    private String privacyFileName = "privacy.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_agreement_detail);
        super.onCreate(savedInstanceState);
        //根据传递的值不同，读取不同的文本内容，显示用户协议 或者 隐私政策

        LinearLayout back = (LinearLayout) findViewById(R.id.ll_back);
        TextView title = (TextView) findViewById(R.id.tv_title);
        //返回键结束此界面
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        Intent it = getIntent();
        String showInfo = "";
        if (null != it){
            String infoType = it.getStringExtra("infoType");
            if ("agreement".equals(infoType)){
                showInfo = GeneralUtils.getContentFromAsset(AgreementDetailActivity.this, agreementFileName);
                title.setText("用户协议");
            }else if("privacy".equals(infoType)){
                showInfo = GeneralUtils.getContentFromAsset(AgreementDetailActivity.this, privacyFileName);
                title.setText("隐私政策");
            }else {
                showInfo = "暂无数据";
            }
        }

        TextView textView = findViewById(R.id.content);
        textView.setText(showInfo);
    }

}
