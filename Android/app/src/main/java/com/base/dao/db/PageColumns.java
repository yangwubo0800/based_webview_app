package com.base.dao.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;


import com.base.utils.db.DBHelper;
import com.base.utils.log.AFLog;
import com.base.bean.db.H5Page;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 业务表的创建语句 以及CRUD 操作逻辑处理
 */
public class PageColumns {

    private static String TAG = "PageColumns";

    public static  String TABLE_NAME(){
        return "xxx_table_name";
    }

    // database field
    public static final String ID = "id";  //ID

    public static final String GROUP_ID = "GROUP_ID";

    public static final String NAME = "NAME";

    public static final String  ALIAS = "ALIAS";

    public static final String TYPE = "TYPE";

    public static final String SORT_NO = "SORT_NO";

    public static final String CREATE_TIME = "CREATE_TIME";

    public static final String CREATOR = "CREATOR";

    public static final String LAST_UPDATE_TIME = "LAST_UPDATE_TIME";

    public static final String LAST_MODIFIER = "LAST_MODIFIER";


    // TODO: 可以再定义一组和前端字段对应的字符串用来作为cursor查询时使用。
    public static final String[] COLUMN_ARRAY = {
            "id",
            "groupId",
            "name",
            "alias",
            "type",
            "sortNo",
            "createTime",
            "creator",
            "lastUpdateTime",
            "lastModifier"
    };



