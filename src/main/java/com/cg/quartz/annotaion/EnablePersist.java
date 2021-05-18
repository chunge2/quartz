package com.cg.quartz.annotaion;

import java.lang.annotation.*;

/**
 *  是否启用持久化
 *  <pre>
 *      启用持久化需要配置相关表,该注解等同quartz.enable-persist
 *  </pre>
 * @author chunges
 * @version 1.0
 * @date 2020/8/14
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Documented
@Inherited
public @interface EnablePersist {

    /**
     * 是否加载未在数据库中配置的Job
     *<pre>
     *     注意该属性如果在配置文件中启用必须配合quartz.enable-persist使用, 否则无效
     *</pre>
     * @return true:加载未在数据库中配置的Job; false: 不加载未在数据库中配置的Job(默认)
     */
    boolean loadUnConfigurationJob() default false;
}