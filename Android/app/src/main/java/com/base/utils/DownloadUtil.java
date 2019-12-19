package com.base.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.base.utils.log.AFLog;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DownloadUtil {

    private static  String TAG = "DownloadUtil";

    //记录前端传递过来的名字
    public static String mCurrentDownloadFileName;

    public static  void downloadBySystem(Context context, String url, String contentDisposition, String mimeType, String fileName) {
        // 指定下载地址
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner();
        // 设置通知的显示类型，下载进行时和完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        // 设置通知栏的标题，如果不设置，默认使用文件名
//        request.setTitle("This is title");
        // 设置通知栏的描述
//        request.setDescription("This is description");
        // 允许在计费流量下下载
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            request.setAllowedOverMetered(true);
        }
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true);
        // 允许漫游时下载
        request.setAllowedOverRoaming(true);
        // 允许下载的网路类型
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE );
        // 设置下载文件保存的路径和文件名
//        String fileName  = URLUtil.guessFileName(url, contentDisposition, mimeType);
        AFLog.d(TAG, "file name is " + fileName);
        if(TextUtils.isEmpty(fileName)){
            ToastUtil.makeTextShowLong(context, "请设置下载文件名称");
            return;
        }
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//        另外可选一下方法，自定义下载路径
//        request.setDestinationUri()
//        request.setDestinationInExternalFilesDir()
        final DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
        // 添加一个下载任务
        long downloadId = downloadManager.enqueue(request);
        AFLog.d(TAG, "downloadId is " + downloadId);
    }


}
