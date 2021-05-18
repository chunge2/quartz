package com.cg.quartz.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * get a spring container context
 *
 * @author chunges
 * @version 1.0
 * @date 2020/8/12
 */
@Component
@Lazy(false)
public class SpringContextUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringContextUtils.applicationContext == null) {
            SpringContextUtils.applicationContext = applicationContext;
        }
    }

    /**
     * Get Spring ApplicationContext
     *
     * @return spring applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * get one bean by bean name
     *
     * @param beanName beanName
     * @return bean instance
     */
    public static Object getBean(String beanName) {
        return getApplicationContext().getBean(beanName);
    }

    /**
     * get one bean by bean type
     *
     * @param beanType beanType
     * @param <T>      bean type
     * @return bean instance
     */
    public static <T> T getBean(Class<T> beanType) {
        return getApplicationContext().getBean(beanType);
    }

    /**
     * get one bean by bean name and bean type
     *
     * @param beanName beanName
     * @param beanType bean class
     * @param <T>      bean type
     * @return bean instance
     */
    public static <T> T getBean(String beanName, Class<T> beanType) {
        return getApplicationContext().getBean(beanName, beanType);
    }

    /**
     * get all beans by bean type
     *
     * @param beanType bean type
     * @param <T>      bean type
     * @return bean instance in a Map<beanName, beanInstance>
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> beanType) {
        return getApplicationContext().getBeansOfType(beanType);
    }

    /**
     * get all beans by annotation type
     *
     * @param annotationType annotation type
     * @return bean instance in a Map<beanName, beanInstance>
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return applicationContext.getBeansWithAnnotation(annotationType);
    }

}
