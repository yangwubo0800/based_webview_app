package com.base.utils.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.base.dao.db.PageColumns;
import com.base.utils.log.AFLog;


/**
 * sqlite 数据库帮助类，用来创建数据库，以及新建表操作。
 */
public class DBHelper {

    private static String TAG = "DBHelper";
    private static DBHelper dbhelper = null;
    private DatabaseHelper databasehelper = null;

    private DBHelper(Context context){
        this.databasehelper = new DatabaseHelper(context );
    }

    //单例模型
    public synchronized static DBHelper getInstance(Context context){
        AFLog.d(TAG,"getInstance dbhelper=" + dbhelper);
        if(dbhelper == null){
            dbhelper = new DBHelper(context);
        }
        return dbhelper;
    }

    public SQLiteDatabase getWritableDB(){
        return databasehelper.getWritableDatabase();
    }


    public SQLiteDatabase getReadableDB(){
        return databasehelper.getReadableDatabase();
    }

    public void closeDB(){
        databasehelper.close();
    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        //数据库名称
        private static final String DATABASE_NAME = "offline.db";
        //当数据库需要升级时，请修改此版本
        private static final int DB_VERSION = 2;

        public DatabaseHelper(Context context, String name,
                              SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        public DatabaseHelper(Context context){
            this(context, DATABASE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
		    AFLog.d(TAG,"onCreate");
		    // TODO: 当有访问数据库操作时，将执行建表操作，所有业务表都在此处建好。
            db.execSQL(PageColumns.CREAT_TABLE(PageColumns.TABLE_NAME()));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO; 升级数据库表处理
            AFLog.d(TAG,"onUpgrade oldVersion=" + oldVersion);
        }

    }

}
