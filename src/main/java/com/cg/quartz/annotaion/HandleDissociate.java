package com.cg.quartz.annotaion;

import com.cg.quartz.constant.em.HandleDissociateEnum;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 处理游离任务
 * <pre>
 *     针对DB中不存在而内存中存在的任务, 将在内存中删除该任务(该情况可能由于DB删除任务信息但未同步内存任务)
 *     并不推荐直接操作任务表配置信息, 尤其是删除任务(建议所有任务信息变更通过后台操作)
 *     处于性能考虑, 处理游离任务模式是随机事件, 可配置每次都处理(配置项quartz.handle-dissociate)
 * </pre>
 *
 * @author chunges
 * @version 1.0
 * @date 2021/01/16
 */

@Retention(RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE)
@Documented
@Inherited
@Component
public @interface HandleDissociate {

    /**
     * 默认随机处理游离任务
     *
     * @return
     */
    HandleDissociateEnum value() default HandleDissociateEnum.RANDOM;
}