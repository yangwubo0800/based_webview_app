package com.base.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <通用工具类>
 *
 * @author XXX
 * @version [版本号, 2016/6/6]
 * @see [相关类/方法]
 * @since [V1]
 */
public final class GeneralUtils {

    private static String TAG = "GeneralUtils";
    /**
     * 判断对象是否为null , 为null返回true,否则返回false
     *
     * @param obj 被判断的对象
     * @return boolean
     */
    public static boolean isNull(Object obj) {
        return (null == obj) ? true : false;
    }

    /**
     * 判断对象是否为null , 为null返回false,否则返回true
     *
     * @param obj 被判断的对象
     * @return boolean
     */
    public static boolean isNotNull(Object obj) {
        return !isNull(obj);
    }

    /**
     * 判断字符串是否为null或者0长度，字符串在判断长度时，先去除前后的空格,空或者0长度返回true,否则返回false
     *
     * @param str 被判断的字符串
     * @return boolean
     */
    public static boolean isNullOrZeroLenght(String str) {
        return (null == str || "".equals(str.trim())) ? true : false;
    }

    /**
     * 判断字符串是否为null或者0长度，字符串在判断长度时，先去除前后的空格,空或者0长度返回false,否则返回true
     *
     * @param str 被判断的字符串
     * @return boolean
     */
    public static boolean isNotNullOrZeroLenght(String str) {
        return !isNullOrZeroLenght(str);
    }

    /**
     * 判断集合对象是否为null或者0大小 , 为空或0大小返回true ,否则返回false
     *
     * @param c collection 集合接口
     * @return boolean 布尔值
     * @see [类、类#方法、类#成员]
     */
    public static boolean isNullOrZeroSize(Collection<? extends Object> c) {
        return isNull(c) || c.isEmpty();
    }

    /**
     * 判断集合对象是否为null或者0大小 , 为空或0大小返回false, 否则返回true
     *
     * @param c collection 集合接口
     * @return boolean 布尔值
     * @see [类、类#方法、类#成员]
     */
    public static boolean isNotNullOrZeroSize(Collection<? extends Object> c) {
        return !isNullOrZeroSize(c);
    }

    /**
     * 判断数字类型是否为null或者0，如果是返回true，否则返回false
     *
     * @param number 被判断的数字
     * @return boolean
     */
    public static boolean isNullOrZero(Number number) {
        if (GeneralUtils.isNotNull(number)) {
            return (number.intValue() != 0) ? false : true;
        }
        return true;
    }

    /**
     * 判断数字类型是否不为null或者0，如果是返回true，否则返回false
     *
     * @param number 被判断的数字
     * @return boolean
     */
    public static boolean isNotNullOrZero(Number number) {
        return !isNullOrZero(number);
    }

