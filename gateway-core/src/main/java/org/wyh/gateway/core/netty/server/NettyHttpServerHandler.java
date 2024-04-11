package org.wyh.gateway.core.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.wyh.gateway.core.netty.processor.NettyProcessor;
import org.wyh.gateway.core.request.HttpRequestWrapper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty
 * @Author: wyh
 * @Date: 2024-01-16 15:28
 * @Description: 负责构造请求包装类对象，然后交给请求处理器类进行进一步处理。
 */
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
    //请求处理器实例
    private final NettyProcessor nettyProcessor;
    /**
     * @date: 2024-01-16 15:42
     * @description: 有参构造器
     * @Param nettyProcessor:
     * @return: null
     */
    public NettyHttpServerHandler(NettyProcessor nettyProcessor){
        this.nettyProcessor = nettyProcessor;
    }
    @Trace
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //监听到Read事件，有数据可读时（即接收到客户端请求时），该方法会被调用
        //pipeline中有HttpObjectAggregator，所以可以强转为FullHttpRequest类型
        FullHttpRequest request = (FullHttpRequest) msg;
        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setFullHttpRequest(request);
        httpRequestWrapper.setNettyCtx(ctx);
        //调用NettyProcessor对象的方法，完成后续的请求处理工作。
        nettyProcessor.process(httpRequestWrapper);
    }
}
