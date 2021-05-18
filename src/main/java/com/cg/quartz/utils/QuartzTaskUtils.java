package com.cg.quartz.utils;

import com.cg.quartz.constant.QuartzConstant;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

/**
 * 定时任务工具类
 * <pre>
 *   用于辅助构建@Task相关Job
 * </pre>
 *
 * @author chunges
 * @version 1.0
 * @date 2020/8/12
 */
public class QuartzTaskUtils {

    /**
     * 创建作业Bean
     *
     * @param targetBeanName 任务Bean名字(该bean必须纳入spring管理)
     * @return 作业bean
     */
    public static MethodInvokingJobDetailFactoryBean buildJobDetailFactoryBean(String targetBeanName) {
        return buildJobDetailFactoryBean(targetBeanName, QuartzConstant.DEFAULT_JOB_GROUP, QuartzConstant.EXECUTE_METHOD, false);
    }

    /**
     * 创建作业Bean
     * <p>注意：目标类默认不允许并发执行</p>
     *
     * @param targetBeanName 任务Bean名字(该bean必须纳入spring管理)
     * @param targetMethod   目标方法
     * @return 作业bean
     */
    public static MethodInvokingJobDetailFactoryBean buildJobDetailFactoryBean(String targetBeanName, String targetMethod) {
        return buildJobDetailFactoryBean(targetBeanName, QuartzConstant.DEFAULT_JOB_GROUP, targetMethod, false);
    }

    /**
     * 创建作业Bean
     *
     * @param targetBeanName  任务Bean名字(该bean必须纳入spring管理)
     * @param targetMethod    目标方法
     * @param allowConcurrent 是否允许并发执行(true:运行; false:禁止)
     * @return 作业bean
     */
    public static MethodInvokingJobDetailFactoryBean buildJobDetailFactoryBean(String targetBeanName, String targetMethod, boolean allowConcurrent) {
        return buildJobDetailFactoryBean(targetBeanName, QuartzConstant.DEFAULT_JOB_GROUP, targetMethod, allowConcurrent);
    }

    /**
     * 创建作业Bean
     *
     * @param targetBeanName  任务Bean名字(该bean必须纳入spring管理)
     * @param jobGroup        job group
     * @param targetMethod    目标方法
     * @param allowConcurrent 是否允许并发执行
     * @return 作业bean
     */
    public static MethodInvokingJobDetailFactoryBean buildJobDetailFactoryBean(String targetBeanName, String jobGroup, String targetMethod, boolean allowConcurrent) {
        MethodInvokingJobDetailFactoryBean jobDetailFactoryBean = new MethodInvokingJobDetailFactoryBean();

        // 设置需要执行的bean名字和方法名
        jobDetailFactoryBean.setTargetBeanName(targetBeanName);
        jobDetailFactoryBean.setTargetMethod(targetMethod);
        jobDetailFactoryBean.setConcurrent(allowConcurrent);

        // name > beanName = JobKey.name, group = JobKey.group
        jobDetailFactoryBean.setBeanName(targetBeanName);
        jobDetailFactoryBean.setGroup(jobGroup);
        return jobDetailFactoryBean;
    }
}