    /**
     * <获取当前日期 格式 yyyyMMddHHmmss> <功能详细描述>
     *
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String getRightNowDateString() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(date);
    }

    /**
     * <获取当前时间 格式yyyy-MM-dd> <功能详细描述>
     *
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String getRightNowDateString2() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);
    }

    /**
     * <获取当前时间 格式yyyy-MM-dd <功能详细描述>
     *
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static Date getRightNowDateTime2() {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        Date date = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return dateFormat.parse(dateFormat.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <保留两位有效数字> <功能详细描述>
     *
     * @param num String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String retained2SignificantFigures(String num) {
        return new BigDecimal(num).setScale(2, RoundingMode.HALF_UP).toString();
    }

    /**
     * <保留1位有效数字> <功能详细描述>
     *
     * @param num String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String retained1SignificantFigures(double num) {
        return new BigDecimal(num).setScale(1).toString();
    }

    /**
     * <减法运算并保留两位有效数字> <功能详细描述>
     *
     * @param subt1 String
     * @param subt2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String subtract(String subt1, String subt2) {
        BigDecimal sub1 = new BigDecimal(subt1);
        BigDecimal sub2 = new BigDecimal(subt2);
        BigDecimal result = sub1.subtract(sub2);
        result = result.setScale(2, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <加法运算并保留两位有效数字> <功能详细描述>
     *
     * @param addend1
     * @param addend2
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String add(String addend1, String addend2) {
        BigDecimal add1 = new BigDecimal(addend1);
        BigDecimal add2 = new BigDecimal(addend2);
        BigDecimal result = add1.add(add2);
        result = result.setScale(2, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <乘法运算并保留两位有效数字> <功能详细描述>
     *
     * @param factor1 String
     * @param factor2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String multiply(String factor1, String factor2) {
        BigDecimal fac1 = new BigDecimal(factor1);
        BigDecimal fac2 = new BigDecimal(factor2);
        BigDecimal result = fac1.multiply(fac2);
        result = result.setScale(2, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <除法运算并保留两位有效数字> <功能详细描述>
     *
     * @param divisor1 String
     * @param divisor2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String divide(String divisor1, String divisor2) {
        BigDecimal div1 = new BigDecimal(divisor1);
        BigDecimal div2 = new BigDecimal(divisor2);
        BigDecimal result = div1.divide(div2, 2, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <除法运算并保留1位有效数字> <功能详细描述>
     *
     * @param divisor1 String
     * @param divisor2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String divide1(String divisor1, String divisor2) {
        BigDecimal div1 = new BigDecimal(divisor1);
        BigDecimal div2 = new BigDecimal(divisor2);
        BigDecimal result = div1.divide(div2, 1, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <除法运算并保留1位有效数字> <功能详细描述>
     *
     * @param divisor1 String
     * @param divisor2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String divide1(long divisor1, int divisor2) {
        BigDecimal div1 = new BigDecimal(divisor1);
        BigDecimal div2 = new BigDecimal(divisor2);
        BigDecimal result = div1.divide(div2, 1, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <除法运算并保留两位有效数字> <功能详细描述>
     *
     * @param divisor1 String
     * @param divisor2 String
     * @return String
     * @see [类、类#方法、类#成员]
     */
    public static String dividePoint1(String divisor1, String divisor2) {
        BigDecimal div1 = new BigDecimal(divisor1);
        BigDecimal div2 = new BigDecimal(divisor2);
        BigDecimal result = div1.divide(div2, 1, RoundingMode.HALF_UP);
        return result.toString();
    }

