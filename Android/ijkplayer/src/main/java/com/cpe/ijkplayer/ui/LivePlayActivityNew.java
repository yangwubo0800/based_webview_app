package com.cpe.ijkplayer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cpe.ijkplayer.IjkVideoView;
import com.cpe.ijkplayer.R;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnBufferingUpdateListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnCompletionListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnErrorListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 直播播放页面
 */
public class LivePlayActivityNew extends Activity {

	private static final String TAG = "LivePlayActivityNew";
	private Context mContext;
	private String path;
	private IjkVideoView mVideoView;
	LinearLayout liveplay_layoutprogress = null;
	LinearLayout btnClose = null;
	LinearLayout ll_title=null;
	private boolean isVisble= true;
	SurfaceView videoinfo_surface=null;
	LinearLayout ll_load_fail;
	private TextView tv_title;

	public static void startActivityForLPAN(Context context,String liveUrl,String liveName){
		context.startActivity(newSelfIntent(context,liveUrl,liveName));
	}

	private static Intent newSelfIntent(Context context,String liveUrl,String liveName){
		Intent intent = new Intent(context, LivePlayActivityNew.class);
		intent.putExtra("path", liveUrl);
		intent.putExtra("title", liveName);
		return intent;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mContext = this;
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		Intent intent = getIntent();
		if(intent.getStringExtra("path")!=null){
			path=intent.getStringExtra("path");
		}
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		this.getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_live_play);
		ll_title=(LinearLayout)findViewById(R.id.ll_title);
		ll_load_fail=(LinearLayout)findViewById(R.id.ll_load_fail);
		tv_title=(TextView)findViewById(R.id.tv_title);
		if(intent.getStringExtra("title")!=null){
			tv_title.setText(intent.getStringExtra("title"));
		}
		videoinfo_surface=(SurfaceView)findViewById(R.id.videoinfo_surface);
		mVideoView = (IjkVideoView) findViewById(R.id.video_view);

		videoinfo_surface.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(isVisble){
					ll_title.setVisibility(View.INVISIBLE);
					isVisble=false;
				}else{
					ll_title.setVisibility(View.VISIBLE);
					isVisble=true;
				}
			}
		});
		btnClose = (LinearLayout) findViewById(R.id.liveplay_close);
		btnClose.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		liveplay_layoutprogress = (LinearLayout) findViewById(R.id.liveplay_layoutprogress);
		playVideo();
	}

	private void playVideo() { 
		try {
//			path="rtmp://live.hkstv.hk.lxdns.com/live/hks";
//			path="http://hls.open.ys7.com/openlive/f01018a141094b7fa138b9d0b856507b.m3u8";
			Uri mVideoUri = Uri.parse(path);
			mVideoView.setVideoURI(mVideoUri);
			mVideoView.start();
			mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
				@Override
				public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
					if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_START)
					{
						liveplay_layoutprogress.setVisibility(View.VISIBLE);
					} else if (i == IMediaPlayer.MEDIA_INFO_BUFFERING_END)
					{
						liveplay_layoutprogress.setVisibility(View.GONE);
					}
					return true;
				}
			});
			mVideoView.setOnErrorListener(new OnErrorListener() {

				@Override
				public boolean onError(IMediaPlayer mp, int what, int extra) {
					Log.e(TAG,"onError:"+what);
					// TODO Auto-generated method stub
					liveplay_layoutprogress.setVisibility(View.GONE);
					ll_load_fail.setVisibility(View.VISIBLE);
					return false;
				}
			});
			mVideoView.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(IMediaPlayer mp) {
					// TODO Auto-generated method stub
					if (mVideoView != null) {
						mVideoView.stopPlayback();
						mVideoView.release(true);
						mVideoView.stopBackgroundPlay();
					} else {
						mVideoView.enterBackground();
					}
					IjkMediaPlayer.native_profileEnd();

					Toast.makeText(mContext, "播放完成", Toast.LENGTH_SHORT);
					finish();
				}
			});
			mVideoView.setOnPreparedListener(new OnPreparedListener() {

				@Override
				public void onPrepared(IMediaPlayer mp) {
					// TODO Auto-generated method stub
					liveplay_layoutprogress.setVisibility(View.GONE);
				}
			});
			mVideoView.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

				@Override
				public void onBufferingUpdate(IMediaPlayer mp, int percent) {
					// TODO Auto-generated method stub

				}
			});

			mVideoView.toggleAspectRatio();

			setVolumeControlStream(AudioManager.STREAM_MUSIC);

		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if ( !mVideoView.isBackgroundPlayEnabled()) {
			mVideoView.stopPlayback();
			mVideoView.release(true);
			mVideoView.stopBackgroundPlay();
		} else {
			mVideoView.enterBackground();
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mVideoView.stopPlayback();
		mVideoView.release(true);
		mVideoView.stopBackgroundPlay();
//		IjkMediaPlayer.native_profileEnd();
	}

}
