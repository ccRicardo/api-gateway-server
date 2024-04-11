package org.wyh.gateway.core.bufferqueue.api;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.bufferqueue
 * @Author: wyh
 * @Date: 2024-03-21 13:52
 * @Description: 缓冲队列的事件监听接口。其中泛型E指的是事件中数据的类型。
 */
public interface EventListener<E> {
    /**
     * @date: 2024-03-21 14:57
     * @description: 消费者线程从RingBuffer中获取到事件时被调用。
                     具体是被DisruptorBufferQueue.DisruptorWorkHandler中的onEvent方法调用
     * @Param eventData:
     * @return: void
     */
    void onEvent(E eventData);
    /**
     * @date: 2024-03-21 14:58
     * @description: 事件处理过程中出现异常时被调用。
                     具体是被DisruptorBufferQueue.DisruptorExceptionHandler中的handleEventException调用
     * @Param ex:
     * @Param sequence:
     * @Param eventData:
     * @return: void
     */
    void onException(Throwable ex, long sequence, E eventData);
}