    public static String CREAT_TABLE(String tableName){
        return new StringBuffer().
                append("CREATE TABLE IF NOT EXISTS ").append(tableName).
                append("(").
                append(ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,").
                append(GROUP_ID).append(" TEXT,").
                append(NAME).append(" TEXT,").
                append(ALIAS).append(" TEXT,").
                append(TYPE).append(" TEXT,").
                append(SORT_NO).append(" INTEGER DEFAULT 0,").
                append(CREATE_TIME).append(" DATE DEFAULT NULL,").
                append(CREATOR).append(" INTEGER DEFAULT 0,").
                append(LAST_UPDATE_TIME).append(" DATE DEFAULT NULL ,").
                append(LAST_MODIFIER).append(" INTEGER DEFAULT 0 ").
                append(");").toString();
    }

    private static String DROP_TABLE(){
        return "DROP TABLE IF EXISTS " +  TABLE_NAME();
    }


    // 查询数据库中的所有记录，以json数组形式返回
    public static JSONArray queryAllPage(Context context){
        String pageListStr = "";
        String querySql = "SELECT *, \"test_group_name\" as GROUP_NAME FROM `xxx_table_name` " +
                "order by  SORT_NO, CREATE_TIME DESC, LAST_UPDATE_TIME DESC";

        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db  = dbHelper.getReadableDB();
        Cursor cursor = db.rawQuery(querySql, null);
        ArrayList<H5Page> pageList = new ArrayList<H5Page>();
        JSONArray jsonArray = new JSONArray();
        JSONObject tmpObj = null;
        if (null != cursor && cursor.getCount() > 0){
            while (cursor.moveToNext()){
                H5Page page = new H5Page();
                page.setId(cursor.getInt(cursor.getColumnIndex(ID)));
                page.setGroupId(cursor.getInt(cursor.getColumnIndex(GROUP_ID)));
                page.setName(cursor.getString(cursor.getColumnIndex(NAME)));
                page.setAlias(cursor.getString(cursor.getColumnIndex(ALIAS)));
                page.setType(cursor.getString(cursor.getColumnIndex(TYPE)));
                page.setGroupName(cursor.getString(cursor.getColumnIndex("GROUP_NAME")));
                String date = cursor.getString(cursor.getColumnIndex(CREATE_TIME));
                AFLog.d(TAG,"====date=" + date);
                pageList.add(page);


                tmpObj = new JSONObject();
                try {
                    tmpObj.put("id", cursor.getInt(cursor.getColumnIndex(ID)));
                    tmpObj.put("groupId", cursor.getInt(cursor.getColumnIndex(GROUP_ID)));
                    tmpObj.put("name", cursor.getString(cursor.getColumnIndex(NAME)));
                    tmpObj.put("alias", cursor.getString(cursor.getColumnIndex(ALIAS)));
                    tmpObj.put("type", cursor.getString(cursor.getColumnIndex(TYPE)));
                    tmpObj.put("groupName", cursor.getString(cursor.getColumnIndex("GROUP_NAME")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonArray.put(tmpObj);
            }
            cursor.close();
        }

        db.close();
        pageListStr = jsonArray.toString();
        AFLog.d(TAG,"=====pageListStr=" + pageListStr);
        return  jsonArray;
    }


    //添加一条业务数据
    public static long addPage(Context context, H5Page page){
        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db  = dbHelper.getWritableDB();
        long insertResult = -1;

        try{
            ContentValues cv = new ContentValues();
            cv.put(GROUP_ID, page.getGroupId());
            cv.put(NAME, page.getName());
            cv.put(ALIAS, page.getAlias());
            cv.put(TYPE, page.getType());
            //自增ID，但是也可以由用户指定
            if (!TextUtils.isEmpty(Integer.toString(page.getId()))){
                cv.put(ID, page.getId());
            }

            insertResult = db.insert("xxx_table_name",
                    null,
                    cv);
            AFLog.d(TAG,"=====addPage insertResult=" + insertResult);
            db.close();
        }catch (Exception e){

        }
        return insertResult;
    }

    //查询一条业务数据
    public static H5Page queryPage(Context context, String id){

        String querySql = "SELECT *, \"test_group_name\" as GROUP_NAME FROM `xxx_table_name` where id=" + id;
        H5Page page = new H5Page();

        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db  = dbHelper.getReadableDB();
        try{
            Cursor cursor = db.rawQuery(querySql, null);
            if (null != cursor && cursor.getCount() > 0){
                //notice move to the next
                cursor.moveToNext();
                page.setId(cursor.getInt(cursor.getColumnIndex(ID)));
                page.setGroupId(cursor.getInt(cursor.getColumnIndex(GROUP_ID)));
                page.setName(cursor.getString(cursor.getColumnIndex(NAME)));
                page.setAlias(cursor.getString(cursor.getColumnIndex(ALIAS)));
                page.setType(cursor.getString(cursor.getColumnIndex(TYPE)));
                page.setGroupName(cursor.getString(cursor.getColumnIndex("GROUP_NAME")));
                String date = cursor.getString(cursor.getColumnIndex(CREATE_TIME));
                AFLog.d(TAG,"====date=" + date);
            }
        }catch (Exception e){
            AFLog.d(TAG,"====e=" + e.toString());
            e.printStackTrace();
        }

        db.close();

        return page;
    }

    //更新一条业务数据
    public static long updatePage(Context context, H5Page page){
        String id;
        long updateResult = -1;
        if (!TextUtils.isEmpty(Integer.toString(page.getId()))){
            id = Integer.toString(page.getId());
        } else {
            return  updateResult;
        }

        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db  = dbHelper.getWritableDB();

        try {
            ContentValues cv = new ContentValues();
            cv.put(GROUP_ID, page.getGroupId());
            cv.put(NAME, page.getName());
            cv.put(ALIAS, page.getAlias());
            cv.put(TYPE, page.getType());

            updateResult = db.update("xxx_table_name",
                    cv,
                    "id=?",
                    new String[]{id});
            AFLog.d(TAG,"=====updatePage updateResult=" + updateResult);
            db.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return updateResult;
    }

    //删除一条业务数据
    public static long deletePage(Context context, String id){
        DBHelper dbHelper = DBHelper.getInstance(context);
        SQLiteDatabase db  = dbHelper.getWritableDB();
        long deleteResult = db.delete("xxx_table_name", "id=?", new String[]{id});
        AFLog.d(TAG,"=====deletePage deleteResult=" + deleteResult);

        return deleteResult;
    }

}
