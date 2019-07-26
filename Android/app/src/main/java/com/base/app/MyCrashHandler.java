package com.base.app;

import android.content.Context;

import com.base.utils.log.AFLog;
import com.base.utils.log.LogSaveUtils;


public class MyCrashHandler implements Thread.UncaughtExceptionHandler {
    private String TAG = "MyCrashHandler";
    private Context mContext;
    private Thread.UncaughtExceptionHandler defaultUncaught;

    public MyCrashHandler(Context context){
        super();
        mContext = context;
        defaultUncaught = Thread.getDefaultUncaughtExceptionHandler();
        AFLog.d(TAG,"MyCrashHandler defaultUncaught=" + defaultUncaught);
        Thread.setDefaultUncaughtExceptionHandler(this); // 设置为当前线程默认的异常处理器
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        AFLog.d(TAG,"uncaughtException defaultUncaught=" + defaultUncaught);
        LogSaveUtils.e("uncaughtException", e);
        // 如果我们没处理异常，并且系统默认的异常处理器不为空，则交给系统来处理
        if(defaultUncaught != null){
            defaultUncaught.uncaughtException(t, e);
        }
    }
}
