package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.base.app.R;
import com.base.utils.log.AFLog;
import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Picasso;

public class PicassoPhotoViewActivity extends Activity{

    private String TAG = "PicassoPhotoViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photoview_simple);

        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        AFLog.d(TAG,"=======onCreate url=" + url);
        final PhotoView photoView = findViewById(R.id.iv_photo);
        //String url = "http://mmbiz.qpic.cn/mmbiz/PwIlO51l7wuFyoFwAXfqPNETWCibjNACIt6ydN7vw8LeIwT7IjyG3eeribmK4rhibecvNKiaT2qeJRIWXLuKYPiaqtQ/0";
        Picasso.with(this)
                .load(url)
                .into(photoView);
    }
}
