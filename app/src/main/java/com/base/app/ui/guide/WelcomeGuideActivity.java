package com.base.app.ui.guide;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.base.constant.Constants;
import com.base.app.R;
import com.base.utils.SpUtils;
import com.base.utils.config.ParseConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 欢迎页
 *
 * @author wwj_748
 *
 */
public class WelcomeGuideActivity extends Activity implements OnClickListener {

    private String TAG = "WelcomeGuideActivity";
    private ViewPager vp;
    private GuideViewPagerAdapter adapter;
    private List<View> views;
    private Button startBtn;

    // 引导页图片资源
    private static final int[] pics = { R.layout.guid_view1,
            R.layout.guid_view2, R.layout.guid_view3, R.layout.guid_view4 };
    // 图片资源
    private static final int[] guidePics = {R.mipmap.pic_guidepage_1, R.mipmap.pic_guidepage_2,
            R.mipmap.pic_guidepage_3, R.mipmap.pic_guidepage_4, R.mipmap.pic_guidepage_5};

    // 底部小点图片
    private ImageView[] dots;

    // 记录当前选中位置
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        LinearLayout dotLayout = findViewById(R.id.ll);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;

        for (int i=0; i<ParseConfig.sBootPageCount; i++) {
            ImageView iv = new ImageView(this);
            iv.setLayoutParams(params);
            ViewGroup.MarginLayoutParams marginParam = new ViewGroup.MarginLayoutParams(iv.getLayoutParams());
            //marginParam.setMargins(20, 20, 20, 20);
            iv.setImageResource(R.drawable.dot_selector);
            iv.setClickable(true);
            iv.setPadding(20, 20, 20, 20);
            //ViewGroup.MarginLayoutParams margin = new ViewGroup.MarginLayoutParams()
            dotLayout.addView(iv);
        }


        views = new ArrayList<View>();

        // 初始化引导页视图列表
//        for (int i = 0; i < pics.length; i++) {
//            View view = LayoutInflater.from(this).inflate(pics[i], null);
//            ImageView iv = view.findViewById(R.id.guide_imageview);
//            Log.d(TAG, "=====iv=" + iv);
//            if (null != iv){
//                iv.setImageResource(R.drawable.ic_app_logo);
//            }
//            if (i == pics.length - 1) {
//                startBtn = (Button) view.findViewById(R.id.btn_login);
//                startBtn.setTag("enter");
//                startBtn.setOnClickListener(this);
//            }
//
//            views.add(view);
//
//        }
        // limit the size from 2 to 5
        int index;
        if (ParseConfig.sBootPageCount < 2) {
            index =2;
        } else if (ParseConfig.sBootPageCount > 5){
            index = 5;
        } else {
            index = ParseConfig.sBootPageCount;
        }
        for (int i = 0; i < index; i++) {
            View view;
            view = LayoutInflater.from(this).inflate(R.layout.guide_view, null);
            if (i == index -1){
                view = LayoutInflater.from(this).inflate(R.layout.guide_view_end, null);
                startBtn = (Button) view.findViewById(R.id.btn_login);
                startBtn.setTag("enter");
                startBtn.setOnClickListener(this);
            }
            ImageView iv = view.findViewById(R.id.guide_imageview);
            if (null != iv){
                iv.setImageResource(guidePics[i]);
            }

            views.add(view);

        }

        vp = (ViewPager) findViewById(R.id.vp_guide);
        // 初始化adapter
        adapter = new GuideViewPagerAdapter(views);
        vp.setAdapter(adapter);
        vp.setOnPageChangeListener(new PageChangeListener());

        initDots();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果切换到后台，就设置下次不进入功能引导页
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        SpUtils.putBoolean(WelcomeGuideActivity.this, Constants.FIRST_OPEN_KEY, false);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initDots() {
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        dots = new ImageView[ParseConfig.sBootPageCount];

        // 循环取得小点图片
        for (int i = 0; i < ParseConfig.sBootPageCount; i++) {
            // 得到一个LinearLayout下面的每一个子元素
            dots[i] = (ImageView) ll.getChildAt(i);
            dots[i].setEnabled(false);// 都设为灰色
            dots[i].setOnClickListener(this);
            dots[i].setTag(i);// 设置位置tag，方便取出与当前位置对应
        }

        currentIndex = 0;
        dots[currentIndex].setEnabled(true); // 设置为白色，即选中状态

    }

    /**
     * 设置当前view
     *
     * @param position
     */
    private void setCurView(int position) {
        if (position < 0 || position >= ParseConfig.sBootPageCount) {
            return;
        }
        vp.setCurrentItem(position);
    }

    /**
     * 设置当前指示点
     *
     * @param position
     */
    private void setCurDot(int position) {
        if (position < 0 || position > ParseConfig.sBootPageCount || currentIndex == position) {
            return;
        }
        dots[position].setEnabled(true);
        dots[currentIndex].setEnabled(false);
        currentIndex = position;
    }

    @Override
    public void onClick(View v) {
        if (v.getTag().equals("enter")) {
            enterMainActivity();
            return;
        }

        int position = (Integer) v.getTag();
        setCurView(position);
        setCurDot(position);
    }


    private void enterMainActivity() {
        Intent intent = new Intent(WelcomeGuideActivity.this,
                SplashActivity.class);
        startActivity(intent);
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        SpUtils.putBoolean(WelcomeGuideActivity.this, Constants.FIRST_OPEN_KEY, false);
        finish();
    }

    private class PageChangeListener implements OnPageChangeListener {
        // 当滑动状态改变时调用
        @Override
        public void onPageScrollStateChanged(int position) {
            // arg0 ==1的时辰默示正在滑动，arg0==2的时辰默示滑动完毕了，arg0==0的时辰默示什么都没做。

        }

        // 当前页面被滑动时调用
        @Override
        public void onPageScrolled(int position, float arg1, int arg2) {
            // arg0 :当前页面，及你点击滑动的页面
            // arg1:当前页面偏移的百分比
            // arg2:当前页面偏移的像素位置

        }

        // 当新的页面被选中时调用
        @Override
        public void onPageSelected(int position) {
            // 设置底部小点选中状态
            setCurDot(position);
        }

    }
}