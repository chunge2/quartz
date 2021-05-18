package com.cg.quartz.utils;

import com.cg.quartz.exception.QuartzException;

import java.util.Collection;
import java.util.Map;

/**
 * 异常工具类
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/18
 */
public class Assert {

    /**
     * 表达式非为FALSE时异常
     *
     * @param expression 表达式
     * @param message 异常信息
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new QuartzException(message);
        }
    }

    /**
     * 表达式非为TRUE时异常
     *
     * @param expression 表达式
     * @param message 异常信息
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new QuartzException(message);
        }
    }

    /**
     * 对象非空时异常
     *
     * @param object 判断对象
     * @param message 异常信息
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new QuartzException(message);
        }
    }

    /**
     * 对象为空时异常
     *
     * @param str 判断对象
     * @param message 异常信息
     */
    public static void notBlank(String str, String message) {
        if (ObjectUtils.isBlank(str)) {
            throw new QuartzException(message);
        }
    }

    /**
     * 对象为空时异常
     *
     * @param object 判断对象
     * @param message 异常信息
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new QuartzException(message);
        }
    }

    /**
     * 集合为空时异常
     *
     * @param collection 判断集合
     * @param message 异常信息
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (ObjectUtils.isEmpty(collection)) {
            throw new QuartzException(message);
        }
    }

    /**
     * Map为空时异常
     *
     * @param map 判断Map
     * @param message 异常信息
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (ObjectUtils.isEmpty(map)) {
            throw new QuartzException(message);
        }
    }

    /**
     * 数组为空时异常
     *
     * @param objects 判断数组
     * @param message 异常信息
     */
    public static void notEmpty(Object[] objects, String message) {
        if (ObjectUtils.isEmpty(objects)) {
            throw new QuartzException(message);
        }
    }


}
