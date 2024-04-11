package org.wyh.gateway.core.request;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Data;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.request
 * @Author: wyh
 * @Date: 2024-01-16 15:50
 * @Description: Http请求包装类，封装了http请求的相关信息
 */
@Data
public class HttpRequestWrapper {
    //完整的http请求对象
    private FullHttpRequest fullHttpRequest;
    //netty channelHandler上下文
    private ChannelHandlerContext nettyCtx;
}
