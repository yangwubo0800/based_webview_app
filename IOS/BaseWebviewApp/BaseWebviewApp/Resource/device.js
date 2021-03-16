/** ================================= **/
/** ===== EventEmitter Api start ==== **/
/** ================================= **/

(function (window, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define(factory);
    } else {
        window.EventEmitter = factory();
    }
})(this, function () {

    function EventEmitter() {
        this._events = this._events || {};
        this._maxListeners = this._maxListeners || defaultMaxListeners;
    }

    // By default EventEmitters will print a warning if more than
    // 10 listeners are added to it. This is a useful default which
    // helps finding memory leaks.
    //
    // Obviously not all Emitters should be limited to 10. This function allows
    // that to be increased. Set to zero for unlimited.
    var defaultMaxListeners = 10;

    EventEmitter.prototype.setMaxListeners = function (n) {
        if (typeof n !== 'number' || n < 0) {
            throw new TypeError('n must be a positive number');
        }
        this._maxListeners = n;
    };

    EventEmitter.prototype.emit = function (type) {
        var er, handler, len, args, i, listeners;

        if (!this._events) {
            this._events = {};
        }

        // If there is no 'error' event listener then throw.
        if (type === 'error') {
            if (!this._events.error || (typeof this._events.error === 'object' && !this._events.error.length)) {
                er = arguments[1];
                if (er instanceof Error) {
                    throw er; // Unhandled 'error' event
                } else {
                    throw new TypeError('Uncaught, unspecified "error" event.');
                }
            }
        }

        handler = this._events[type];

        if (typeof handler === 'undefined') {
            return false;
        }

        if (typeof handler === 'function') {
            switch (arguments.length) {
                // fast cases
                case 1:
                    handler.call(this);
                    break;
                case 2:
                    handler.call(this, arguments[1]);
                    break;
                case 3:
                    handler.call(this, arguments[1], arguments[2]);
                    break;
                // slower
                default:
                    len = arguments.length;
                    args = new Array(len - 1);
                    for (i = 1; i < len; i++) {
                        args[i - 1] = arguments[i];
                    }
                    handler.apply(this, args);
            }
        } else if (typeof handler === 'object') {
            len = arguments.length;
            args = new Array(len - 1);
            for (i = 1; i < len; i++) {
                args[i - 1] = arguments[i];
            }

            listeners = handler.slice();
            len = listeners.length;
            for (i = 0; i < len; i++) {
                listeners[i].apply(this, args);
            }
        }

        return true;
    };

    EventEmitter.prototype.addListener = function (type, listener) {
        var m;

        if (typeof listener !== 'function') {
            return this; // ignored
            //throw new TypeError('listener must be a function');
        }

        if (!this._events) {
            this._events = {};
        }

        // To avoid recursion in the case that type === "newListener"! Before
        // adding it to the listeners, first emit "newListener".
        if (this._events.newListener) {
            this.emit('newListener', type, typeof listener.listener === 'function' ? listener.listener : listener);
        }

        if (!this._events[type])
        // Optimize the case of one listener. Don't need the extra array object.
        {
            this._events[type] = listener;
        } else if (typeof this._events[type] === 'object')
        // If we've already got an array, just append.
        {
            this._events[type].push(listener);
        } else
        // Adding the second element, need to change to array.
        {
            this._events[type] = [this._events[type], listener];
        }

        // Check for listener leak
        if (typeof this._events[type] === 'object' && !this._events[type].warned) {
            m = this._maxListeners;
            if (m && m > 0 && this._events[type].length > m) {
                this._events[type].warned = true;
                console.error('(node) warning: possible EventEmitter memory ' + 'leak detected. %d listeners added. ' + 'Use emitter.setMaxListeners() to increase limit.', this._events[type].length);
                console.trace();
            }
        }

        return this;
    };

    EventEmitter.prototype.on = EventEmitter.prototype.addListener;

    EventEmitter.prototype.once = function (type, listener) {
        if (typeof listener !== 'function') {
            throw new TypeError('listener must be a function');
        }

        function g() {
            this.removeListener(type, g);
            listener.apply(this, arguments);
        }

        g.listener = listener;
        this.on(type, g);

        return this;
    };

    // emits a 'removeListener' event iff the listener was removed
    EventEmitter.prototype.removeListener = function (type, listener) {
        var list, position, length, i;

        if (typeof listener !== 'function') {
            throw new TypeError('listener must be a function');
        }

        if (!this._events || !this._events[type]) {
            return this;
        }

        list = this._events[type];
        length = list.length;
        position = -1;

        if (list === listener || (typeof list.listener === 'function' && list.listener === listener)) {
            delete this._events[type];
            if (this._events.removeListener) {
                this.emit('removeListener', type, listener);
            }

        } else if (typeof list === 'object') {
            for (i = length; i-- > 0;) {
                if (list[i] === listener || (list[i].listener && list[i].listener === listener)) {
                    position = i;
                    break;
                }
            }

            if (position < 0) {
                return this;
            }

            if (list.length === 1) {
                list.length = 0;
                delete this._events[type];
            } else {
                list.splice(position, 1);
            }

            if (this._events.removeListener) {
                this.emit('removeListener', type, listener);
            }
        }

        return this;
    };

    EventEmitter.prototype.removeAllListeners = function (type) {
        var key, listeners;

        if (!this._events) {
            return this;
        }

        // not listening for removeListener, no need to emit
        if (!this._events.removeListener) {
            if (arguments.length === 0) {
                this._events = {};
            } else if (this._events[type]) {
                delete this._events[type];
            }
            return this;
        }

        // emit removeListener for all listeners on all events
        if (arguments.length === 0) {
            for (key in this._events) {
                if (key === 'removeListener') continue;
                this.removeAllListeners(key);
            }
            this.removeAllListeners('removeListener');
            this._events = {};
            return this;
        }

        listeners = this._events[type];

        if (typeof listeners === 'function') {
            this.removeListener(type, listeners);
        } else {
            // LIFO order
            while (listeners.length) {
                this.removeListener(type, listeners[listeners.length - 1]);
            }
        }
        delete this._events[type];

        return this;
    };

    EventEmitter.prototype.listeners = function (type) {
        var ret;
        if (!this._events || !this._events[type]) {
            ret = [];
        } else if (typeof this._events[type] === 'function') {
            ret = [this._events[type]];
        } else {
            ret = this._events[type].slice();
        }
        return ret;
    };

    EventEmitter.listenerCount = function (emitter, type) {
        var ret;
        if (!emitter._events || !emitter._events[type]) {
            ret = 0;
        } else if (typeof emitter._events[type] === 'function') {
            ret = 1;
        } else {
            ret = emitter._events[type].length;
        }
        return ret;
    };

    return EventEmitter;
});

