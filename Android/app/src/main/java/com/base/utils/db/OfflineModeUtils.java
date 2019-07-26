package com.base.utils.db;

import android.content.Context;
import android.text.TextUtils;

import com.hnac.bean.db.H5Page;
import com.base.dao.db.PageColumns;
import com.base.utils.GsonHelper;
import com.base.utils.log.AFLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *  离线操作本地数据库工具类，此类可以用来访问本地数据库，并将结果组装成前端需要的数据格式返回
 *  同时对于所有支持离线操作的后台接口，可以在此类中进行注册，便于拦截后识别转换。
 */
public class OfflineModeUtils {

    private String TAG = "OfflineModeUtils";
    private Context mContext;
    private static OfflineModeUtils mInstance;
    public static String[] mNeedBlockLastPartUrls = {
            "addInspectionProblem",
            "getInspectionTaskViewList",
            "modifyInspectionTask",
            "getInspectionTaskView",
            "fileUpload",
            "getInspectionDictView",
            "insertInspectionEvent",
            "insertInspectionEventAttachmentBatch",
            "insertInspectionEventRecord",
            "selectInspectionEventView",
            "getInspectionEventViewList",
            "insertInspectionProblemAttachmentBatch"
    };

    public OfflineModeUtils(Context context) {
        mContext = context;
    }

    public static OfflineModeUtils getInstance(Context context){
        if (null == mInstance) {
            synchronized (OfflineModeUtils.class){
                if (null == mInstance) {
                    mInstance = new OfflineModeUtils(context);
                }
            }
        }

        return mInstance;
    }


    /**
     * 支持离线操作的后台接口和本地数据库操作接口映射关系方法。
     * params中存放POST方式传递的参数， GET方式参数还是放在url中，
     * 参数类型通过contentType进行说明，支持 application/x-www-form-urlencoded 和 application/json 两种。
     * 具体根据前后端的使用来选择，在安卓端使用json方式方便解析参数。
     * @param url
     * @param params
     * @param contentType
     * @return
     */
    public String urlMapToLocalApi(String url, String params, String contentType){
        AFLog.d(TAG,"=====urlMapToLocalApi url="+url);
        String response = "";
        if (!TextUtils.isEmpty(url)){

            // 查询所有记录列表，get 方式，无参数
            if (url.contains("queryAllH5Page")){
                response = queryAllPage();
            }

            // 新增记录，POST 方式， json和urlEncode方式参数都支持
            if (url.contains("addH5Page")){
                response = addPage(params, contentType);
            }

            // 查询单条记录，GET 方式, 参数在url中
            if (url.contains("queryH5Page")){
                response = queryOnePage(url);
            }

            // 更新记录，POST 方式， json格式参数
            if (url.contains("updateH5Page")){
                response = updatePage(params);
            }

            // 删除记录，POST 方式， json和urlEncode方式参数都支持
            if (url.contains("deleteH5Page")){
                response = deletePage(params, contentType);
            }

            // 上传文件
            if (url.contains("UploadFile")){
                // TODO: 将文件放入指定目录，并将文件目录存储存储到数据库中进行记录
                response = "离线模式文件上传成功（TODO）";
            }
        }

        return response;
    }





    // TODO: 具体业务针对各个表的操作接口， 可以放在此类，如果业务多，可以新建类放置。

