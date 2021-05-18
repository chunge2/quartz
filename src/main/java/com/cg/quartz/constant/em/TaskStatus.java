package com.cg.quartz.constant.em;

import lombok.Getter;

import java.io.Serializable;

/**
 * 定时任务状态
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/26
 */
@Getter
public enum TaskStatus implements Serializable {

    /**
     * 启用
     */
    ENABLE(1, "启用"),
    /**
     * 关闭
     */
    DISABLE(0, "关闭");

    private int code;

    private String message;

    TaskStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 获取任务状态
     *
     * @param code 任务状态code
     * @return 任务状态(默认返回null)
     */
    public static TaskStatus getTaskStatus(int code) {
        if (ENABLE.getCode() == code) {
            return ENABLE;
        }
        return TaskStatus.DISABLE.getCode() == code ? TaskStatus.DISABLE: null;
    }

}
