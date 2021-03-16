package com.base.constant;

public class Constants {

    /** SharedPreference文件名列表 */
    public static final String PREF_NAME_USERINFO = "userinfo";

    public static final String PREF_NAME_SETTING = "setting";

    /** 判断应用是否第一次打开，是否需要引导页显示的key */
    public static final String FIRST_OPEN_KEY = "first_open";

    /** 服务器地址信息*/
    public static final String SERVER_ADDRESS = "server_address";

    /** 用户是否进入离线模式*/
    public static final String OFFLINE_MODE = "offline_mode";


    /** 解析应用配置文件xml*/
    public static final String APP_XML_ROOT = "appInfos";
    //首页url标签
    public static final String FIRST_PAGE_URL = "first_page_url";
    //返回键处理url标签
    public static final String HOME_URLS = "home_urls";
    //引导页界面的个数标签，范围目前限制在2到5页之间，各项目配置后一定要替换正确的对应图片
    public static final String GUIDE_PAGE_COUNT = "guide_page_count";
    //是否需要引导页界面的标签
    public static final String NEED_GUIDE_PAGE= "need_guide_page";
    //配置内容属性
    public static final String ATTRIBUTE_KEY = "url";
    //配置引导页界面的个数属性
    public static final String COUNT_KEY = "count";
    //配置是否使用引导页界面的属性
    public static final String NEED_KEY = "need";
    //需要打包本地js的文件
    public static final String LOCAL_JS_FILE = "local_js_file";
    //配置js文件名称属性
    public static final String JS_NAME_KEY = "jsName";
    //XML文件名
    public static final String APP_XML_FILE_NAME = "app_config.xml";
    //放入data目录下的备份XML文件名
    public static final String APP_XML_BACKUP_FILE_NAME = "app_config_backup.xml";
    //是否从asset中加载过配置文件
    public static final String HAS_LOADED_ASSET_KEY = "has_loaded_asset";
    //是否需要极光推送的标签
    public static final String NEED_JPUSH= "need_jpush";
    //是否需要百度云推送的标签
    public static final String NEED_BAIDU_PUSH= "need_baiduPush";
    //是否需要个推推送的标签
    public static final String NEED_GETUI_PUSH= "need_GTPush";
    /**
     * 通知开关的key ，设置字符串开启为on, 关闭为off， 默认开启
     */
    public static final String NOTIFYCATION_SWITCH_KEY="notification_switch";

    /**
     * 三方推送消息点击跳转地址, 基于之前IOS已经使用过极光推送，为了兼容前端，使用的key字段为historyAlarmUrl
     */
    public static final String PUSH_MESSAGE_JUMP_URL_KEY="historyAlarmUrl";

    /**
     * 语音识别调用接口 科大讯飞
     */
    public static final String SPEECH_IFLY = "ifly";
    /**
     * 语音识别调用接口 百度
     */
    public static final String SPEECH_BAIDU = "baidu";
    /**
     * 语音识别调用接口 腾讯
     */
    public static final String SPEECH_TENCENT = "tencent";

    //定义activity里面请求码
    //界面跳转请求
    public static final int TAKE_PHOTO_REQUEST = 2;
    public static final int RECORD_VIDEO_REQUEST = 3;
    public static final int SCAN_QRCODE_REQUEST = 4;
    public static final int TAKE_PHOTO_AND_UPLOAD_REQUEST = 5;
    public static final int RECORD_VIDEO_AND_UPLOAD_REQUEST = 6;
    public static final int OPEN_ALERT_WINDOW_PERMISSION = 7;
    public static final int TAKE_PHOTO_AND_COMPRESS_UPLOAD_REQUEST = 8;
    //请求权限
    public static final int PERMISSION_REQUEST_CAMERA_FOR_PHOTO = 100;
    public static final int PERMISSION_REQUEST_CAMERA_FOR_VIDEO = 101;
    public static final int PERMISSION_REQUEST_CAMERA_FOR_SCAN = 102;
    public static final int PERMISSION_REQUEST_FOR_LOCATION = 103;
    public static final int PERMISSION_REQUEST_FOR_STORAGE = 104;
    public static final int PERMISSION_REQUEST_FOR_EZOPENPLAY = 105;
    public static final int PERMISSION_REQUEST_FOR_IMEI = 106;
    public static final int PERMISSION_REQUEST_FOR_INPUT_TAG_STORAGE = 107;
    public static final int PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO = 108;
    public static final int PERMISSION_REQUEST_FOR_INPUT_TAG_RECORD_VIDEO = 109;
    public static final int PERMISSION_REQUEST_FOR_SAVE_LOG = 110;
    public static final int PERMISSION_REQUEST_FOR_INPUT_TAG_TAKE_PHOTO_COMPRESS = 111;
    public static final int PERMISSION_REQUEST_FOR_SPEECH_RECOGNIZE = 112;

    //更新位置信息消息
    public static final int MSG_FOR_LOCATION = 10001;
    //安卓5.0版本webview无网络提示
    public static final int MSG_FOR_NO_NETWORK= 10002;
    //获取萤石云视频列表信息
    public static final int MSG_FOR_GET_EZOPEN_VIDEO_LIST= 10003;
    //获取加密IMEI，因为授权问题，可能异步返回
    public static final int MSG_FOR_GET_MD5_IMEI= 10004;
    //获取扫码字符串信息
    public static final int MSG_FOR_GET_SCAN_INFO= 10005;
    //返回语音识别信息
    public static final int MSG_FOR_SPEECH_TEXT= 10006;
    //子线程中需要显示toast
    public static final int MSG_FOR_SHOW_TOAST= 10007;
    //语音识别信息开始
    public static final int MSG_FOR_SPEECH_BEGIN= 10008;
    //语音识别信息结束
    public static final int MSG_FOR_SPEECH_END= 10009;
}
