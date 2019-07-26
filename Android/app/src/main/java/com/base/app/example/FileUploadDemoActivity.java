package com.base.app.example;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.base.app.R;
import com.base.utils.FileUtils;
import com.base.utils.log.AFLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FileUploadDemoActivity extends AppCompatActivity {

    private String TAG = "FileUploadDemoActivity";
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_file_upload_demo);
        findViews();
    }

    private void findViews() {
        Button btUpload = (Button) findViewById(R.id.upload);
        btUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileupload(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "上传失败！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "上传成功！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        Button btDownload = (Button) findViewById(R.id.download);
        btDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String downloadUrl = "http://192.168.65.41:8088/FileUploadSampleForApp/upload/PIC_20181115_173051.jpeg";
                String dir = "/sdcard/XXXXX/";
                String name = "download.jpg";
                downloadFile(downloadUrl, dir, name);
            }
        });
    }

    public void fileupload(Callback callback) {
        Log.i(TAG,"########################fileupload");
        // 获得输入框中的路径
        String path = "/sdcard/XXX/photo/PIC_20181016_100701.jpeg";
        File file = new File(path);
        // 上传文件使用MultipartBody.Builder
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", "xxxx") // 提交普通字段
                .addFormDataPart("image", file.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), file)) // 提交图片，第一个参数是键（name="第一个参数"），第二个参数是文件名，第三个是一个RequestBody
                .build();


        // 上传多个文件，某一个目录下的所有文件
        String directoryPath = "/sdcard/XXX/photo/";
        ArrayList<String> test = FileUtils.getAllFilePathForDirectory(directoryPath);
        MultipartBody.Builder testBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (String filePath : test){
            File testFile = new File(filePath);
            AFLog.d(TAG,"#########filePath="+ filePath);
            testBuilder.addFormDataPart("file", testFile.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), testFile));
        }
        RequestBody requestBody2 = testBuilder.build();

        // POST请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://192.168.65.41:8088/FileUploadSampleForApp/UploadFile")
                .post(requestBody2)
                .build();
        client.newCall(request).enqueue(callback);
    }



    public void downloadFile(final String url, final String destFileDir, final String destFileName) {
        Log.i(TAG,"########################downloadFile");
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败监听回调
//                listener.onDownloadFailed(e);
                Log.i(TAG,"########################downloadFile onFailure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG,"########################downloadFile onResponse");
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                File dir = new File(destFileDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, destFileName);
                try {
                    is = response.body().byteStream();
                    long total = response.body().contentLength();
                    fos = new FileOutputStream(file);
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        sum += len;
//                        int progress = (int) (sum * 1.0f / total * 100);
//                        // 下载中更新进度条
//                        listener.onDownloading(progress);
                    }
                    fos.flush();
                    Log.i(TAG,"########################downloadFile finish");
                    // 下载完成
//                    listener.onDownloadSuccess(file);
                } catch (Exception e) {
                    e.printStackTrace();
//                    listener.onDownloadFailed(e);
                } finally {
                    try {
                        if (is != null)
                            is.close();
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null)
                            fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}
