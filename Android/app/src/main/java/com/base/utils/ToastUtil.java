package com.base.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class ToastUtil {
    private static Toast mToast;

    /**
     * 使用getApplicationContext作为context,防止activity引用造成内存泄漏
     * * @param context
     * @param msg
     */
    public static void makeText(Context context, String msg) {
        if ((mToast == null) && (null!= context)) {
            mToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(msg);
        }
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    public static void makeTextShowLong(Context context, String msg) {
        if ((mToast == null) && (null!= context)) {
            mToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG);
        } else {
            mToast.setText(msg);
        }
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }
}
