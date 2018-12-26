package com.cpe.ijkplayer.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cpe.ijkplayer.IjkVideoView;
import com.cpe.ijkplayer.R;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkVideoPlayActivity extends AppCompatActivity {

    private static final String TAG = "IjkVideoPlayActivity";
    private String mVideoPath;
    private Uri mVideoUri;
    private IjkVideoView mVideoView;
    //视频加载完成之前的等待进度提示
    private LinearLayout mLoadProcess;
    //点击显示视频标题或者返回等信息
    private SurfaceView mVideoInfo;
    private boolean mIsVideoInfoVisble = true;
    private LinearLayout mTitleInfo;
    //控制面板
    private LinearLayout mControlPanel;
//    private ImageView mPlayPauseButton;
    private SeekBar mSeekBar;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private LinearLayout ll_load_fail;
    private ImageView btnplayOrPause;
    private boolean isFullScreen = false;
    ImageView fullscreenImage = null;
    LinearLayout ll_back;

    public static Intent newIntent(Context context, String videoPath, String videoTitle) {
        Intent intent = new Intent(context, IjkVideoPlayActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("videoTitle", videoTitle);
        return intent;
    }

    public static void startActivityForIJKPlayer(Context context, String videoPath, String videoTitle) {
        context.startActivity(newIntent(context, videoPath, videoTitle));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ijkplayer_hnac_activity_ijk_video_play);
        //getSupportActionBar().hide();
        Log.d(TAG,"=====onCreate");

        ll_back = (LinearLayout) findViewById(R.id.video_play_back);
        ll_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"=====backIcon finish");
                finish();
            }
        });
        String videoTitle =  getIntent().getStringExtra("videoTitle");
        TextView title = (TextView) findViewById(R.id.video_title);
        title.setText(videoTitle);

        ll_load_fail=(LinearLayout)findViewById(R.id.ll_load_fail);

        mTitleInfo = (LinearLayout)findViewById(R.id.ll_title);
        //控制面板,需要等播放之后才显示出来
        mControlPanel = (LinearLayout)findViewById(R.id.control_panel);
        //播放按钮
   /*     mPlayPauseButton =(ImageView) findViewById(R.id.play_pause_button);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerProcess();
            }
        });*/
        //播放进度条
        mSeekBar = (SeekBar) findViewById(R.id.video_seek_bar);
        mCurrentTime = (TextView)findViewById(R.id.tv_currentTime);
        mTotalTime = (TextView)findViewById(R.id.tv_allTime);

        //设置各个控件的动作
        mVideoInfo = (SurfaceView) findViewById(R.id.video_info_surface);


        mLoadProcess = (LinearLayout) findViewById(R.id.video_play_progress_layout);

        // handle arguments
        mVideoPath = getIntent().getStringExtra("videoPath");

        btnplayOrPause = (ImageView) findViewById(R.id.videoinfo_playbtn);
        btnplayOrPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pause();
            }
        });

        fullscreenImage = (ImageView) findViewById(R.id.videoinfo_fullscreen);
        fullscreenImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                FullScreen();
            }
        });

        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (!TextUtils.isEmpty(intentAction)) {
            if (intentAction.equals(Intent.ACTION_VIEW)) {
                mVideoPath = intent.getDataString();
            } else if (intentAction.equals(Intent.ACTION_SEND)) {
                mVideoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    String scheme = mVideoUri.getScheme();
                    if (TextUtils.isEmpty(scheme)) {
                        Log.e(TAG, "Null unknown scheme\n");
                        finish();
                        return;
                    }
                    if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
                        mVideoPath = mVideoUri.getPath();
                    } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        Log.e(TAG, "Can not resolve content below Android-ICS\n");
                        finish();
                        return;
                    } else {
                        Log.e(TAG, "Unknown scheme " + scheme + "\n");
                        finish();
                        return;
                    }
                }
            }
        }

        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
        // prefer mVideoPath
        if (mVideoPath != null)
            mVideoView.setVideoPath(mVideoPath);
        else if (mVideoUri != null)
            mVideoView.setVideoURI(mVideoUri);
        else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }
        //将所有控件都找出来初始化之后，再来进行统一配置，放置空指针出现
        playVideoConfig();
    }

    private void FullScreen() {
        if (!isFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            if(mVideoView.isPlaying()){
                btnplayOrPause.setVisibility(View.VISIBLE);
                msg_hndlr.sendEmptyMessageDelayed(102, 3000);//隔3秒标题，进度条等消失
            }
            fullscreenImage.setVisibility(View.GONE);
            ll_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FullScreen();
                }
            });
            isFullScreen = true;

        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            if(mVideoView.isPlaying()){
                btnplayOrPause.setVisibility(View.GONE);
            }
            fullscreenImage.setVisibility(View.VISIBLE);
            ll_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            isFullScreen = false;

        }
