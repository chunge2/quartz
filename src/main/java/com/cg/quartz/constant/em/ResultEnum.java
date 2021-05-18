package com.cg.quartz.constant.em;

import com.cg.quartz.constant.ResultConstant;
import lombok.Getter;

/**
 * 结果信息枚举
 *
 * @author chunge
 * @version 1.0
 * @date 2020/9/12
 */
@Getter
public enum ResultEnum {
    /**
     * 响应成功
     */
    RESPONSE_SUCCESS(ResultConstant.RESPONSE_SUCCESS_CODE, "响应成功"),
    /**
     * 响应失败
     */
    RESPONSE_FAIL(ResultConstant.RESPONSE_FAIL_CODE, "响应失败"),
    /**
     * 系统繁忙
     */
    SYSTEM_BUSY(ResultConstant.SYSTEM_BUSY_CODE, "系统繁忙");

    /**
     * 响应码
     */
    private int code;
    /**
     * 响应消息
     */
    private String message;

    ResultEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据响应码获取响应消息结果枚举
     * <p>默认返回<b>响应失败</b>枚举</p>
     *
     * @param code 响应码
     * @return 响应消息结果枚举
     */
    public static ResultEnum getResponseResultEnum(int code) {
        for (ResultEnum resultEnum : ResultEnum.values()) {
            if (resultEnum.getCode() == code) {
                return resultEnum;
            }
        }
        return ResultEnum.RESPONSE_FAIL;
    }
}
