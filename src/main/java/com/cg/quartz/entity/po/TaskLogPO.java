package com.cg.quartz.entity.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 定时任务日志PO
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/23
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
@TableName("t_quartz_task_log")
public class TaskLogPO {

    /**
     * pk
     */
    private Integer id;

    /**
     * 日志类型
     */
    private String type;

    /**
     * 日志线程
     */
    private String thread;

    /**
     * 类名
     */
    private String className;

    /**
     * 任务名
     */
    private String task;

    /**
     * 任务名
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public TaskLogPO(String type, String thread, String className, String task, String content) {
        this.type = type;
        this.thread = thread;
        this.className = className;
        this.task = task;
        this.content = content;
    }

    public TaskLogPO(String type, String thread, String className) {
        this.type = type;
        this.thread = thread;
        this.className = className;
    }
}