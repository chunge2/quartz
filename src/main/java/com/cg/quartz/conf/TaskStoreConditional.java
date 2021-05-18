package com.cg.quartz.conf;

import com.cg.quartz.annotaion.EnablePersist;
import com.cg.quartz.constant.QuartzConstant;
import com.cg.quartz.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 是否启用持久化以决定是否需加载TaskStoreService(最大程度简化非持久化模式下使用Quartz所依赖组件和配置)
 * <pre>
 *     背景: 非持久化模式下必须使用@MapperScan扫描TaskStoreDao所在包创建DAO才能启动, 而使用mapper扫描后必须引入数据库相关配置, 非持久化使用导入成本过高
 *     导入链路: TaskStoreDao -> @MapperScan -> sqlSessionFactory/sqlSessionTemplate(required) -> import database support component(pom) -> mybatis configuration
 *     解决: 根据启动Quartz模式来动态决定是否需要加载TaskStoreService(非持久化模式下不需要加载)
 * </pre>
 *
 * @author chunge
 * @version 1.0
 * @date 2020/11/25
 */
@Slf4j
public class TaskStoreConditional implements Condition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        // 检查配置文件是否启用持久化或是否持有启动类@EnablePersist
        String persistence = conditionContext.getEnvironment().getProperty(QuartzConstant.ENABLE_PERSIST_FLAG);
        return ObjectUtils.notBlank(persistence) ? Boolean.valueOf(persistence) : isEnablePersist();
    }

    /**
     * 启动类是否启用持久化
     *
     * @return true:持久化; false:未持久化
     */
    private boolean isEnablePersist() {
        try {
            String mainClassName = ObjectUtils.getMainClassName();
            if (ObjectUtils.notBlank(mainClassName)) {
                return ObjectUtils.createObject(mainClassName, Object.class).getClass().isAnnotationPresent(EnablePersist.class);
            }
        } catch (Exception e) {
            log.error("[quartz], check main class isAnnotationPresent @EnablePersist catch a exception, caused by ==>", e);
        }
        return false;
    }

}