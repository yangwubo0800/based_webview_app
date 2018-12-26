package com.base.utils.config;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.base.constant.Constants;
import com.base.utils.SpUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ParseConfig {

    private static final String TAG = "ParseConfig";

    //解析后的加载首页
    public static String sFirstLoadPage;
    //解析后的需要处理返回键的页面
    public static ArrayList<String> sHomeUrls;
    //解析后引导页面的个数, 默认值为2
    public static int sBootPageCount = 2;
    //是否需要引导页功能
    public static boolean sNeedGuidePage = true;


    /**
     * 从asset目录下加载 配置xml资源到data目录
     * @param context
     * @return
     */
    public static boolean loadConfigXmlFromAsset(Context context) {
        //asset file name
        String fileName = Constants.APP_XML_FILE_NAME;
        InputStream assetsDB = null;
        OutputStream main = null;
        OutputStream backup = null;

        try {
            //get xml file from asset
            assetsDB = context.getResources().getAssets().open(fileName);

            int length = assetsDB.available();
            if (length <= 0) {
                Log.w(TAG, "loadConfigXmlFromAsset length=" + length);
                return false;
            }
            //main xml file
            String mainDataFilePath = "/data/data/" + context.getPackageName() + "/" + Constants.APP_XML_FILE_NAME;
            String backupDataFilePath = "/data/data/" + context.getPackageName() + "/" + Constants.APP_XML_BACKUP_FILE_NAME;
            main = new FileOutputStream(mainDataFilePath);
            //backup xml file
            backup = new FileOutputStream(backupDataFilePath);

            if ((main == null) || (backup == null)) {
                Log.e(TAG, "loadXmlFromAsset new file error");
                assetsDB.close();
                return false;
            }

            int size = 1024;
            byte[] buffer = new byte[size];
            int len = 0;
            while ((len = assetsDB.read(buffer)) > 0) {
                //write to main xml
                main.write(buffer, 0, len);
                //write to backup xml
                backup.write(buffer, 0, len);
            }
            //close main xml
            main.flush();
            main.close();

            //close backup
            backup.flush();
            backup.close();

            //close asset xml stream
            assetsDB.close();

        } catch (Exception e) {
            Log.w(TAG, "loadXmlFromAsset Exception");
            e.printStackTrace();
            return false;
        } finally {
            if (main != null) {
                try {
                    main.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (backup != null) {
                try {
                    backup.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (assetsDB != null) {
                try {
                    assetsDB.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    /**
     * 处理加载解析
     * @param context
     * @param fromAsset
     */
    public static void loadProcess(Context context, boolean fromAsset) {
        if (sHomeUrls == null) {
            sHomeUrls = new ArrayList<String>();
            loadAppConfig(context, fromAsset);
        } else {
            Log.w(TAG, "initConfig sHomeUrls not null");
            sHomeUrls.clear();
            loadAppConfig(context, fromAsset);
        }
    }

    /**
     * 解析具体的xml文件
     * @param context
     * @param fromAsset
     */
    public static void loadAppConfig(Context context, boolean fromAsset) {
        Log.d(TAG, "loadAppConfig fromAsset=" + fromAsset);
        InputStream inputStream = null;
        String mainDataFilePath = "/data/data/" + context.getPackageName() + "/" + Constants.APP_XML_FILE_NAME;

        try {
            //we need to distinguish the config xml dir
            if (fromAsset) {
                //load from asset dir config xml file
                inputStream = context.getResources().getAssets().open(Constants.APP_XML_FILE_NAME);
            } else {
                //load from data dir config xml file
                inputStream = new FileInputStream(mainDataFilePath);
            }

            if (null == inputStream) {
                return;
            }

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {
                    case XmlPullParser.START_DOCUMENT:
                        Log.d(TAG, "loadCBChannelConfig START_DOCUMENT");
                        break;
                    case XmlPullParser.START_TAG:
                        if (Constants.FIRST_PAGE_URL.equals(parser.getName())) {
                            sFirstLoadPage = parser.getAttributeValue(null, Constants.ATTRIBUTE_KEY);
                            Log.d(TAG, "loadAppConfig sFirstLoadPage=" + sFirstLoadPage);
                        } else if (Constants.HOME_URLS.equals(parser.getName())) {
                            String homeUrl = parser.getAttributeValue(null, Constants.ATTRIBUTE_KEY);
                            if (!TextUtils.isEmpty(homeUrl)) {
                                Log.d(TAG, "loadAppConfig homeUrl=" + homeUrl);
                                sHomeUrls.add(homeUrl);
                            }
                        } else if (Constants.GUIDE_PAGE_COUNT.equals(parser.getName())) {
                            String guidePageCount = parser.getAttributeValue(null, Constants.COUNT_KEY);
                            Log.d(TAG, "loadAppConfig guidePageCount=" + guidePageCount);
                            sBootPageCount = Integer.parseInt(guidePageCount);
                        } else if (Constants.NEED_GUIDE_PAGE.equals(parser.getName())) {
                            String needGuidePage = parser.getAttributeValue(null, Constants.NEED_KEY);
                            Log.d(TAG, "loadAppConfig needGuidePage=" + needGuidePage);
                            sNeedGuidePage = Integer.parseInt(needGuidePage) == 0 ? false : true;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        Log.d(TAG, "loadAppConfig END_DOCUMENT");
                        break;
                }
                event = parser.next();
            }
        } catch (XmlPullParserException e) {
            Log.e(TAG, "XmlPullParserException", e);
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 应用启动时用来初始化加载解析配置xml
     * @param context
     */
    public static void initAppConfig(Context context) {
        long start = System.currentTimeMillis();

        //get shared preference key to check whether loaded asset or not
        SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
        //boolean hasLoadedAsset = SpUtils.getBoolean(context,Constants.HAS_LOADED_ASSET_KEY);
        // we have no demand for change the xml content dynamically, so load it from asset every time.
        boolean hasLoadedAsset = false;
        Log.d(TAG, "hasLoadedAsset=" + hasLoadedAsset);
        if (!hasLoadedAsset) {
            boolean loadResult = loadConfigXmlFromAsset(context);
            Log.d(TAG, "load asset to data Result=" + loadResult);
            // if loaded from asset failed, we need still parse config from asset
            // otherwise, load from data
            if (loadResult) {
                //record the load result to sharePreference
                // due to region change will clean the default preference, so we need use
                //another preference to record it.
                SpUtils.setSpFileName(Constants.PREF_NAME_SETTING);
                SpUtils.putBoolean(context, Constants.HAS_LOADED_ASSET_KEY, true);
                // TODO: load from data
                loadProcess(context, false);

            } else {
                //load from asset
                loadProcess(context, true);
            }
        } else {
            // if the asset has loaded succeed, just load the config from data
            loadProcess(context, false);
        }

        long end = System.currentTimeMillis();
        Log.d(TAG, "init cb config spent time=" + (end - start) + " ms");
    }
}
