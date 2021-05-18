package com.cg.quartz.log;

import com.cg.quartz.annotaion.TaskLog;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.constant.em.TaskLogTemplateEnum;
import com.cg.quartz.entity.po.TaskLogBO;
import com.cg.quartz.entity.po.TaskLogPO;
import com.cg.quartz.service.TaskLogStoreService;
import com.cg.quartz.utils.Assert;
import com.cg.quartz.utils.ExceptionUtils;
import com.cg.quartz.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时任务日志Handler
 *
 * @author chunge
 * @version 1.0
 * @date 2020/12/22
 */
public class TaskLogHandler {

    private final Logger log = LoggerFactory.getLogger(TaskLogHandler.class);

    /**
     * 日志持久服务(非持久化模式为null)
     */
    private static TaskLogStoreService logStoreService;

    /**
     * 是否异步日志
     */
    private static boolean asyncLog;

    /**
     * 填充日志正则表达式
     */
    private Pattern pattern = Pattern.compile("\\{[^}]*\\}");

    /**
     * 日志分隔符
     */
    private final static String LOG_LINE_SEPARATOR = ";";

    private TaskLogHandler() {
    }

    public static void init(TaskLogStoreService logStoreService, boolean asyncLog) {
        TaskLogHandler.logStoreService = logStoreService;
        TaskLogHandler.asyncLog = asyncLog;
    }

    /**
     * 日志内部类(用于构造线程安全实例)
     */
    private static class TaskLoggerHolder {
        private final static TaskLogHandler INSTANCE = new TaskLogHandler();
    }

    public static TaskLogHandler getInstance() {
        return TaskLoggerHolder.INSTANCE;
    }

    /**
     * 获取外层调用者文件名(即调用本类 info, warn, error方法者)
     *
     * @return 外层调用者文件名
     */
    private String getInvokerClassName() {
        StackTraceElement[] stack = (new Throwable()).getStackTrace();
        StackTraceElement ste;
        String invokerFileName = null;

        // 0层调用:getInvokerFileName(), 1层调用: info()/warn()/error(), 2层调用: 外部调用者
        if (ObjectUtils.notEmpty(stack) && ObjectUtils.notNull(ste = stack[QuartzConstant.TWO])) {
            invokerFileName = ste.getClassName();
        }
        return ObjectUtils.notBlank(invokerFileName) ? invokerFileName : QuartzConstant.EMPTY_STRING;
    }

    /**
     * 获取简短任务名
     *
     * @param invokerClassName 调用类名
     * @return 简短任务名
     */
    private String getInvokerTask(String invokerClassName) {
        if (ObjectUtils.isBlank(invokerClassName)) {
            return QuartzConstant.EMPTY_STRING;
        }
        int index = invokerClassName.lastIndexOf('.');
        return invokerClassName.substring(index + 1);
    }

    /**
     * 日志持久化
     *
     * @param type 日志类型
     * @param className 任务名(全限定类名)
     * @param logBo 日志BO
     * @see com.cg.quartz.annotaion.TaskLog.LogLevel
     */
    private void storeLog(String type, String className, TaskLogBO logBo) {
        boolean canStoreLog = ObjectUtils.notNull(logStoreService);
        if (!canStoreLog) {
            return;
        }
        if (asyncLog) {
            CompletableFuture.runAsync(new TaskLogRunnable(new TaskLogPO(type, Thread.currentThread().getName(), className), logBo));
            return;
        }
        logStoreService.save(new TaskLogPO(type, Thread.currentThread().getName(), className, getInvokerTask(className), parseLogTemplate(logBo)));
    }

    /**
     * 解析日志
     *
     * @param logBo 日志BO
     * @return 解析日志模版内容
     */
    private String parseLogTemplate(TaskLogBO logBo) {
        try {
            Assert.notNull(logBo.getTemplateEnum(), "日志模版类型为空");
            if (TaskLogTemplateEnum.SIMPLE == logBo.getTemplateEnum()) {
                return logBo.getContent();
            }
            if (TaskLogTemplateEnum.EXCEPTION == logBo.getTemplateEnum()) {
                return QuartzConstant.ERROR_LOG_MARK_HEAD + ExceptionUtils.getStackTrace(logBo.getThrowable(), null);
            }

            // 遍历参数, 按顺序查找{}占位符, 替换为参数
            Object[] params = logBo.getObjects();
            String tempFormat = QuartzConstant.EMPTY_STRING;
            if (ObjectUtils.notEmpty(params)) {
                Matcher matcher = pattern.matcher(logBo.getFormat());
                for (Object param : params) {
                    matcher.reset(tempFormat = matcher.replaceFirst(String.valueOf(param)));
                }
            }
            return tempFormat;
        } catch (Exception e) {
            log.error("[quartz], parse log template catch a exception, caused by==>", e);
        }
        return QuartzConstant.ERROR_PARSE_LOG;
    }