    /**
     * <将YYYYMMDDHHmmss 转换为 YYYY-MM-DD> <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String splitTodate(String str) {
        if (isNullOrZeroLenght(str) || str.length() != 14) {
            return str;
        }

        String strs = "";
        strs = str.substring(0, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8);
        return strs;
    }

    /**
     * <将YYYYMMDDHHmmss 转换为 YYYY-MM-DD hh:mm> <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String splitToMinute(String str) {
        if (isNullOrZeroLenght(str) || str.length() != 14) {
            return str;
        }

        String strs = "";
        strs =
                str.substring(0, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8) + " " + str.substring(8, 10)
                        + ":" + str.substring(10, 12);
        return strs;
    }

    /**
     * <将YYYY-MM-DD 转换为 YYYYMMDD> <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String splitToMinuteNoLine(String str) {
        if (!str.contains("-")) {
            return str;
        }
        String strs = "";
        String[] strsArr = str.split("-");
        if (strsArr[1].length() == 1) {
            strsArr[1] = "0" + strsArr[1];
        }

        if (strsArr[2].length() == 1) {
            strsArr[2] = "0" + strsArr[2];
        }

        strs = strsArr[0] + strsArr[1] + strsArr[2];
        return strs;
    }

    /**
     * <将YYYYMMDDHHmmss 转换为 YYYY-MM-DD hh:mm:ss> <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String splitToSecond(String str) {
        if (isNullOrZeroLenght(str) || str.length() != 14) {
            return str;
        }

        String strs = "";
        strs =
                str.substring(0, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8) + " " + str.substring(8, 10)
                        + ":" + str.substring(10, 12) + ":" + str.substring(12, 14);
        return strs;
    }

    /**
     * <将YYYYMMDDHHmmss 转换为 YY-MM-DD hh:mm:ss> <功能详细描述>
     *
     * @param str
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String splitToYear(String str) {
        if (isNullOrZeroLenght(str) || str.length() != 14) {
            return str;
        }

        String strs = "";
        strs =
                str.substring(2, 4) + "-" + str.substring(4, 6) + "-" + str.substring(6, 8) + " " + str.substring(8, 10)
                        + ":" + str.substring(10, 12) + ":" + str.substring(12, 14);
        return strs;
    }


    /**
     * 获取版本信息
     *
     * @return
     * @throws Exception
     */
    public static String getVersionName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            String version = packInfo.versionName;
            return version;
        } catch (NameNotFoundException e) {
        }
        return "";
    }

    /**
     * <邮箱判断>
     * <功能详细描述>
     *
     * @param email
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean isEmail(String email) {
        String str =
                "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * <手机号码判断>
     *
     * @param tel
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean isTel(String tel) {
        String str = "^[0-9]{11}$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(tel);
        return m.matches();
    }

    /**
     * <邮编判断>
     * <功能详细描述>
     *
     * @param post
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean isPost(String post) {
        String patrn = "^[1-9][0-9]{5}$";
        Pattern p = Pattern.compile(patrn);
        Matcher m = p.matcher(post);
        return m.matches();
    }

    /**
     * <密码规则判断>
     * <功能详细描述>
     *
     * @param password
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean IsPassword(String password) {
        String str = "^[A-Za-z0-9_]{6,20}$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(password);
        return m.matches();
    }

    /**
     * <密码位数判断>
     * <功能详细描述>
     *
     * @param password
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean IsPasswordDigit(String password) {
        String str = "^[^ ]{6,20}$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(password);
        return m.matches();
    }

    /**
     * <密码位数判断>
     * <功能详细描述>
     *
     * @param certificate
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static boolean Iscertificate(String certificate) {
        String str = "[0-9]{17}([0-9]|[xX])";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(certificate);
        return m.matches();
    }

    /**
     * <获取imei>
     * <功能详细描述>
     *
     * @param context
     * @return
     * @see [类、类#方法、类#成员]
     */
    public static String getDeviceId(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    /**
     * http://stackoverflow.com/questions/3495890/how-can-i-put-a-listview-into-a-scrollview-without-it-collapsing/3495908#3495908
     *
     * @param listView
     */
    public static void setListViewHeightBasedOnChildrenExtend(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new LayoutParams(desiredWidth, LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    // 去除textview的排版问题
    public static String ToDBC(String input) {
        char[] c = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == 12288) {
                c[i] = (char) 32;
                continue;
            }
            if (c[i] > 65280 && c[i] < 65375)
                c[i] = (char) (c[i] - 65248);
        }
        return new String(c);
    }

    /**
     * 获取设备型号
     *
     * @return
     */
    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 返回当前程序版本名
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
        } catch (Exception e) {
        }
        return versionName;
    }

    /**
     *获取基本路径
     * @return
     * @throws IOException
     */
    public static File getBasePath() throws IOException {
//			File basePath = new File(Environment.getExternalStorageDirectory(),  "XXEmergency/image");
        File basePath = new File(Environment.getExternalStorageDirectory(),  "DCIM/Camera");

        if (!basePath.exists()){
            if (!basePath.mkdirs()){
                throw new IOException(String.format("%s cannot be created!", basePath.toString()));
            }
        }

        if (!basePath.isDirectory()){
            throw new IOException(String.format("%s is not a directory!", basePath.toString()));
        }
        return basePath;
    }

    /**
     *获取ini文件路径
     * @return
     * @throws IOException
     */
    public static File getIniPath(String strIniPath) throws IOException {
        //notifyNew.ini 通知更新ID文件； unread.ini 未读通知ID文件
        File basePath = new File(Environment.getExternalStorageDirectory(), strIniPath);

        if (!basePath.exists()){
            if (!basePath.mkdirs()){
                throw new IOException(String.format("%s cannot be created!", basePath.toString()));
            }
        }

        if (!basePath.isDirectory()){
            throw new IOException(String.format("%s is not a directory!", basePath.toString()));
        }
        return basePath;
    }

    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    /*
     * 判断服务是否启动,context上下文对象 ，className服务的name
     */
    public static boolean isServiceRunning(Context mContext, String serviceName) {

        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(100);
        if (myList.size() <= 0) {
            return false;
        }
        int size = myList.size();
        for (int i = 0; i < size; i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    //string list是否含有某个值
    public static boolean listHasValue(List<String> list,String str){
        boolean ret=false;
        for (int i=0;i<list.size();i++){
            if(list.get(i).equals(str)){
                ret=true;
            }
        }
        return ret;
    }

    //获取字符串前多少位
    public static  String getSubstr(String str,int length){
        if(str!=null){
            if(str.length()>length){
                return  str.substring(0,length-2)+"...";
            }else
                return str;
        }
        return "";
    }

    /**
     * 按万、千万级别返回数值
     * @param val
     * @return
     */
    public static String getShortVaule(long val){
        if(val>99999999){
            return divide1(val,10000000);
        }else if(val>99999){
            return divide1(val,10000);
        }else {
            return String.valueOf(val);
        }
    }

    /**
     * 按万、千万级别返回单位
     * @param val
     * @return
     */
    public static String getShortUnit(long val){
        if(val>99999999){
            return "千万";
        }else if(val>99999){
            return "万";
        }else {
            return "";
        }
    }

    /**
     * 禁止EditText输入空格
     * @param editText
     */
    public static void setEditTextInhibitInputSpace(EditText editText){
        InputFilter filter=new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if(source.equals(" "))
                    return "";
                else return null;
            }
        };
        editText.setFilters(new InputFilter[]{filter});
    }

    /**
     * str中是否含有空格
     * @param
     */
    public static boolean haveBlank(String str){
        if(str.indexOf(" ")>=0){
            return true;
        }else{
            return false;
        }
    }

    /* *//**
     * 自定义接口协议
     *
     *
     *//*
    public static void  updateDiy(Activity activity) {

        //更新版本
        if (Build.VERSION.SDK_INT >= 23) {

            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (activity.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    activity.requestPermissions(permissions, 101);
                }
            }
        }

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();

        Map<String, String> params = new HashMap<String, String>();

        ApplicationInfo appInfo = null;
        String version ="";
        String versioncode ="";
        try {
            appInfo = activity.getPackageManager().
                    getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            PackageManager manager = activity.getPackageManager();
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(), 0);
            version = info.versionName;
            versioncode=String.valueOf(info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String appKey=appInfo.metaData.getString("UPDATE_APP_KEY");
        params.put("appKey", appKey);
        params.put("appVersion", version);
        params.put("appVersionCode", versioncode);
        //params.put("key2", "222");


        new UpdateAppManager
                .Builder()
                //必须设置，当前Activity
                .setActivity(activity)
                //必须设置，实现httpManager接口的对象
                .setHttpManager(new OkGoUpdateHttpUtil())
                //必须设置，更新地址
                .setUpdateUrl(URLUtil.getHTTPServerIP()+URLUtil.VERSION_UPDATE)

                //以下设置，都是可选
                //设置请求方式，默认get
                .setPost(false)
                //不显示通知栏进度条
                //.dismissNotificationProgress()
                //是否忽略版本
//                .showIgnoreVersion()
                //添加自定义参数，默认version=1.0.0（app的versionName）；apkKey=唯一表示（在AndroidManifest.xml配置）
                .setParams(params)
                //设置点击升级后，消失对话框，默认点击升级后，对话框显示下载进度
                .hideDialogOnDownloading(false)
                //设置头部，不设置显示默认的图片，设置图片后自动识别主色调，然后为按钮，进度条设置颜色
                .setTopPic(R.drawable.image_version_update)
                //为按钮，进度条设置颜色，默认从顶部图片自动识别。
//                .setThemeColor(ColorUtil.getRandomColor())
                //设置apk下砸路径，默认是在下载到sd卡下/Download/1.0.0/test.apk
                .setTargetPath(path)
                //设置appKey，默认从AndroidManifest.xml获取，如果，使用自定义参数，则此项无效
//                .setAppKey("ab55ce55Ac4bcP408cPb8c1Aaeac179c5f6f")

                .build()
                //检测是否有新版本
                .checkNewApp(new UpdateCallback() {
                    *//**
     * 解析json,自定义协议
     *
     * @param json 服务器返回的json
     * @return UpdateAppBean
     *//*
                    @Override
                    protected UpdateAppBean parseJson(String json) {
                        UpdateAppBean updateAppBean = new UpdateAppBean();
                        try {
                            JSONObject jsonObject = new JSONObject(json);
                            updateAppBean
                                    //（必须）是否更新Yes,No
                                    .setUpdate(jsonObject.optString("update"))
                                    //（必须）新版本号，
                                    .setNewVersion(jsonObject.optString("new_version"))
                                    //（必须）下载地址
                                    .setApkFileUrl(URLUtil.getHTTPServerIP()+jsonObject.optString("apk_file_url"))
                                    //（必须）更新内容
                                    .setUpdateLog(jsonObject.optString("update_log"))
                                    //测试内容过度
//                                    .setUpdateLog("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16")
                                    //大小，不设置不显示大小，可以不设置
                                    .setTargetSize(jsonObject.optString("target_size"))
                                    //是否强制更新，可以不设置
                                    .setConstraint(false)
                            //设置md5，可以不设置
                            // .setNewMd5(jsonObject.optString("new_md51"))
                            ;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return updateAppBean;
                    }

                    @Override
                    protected void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager) {
                        updateAppManager.showDialogFragment();
                    }

                    *//**
     * 网络请求之前
     *//*
                    @Override
                    public void onBefore() {
//                        CProgressDialogUtils.showProgressDialog(WelcomeActivity.this);
                    }

                    *//**
     * 网路请求之后
     *//*
                    @Override
                    public void onAfter() {
//                        CProgressDialogUtils.cancelProgressDialog(WelcomeActivity.this);
                    }

                    *//**
     * 没有新版本
     *//*
                    @Override
                    public void noNewApp() {
//                        Toast.makeText(WelcomeActivity.this, "没有新版本", Toast.LENGTH_SHORT).show();
                    }
                });

    }*/

    /**
     * 给字符串进行MD5加密
     * @param string
     * @return
     */
    public static String md5forString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 获取asset目录下的文件中内容
     * @param context
     * @param fileName
     * @return
     */
    public static String getContentFromAsset(Context context, String fileName) {
        String str = null;
        try {
            InputStream inputStream = context.getAssets().open(fileName);

            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            BufferedReader reader = new BufferedReader(inputStreamReader);
            StringBuffer sb = new StringBuffer("");
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            //赋值字符串，关闭资源
            str = sb.toString();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"getContentFromAsset str is:"+ str);
        return str;
    }


    /**
     * 根据mainifest.xml中的 metakey获取数据
     * @param context
     * @param metaKey
     * @return
     */
    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "error " + e.getMessage());
        }
        return apiKey;
    }


    /**
     * 根据mainifest.xml中的 metakey获取int数据
     * @param context
     * @param metaKey
     * @return
     */
    public static Integer getMetaIntValue(Context context, String metaKey) {
        Bundle metaData = null;
        Integer apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getInt(metaKey);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "error " + e.getMessage());
        }
        return apiKey;
    }

}