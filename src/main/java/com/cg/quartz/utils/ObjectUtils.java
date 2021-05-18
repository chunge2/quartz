package com.cg.quartz.utils;

import com.cg.quartz.constant.QuartzConstant;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 对象工具类
 *
 * @author chunge
 * @version 1.0
 * @date 2020/8/26
 */
public class ObjectUtils {

    /**
     * 判断对象是否为空
     *
     * @param object obj
     * @return true:空对象; false:非空对象
     */
    public static <T> boolean isNull(T object) {
        return object == null;
    }

    /**
     * 判断对象是否为非空
     *
     * @param object obj
     * @return true:非空对象; false:空对象
     */
    public static <T> boolean notNull(T object) {
        return object != null;
    }

    /**
     * 判断字符串为空
     *
     * @param str 原字符串
     * @return true 字符串为null或"", false字符串非null(非"")
     */
    public static boolean isBlank(String str) {
        return str == null || "".equals(str);
    }

    /**
     * 判断字符串非空
     *
     * @param str 原字符串
     * @return true 字符串非null(非""), false 字符串为null或""
     */
    public static boolean notBlank(String str) {
        return str != null && !"".equals(str);
    }

    /**
     * 字符串首字母小写
     *
     * @param str 原字符串
     * @return 首字母小写字符串
     */
    public static String lowerFirst(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 判断数组是否为空
     *
     * @param objects objects array
     * @return true:空数组; false:非空数组
     */
    public static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length < 1;
    }

    /**
     * 判断数组是否非空
     *
     * @param objects objects array
     * @return true:非空数组; false:空数组
     */
    public static boolean notEmpty(Object[] objects) {
        return objects != null && objects.length > 0;
    }

    /**
     * 判断集合是否为空
     *
     * @param collection collection
     * @return true:空集合; false:非空集合
     */
    public static <E> boolean isEmpty(Collection<E> collection) {
        return collection == null || collection.size() < 1;
    }

    /**
     * 判断集合是否为非空
     *
     * @param collection collection
     * @return true:非空集合; false:空集合
     */
    public static <E> boolean notEmpty(Collection<E> collection) {
        return collection != null && collection.size() > 0;
    }

    /**
     * 判断map是否为空
     *
     * @param map map
     * @return true:空Map; false:非空Map
     */
    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.size() < 1;
    }

    /**
     * 判断Map是否非空
     *
     * @param map map
     * @return true:非空Map; false:空Map
     */
    public static <K, V> boolean notEmpty(Map<K, V> map) {
        return map != null && map.size() > 0;
    }

    /**
     * List转换
     * <pre>
     *    内部依赖Spring BeanUtils#copyProperties
     *    List数量比较大时不推荐使用该方法转换, 可能导致转换过慢
     * </pre>
     *
     * @param source source list
     * @param target target list item class
     * @param <S> S
     * @param <T> T
     * @return 转换后List
     */
    public static <S, T> List<T> convertTo(Collection<S> source, Class<T> target) {
        Assert.isTrue(notNull(source), "source list is null");
        Assert.isTrue(notNull(target), "target is null");
        List<T> list = new ArrayList<>(source.size());
        try {
            for (S s : source) {
                T target0 = target.newInstance();
                BeanUtils.copyProperties(s, target0);
                list.add(target0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 反射创建对象
     *
     * @param className class name
     * @param beanType bean type
     * @param <T> T
     * @return class instance
     */
    public static <T> T createObject(String className, Class<T> beanType) {
        try {
            Object instance = Class.forName(className).newInstance();
            Assert.isTrue(beanType.isAssignableFrom(instance.getClass()), "the created instance not equal input bean type");
            return (T) instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取正在运行main方法的类名
     *
     * @return 正在运行main方法的类名(默认返回"")
     */
    public static String getMainClassName() {
        StackTraceElement[] stackTraceElements = new RuntimeException().getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            if (QuartzConstant.MAIN.equals(stackTraceElement.getMethodName())) {
                return stackTraceElement.getClassName();
            }
        }
        return QuartzConstant.EMPTY_STRING;
    }
}