package com.campus.eventsys.utils;

/**
 * 活动相关状态名称工具类
 * 统一管理活动状态和报名状态的中文名称映射
 */
public class ActivityStatusUtil {

    private ActivityStatusUtil() {}

    /**
     * 获取活动状态名称
     * 状态码：-2-已下架, -1-已拒绝, 0-草稿, 1-待审核, 2-报名中, 3-即将开始, 4-进行中, 5-已结束
     */
    public static String getActivityStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case -2: return "已下架";
            case -1: return "已拒绝";
            case 0: return "草稿";
            case 1: return "待审核";
            case 2: return "报名中";
            case 3: return "即将开始";
            case 4: return "进行中";
            case 5: return "已结束";
            default: return "未知";
        }
    }

    /**
     * 获取报名状态名称
     * 状态码：0-待审核, 1-已通过, 2-已拒绝, 3-已签到, 4-已取消
     */
    public static String getRegistrationStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审核";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已签到";
            case 4: return "已取消";
            default: return "未知";
        }
    }
}
