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

window.Android = {
    //扫描二维码
    scanQRCode(type){
        window.functionTag.scanQRCode(type)
    },
    //获取定位方法
    location(type){
        window.functionTag.getLocationInfo(type)
    },
    deviceId(){
        window.functionTag.getDeviceId()
    },
    //告警消息服务
    alarm(options){
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
};
Android.__proto__ = EventEmitter.prototype;

Android.on("location", function(res){
    alert(res)
    Android.emit("locationRs", JSON.parse(res))
})
Android.on("qrCode", function(res){
    alert(res)
    Android.emit("qrCodeRs", JSON.parse(res))
})
Android.on("deviceId", function(res){
    alert(res)
    Android.emit("deviceIdRs", JSON.parse(res))
})