    class TaskLogRunnable implements Runnable {
        private TaskLogPO logPo;
        private TaskLogBO logBo;

        private TaskLogRunnable(TaskLogPO logPo, TaskLogBO logBo) {
            this.logPo = logPo;
            this.logBo = logBo;
        }

        @Override
        public void run() {
            try {
                String content = parseLogTemplate(logBo);
                if (!QuartzConstant.ERROR_PARSE_LOG.equals(content)) {
                    logPo.setContent(content);
                    logPo.setTask(getInvokerTask(logPo.getClassName()));
                    logStoreService.save(logPo);
                }
            } catch (Exception e) {
                log.error("[quartz], save log to database catch a exception, caused by ==>", e);
            }
        }
    }

    /**
     * Log a message at the INFO level.
     *
     * @param msg the message string to be logged
     */
    public void info(String msg) {
        log.info(msg);
        storeLog(TaskLog.LogLevel.INFO.name(), getInvokerClassName(), new TaskLogBO(msg, TaskLogTemplateEnum.SIMPLE));
    }

    /**
     * Log a message at the INFO level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void info(String format, Object arg) {
        log.info(format, arg);
        storeLog(TaskLog.LogLevel.INFO.name(), getInvokerClassName(), new TaskLogBO(format, new Object[]{arg}, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log a message at the INFO level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the INFO level. </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void info(String format, Object arg1, Object arg2) {
        log.info(format, arg1, arg2);
        storeLog(TaskLog.LogLevel.INFO.name(), getInvokerClassName(), new TaskLogBO(format, new Object[]{arg1, arg2}, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log a message at the INFO level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the INFO level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for INFO. The variants taking
     * {@link #info(String, Object) one} and {@link #info(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void info(String format, Object... arguments) {
        log.info(format, arguments);
        storeLog(TaskLog.LogLevel.INFO.name(), getInvokerClassName(), new TaskLogBO(format, arguments, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log an exception (throwable) at the INFO level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void info(String msg, Throwable t) {
        log.info(msg, t);
        storeLog(TaskLog.LogLevel.INFO.name(), getInvokerClassName(), new TaskLogBO(t, TaskLogTemplateEnum.EXCEPTION));
    }

    /**
     * Log a message with the specific Marker at the INFO level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    public void info(Marker marker, String msg) {
        log.info(marker, msg);
    }

    /**
     * This method is similar to {@link #info(String, Object)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg    the argument
     */
    public void info(Marker marker, String format, Object arg) {
        log.info(marker, format, arg);
    }

    /**
     * This method is similar to {@link #info(String, Object, Object)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log.info(marker, format, arg1, arg2);
    }

    /**
     * This method is similar to {@link #info(String, Object...)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker    the marker data specific to this log statement
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void info(Marker marker, String format, Object... arguments) {
        log.info(marker, format, arguments);
    }

    /**
     * This method is similar to {@link #info(String, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    public void info(Marker marker, String msg, Throwable t) {
        log.info(marker, msg, t);
    }

    /**
     * Log a message at the WARN level.
     *
     * @param msg the message string to be logged
     */
    public void warn(String msg) {
        log.warn(msg);
        storeLog(TaskLog.LogLevel.WARN.name(), getInvokerClassName(), new TaskLogBO(msg, TaskLogTemplateEnum.SIMPLE));
    }