/** ================================= **/
/** ====== EventEmitter Api end ===== **/
/** ================================= **/


(function (window, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define(factory);
    } else {
        window.MobileApi = factory();
    }
})(this, function () {
    function MobileApi(type){
        MobileApi.prototype.__proto__ = EventEmitter.prototype;
        let api;
        if(type == 'Android'){
            AndroidApi.prototype.__proto__ = MobileApi.prototype;
            api = new AndroidApi();
        }else if(type == 'IOS'){
            IOSApi.prototype.__proto__ = MobileApi.prototype;
            api = new IOSApi();
        }else {
            api = this;
        }
        return api;
    }

    /**
     * 扫描二维码
     * @param {String} type
     */
    MobileApi.prototype.scanQRCode = function(type){
    }


    /**
     * 获取定位方法
     * @param {String} type
     */
    MobileApi.prototype.location = function(type){
    }

    /**
     * 关闭定位服务
     */
    MobileApi.prototype.closeLocation = function(){
    }

    /**
     * 获取设备Imei
     */
    MobileApi.prototype.deviceId = function(){
    }

    /**
     * 告警消息服务
     * @param {Object} options
     */
    MobileApi.prototype.alarm = function(options){
    }

    /**
     * 设置变量存储
     * @param {String} key
     * @param {String} value
     */
    MobileApi.prototype.setItem = function(key, value){
    }

    /**
     * 获取存储值
     * @param {String} key
     */
    MobileApi.prototype.getItem = function(key){
    }

    /**
     * 获取App的版本信息
     * @param {String} type
     */
    MobileApi.prototype.getAppInfo = function(type){
    }

    /**
     * 打电话
     * @param {String} number
     */
    MobileApi.prototype.callNumber = function(number){
    }

    /**
     * 更新app
     * @param {String} address
     */
    MobileApi.prototype.updateApp = function(address){
    }
    
    /**
     * 图片预览
     * @param {String} url
     */
    MobileApi.prototype.previewPhoto = function(url){
    }
    /**
     * 视频播放
     * @param {String} videoUrl
     * @param {String} videoTitle
     */
    MobileApi.prototype.videoPlay = function(videoUrl, videoTitle){
    }
    /**
     * 视频直播
     * @param {String} videoUrl
     * @param {String} type 直播类型
     * @param {String} videoTitle
     */
    MobileApi.prototype.livePlay = function(liveUrl, liveTitle, type, configUrl){
    }
    /**
     * 获取缓存大小
     * @param {String} address
     */
    MobileApi.prototype.getAppCacheSize = function(){
    }
    /**
     * 清除缓存
     */
    MobileApi.prototype.clearCache = function(){
    }
    /**
     * 打开h5实时画面
     * @param {String} url
     */
    MobileApi.prototype.openRealHtml = function(url){
    }
    /**
     * 设置横竖屏
     * @param {String} orientation
     */
    MobileApi.prototype.setOrientation = function(orientation){
    }
    /**
     * 启动消息推送服务
     * @param {String} clientId
     * @param {String} stationId
     * @param {String} topics
     */
    MobileApi.prototype.startMessageSevice = function(clientId, stationId, topics){
    }
    /**
     * 停止消息推送服务
     */
    MobileApi.prototype.stopMessageSevice = function(){
    }
    /**
     * 热成像仪
     */
    MobileApi.prototype.takeFlirPhone = function(){
    }

    /**
     * 文件下载
     * @param {String} url
     * @param {String} fileName
     */
    MobileApi.prototype.download = function(url, fileName){
    }
    // startMapNavigation
    //跳转第三方导航
    MobileApi.prototype.startMapNavigation = function(params){
    }
    /**
     * 设置极光推送标签和点击消息页面跳转地址
     *  params = {
            "tags":"stationId001",
            "jumpUrl":"https://www.sina.com.cn/"
        }
     */
    MobileApi.prototype.setJPushTagAndJumpUrl = function(params){
    }
    /**
     * 清除极光推送标签
     *
     */
    MobileApi.prototype.cleanJPushTag = function(){
    }

    /**
     * 设置消息推送标签和点击消息页面跳转地址
     *  params = {
            "pushPlatform":"jiguang"
            "tags":"user01",
            "jumpUrl":"https://www.sina.com.cn/"
        }
     参数对应key说明， pushPlatform 为推送平台，jiguang 代表极光， getui 代表个推， baidu 代表百度云；tags 为设置的标签； jumpUrl 为消息跳转页面菜单地址。
     */
    MobileApi.prototype.setMsgPushTagAndJumpUrl = function(params){
    }
    /**
     * 清除消息推送标签
     接口参数传入对应平台，各个平台的字符串名称和设置接口中保持一致。
     *
     */
    MobileApi.prototype.cleanMsgPushTag = function(params){
    }


    /**
     * 开启语音识别
     *  params = {
            "speechPlatform":"ifly"，
            "useDefaultSetting":"1"
        }
      传递参数为json格式字符串，speechPlatform 为识别平台，支持科大讯飞ifly， 百度baidu, 腾讯tencent,
      useDefaultSetting  是否使用默认设置，1 为使用默认设置，则接口中会用默认设置进行， 0 为不适用默认设置，可以根据具体平台的参数进行设置
      关于各个平台的具体参数支持请查看使用说明
     */
    MobileApi.prototype.startSpeechRecognize = function(params){
    }

    /**
     * 关闭语音识别
     *  params = {
            "speechPlatform":"ifly"
        }
      传递参数为json格式字符串，speechPlatform 为识别平台，支持科大讯飞ifly， 百度baidu, 腾讯tencent
     */
    MobileApi.prototype.stopSpeechRecognize = function(params){
    }

    return MobileApi;
});



