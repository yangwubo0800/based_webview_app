package com.base.utils.http;

/**
 * <参数类>
 *
 * @author XXX
 * @version [版本号, 2016/6/8]
 * @see [相关类/方法]
 * @since [V1]
 */
public class Param {
    public Param() {
    }

    public Param(String key, String value) {
        this.key = key;
        this.value = value != null ? value : "";
    }

    public Param(String key, int value) {
        this.key = key;
        this.value = value + "";
    }

    String key;

    String value;
}
