package org.wyh.gateway.core.netty.processor;

import org.wyh.gateway.core.netty.LifeCycle;
import org.wyh.gateway.core.request.HttpRequestWrapper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty.processor
 * @Author: wyh
 * @Date: 2024-01-17 9:21
 * @Description: 网关的请求处理接口，负责对netty server接收到的请求进行处理，并写回响应结果
 */
public interface NettyProcessor extends LifeCycle {
    /**
     * @date: 2024-01-17 9:22
     * @description: 对请求进行处理，具体包括过滤器链处理和写回响应结果
     * @Param httpRequestWrapper:
     * @return: void
     */
    void process(HttpRequestWrapper requestWrapper);
}
