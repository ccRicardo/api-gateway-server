package org.wyh.gateway.core.bufferqueue.api;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.bufferqueue
 * @Author: wyh
 * @Date: 2024-03-20 15:16
 * @Description: 面向多生产者多消费者的缓冲（区）队列接口。其中泛型E指的是事件中数据的类型。
                 在本网关系统中，事件对应网关接收到的请求，具体来说是生产者创建的HttpRequestWrapper对象
                 而生产者和消费者取决于缓冲队列的具体实现。
                 缓冲队列的主要作用是平衡生产者和消费者之间的速度差异，提高系统的抗压和承载能力，
                 以防在面对高并发场景时发生数据丢失，从而大大了提高系统的可用性。
 */
public interface BufferQueue<E> {
    /**
     * @date: 2024-03-21 9:38
     * @description: 添加一个事件。若失败则抛出异常，并调用监听器的异常处理方法。
     * @Param eventValue:
     * @return: void
     */
    void add(E eventValue);
    /**
     * @date: 2024-03-21 9:39
     * @description: 添加多个事件。若失败则抛出异常，并调用监听器的异常处理方法。
     * @Param eventValues:
     * @return: void
     */
    void add(E... eventValues);
    /**
     * @date: 2024-03-21 9:39
     * @description: 尝试添加一个事件，若失败则直接返回false。
     * @Param eventValue:
     * @return: boolean
     */
    boolean tryAdd(E eventValue);
    /**
     * @date: 2024-03-21 9:40
     * @description: 尝试添加多个事件，若失败则直接返回false。
     * @Param eventValues:
     * @return: boolean
     */
    boolean tryAdd(E... eventValues);
    /**
     * @date: 2024-03-21 9:40
     * @description: 启动缓冲队列
     * @return: void
     */
    void start();
    /**
     * @date: 2024-03-21 9:41
     * @description: 关闭缓冲队列，释放资源
     * @return: void
     */
    void shutDown();
    /**
     * @date: 2024-03-21 9:41
     * @description: 判断缓冲队列是否关闭
     * @return: boolean
     */
    boolean isShutDown();
}
