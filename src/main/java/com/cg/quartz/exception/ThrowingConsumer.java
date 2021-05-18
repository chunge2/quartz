package com.cg.quartz.exception;

/**
 * @author chunge
 * @version 1.0
 * @date 2020/11/9
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws E exception
     */
    void accept(T t) throws E;
}