    public String queryAllPage() {
        String response = "";
        JSONArray queryPageStr = PageColumns.queryAllPage(mContext);
        AFLog.d(TAG, "=====queryAllH5Page queryPageStr=" + queryPageStr);

        try {
            // TODO: 可以根据前端传过来的分页查询参数，查询sqlite, 并且填入对应字段值，此处为写死值
            JSONObject dataObject = new JSONObject();
            dataObject.put("pageNo", 1);
            dataObject.put("pageSize", 10);
            dataObject.put("total", 2);
            dataObject.put("totalPage", 1);
            dataObject.put("rows", queryPageStr);

            JSONObject object = new JSONObject();
            object.put("status", 1);
            object.put("data", dataObject);

            response = object.toString();
            AFLog.d(TAG, "#########queryAllPage response=" + response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return response;
    }


    public String addPage(String param, String contentType){
        String response = "";
        H5Page  page = new H5Page();
        AFLog.d(TAG, "#########deletePage contentType=" + contentType
                +" param=" + param);
        if ("application/x-www-form-urlencoded".equals(contentType)){
            // 参数示例格式
            //id=110&name=yuanxiaojie.html&alias=yuanxiao&groupId=3234285&type=Android
            String[] addParams = param.split("&");
            for (int i=0; i<addParams.length; i++){
                String[] keyValue = addParams[i].split("=");
                String key = keyValue[0];
                String value = keyValue[1];

                if ("id".equals(key)){
                    page.setId(Integer.valueOf(value));
                }else if("name".equals(key)){
                    page.setName(value);
                }else if("alias".equals(key)){
                    page.setAlias(value);
                }else if("groupId".equals(key)){
                    page.setGroupId(Integer.valueOf(value));
                }else if("type".equals(key)){
                    page.setType(value);
                }
            }
        } else if ("application/json".equals(contentType)){
            page = GsonHelper.toType(param, H5Page.class);
        } else {
            AFLog.d(TAG, "#########deletePage unsupported param format");
        }

        long addResult = PageColumns.addPage(mContext, page);
        AFLog.d(TAG, "#########addPage addResult=" + addResult);
        try {

            JSONObject object = new JSONObject();
            if (addResult != -1){
                object.put("status", 1);
                object.put("msg", "操作成功");
            } else {
                object.put("status", addResult);
                object.put("msg", "操作失败");
            }

            response = object.toString();
            AFLog.d(TAG, "#########addPage response=" + response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  response;
    }


    public String queryOnePage(String jsonStr){
        String response = "";
        int questionMarkIndex = jsonStr.indexOf("?");
        //url=http://192.168.65.41:8081/H5RealMonitor-web/h5realmonitor/page/queryH5Page?id=1
        String id = jsonStr.substring(questionMarkIndex+3, jsonStr.length());
        AFLog.d(TAG, "#########queryOnePage id=" + id);
        H5Page page = PageColumns.queryPage(mContext, id);
        AFLog.d(TAG, "#########queryOnePage page=" + (page==null? null: page.toString()));
        String pageStr = GsonHelper.toJson(page);
        try {

            JSONObject object = new JSONObject();
            object.put("status", 1);
            object.put("msg", "操作成功");
            object.put("data", pageStr);

            response = object.toString();
            AFLog.d(TAG, "#########queryOnePage response=" + response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  response;
    }


    public String updatePage(String jsonStr){
        String response = "";
        H5Page page = GsonHelper.toType(jsonStr, H5Page.class);
        long updateResult = PageColumns.updatePage(mContext, page);

        try {

            JSONObject object = new JSONObject();
            if (updateResult != 0){
                object.put("status", 1);
                object.put("msg", "操作成功");
            } else {
                object.put("status", updateResult);
                object.put("msg", "操作失败");
            }

            response = object.toString();
            AFLog.d(TAG, "#########queryOnePage response=" + response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  response;
    }

    public String deletePage(String param, String contentType){
        String response = "";
        String id = "";
        // TODO: 需要支持 x-www-form-urlencoded 和 json 两种格式解析
        AFLog.d(TAG, "#########deletePage contentType=" + contentType
        +" param=" + param);
        if ("application/x-www-form-urlencoded".equals(contentType)){
            String[] deleteParams = param.split("&");
            for (int i=0; i<deleteParams.length; i++){
                if (deleteParams[i].contains("id=")){
                    int equalIndex = deleteParams[i].indexOf("=");
                    id = deleteParams[i].substring(equalIndex+1, deleteParams[i].length());
                }
            }
        } else if ("application/json".equals(contentType)){
            H5Page page = GsonHelper.toType(param, H5Page.class);
            if (null != page){
                id = Integer.toString(page.getId());
            }
        } else {
            AFLog.d(TAG, "#########deletePage unsupported param format");
        }

        AFLog.d(TAG, "#########deletePage id=" + id);
        long deleteResult = PageColumns.deletePage(mContext, id);

        try {

            JSONObject object = new JSONObject();
            if (deleteResult != 0){
                object.put("status", 1);
                object.put("msg", "操作成功");
            } else {
                object.put("status", deleteResult);
                object.put("msg", "操作失败");
            }

            response = object.toString();
            AFLog.d(TAG, "#########deletePage response=" + response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  response;
    }

}
