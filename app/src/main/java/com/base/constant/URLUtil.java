package com.base.constant;


import android.text.TextUtils;
import android.util.Log;

import com.base.utils.SpUtils;
import android.content.Context;

/**
 * <网络请求url地址>
 * <功能详细描述>
 *
 * @author XXX
 * @version [版本号, 2016/6/6]
 * @see [相关类/方法]
 * @since [V1]
 */
public class URLUtil {

    private static String TAG = "URLUtil";




    /**
     * 更新版本
     */
    public static final String VERSION_UPDATE ="mobile/sys/getVersion.do";


    /**
     * 获取最新的告警信息
     */
    public static final String GET_LEAST_ALART="energy/soe/getLastSoeInfo.do";


    /**
     * 获取监控视频类型列表
     */
    public static final String GET_VIDEO_TYPE_LIST="mobile/sys/getVideoMonitorList";

    /**
     * 获取萤石云的AccessToken
     */
    public static final String GetAccessToken= "https://open.ys7.com/api/lapp/token/get";

    /**
     * 获取萤石云设备列表
     */
    public static final String GetESODeviceList= "https://open.ys7.com/api/lapp/device/list";


    /**
     * 前端登录成功后，调用设置，IP地址或者域名
     * @param context
     * @param serverIP
     */
    public static void setServerIP(Context context, String serverIP){
        if (!TextUtils.isEmpty(serverIP)){
            SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
            SpUtils.putString(context, Constants.SERVER_ADDRESS, serverIP);
        }else {
            Log.e(TAG,"setServerIP serverIP is empty");
        }

    }

    /**
     * 获取IP或者域名
     * @param context
     * @return
     */
    public static String getServerIP(Context context){
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        String address = SpUtils.getString(context, Constants.SERVER_ADDRESS);
        return address;
    }

    /**
     * 获取带有HTTP协议前缀的地址
     * @return
     */
    public static String getHTTPServerIP(Context context){
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        String address = SpUtils.getString(context, Constants.SERVER_ADDRESS);
        return "http://" + address + "/";
    }

}