//        setVideoSize();
    }

    // 暂停
    private void Pause() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            btnplayOrPause.setImageResource(R.drawable.ic_video_stop);
            if(!isFullScreen){
                btnplayOrPause.setVisibility(View.VISIBLE);
            }
        } else {
            mVideoView.start();
            btnplayOrPause.setImageResource(R.drawable.ic_video_play_l);
            if(!isFullScreen){
                btnplayOrPause.setVisibility(View.GONE);
            }else{
                msg_hndlr.sendEmptyMessageDelayed(102, 3000);//隔3秒标题，进度条等消失
            }

        }

    }

    Handler msg_hndlr = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case 101:
//                    setVideoSize();
                    break;
                case 102://隔3秒标题，进度条等消失
                    if(mVideoView.isPlaying()&&isFullScreen){
                        mTitleInfo.setVisibility(View.GONE);
                        mControlPanel.setVisibility(View.GONE);
                        btnplayOrPause.setVisibility(View.GONE);
//                        isVisble=false;
                    }

                    break;
            }

            return false;
        }
    });

    private void initSurfaceClick(){
        mVideoInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(!isFullScreen){
                    Pause();
                }else{
                    if (mIsVideoInfoVisble) {
                        mTitleInfo.setVisibility(View.GONE);
                        mControlPanel.setVisibility(View.GONE);
                        btnplayOrPause.setVisibility(View.GONE);
                        mIsVideoInfoVisble = false;
                    } else {
                        mTitleInfo.setVisibility(View.VISIBLE);
                        mControlPanel.setVisibility(View.VISIBLE);
                        mIsVideoInfoVisble = true;
                        if(mLoadProcess.getVisibility()!=View.VISIBLE){
                            btnplayOrPause.setVisibility(View.VISIBLE);
                        }
//                        msg_hndlr.sendEmptyMessageDelayed(102, 3000);//隔3秒标题，进度条等消失
                    }
                }

                return false;
            }
        });
    }

    private void playVideoConfig() {
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener(){
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG,"=====playVideo onPrepared");
                initSurfaceClick();
                //隐藏加载进度
                mLoadProcess.setVisibility(View.GONE);
                //显示播放控制面板
                mControlPanel.setVisibility(View.VISIBLE);
                fullscreenImage.setVisibility(View.VISIBLE);
                //设置播放进度
                mSeekBar.setMax(mVideoView.getDuration());
                mSeekBar.setOnSeekBarChangeListener(new SeekBarChangeListener());
                mSeekBar.setEnabled(true);
//                mSeekBar.setVisibility(View.VISIBLE);
                mHandler.post(updateThread);
            }
        });

        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener(){
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                Log.e(TAG,"info:"+i);
                if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    mLoadProcess.setVisibility(View.VISIBLE);
                    if(!isFullScreen){
                        if(!mVideoView.isPlaying()){
                            btnplayOrPause.setVisibility(View.GONE);
                        }
                    }else{
                        btnplayOrPause.setVisibility(View.GONE);
                    }
                } else if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    // 此接口每次回调完START就回调END,若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
                    mLoadProcess.setVisibility(View.GONE);
                    if(!isFullScreen){
                        if(!mVideoView.isPlaying()){
                            btnplayOrPause.setVisibility(View.VISIBLE);
                        }
                    }else{
                        if(mControlPanel.getVisibility()==View.VISIBLE){
                            btnplayOrPause.setVisibility(View.VISIBLE);
                        }
                    }
                }
                return true;
            }
        });

        mVideoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                mHandler.removeCallbacks(updateThread);
                mSeekBar.setMax(mVideoView.getDuration());
                mSeekBar.setProgress(0);
                mSeekBar.setSecondaryProgress(0);
                mSeekBar.setEnabled(true);
                //点击可以重新播放
