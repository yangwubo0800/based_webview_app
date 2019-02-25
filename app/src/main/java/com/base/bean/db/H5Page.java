package com.hnac.bean.db;

import java.util.Date;

public class H5Page {
    private Integer id;

    private Integer groupId;

    private String name;

    private String alias;

    private String type;

    private Integer sortNo;

    private Date createTime;

    private Integer creator;

    private Date lastUpdateTime;

    private Integer lastModifier;

    //group name need to be show
    private String groupName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias == null ? null : alias.trim();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Integer getCreator() {
        return creator;
    }

    public void setCreator(Integer creator) {
        this.creator = creator;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public Integer getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(Integer lastModifier) {
        this.lastModifier = lastModifier;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName == null ? null : groupName.trim();
    }

    @Override
    public String toString() {
        return "H5Page [id=" + id + ", groupId=" + groupId + ", name=" + name + ", alias=" + alias + ", type=" + type
                + ", sortNo=" + sortNo + ", createTime=" + createTime + ", creator=" + creator + ", lastUpdateTime="
                + lastUpdateTime + ", lastModifier=" + lastModifier + ", groupName=" + groupName + "]";
    }


}
