package org.wyh.gateway.core.context;


import io.netty.channel.ChannelHandlerContext;
import org.wyh.gateway.common.config.Rule;

import java.util.function.Consumer;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.context
 * @Author: wyh
 * @Date: 2024-01-03 10:47
 * @Description: 网关中一次请求的上下文接口
 */
public interface IContext {
    //上下文的生命周期
    // TODO: 目前定义了四个状态，考虑再定义一个初始化状态。此外，可以考虑用内部枚举类来表示状态。
    //运行中
    int RUNNING = 0;
    //请求完成或出现异常，需写回响应
    int WRITTEN = 1;
    //写回完成
    int COMPLETED = 2;
    //请求结束
    int TERMINATED = -1;

    //设置上下文状态
    void setRunning();
    void setWritten();
    void setCompleted();
    void setTerminated();

    //判断上下文状态
    boolean isRunning();
    boolean isWritten();
    boolean isCompleted();
    boolean isTerminated();

    /**
     * @date: 2024-01-05 15:41
     * @description: 获取请求转发要用到的协议（目前只有http）
     * @return: java.lang.String
     */
    String getProtocol();
    /**
     * @date: 2024-01-05 15:41
     * @description: 获取请求对象
     * @return: java.lang.Object
     */
    Object getRequest();
    /**
     * @date: 2024-01-08 9:58
     * @description: 设置响应对象
     * @return: void
     */
    void setResponse(Object response);
    /**
     * @date: 2024-01-05 15:42
     * @description: 获取响应对象（请求结果）
     * @return: java.lang.Object
     */
    Object getResponse();
    /**
     * @date: 2024-01-08 9:55
     * @description: 获取过滤规则
     * @return: org.wyh.common.config.Rule
     */
    Rule getRule();
    /**
     * @date: 2024-01-08 10:00
     * @description: 设置异常信息
     * @Param throwable:
     * @return: void
     */
    void setThrowable(Throwable throwable);
    /**
     * @date: 2024-01-05 15:42
     * @description: 获取异常信息
     * @return: java.lang.Throwable
     */
    Throwable getThrowable();
    /**
     * @date: 2024-01-08 10:00
     * @description: 设置上下文参数
     * @Param key:
     * @Param obj:
     * @return: void
     */
    void setAttribute(String key, Object value);
    /**
     * @date: 2024-01-05 15:43
     * @description: 获取上下文参数
     * @Param key:
     * @return: java.lang.Object
     */
    Object getAttribute(String key);
    /**
     * @date: 2024-01-08 10:03
     * @description: 获取Netty ChannelHandler的上下文信息
     * @return: io.netty.channel.ChannelHandlerContext
     */
    ChannelHandlerContext getNettyCtx();
    /**
     * @date: 2024-01-08 10:08
     * @description: 判断是否为长连接
     * @return: boolean
     */
    boolean isKeepAlive();
    /**
     * @date: 2024-01-08 10:08
     * @description: 释放请求对象
     * @return: void
     */
    void releaseRequest();
    /**
     * @date: 2024-01-08 10:11
     * @description: 设置（写回完成后的）回调函数，完成后处理
     * @Param consumer:
     * @return: void
     */
    void setCompletedCallBack(Consumer<IContext> consumer);
    /**
     * @date: 2024-01-18 14:40
     * @description: 调用（写回完成后的）回调函数，完成后处理
     * @return: void
     */
    void invokeCompletedCallBack();


}
