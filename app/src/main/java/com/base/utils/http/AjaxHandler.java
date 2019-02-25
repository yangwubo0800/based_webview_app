package com.base.utils.http;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.base.constant.Constants;
import com.base.utils.SpUtils;
import com.base.utils.db.OfflineModeUtils;
import com.base.utils.log.AFLog;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

//import com.squareup.okhttp.Call;
//import com.squareup.okhttp.Callback;
//import com.squareup.okhttp.FormEncodingBuilder;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;
import wendu.dsbridge.CompletionHandler;

/**
 * Created by du on 2017/10/31.
 *
 * This class handles the Ajax requests forwarded by fly.js in DWebView
 * More about fly.js see https://github.com/wendux/fly
 */

public class AjaxHandler {
    private static String TAG = "AjaxHandler";


    public static void onAjaxRequest(final JSONObject requestData,
                                     final CompletionHandler handler,
                                     Context context){
        //define use for local visit.
        String mUrl;
        String mContentType = "";
        String mPostData = "";

        AFLog.d(TAG,"=====onAjaxRequest requestData="+ requestData.toString());
        // Define response structure
        final Map<String, Object> responseData=new HashMap<>();
        responseData.put("statusCode",0);

        try {
            int timeout =requestData.getInt("timeout");
            // Create a okhttp instance and set timeout
            final OkHttpClient okHttpClient = new OkHttpClient
                    .Builder()
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();

            // Determine whether you need to encode the response result.
            // And encode when responseType is stream.
            String contentType="";
            boolean encode=false;
            String responseType=requestData.optString("responseType",null);
            if(responseType!=null&&responseType.equals("stream")){
                encode=true;
            }

            Request.Builder rb= new Request.Builder();
            rb.url(requestData.getString("url"));
            mUrl = requestData.getString("url");
            AFLog.d(TAG,"=====onAjaxRequest mUrl="+ mUrl);
            JSONObject headers=requestData.getJSONObject("headers");

            // Set request headers
            Iterator iterator = headers.keys();
            while(iterator.hasNext()){
                String key = (String) iterator.next();
                String value = headers.getString(key);
                String lKey=key.toLowerCase();
                if(lKey.equals("cookie")){
                    // Here you can use CookieJar to manage cookie in a unified way with you native code.
                    continue;
                }
                if(lKey.toLowerCase().equals("content-type")){
                    contentType=value;
                    mContentType = value;
                    AFLog.d(TAG,"=====onAjaxRequest mContentType="+ mContentType);
                }
                rb.header(key,value);
            }

            // Create request body
            if(requestData.getString("method").equals("POST")){
                RequestBody requestBody;
                // TODO: 对于文件上传和下载需要区分处理, 使用不同的request body
                mPostData = requestData.getString("body");
                AFLog.d(TAG,"=====onAjaxRequest mPostData="+ mPostData);
                if (!TextUtils.isEmpty(mPostData)){
                    ArrayList<String> filePaths = new ArrayList<String>();
                    // notice: 这里的解析方式为接收encodeUrl 编码方式的参数
                    if (mPostData.contains("fileProcess")){
                        String[]  fileProcessParams = mPostData.split("&");
                        for(int i=0; i< fileProcessParams.length; i++){
                            if (fileProcessParams[i].contains("filePath")){
                                int equalIndex = fileProcessParams[i].indexOf("=");
                                String filePath = fileProcessParams[i].substring(equalIndex+1, fileProcessParams[i].length());
                                AFLog.d(TAG,"=====filePath=" + filePath);
                                filePaths.add(filePath);
                            }
                        }

                        // 拼接文件上传参数
                        MultipartBody.Builder fileUploadBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                        for (String filePath : filePaths){
                            String decodePath = URLDecoder.decode(filePath);
                            AFLog.d(TAG,"#########filePath="+ filePath + "\n"
                            +" decodePath=" + decodePath);
                            File uploadFile = new File(decodePath);
                            fileUploadBuilder.addFormDataPart("file", uploadFile.getName(),
                                    RequestBody.create(MediaType.parse("application/octet-stream"), uploadFile));
                        }
                        requestBody = fileUploadBuilder.build();
                    } else {
                        requestBody= RequestBody
                                .create(MediaType.parse(contentType),requestData.getString("body"));
                    }
                    // 将参数传过去
                    rb.post(requestBody);
                }

            }


            // TODO: offline and online mode process
            SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
            boolean offlineMode = SpUtils.getBoolean(context, Constants.OFFLINE_MODE, false);
            AFLog.d(TAG,"############offlineMode=" + offlineMode);
            if (offlineMode) {
                String localResponse = OfflineModeUtils.getInstance(context).urlMapToLocalApi(mUrl, mPostData, mContentType);
                responseData.put("responseText",localResponse);
                responseData.put("statusCode",200);
                responseData.put("statusMessage","OK");
                handler.complete(new JSONObject(responseData).toString());
            } else {

                // Create and send HTTP requests
                Call call=okHttpClient.newCall(rb.build());
                final boolean finalEncode = encode;
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        responseData.put("responseText",e.getMessage());
                        handler.complete(new JSONObject(responseData).toString());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String data;
                        // If encoding is needed, the result is encoded by Base64 and returned
                        if(finalEncode){
                            data= Base64.encodeToString(response.body().bytes(), Base64.DEFAULT);
                        }else{
                            data=response.body().string();
                        }
                        responseData.put("responseText",data);
                        responseData.put("statusCode",response.code());
                        responseData.put("statusMessage",response.message());
                        Map<String, List<String>> responseHeaders= response.headers().toMultimap();
//                    responseHeaders.remove(null);
                        responseData.put("headers",responseHeaders);
                        handler.complete(new JSONObject(responseData).toString());
                    }
                });
            }
        }catch (Exception e){
            responseData.put("responseText",e.getMessage());
            handler.complete(new JSONObject(responseData).toString());
        }
    }
}