    /**
     * Log a message at the WARN level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void warn(String format, Object arg) {
        log.warn(format, arg);
        storeLog(TaskLog.LogLevel.WARN.name(), getInvokerClassName(), new TaskLogBO(format, new Object[]{arg}, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log a message at the WARN level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the WARN level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for WARN. The variants taking
     * {@link #warn(String, Object) one} and {@link #warn(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void warn(String format, Object... arguments) {
        log.warn(format, arguments);
        storeLog(TaskLog.LogLevel.WARN.name(), getInvokerClassName(), new TaskLogBO(format, arguments, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log a message at the WARN level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the WARN level. </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void warn(String format, Object arg1, Object arg2) {
        log.warn(format, arg1, arg2);
        storeLog(TaskLog.LogLevel.WARN.name(), getInvokerClassName(), new TaskLogBO(format, new Object[]{arg1, arg2}, TaskLogTemplateEnum.MORE));
    }

    /**
     * Log an exception (throwable) at the WARN level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void warn(String msg, Throwable t) {
        log.warn(msg, t);
        storeLog(TaskLog.LogLevel.WARN.name(), getInvokerClassName(), new TaskLogBO(t, TaskLogTemplateEnum.EXCEPTION));
    }

    /**
     * Log a message with the specific Marker at the WARN level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    public void warn(Marker marker, String msg) {
        log.warn(marker, msg);
    }

    /**
     * This method is similar to {@link #warn(String, Object)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg    the argument
     */
    public void warn(Marker marker, String format, Object arg) {
        log.warn(marker, format, arg);
    }

    /**
     * This method is similar to {@link #warn(String, Object, Object)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log.warn(marker, format, arg1, arg2);
    }

    /**
     * This method is similar to {@link #warn(String, Object...)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker    the marker data specific to this log statement
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void warn(Marker marker, String format, Object... arguments) {
        log.warn(marker, format, arguments);
    }

    /**
     * This method is similar to {@link #warn(String, Throwable)} method
     * except that the marker data is also taken into consideration.
     *
     * @param marker the marker data for this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    public void warn(Marker marker, String msg, Throwable t) {
        log.warn(marker, msg, t);
    }

    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     */
    public void error(String msg) {
        log.error(msg);
    }

    /**
     * Log a message at the ERROR level according to the specified format
     * and argument.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param format the format string
     * @param arg    the argument
     */
    public void error(String format, Object arg) {
        log.error(format, arg);
    }

    /**
     * Log a message at the ERROR level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous object creation when the logger
     * is disabled for the ERROR level. </p>
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void error(String format, Object arg1, Object arg2) {
        log.error(format, arg1, arg2);
    }

    /**
     * Log a message at the ERROR level according to the specified format
     * and arguments.
     * <p/>
     * <p>This form avoids superfluous string concatenation when the logger
     * is disabled for the ERROR level. However, this variant incurs the hidden
     * (and relatively small) cost of creating an <code>Object[]</code> before invoking the method,
     * even if this logger is disabled for ERROR. The variants taking
     * {@link #error(String, Object) one} and {@link #error(String, Object, Object) two}
     * arguments exist solely in order to avoid this hidden cost.</p>
     *
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void error(String format, Object... arguments) {
        log.error(format, arguments);
    }

    /**
     * Log an exception (throwable) at the ERROR level with an
     * accompanying message.
     *
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     */
    public void error(String msg, Throwable t) {
        log.error(msg, t);
        storeLog(TaskLog.LogLevel.ERROR.name(), getInvokerClassName(), new TaskLogBO(t, TaskLogTemplateEnum.EXCEPTION));
    }

    /**
     * Log a message with the specific Marker at the ERROR level.
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     */
    public void error(Marker marker, String msg) {
        log.error(marker, msg);
    }

    /**
     * This method is similar to {@link #error(String, Object)} method except that the
     * marker data is also taken into consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg    the argument
     */
    public void error(Marker marker, String format, Object arg) {
        log.error(marker, format, arg);
    }

    /**
     * This method is similar to {@link #error(String, Object, Object)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     */
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log.error(marker, format, arg1, arg2);
    }

    /**
     * This method is similar to {@link #error(String, Object...)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker    the marker data specific to this log statement
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     */
    public void error(Marker marker, String format, Object... arguments) {
        log.error(marker, format, arguments);
    }

    /**
     * This method is similar to {@link #error(String, Throwable)}
     * method except that the marker data is also taken into
     * consideration.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     */
    public void error(Marker marker, String msg, Throwable t) {
        log.error(marker, msg, t);
    }

    /**
     * only write to database, if enable store config
     *
     * @param msg log message
     * @param invoker invoker class name
     */
    public void onlyWrite(String msg, String invoker){
        storeLog(TaskLog.LogLevel.INFO.name(), invoker, new TaskLogBO(msg, TaskLogTemplateEnum.SIMPLE));
    }
}