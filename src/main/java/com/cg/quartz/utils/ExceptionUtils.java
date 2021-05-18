package com.cg.quartz.utils;

import com.cg.quartz.exception.ThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author chunge
 * @version 1.0
 * @date 2020/11/9
 */
public class ExceptionUtils {
    private static Logger logger = LoggerFactory.getLogger(ExceptionUtils.class);

    /**
     * 受检异常处理
     * <pre>
     *     可处理抛出Exception及子类
     * </pre>
     *
     * @param throwingConsumer throwingConsumer
     * @param <T>              T
     * @return consumer
     */
    public static <T> Consumer<T> handleCheckedException(ThrowingConsumer<T, Exception> throwingConsumer) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception e) {
                logger.error("[ExceptionUtils], execute catch a exception, caused by ==>", e);
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * 受检异常处理
     * <pre>
     *     可处理抛出Exception及子类
     *     默认抛出RuntimeException
     * </pre>
     *
     * @param throwingConsumer throwingConsumer
     * @param errorMessage     异常信息
     * @param <T>              T
     * @return consumer
     */
    public static <T> Consumer<T> handleCheckedException(ThrowingConsumer<T, Exception> throwingConsumer, String errorMessage) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception e) {
                throw new RuntimeException(errorMessage, e);
            }
        };
    }


    /**
     * 受检异常(自定义异常类型)
     *
     * @param throwingConsumer throwingConsumer
     * @param exceptionClass   期待异常类型
     * @param <T>              T
     * @return consumer
     */
    public static <T, E extends Exception> Consumer<T> handleCheckedExceptionClass(
            ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {
        return i -> {
            try {
                throwingConsumer.accept(i);
            } catch (Exception e) {
                try {
                    throw new RuntimeException(exceptionClass.cast(e));
                } catch (ClassCastException ex) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * 非受检异常
     *
     * @param consumer       throwingConsumer
     * @param exceptionClass 期待异常类型
     * @param <T>            T
     * @param <E>            E
     * @return consumer
     */
    public static <T, E extends Exception> Consumer<T> handleUncheckedException(Consumer<T> consumer, Class<E> exceptionClass) {
        return i -> {
            try {
                consumer.accept(i);
            } catch (Exception e) {
                try {
                    throw new RuntimeException(exceptionClass.cast(e));
                } catch (ClassCastException ex) {
                    throw e;
                }
            }
        };
    }

    /**
     * 获取异常栈信息
     *
     * @param t 异常信息
     * @return 异常栈信息
     */
    public static String getStackTrace(Throwable t) {
        return getExceptionStackTrace(t, System.lineSeparator());
    }

    /**
     * 获取异常栈信息
     *
     * @param t             异常信息
     * @param lineSeparator 换行分隔符(为null时使用操作系统分隔符)
     * @return 异常栈信息
     */
    public static String getStackTrace(Throwable t, String lineSeparator) {
        lineSeparator = ObjectUtils.isBlank(lineSeparator) ? System.lineSeparator() : lineSeparator;
        return getExceptionStackTrace(t, lineSeparator);
    }

    /**
     * 获取异常栈信息
     *
     * @param t 异常信息
     * @return 异常栈信息
     */
    private static String getExceptionStackTrace(Throwable t, String lineSeparator) {
        Assert.notNull(t, "Throwable is null");
        StringBuilder strBuilder = new StringBuilder(256);

        // 首次异常信息(最外层异常信息)
        strBuilder.append(t.toString()).append(lineSeparator);
        StackTraceElement[] stackTraces = t.getStackTrace();
        for (StackTraceElement stackTrace : stackTraces) {
            strBuilder.append("\tat ").append(stackTrace).append(lineSeparator);
        }
        Throwable cause = (t == t.getCause() ? null : t.getCause());
        if (ObjectUtils.notNull(cause)) {
            strBuilder.append(getFullStackTrace(t.getStackTrace(), t.getCause(), lineSeparator));
        }
        return strBuilder.toString();
    }

    /**
     * 递归获取完整异常栈
     *
     * @param curStackTrace 当前栈
     * @param cause         当前栈错误原因
     * @param lineSeparator 日志分隔符
     * @return 完整异常栈
     */
    private static String getFullStackTrace(StackTraceElement[] curStackTrace, Throwable cause, String lineSeparator) {
        StackTraceElement[] trace = cause.getStackTrace();
        int m = trace.length - 1;
        int n = curStackTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(curStackTrace[n])) {
            m--;
            n--;
        }
        int framesInCommon = trace.length - 1 - m;
        StringBuilder builder = new StringBuilder(32);
        builder.append("Caused by: ").append(cause.toString()).append(lineSeparator);
        for (int i = 0; i <= m; i++) {
            builder.append("\tat ").append(trace[i]).append(lineSeparator);
        }
        if (framesInCommon != 0) {
            builder.append("\t... ").append(framesInCommon).append(" more").append(lineSeparator);
        }

        // 递归调用获取所有异常栈信息
        Throwable ourCause = (cause == cause.getCause() ? null : cause.getCause());
        if (ourCause != null) {
            String stackTrace = getFullStackTrace(cause.getStackTrace(), cause, lineSeparator);
            builder.append(stackTrace);
        }
        return builder.toString();
    }

}