/** ================================= **/
/** ======= Android Api start ======= **/
/** ================================= **/

(function (window, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define(factory);
    } else {
        window.AndroidApi = factory();
    }
})(this, function () {
    function AndroidApi(){
        var _self = this;
        _self.on("location", function(res){
            _self.emit("locationRs", JSON.parse(res))
        })
        _self.on("qrCode", function(res){
            _self.emit("qrCodeRs", JSON.parse(res))
        })
        _self.on("deviceId", function(res){
            _self.emit("deviceIdRs", JSON.parse(res))
        })
        _self.on("thermal_image", function(res){
            _self.emit("thermal", res)
        })
        _self.on("speechText", function(res){
            _self.emit("speechTextRs", res)
        })
        _self.on("speechBegin", function(res){
            _self.emit("speechBeginRs", res)
        })
        _self.on("speechEnd", function(res){
            _self.emit("speechEndRs", res)
        })
    }

    /**
     * 扫描二维码
     * @param {String} type
     */
    AndroidApi.prototype.scanQRCode = function(type){
        try {
            window.functionTag.scanQRCode(type)
        } catch (error) {
            
        }
    }

    /**
     * 获取定位方法
     * @param {String} type
     */
    AndroidApi.prototype.location = function(type){
        try {
            window.functionTag.getLocationInfo(type)
        } catch (error) {
            
        }
    }

    /**
     * 关闭定位服务
     */
    AndroidApi.prototype.closeLocation = function(){
        try {
            window.functionTag.cancelMonitorLocation()
        } catch (error) {
            
        }
    }

    /**
     * 获取设备Imei
     */
    AndroidApi.prototype.deviceId = function(){
        try {
            window.functionTag.getDeviceId()
        } catch (error) {
            
        }
    }

    /**
     * 告警消息服务
     * @param {Object} options
     */
    AndroidApi.prototype.alarm = function(options){
        //设置服务器地址
        if(!options.ip || !options.session || !options.stationId || !options.user){
            console.error('缺少所需的启动参数，启动参数(ip/session/stationId/user)')
            return ;
        }
        window.functionTag.setServerAddress(options.ip)
        window.functionTag.setSession(options.session)
        window.functionTag.setStationId(options.stationId)
        window.functionTag.setUser(options.user);

        //启动告警服务
        window.functionTag.startAlarmService();
    }

    /**
     * 设置变量存储
     * @param {String} key
     * @param {String} value
     */
    AndroidApi.prototype.setItem = function(key, value){
        try {
            window.functionTag.setKeyValue(key, value)
        } catch (error) {
        }
    }

    /**
     * 获取存储值
     * @param {String} key
     */
    AndroidApi.prototype.getItem = function(key){
        var rs = '';
        try {
            rs = window.functionTag.getValueByKey(key)
        } catch (error) {
        }
        return rs;
    }

    /**
     * 获取App的版本信息
     * @param {String} type
     */
    AndroidApi.prototype.getAppInfo = function(){
        var rs = {};
        try {
            rs = JSON.parse(window.functionTag.getAppInfo())
        } catch (error) {
        }
        return rs;
    }

    /**
     * 打电话
     * @param {String} number
     */
    AndroidApi.prototype.callNumber = function(number){
        try {
            window.functionTag.CallNumber(number)
        } catch (error) {
        }
    }

    /**
     * 更新app
     * @param {String} address
     */
    AndroidApi.prototype.updateApp = function(address){
        var address = address || '';
        try {
            window.functionTag.checkNewVersion(address)
        } catch (error) {
        }
    }
    
    /**
     * 图片预览
     * @param {String} url
     */
    AndroidApi.prototype.previewPhoto = function(url){
        try {
            window.functionTag.previewPhoto(url)
        } catch (error) {
        }
    }
    /**
     * 视频播放
     * @param {String} videoUrl
     * @param {String} videoTitle
     */
    AndroidApi.prototype.videoPlay = function(videoUrl, videoTitle){
        try {
            window.functionTag.videoPlay(videoUrl, videoTitle)
        } catch (error) {
        }
    }
    /**
     * 视频直播
     * @param {String} videoUrl
     * @param {String} videoTitle
     */
    AndroidApi.prototype.livePlay = function(liveUrl, liveTitle, type, configUrl){
        try {
            if(type == 'EZ_Video'){
                window.functionTag.livePlay("standard", liveUrl, liveTitle)
                if(configUrl){
                    window.functionTag.setVideoConfigInfoUrl(configUrl, type);
                }
            }else if(type == 'HIK_Video'){
                window.functionTag.startHikVideoPlay(liveUrl, liveTitle)
                if(configUrl){
                    window.functionTag.setVideoConfigInfoUrl(configUrl, type);
                }
            }
        } catch (error) {
        }
    }
    /**
     * 获取缓存大小
     * @param {String} address
     */
    AndroidApi.prototype.getAppCacheSize = function(){
        var rs = 0;
        try {
            rs = window.functionTag.getAppCacheSize();
        } catch (error) {
        }
        return rs;
    }


    /**
     * 清除缓存
     */
    AndroidApi.prototype.clearCache = function(){
        try {
            window.functionTag.CleanWebCache();
        } catch (error) {
        }
    }

    /**
     * 打开h5实时画面
     * @param {String} url
     */
    AndroidApi.prototype.openRealHtml = function(url){
        try {
            window.functionTag.startRealHtmlActivity(url)
        } catch (error) {
        }
    }
     /**
     * 设置横竖屏
     * @param {String} orientation
     */
    AndroidApi.prototype.setOrientation = function(orientation){
        try{
            window.functionTag.setOrientation(orientation)
        } catch(error){

        }
    }
    /**
     * 启动消息推送服务
     * @param {String} clientId
     * @param {String} stationId
     * @param {String} topics
     */
    AndroidApi.prototype.startMessageSevice = function(clientId, stationId, topics){
        try {
            window.functionTag.setTopics(topics)
            window.functionTag.setClientId(clientId)
            window.functionTag.setStationId(stationId)
            window.functionTag.startMqttService()
        } catch (error) {
            
        }
    }
    /**
     * 停止消息推送服务
     */
    AndroidApi.prototype.stopMessageSevice = function(){
        try {
            window.functionTag.stopMqttService()
        } catch (error) {
                
        }
    }
    /**
     * 热成像仪
     */
    AndroidApi.prototype.takeFlirPhone = function(){
        try {
            window.functionTag.takeFlirPhone('thermal_image')
        } catch (error) {
        }
    }

    /**
     * 文件下载
     * @param {String} url
     * @param {String} fileName
     */
    AndroidApi.prototype.download = function(url, fileName){
        try {
            window.functionTag.DownloadFileByName(url, fileName)
        } catch (error) {
        }
    }
    //跳转第三方导航
    AndroidApi.prototype.startMapNavigation = function(params){
        try {
            window.functionTag.startMapNavigation(JSON.stringify(params));
        } catch (error) {
        }
    }
   /**
    * 设置极光推送标签和点击消息页面跳转地址
    *  params = {
    "tags":"stationId001",
    "jumpUrl":"https://www.sina.com.cn/"
    }
    */
    AndroidApi.prototype.setJPushTagAndJumpUrl = function(params){
            try {
                window.functionTag.setJPushTagAndJumpUrl(JSON.stringify(params));
            } catch (error) {
        }
    }
    //清除极光推送标签
    AndroidApi.prototype.cleanJPushTag = function(){
        try {
            window.functionTag.cleanJPushTag()
        } catch (error) {
        }
    }



    /**
     * 设置消息推送标签和点击消息页面跳转地址
     *  params = {
            "pushPlatform":"jiguang"
            "tags":"user01",
            "jumpUrl":"https://www.sina.com.cn/"
        }
     */
    AndroidApi.prototype.setMsgPushTagAndJumpUrl = function(params){
            try {
                var pushPlatform = params["pushPlatform"]
                if("jiguang" == pushPlatform){
                    window.functionTag.setJPushTagAndJumpUrl(JSON.stringify(params));
                }else if("baidu" == pushPlatform){
                    window.functionTag.setBaiduPushTagAndJumpUrl(JSON.stringify(params));
                }else if("getui" == pushPlatform){
                    window.functionTag.setGTPushTagAndJumpUrl(JSON.stringify(params));
                }

            } catch (error) {
        }
    }

    /**
     * 清除消息推送标签
     *
     */
    AndroidApi.prototype.cleanMsgPushTag = function(pushPlatform){
        try {

            if("jiguang" == pushPlatform){
            alert("jiguang")
                window.functionTag.cleanJPushTag();
            }else if("baidu" == pushPlatform){
                window.functionTag.cleanBaiduPushTag();
            }else if("getui" == pushPlatform){
                window.functionTag.cleanGTPushTag();
            }

        } catch (error) {
        }
    }

      /**
       * 开启语音识别
       *  params = {
              "speechPlatform":"ifly"，
              "useDefaultSetting":"1"
          }
        传递参数为json格式字符串，speechPlatform 为识别平台，支持科大讯飞ifly， 百度baidu, 腾讯tencent,
        useDefaultSetting  是否使用默认设置，1 为使用默认设置，则接口中会用默认设置进行， 0 为不适用默认设置，可以根据具体平台的参数进行设置
        关于各个平台的具体参数支持请查看使用说明
       */
     AndroidApi.prototype.startSpeechRecognize = function(params){
                try {
                    var speechPlatform = params["speechPlatform"]
                    var useDefaultSetting = params["useDefaultSetting"]
                    var emptyParam

//                    if("ifly" == speechPlatform){
//                        if("1" == useDefaultSetting){
//                            window.functionTag.iflyStartRecord(emptyParam);
//                        }else{
//                            window.functionTag.iflyStartRecord(JSON.stringify(params));
//                        }
//                    }else if("baidu" == speechPlatform){
//                        if("1" == useDefaultSetting){
//                            window.functionTag.baiduStartRecord(emptyParam);
//                        }else{
//                            window.functionTag.baiduStartRecord(JSON.stringify(params));
//                        }
//                    }else if("tencent" == speechPlatform){
//                       if("1" == useDefaultSetting){
//                           window.functionTag.tencentStartRecord(emptyParam);
//                       }else{
//                           window.functionTag.tencentStartRecord(JSON.stringify(params));
//                       }
//                    }
                    window.functionTag.speechRecStartTimeout60S(JSON.stringify(params));
                } catch (error) {
            }
        }

      /**
       * 关闭语音识别
       *  params = {
              "speechPlatform":"ifly"
          }
        传递参数为json格式字符串，speechPlatform 为识别平台，支持科大讯飞ifly， 百度baidu, 腾讯tencent
       */
      AndroidApi.prototype.stopSpeechRecognize = function(speechPlatform){
                try {
//                    if("ifly" == speechPlatform){
//                        window.functionTag.iflyStopRecord();
//                    }else if("baidu" == speechPlatform){
//                        window.functionTag.baiduStopRecord();
//                    }else if("tencent" == speechPlatform){
//                        window.functionTag.tencentStopRecord();
//                    }
                    window.functionTag.speechRecStopTimeout60S(speechPlatform);
                } catch (error) {
            }
        }


    return AndroidApi;
});

