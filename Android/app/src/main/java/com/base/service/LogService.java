package com.base.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.base.app.R;
import com.base.utils.NotificationUtils;
import com.base.utils.log.AFLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class LogService extends Service {

    private String TAG = "LogService";
    private ListView listview;
    private LinkedList<LogLine> logList = new LinkedList<LogLine>();
    private LogAdapter mAdapter;
    private final int MAX_LINE = 3;
    private SimpleDateFormat LOGCAT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");


    //定义浮动窗口布局
    LinearLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
    WindowManager mWindowManager;
    Button mHideFloatWindow;
    public boolean mFloatShow =false;


    @Override
    public void onCreate()
    {
        // TODO Auto-generated method stub
        super.onCreate();
        AFLog.i(TAG, "onCreate");
    }

    @Override
    public IBinder onBind(Intent arg0) {
        AFLog.i(TAG, "onBind createFloatView");
        createFloatView();
        return new MsgBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        AFLog.i(TAG, "onUnbind removeFloatWindow");
        removeFloatWindow();
        return super.onUnbind(intent);
    }

    public class MsgBinder extends Binder {
        /**
         * 获取当前Service的实例
         * @return
         */
        public LogService getService(){
            return LogService.this;
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        AFLog.d(TAG,"LogService onStartCommand");
        startForeground(103, NotificationUtils.getInstance(this).getNotification(null));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        AFLog.i(TAG, "onDestroy");
        super.onDestroy();
    }

    public void createFloatView()
    {
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        AFLog.i(TAG, "mWindowManager--->" + mWindowManager);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

         /*// 设置悬浮窗口长宽数据
        wmParams.width = 200;
        wmParams.height = 80;*/

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        mFloatShow = true;
        //浮动窗口按钮
//        mFloatView = (Button)mFloatLayout.findViewById(R.id.float_id);
        mHideFloatWindow = mFloatLayout.findViewById(R.id.hide_float);
        mHideFloatWindow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFloatWindow();
            }
        });

        listview = mFloatLayout.findViewById(R.id.log_listview);
        logList = new LinkedList<LogLine>();
        mAdapter = new LogAdapter(this, logList);
        listview.setAdapter(mAdapter);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        AFLog.i(TAG, "Width/2--->" + mFloatLayout.getMeasuredWidth()/2);
        AFLog.i(TAG, "Height/2--->" + mFloatLayout.getMeasuredHeight()/2);
        //设置监听浮动窗口的触摸移动
        mFloatLayout.setOnTouchListener(new View.OnTouchListener()
        {

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                // TODO Auto-generated method stub
                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
                wmParams.x = (int) event.getRawX() - mFloatLayout.getMeasuredWidth()/2;
                AFLog.i(TAG, "RawX" + event.getRawX());
                AFLog.i(TAG, "X" + event.getX());
                //减25为状态栏的高度
                wmParams.y = (int) event.getRawY() - mFloatLayout.getMeasuredHeight()/2 - 25;
                AFLog.i(TAG, "RawY" + event.getRawY());
                AFLog.i(TAG, "Y" + event.getY());
                //刷新
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;  //此处必须返回false，否则OnClickListener获取不到监听
            }
        });

//        mFloatLayout.setOnClickListener(new View.OnClickListener()
//        {
//
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//                Toast.makeText(LogService.this, "onClick", Toast.LENGTH_SHORT).show();
//            }
//        });
    }


    private void removeFloatWindow(){
        // TODO Auto-generated method stub
        if(mFloatLayout != null) {
            //移除悬浮窗口
            mWindowManager.removeView(mFloatLayout);
        }
        mFloatShow = false;
    }


    class LogAdapter extends ArrayAdapter<LogLine> {

        private LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        public LogAdapter(Context context, List<LogLine> objects) {
            super(context, 0, objects);
        }

        public void add(LogLine line) {
            logList.add(line);
            notifyDataSetChanged();
        }

        @Override
        public LogLine getItem(int position) {
            return logList.get(position);
        }

        @Override
        public int getCount() {
            return logList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LogLine line = getItem(position);
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = inflator.inflate(R.layout.log_line, parent, false);
                holder.time = (TextView) convertView.findViewById(R.id.log_time);
                holder.content = (TextView) convertView.findViewById(R.id.log_content);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.time.setText(line.time);
            holder.content.setText(line.content);
            if (line.color != 0) {
                holder.content.setTextColor(line.color);
            } else {
                holder.content.setTextColor(getResources().getColor(android.R.color.white));
            }
            return convertView;
        }

    }

    class ViewHolder {
        public TextView time;
        public TextView content;
    }

    private void buildLogLine(String line) {
        LogLine log = new LogLine();
        log.time = LOGCAT_TIME_FORMAT.format(new Date()) + ": ";
        if (line.startsWith("I")) {
            log.color = Color.parseColor("#008f86");
        } else if (line.startsWith("V")) {
            log.color = Color.parseColor("#fd7c00");
        } else if (line.startsWith("D")) {
            log.color = Color.parseColor("#8f3aa3");
        } else if (line.startsWith("E")) {
            log.color = Color.parseColor("#fe2b00");
        }

        log.content = line;

        while (logList.size() > MAX_LINE) {
            logList.remove();
        }
        mAdapter.add(log);
    }

    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            buildLogLine(msg.obj.toString());
        };
    };

    class LogLine {
        public String time;
        public String content;
        public int color;
    }

}
