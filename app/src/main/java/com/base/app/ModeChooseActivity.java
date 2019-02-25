package com.base.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.base.constant.Constants;
import com.base.utils.SpUtils;


public class ModeChooseActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_choose);
        findViews();
    }

    private void findViews() {
        Button online = findViewById(R.id.online_mode);
        Button offline = findViewById(R.id.offline_mode);
        online.setOnClickListener(this);
        offline.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.online_mode:
                SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
                SpUtils.putBoolean(this, Constants.OFFLINE_MODE, false);
                break;
            case R.id.offline_mode:
                SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
                SpUtils.putBoolean(this, Constants.OFFLINE_MODE, true);
                break;
        }

        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
    }
}
