package com.base.bean;

import java.util.List;


/**
 * 用户信息
 */
public class User {
    private String id;

    private String name;

    private String phone;

    private String registTime;

    private String email;

    private String lastLonginTime;


    private List<Role> roleInfo;

    public User(String id) {
        this.id = id;
    }

    public User(String id, String name, List<Role> roleInfo) {
        this.id = id;
        this.name = name;
        this.roleInfo = roleInfo;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRegistTime() {
        return registTime;
    }

    public void setRegistTime(String registTime) {
        this.registTime = registTime;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLastLonginTime() {
        return lastLonginTime;
    }

    public void setLastLonginTime(String lastLonginTime) {
        this.lastLonginTime = lastLonginTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Role> getRoleInfo() {
        return roleInfo;
    }

    public void setRoleInfo(List<Role> roleInfo) {
        this.roleInfo = roleInfo;
    }
}