/** ================================= **/
/** ======== Android Api end ======== **/
/** ================================= **/



/** ================================= **/
/** ========= IOS Api start ========= **/
/** ================================= **/

(function (window, factory) {
    if (typeof exports === 'object') {
        module.exports = factory();
    } else if (typeof define === 'function' && define.amd) {
        define(factory);
    } else {
        window.IOSApi = factory();
    }
})(this, function () {

    function IOSApi(){
        var _self = this;
        _self.on("location", function(res){
            _self.emit("locationRs", JSON.parse(res))
        })
        _self.on("qrCode", function(res){
            _self.emit("qrCodeRs", JSON.parse(res))
        })
        _self.on("deviceId", function(res){
            _self.emit("deviceIdRs", JSON.parse(res))
        })
        _self.on("thermal_image", function(res){
            _self.emit("thermal", res)
        })
        _self.on("speechText", function(res){
            _self.emit("speechTextRs", res)
        })
        _self.on("speechBegin", function(res){
            _self.emit("speechBeginRs", res)
        })
        _self.on("speechEnd", function(res){
            _self.emit("speechEndRs", res)
        })
    }

    /**
     * 扫描二维码
     * @param {String} type
     */
    IOSApi.prototype.scanQRCode = function(type){
        try {
            dsBridge.call("ios.scanQRCode", type)
        } catch (error) {
        }
    }

    /**
     * 获取定位方法
     * @param {String} type
     */
    IOSApi.prototype.location = function(type){
        try {
            dsBridge.call("ios.locate", type)
        } catch (error) {
            
        }
    }

    /**
     * 关闭定位服务
     */
    IOSApi.prototype.closeLocation = function(){
        try {
        } catch (error) {
            
        }
    }

    /**
     * 获取设备Imei
     */
    IOSApi.prototype.deviceId = function(){
        try {
            var deviceId = dsBridge.call("ios.getDeviceId");
            this.emit("deviceId", JSON.stringify({"deviceId": deviceId}))
        } catch (error) {
            
        }
    }

    /**
     * 告警消息服务
     * @param {Object} options
     */
    IOSApi.prototype.alarm = function(options){
    }

    /**
     * 设置变量存储
     * @param {String} key
     * @param {String} value
     */
    IOSApi.prototype.setItem = function(key, value){
        try {
            dsBridge.call("ios.setValueByKey", JSON.stringify({[key]: value}))
        } catch (error) {
        }
    }

    /**
     * 获取存储值
     * @param {String} key
     */
    IOSApi.prototype.getItem = function(key){
        var rs = '';
        try {
            rs = dsBridge.call("ios.getValueByKey",  key);
            var type = Object.prototype.toString.call(rs).slice(8,-1)
            if(type && type != 'String'){
                rs = JSON.stringify(rs);
            }
        } catch (error) {
        }
        return rs;
    }

    /**
     * 获取App的版本信息
     * @param {String} type
     */
    IOSApi.prototype.getAppInfo = function(){
        var rs = {};
        try {
            rs = JSON.parse(dsBridge.call("ios.getAppInfo"))
        } catch (error) {
        }
        return rs;
    }

    /**
     * 打电话
     * @param {String} number
     */
    IOSApi.prototype.callNumber = function(number){
        try {
            dsBridge.call("ios.dailNumber", number)
        } catch (error) {
        }
    }

    /**
     * 更新app
     * @param {String} address
     */
    IOSApi.prototype.updateApp = function(address){
    }
    
    /**
     * 图片预览
     * @param {String} url
     */
    IOSApi.prototype.previewPhoto = function(url){
        try {
            dsBridge.call("ios.imagePreview", url)
        } catch (error) {
        }
    }
    /**
     * 视频播放
     * @param {String} videoUrl
     * @param {String} videoTitle
     */
    IOSApi.prototype.videoPlay = function(videoUrl, videoTitle){
        try {
            dsBridge.call("ios.videoPlay", videoUrl)
        } catch (error) {
        }
    }
    /**
     * 视频直播
     * @param {String} videoUrl
     * @param {String} videoTitle
     */
    IOSApi.prototype.livePlay = function(liveUrl, liveTitle, type, configUrl){
        try {
            if(type == 'EZ_Video'){
                if(configUrl){
                    dsBridge.call("ios.ijkLivePlay", liveUrl)
                    dsBridge.call("ios.setVideoConfigInfoGetUrl", configUrl)
                }else{
                    var params = {
                        "url": liveUrl,
                        "videoTitle": liveTitle
                    }
                    dsBridge.call("ios.ijkLivePlayWithTitle", JSON.stringify(params))
                }
            }else if(type == 'HIK_Video'){
                var params = {
                    "url": liveUrl,
                    "videoTitle": liveTitle
                }
                dsBridge.call("ios.hikVideoPlay", JSON.stringify(params))
                if(configUrl){
                    dsBridge.call("ios.setHikVideoConfigInfoGetUrl", configUrl)
                }
            }

            dsBridge.call("ios.ijkLivePlay", liveUrl)
        } catch (error) {
        }
    }
    /**
     * 获取缓存大小
     * @param {String} address
     */
    IOSApi.prototype.getAppCacheSize = function(){
        var rs = 0;
        try {
            rs = dsBridge.call("ios.getCacheSize");
        } catch (error) {
        }
        return rs;
    }
    /**
     * 清除缓存
     */
    IOSApi.prototype.clearCache = function(){
        try {
            dsBridge.call("ios.clearCache");
        } catch (error) {
        }
    }
    /**
     * 打开h5实时画面
     * @param {String} url
     */
    IOSApi.prototype.openRealHtml = function(url){
        try {
            dsBridge.call("ios.startRealHtml",  url)
        } catch (error) {
        }
    }
    /**
     * 启动消息推送服务
     * @param {String} clientId
     * @param {String} stationId
     * @param {String} topics
     */
    IOSApi.prototype.startMessageSevice = function(clientId, stationId, topics){
        try {
            dsBridge.call("ios.setJPushTag ", stationId)
        } catch (error) {
            
        }
    }
    /**
     * 停止消息推送服务
     */
    IOSApi.prototype.stopMessageSevice = function(){
        try {
        } catch (error) {
                
        }
    }
    /**
     * 热成像仪
     */
    IOSApi.prototype.takeFlirPhone = function(){
        try {
        } catch (error) {
        }
    }

    /**
     * 拍照
     */
    IOSApi.prototype.takePhone = function(){
        try {
            dsBridge.call("ios.takePhoto")
        } catch (error) {
        }
    }

    /**
     * 录像
     */
    IOSApi.prototype.takeVideo = function(){
        try {
            dsBridge.call("ios.recordVideo")
        } catch (error) {
        }
    }

    /**
     * 检测网络是否可用
     */
    IOSApi.prototype.checkNetwork = function(){
        var rs = 0;
        try {
            rs = dsBridge.call("ios.checkNetwork");
        } catch (error) {
        }
        return rs;
    }

    IOSApi.prototype.download = function(url, fileName){
        try {
        } catch (error) {
        }
    }
    

    //跳转第三方导航
    IOSApi.prototype.startMapNavigation = function(params){
        try {
            dsBridge.call("ios.doNavigation", JSON.stringify(params))
        } catch (error) {
        }
    }
    //设置极光推送
    // params = {
    //     "tags":"stationId001",
    //     "jumpUrl":"https://www.sina.com.cn/"
    // }

    IOSApi.prototype.setJPushTagAndJumpUrl = function(params){
        try {
            dsBridge.call("ios.setJPushTagAndJumpUrl", JSON.stringify(params))
        } catch (error) {
        }
    }
    //清除极光推送
    IOSApi.prototype.cleanJPushTag = function(){
        try {
            dsBridge.call("ios.cleanJPushTag")
        } catch (error) {
        }
    }
    
    //极光、百度、个推三个平台统一调用接口
    /**
     * 设置消息推送标签和点击消息页面跳转地址
     *  params = {
            "pushPlatform":"jiguang"
            "tags":"user01",
            "jumpUrl":"https://www.sina.com.cn/"
        }
     */
    IOSApi.prototype.setMsgPushTagAndJumpUrl = function(params){
        try {
            var pushPlatform = params["pushPlatform"]
            if("jiguang" == pushPlatform){
                dsBridge.call("ios.setJPushTagAndJumpUrl", JSON.stringify(params))
            }else if("baidu" == pushPlatform){
                dsBridge.call("ios.setBaiduPushTagAndJumpUrl", JSON.stringify(params))
            }else if("getui" == pushPlatform){
                dsBridge.call("ios.setGTPushTagAndJumpUrl", JSON.stringify(params))
            }
        } catch (error) {
        }
    }
    //清除极光推送
    IOSApi.prototype.cleanMsgPushTag = function(pushPlatform){
        try {
            if("jiguang" == pushPlatform){
                dsBridge.call("ios.cleanJPushTag")
            }else if("baidu" == pushPlatform){
                dsBridge.call("ios.cleanBaiduPushTag")
            }else if("getui" == pushPlatform){
                //个推没有清除接口
            }
        } catch (error) {
        }
    }
    // 开启语音识别
    IOSApi.prototype.startSpeechRecognize = function(params){
        try {
            var speechPlatform = params["speechPlatform"]
            var useDefaultSetting = params["useDefaultSetting"]
            var emptyParam

//            if("ifly" == speechPlatform){
//                if("1" == useDefaultSetting){
//                    dsBridge.call("ios.IFlyStartRecord")
//                }else{
//                    dsBridge.call("ios.IFlyStartRecord", JSON.stringify(params))
//                }
//            }else if("baidu" == speechPlatform){
//                if("1" == useDefaultSetting){
//                    dsBridge.call("ios.BaiduStartRecord")
//                }else{
//                    dsBridge.call("ios.BaiduStartRecord", JSON.stringify(params))
//                }
//            }else if("tencent" == speechPlatform){
//                if("1" == useDefaultSetting){
//                    dsBridge.call("ios.TencentStartRecord")
//                }else{
//                    dsBridge.call("ios.TencentStartRecord", JSON.stringify(params))
//                }
//            }
            dsBridge.call("ios.SpeechRecStartTimeout60S", JSON.stringify(params))
        } catch (error) {
        }
    }
    //关闭语音识别
    IOSApi.prototype.stopSpeechRecognize = function(speechPlatform){
        try {
//            if("ifly" == speechPlatform){
//                dsBridge.call("ios.IFlyStopRecord")
//            }else if("baidu" == speechPlatform){
//                dsBridge.call("ios.BaiduStopRecord")
//            }else if("tencent" == speechPlatform){
//                dsBridge.call("ios.TencentStopRecord")
//            }
            dsBridge.call("ios.speechRecStopTimeout60S", speechPlatform)
        } catch (error) {
        }
    }
    
    return IOSApi;
});

/** ================================= **/
/** ========== IOS Api end ========== **/
/** ================================= **/


window.MOBILE_TYPE = '';


if(window.functionTag){
    window.MOBILE_TYPE = 'Android'
}else if(window.dsBridge && window.dsBridge.hasNativeMethod('ios.platformCheck')){
    window.MOBILE_TYPE = 'IOS'
}else{
    window.MOBILE_TYPE = 'Browser'
}


window.MOBILE_API = new MobileApi(MOBILE_TYPE)
