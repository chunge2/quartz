package com.cg.quartz.entity.po;

import com.cg.quartz.constant.em.TaskLogTemplateEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 日志BO
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/23
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class TaskLogBO {

    /**
     * 简单日志内容
     */
    private String content;

    /**
     * 日志格式
     */
    private String format;

    /**
     * 填充参数
     */
    private Object[] objects;

    /**
     * 异常信息
     */
    private Throwable throwable;

    /**
     * 日志模版枚举
     */
    private TaskLogTemplateEnum templateEnum;

    public TaskLogBO(String content, TaskLogTemplateEnum templateEnum) {
        this.content = content;
        this.templateEnum = templateEnum;
    }

    public TaskLogBO(String format, Object[] objects, TaskLogTemplateEnum templateEnum) {
        this.format = format;
        this.objects = objects;
        this.templateEnum = templateEnum;
    }

    public TaskLogBO(Throwable throwable, TaskLogTemplateEnum templateEnum) {
        this.throwable = throwable;
        this.templateEnum = templateEnum;
    }
}
