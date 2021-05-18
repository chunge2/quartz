package com.cg.quartz.api.result;


import com.cg.quartz.constant.em.ResultEnum;
import com.cg.quartz.exception.QuartzException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * RPC 结果集
 *
 * @author chunge
 * @version 1.0
 * @date 2020/9/12
 */
@Setter
@Getter
@ToString
public class RpcResult<T> implements Serializable {
    /**
     * 结果响应码
     */
    private int code;
    /**
     * 结果信息
     */
    private String message;
    /**
     * 结果数据
     */
    private T data;

    /**
     * 构建结果信息
     */
    public RpcResult() {
    }

    /**
     * 构建结果信息
     * <p>默认结果响应码为<b>响应成功</b></p>
     *
     * @param message 结果响应消息
     * @param data    结果内容
     */
    public RpcResult(String message, T data) {
        this(ResultEnum.RESPONSE_SUCCESS.getCode(), message, data);
    }

    /**
     * 构建结果信息
     * <p>结果消息根据传入结果响应码获取, 默认<b>响应失败</b></p>
     *
     * @param code 结果响应码
     * @param data 结果内容
     */
    public RpcResult(int code, T data) {
        this(code, ResultEnum.RESPONSE_SUCCESS.getMessage(), data);
    }

    /**
     * 构建响应结果信息
     *
     * @param code    结果响应码
     * @param message 结果消息
     * @param data    结果内容
     */
    public RpcResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构建成功结果
     *
     * @param data 结果数据
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildSuccessResp(T data) {
        return new RpcResult<>(ResultEnum.RESPONSE_SUCCESS.getCode(), ResultEnum.RESPONSE_SUCCESS.getMessage(), data);
    }

    /**
     * 构建成功结果
     *
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildSuccessResp(String message) {
        return new RpcResult<>(ResultEnum.RESPONSE_SUCCESS.getCode(), message, null);
    }

    /**
     * 构建成功结果
     *
     * @param data 结果数据
     * @param message 结果信息
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildSuccessResp(T data, String message) {
        return new RpcResult<>(ResultEnum.RESPONSE_SUCCESS.getCode(), message, data);
    }

    /**
     * 构建失败结果
     *
     * @param data 结果数据
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildFailResp(T data) {
        return new RpcResult<>(ResultEnum.RESPONSE_FAIL.getCode(), ResultEnum.RESPONSE_FAIL.getMessage(), data);
    }


    /**
     * 构建失败结果
     *
     * @param data 结果数据
     * @param errorMessage 错误信息
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildFailResp(T data, String errorMessage) {
        return new RpcResult<>(ResultEnum.RESPONSE_FAIL.getCode(),
                StringUtils.isEmpty(errorMessage) ? ResultEnum.RESPONSE_FAIL.getMessage() : errorMessage,
                data);
    }


    /**
     * 构建失败结果
     * <pre>如果是QuartzException将自行获取错误信息</pre>
     *
     * @param data 结果数据
     * @param e exception
     * @return 结果信息
     */
    public static <T>RpcResult<T> buildFailResp(T data, Exception e) {
        if (e instanceof QuartzException) {
            return buildFailResp(null, e.getMessage());
        }
        return new RpcResult<>(ResultEnum.RESPONSE_FAIL.getCode(), StringUtils.isEmpty(e.getMessage()) ? "Maybe NullPointerException" : e.getMessage(), data);
    }

}