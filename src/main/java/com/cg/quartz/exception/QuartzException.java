package com.cg.quartz.exception;

/**
 * 定时任务异常
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/18
 */
public class QuartzException extends RuntimeException {

    public QuartzException() {
    }

    public QuartzException(String message) {
        super(message);
    }

    public QuartzException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuartzException(Throwable cause) {
        super(cause);
    }
}