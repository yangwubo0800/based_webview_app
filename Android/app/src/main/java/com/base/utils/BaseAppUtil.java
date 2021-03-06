package com.base.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.PermissionChecker;

import com.base.app.BaseWebviewApp;
import com.base.utils.log.AFLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BaseAppUtil {

    private static final String TAG = BaseAppUtil.class.getSimpleName();

    // 提供设置图片压缩的高和宽
    public static int mCompressImageWidth;
    public static int mCompressImageHeight;


    /**
     * 检测是否联网
     * @param activity
     * @return
     */
    public static boolean isNetworkAvailable(Context activity) {
        //得到应用上下文
        Context context = activity.getApplicationContext();
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    //删除文件夹和文件夹里面的文件
    public static void deleteDir(final String pPath) {
        File dir = new File(pPath);
        deleteDirWithFile(dir);
    }

    public static void deleteDirWithFile(File dir) {

        try {
            if (dir == null || !dir.exists() || !dir.isDirectory()) {
                return;
            }
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    file.delete(); // 删除所有文件
                }
                else if (file.isDirectory()) {
                    deleteDirWithFile(file); // 递规的方式删除文件夹
                }
            }
            dir.delete();// 删除目录本身
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 删除指定目录下的缓存文件
     * @param context
     */
    public static void clearCacheFile(Context context) {

        // TODO: 清理缓存, 删除相关缓存目录下的文件
        String cachePath = context.getCacheDir().getPath();
        deleteDir(cachePath);

        AFLog.d(TAG,"clearCacheFile cachePath=" + cachePath);

        String webviewCachePath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            //高版本的目录是否如此需要测试，是否可以使用模拟器
            webviewCachePath = context.getDataDir().getPath() + "/app_webview";
        } else {
            webviewCachePath = "/data/data/" + context.getPackageName() + "/app_webview";
        }
        deleteDir(webviewCachePath);

        AFLog.d(TAG,"clearCacheFile webviewCachePath=" + webviewCachePath);


        //delete  code cache  dir
        String codeCachePath = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            codeCachePath = context.getCodeCacheDir().getPath();
        } else {
            //低版本的目录是否如此需要测试，是否可以使用模拟器
            codeCachePath = "/data/data/" + context.getPackageName() + "/code_cache";
        }
        deleteDir(codeCachePath);

        AFLog.d(TAG,"clearCacheFile codeCachePath=" + codeCachePath);

    }

    /**
     * 拨号功能，将号码送至拨号盘，准备拨打
     * @param activity
     * @param number
     */
    public static void callNumber(Activity activity, String number) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+number));
        activity.startActivity(intent);
    }

    /**
     * 检测权限是否授权, 如果已经授权，返回true
     * @param context
     * @param permission
     * @return
     */
    public static boolean selfPermissionGranted(Context context, String permission) {
        int targetSdkVersion = 0;
        boolean ret = false;

        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
            AFLog.d(TAG,"selfPermissionGranted targetSdkVersion="+targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                ret = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            } else {
                ret = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED;
            }
        }else{
            return true;
        }
        AFLog.d(TAG,"selfPermissionGranted permission:" + permission +" grant:" + ret);
        return ret;
    }

    /**
     * 判断GPS是否开启，GPS或者AGPS开启一个就认为是开启的
     * @param context
     * @return true 表示开启
     */
    public static boolean isGPSOpen(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        // 通过GPS卫星定位，定位级别可以精确到街（通过24颗卫星定位，在室外和空旷的地方定位准确、速度快）
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // 通过WLAN或移动网络(3G/2G)确定的位置（也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物（建筑群或茂密的深林等）密集的地方定位）
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }

        return false;
    }

    /**
     * 跳转到GPS设置界面
     * @param context
     */
    public static void openGPS(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    /*
     * 判断服务是否启动,context上下文对象 ，className服务的name
     */
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }
        AFLog.d("OnlineService：",className);
        for (int i = 0; i < serviceList.size(); i++) {
            AFLog.d("serviceName：",serviceList.get(i).service.getClassName());
            if (serviceList.get(i).service.getClassName().contains(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    /**
     * 申请权限后检查权限是否全部授予判断
     * @param grantResults
     * @return
     */
    public static boolean permissionGrantedCheck(int[] grantResults){
        boolean result = true;
        if (null != grantResults){
            for (int i=0; i<grantResults.length; i++){
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 判断 悬浮窗口权限是否打开
     * @param context
     * @return true 允许  false禁止
     */
    public static boolean checkAlertWindowsPermission(Context context) {
        try {

            Object object = context.getSystemService(Context.APP_OPS_SERVICE);
            if (object == null) {
                return false;
            }
            Class localClass = object.getClass();
            Class[] arrayOfClass = new Class[3];
            arrayOfClass[0] = Integer.TYPE;
            arrayOfClass[1] = Integer.TYPE;
            arrayOfClass[2] = String.class;
            Method method = localClass.getMethod("checkOp", arrayOfClass);
            if (method == null) {
                return false;
            }
            Object[] arrayOfObject1 = new Object[3];
            arrayOfObject1[0] = 24;
            arrayOfObject1[1] = Binder.getCallingUid();
            arrayOfObject1[2] = context.getPackageName();
            int m = ((Integer) method.invoke(object, arrayOfObject1));
            return m == AppOpsManager.MODE_ALLOWED;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


    /**
     * 判断某个字符串是否包含在字符数组中
     * @param strArray
     * @param str
     * @return
     */
    public static boolean strArrayContains(String[] strArray, String str) {
        boolean result = false;
        if ((null != strArray) && (null != str)) {
            for(int i=0; i<strArray.length; i++) {
                if (str.equals(strArray[i])) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * 获取设备信息以及软件版本信息
     * @return
     */
    public static String collectDeviceInfo(){
        String deviceInfo = "";
        //获得包管理器
        try {
            Context context = BaseWebviewApp.getInstance().getApplicationContext();
            PackageManager pm = context.getPackageManager();
            //得到该应用的信息
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),PackageManager.GET_ACTIVITIES);
            if(pi != null)
            {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                String packageName = pi.packageName;

                deviceInfo = "phone_brand:" + android.os.Build.BRAND + " \n"
                        + "phone_version:" + android.os.Build.VERSION.RELEASE + "\n"
                        + "packageName:" + packageName + "\n"
                        + "versionName:" + versionName + "\n"
                        + "versionCode:" + versionCode + "\n"
                ;

            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            AFLog.d(TAG,"获取信息失败");
        }
        //反射机制
        Field[] fields = Build.class.getDeclaredFields();
        for(Field field : fields)
        {
            try {
                field.setAccessible(true);
                deviceInfo = deviceInfo + field.getName() + ":" + field.get("").toString() + "\n";
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return deviceInfo;
    }

    /**
     * 功能：下载文件
     * 参数：url 下载链接
     * 返回值：无
     */
    public static void downloadByBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }



    /**
     * 功能：将SD卡路径下的图片使用采样压缩成指定高宽的bitmap
     * 参数：path 图片路径
     * 返回值：压缩后的bitmap
     */
    public static Bitmap convertToBitmap(String path) {
        //先读取设置的高宽，如果没有设置，则默认都为1080
        int w;
        int h;
        if (mCompressImageWidth == 0 || mCompressImageHeight == 0){
            w = 1080;
            h = 1080;
        }else {
            w = mCompressImageWidth;
            h = mCompressImageHeight;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        // 设置为ture只获取图片大小
        opts.inJustDecodeBounds = true;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 返回为空
        BitmapFactory.decodeFile(path, opts);
        int width = opts.outWidth;
        int height = opts.outHeight;
        float scaleWidth = 0.f, scaleHeight = 0.f;
        if (width > w || height > h) {
            // 缩放
            scaleWidth = ((float) width) / w;
            scaleHeight = ((float) height) / h;
        }
        opts.inJustDecodeBounds = false;
        float scale = Math.max(scaleWidth, scaleHeight);
        // TODO: 缩放尺寸根据宽度来定

        opts.inSampleSize = (int)scale;
        AFLog.d(TAG, "convertToBitmap scaleWidth="+scaleWidth +" scaleHeight="+scaleHeight
                +" scale="+scale +" opts.inSampleSize="+opts.inSampleSize);
        WeakReference weak = new WeakReference(BitmapFactory.decodeFile(path, opts));
        return Bitmap.createScaledBitmap((Bitmap) weak.get(), w, h, true);
    }


    /**
     * 功能：将bitmap 转换成jpeg格式存储到指定路径
     * 参数：bitmap 位图， savePath 图片存储全路径
     * 返回值：存储路径
     */
    public static String saveBitToJpg(Bitmap bitmap, String savePath) {
        File file = new File(savePath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return savePath;
    }

}
