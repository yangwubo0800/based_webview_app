package com.base.bean;

/**
 * 角色
 */
public class Role {
    private String RoleID;

    private String RoleField;

    private String RoleName;

    private String ParentID;

    public Role(String roleID, String roleField, String roleName, String parentID) {
        RoleID = roleID;
        RoleField = roleField;
        RoleName = roleName;
        ParentID = parentID;
    }

    public String getRoleID() {
        return RoleID;
    }

    public void setRoleID(String roleID) {
        RoleID = roleID;
    }

    public String getRoleField() {
        return RoleField;
    }

    public void setRoleField(String roleField) {
        RoleField = roleField;
    }

    public String getRoleName() {
        return RoleName;
    }

    public void setRoleName(String roleName) {
        RoleName = roleName;
    }

    public String getParentID() {
        return ParentID;
    }

    public void setParentID(String parentID) {
        ParentID = parentID;
    }
}