package com.campus.eventsys.enums;

import lombok.Getter;

/**
 * 活动参与者角色枚举
 */
@Getter
public enum ActivityRole {

    /**
     * 组织者/管理者：拥有修改活动信息的权限
     */
    ORGANIZER(1, "组织者"),

    /**
     * 普通参与者：可以参与活动并签到
     */
    PARTICIPANT(2, "参与者");

    private final int code;
    private final String desc;

    ActivityRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ActivityRole getByCode(int code) {
        for (ActivityRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return PARTICIPANT;
    }
}
