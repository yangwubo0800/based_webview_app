package com.base.receiver;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.base.utils.log.AFLog;

import java.io.File;

import static android.content.Context.DOWNLOAD_SERVICE;

public class MyDownloadCompleteReceiver extends BroadcastReceiver {

    private String TAG = "MyDownloadCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        AFLog.d(TAG," MyDownloadCompleteReceiver intent is " + intent.toString());
        if (intent != null) {
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                AFLog.d(TAG, " MyDownloadCompleteReceiver downloadId is " + downloadId);
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                String type = downloadManager.getMimeTypeForDownloadedFile(downloadId);
                AFLog.d(TAG, " MyDownloadCompleteReceiver downloadId type is " + type);
                if (TextUtils.isEmpty(type)) {
                    type = "*/*";
                    String msg = "下载完成，请进入下载管理中查看文件";
                    Toast toast=Toast.makeText(context, msg, Toast.LENGTH_LONG);
                    toast.setText(msg);
                    toast.show();
                    return;
                }
                Uri uri = downloadManager.getUriForDownloadedFile(downloadId);
                AFLog.d(TAG, " MyDownloadCompleteReceiver downloadId uri is " + uri);
                if (uri != null) {
                    Intent handlerIntent = new Intent(Intent.ACTION_VIEW);
                    handlerIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    handlerIntent.setDataAndType(uri, type);
                    context.startActivity(handlerIntent);
                }
            }
        }
    }


    private void openAssignFolder(Context context, String path){
        File file = new File(path);
        if(null==file || !file.exists()){
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setDataAndType(Uri.fromFile(file), "*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

//        intent.addCategory(Intent.CATEGORY_DEFAULT);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.setDataAndType(Uri.fromFile(file), "file/*");
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        Uri uri = Uri.fromFile(file);
//        intent.addCategory(Intent.CATEGORY_DEFAULT);
        try {
            context.startActivity(intent);
//            startActivity(Intent.createChooser(intent,"选择浏览工具"));
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
