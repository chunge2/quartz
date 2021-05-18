package com.cg.quartz.utils;

/**
 * 随机数工具类
 *
 * @author chunge
 * @version 1.0
 * @date 2021/1/16
 */
public class RandomUtils {

    /**
     * 随机概率(类似扔硬币正反面概率)
     * <p>默认范围从1~100向2取模概率</p>
     *
     * @return 随机概率
     */
    public static boolean getRandomProbability() {
        return getRandomProbability(1, 100, 2);
    }

    /**
     * 随机概率(类似扔硬币正反面概率)
     * <p>eg: getRandomProbability(1, 100) 产生1~100之间的能被2取模随机概率</p>
     *
     * @param begin     开始范围
     * @param end       结束范围
     * @param modNumber 取模数
     * @return 随机概率
     */
    public static boolean getRandomProbability(int begin, int end, int modNumber) {
        int randomNumber = (int) (Math.random() * end + begin);
        return randomNumber % modNumber == 1;
    }

    /**
     * 生成随机数
     * <p>length, min, max 必须非零,否则返回-1</p>
     *
     * @param length 随机数长度(位数)
     * @param min    单次生成随机数最小值(0)
     * @param max    单次生成随机数最大值(10)
     * @return 随机数[0 ~ 9)
     */
    public static int getRandom(int length, int min, int max) {
        int maxLimit = 10;
        if (length <= 0 || min < 0 || min > max || max > maxLimit) {
            return -1;
        }
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(getRandom(min, max));
        }
        return Integer.parseInt(builder.toString());
    }

    /**
     * 生成随机数
     *
     * @param length 随机数长度(位数)
     * @return 随机数
     */
    public static int getRandom(int length) {
        return length == 1 ? getRandom(1, 10) : getRandom(length, 1, 10);
    }

    /**
     * 生成随机数
     *
     * @param min 单次生成随机数最小值
     * @param max 单次生成随机数最大值
     * @return 随机数
     */
    private static int getRandom(int min, int max) {
        return (int) (Math.random() * (max - min) + min);
    }

}