//                mPlayPauseButton.setImageResource(R.drawable.ijkplayer_hnac_stop);
                mHandler.post(updateThread);
                mVideoView.start();

            }
        });

        mVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Log.e("error","error number"+i+";"+i1);
                //隐藏加载进度
                mLoadProcess.setVisibility(View.GONE);
                ll_load_fail.setVisibility(View.VISIBLE);
                btnplayOrPause.setVisibility(View.GONE);
                mControlPanel.setVisibility(View.GONE);
                fullscreenImage.setVisibility(View.GONE);
                if(mVideoView!=null){
                    mVideoView.stopPlayback();
                    mVideoView.release(true);
                    mVideoView.stopBackgroundPlay();
                }
                return false;
            }
        });
    }

    // 播放处理，只支持播放和暂停
//    private void playerProcess() {
//        if (mVideoView.isPlaying()) {
//            mVideoView.pause();
//            mPlayPauseButton.setImageResource(R.drawable.ijkplayer_hnac_stop);
//        } else {
//            mVideoView.start();
//            mPlayPauseButton.setImageResource(R.drawable.ijkplayer_hnac_play);
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        mVideoView.start();
        Log.d(TAG,"=====onStart start play video");
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "=====onPause");
        super.onPause();

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "=====onResume");
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "=====onStop isBackgroundPlayEnabled=" + mVideoView.isBackgroundPlayEnabled());
        //没有设置后台播放，直接停掉
        if (!mVideoView.isBackgroundPlayEnabled()) {
            mVideoView.stopPlayback();
            mVideoView.release(true);
            mVideoView.stopBackgroundPlay();
        } else {
//                mVideoView.enterBackground();
            mVideoView.pause();
            btnplayOrPause.setImageResource(R.drawable.ic_video_play_l);
            if(isFullScreen){
                mTitleInfo.setVisibility(View.GONE);
                mControlPanel.setVisibility(View.GONE);
            }
            btnplayOrPause.setVisibility(View.GONE);
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "=====onDestroy");
        mVideoView.stopPlayback();
        mVideoView.release(true);
        mVideoView.stopBackgroundPlay();
    }

    // 进度条控制
    private class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            // TODO Auto-generated method stub
            if (progress >= 0) {
                // 如果是用户手动拖动控件，则设置视频跳转。
                if (fromUser) {
                    mVideoView.seekTo(progress);
                }

                int allCount = mVideoView.getDuration();
                int currentCount = mVideoView.getCurrentPosition();

                mCurrentTime.setText(String.format("%02d:%02d",
                        (int) currentCount / 60000,
                        (int) (currentCount / 1000) % 60));
                mTotalTime.setText(String.format("%02d:%02d", allCount / 60000,
                        (allCount / 1000) % 60));

            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub

        }
    }

    // 播放进度条更新进程
    Handler mHandler = new Handler();
    Runnable updateThread = new Runnable() {
        public void run() {
            if (mVideoView != null) {
                mSeekBar.setProgress(mVideoView.getCurrentPosition());
                mHandler.postDelayed(updateThread, 100);
            }
        }
    };

}
