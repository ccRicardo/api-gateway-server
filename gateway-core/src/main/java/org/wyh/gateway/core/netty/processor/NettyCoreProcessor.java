package org.wyh.gateway.core.netty.processor;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.BaseException;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.chainfactory.GatewayFilterChainFactory;
import org.wyh.gateway.core.helper.RequestHelper;
import org.wyh.gateway.core.helper.ResponseHelper;
import org.wyh.gateway.core.request.HttpRequestWrapper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty.processor
 * @Author: wyh
 * @Date: 2024-01-17 9:24
 * @Description: 网关请求处理器的基本实现类（未使用缓冲队列）。
                 负责对netty server接收到的请求进行处理，并写回响应结果。
 */
@Slf4j
public class NettyCoreProcessor implements NettyProcessor{
    //过滤器链工厂类，用于生成过滤器链对象。
    private GatewayFilterChainFactory filterChainFactory;
    //静态配置信息
    private Config config;
    /**
     * @date: 2024-03-25 15:05
     * @description: 无参构造器，主要是为了调用init初始化方法
     * @return: null
     */
    public NettyCoreProcessor(Config config){
        this.config = config;
        init();
    }
    /**
     * @date: 2024-01-18 15:11
     * @description: 请求失败时调用（准确来说，是网关内部出现了异常，导致未能成功发送请求）。写回异常信息，并且释放连接。
     * @Param nettyCtx:
     * @Param fullRequest:
     * @Param fullResponse:
     * @return: void
     */
    private void doWriteAndRelease(ChannelHandlerContext nettyCtx, FullHttpRequest fullRequest,
                                   FullHttpResponse fullResponse){
        //写回响应，之后通过ChannelFutureListener.CLOSE关闭连接
        // TODO: 2024-05-24 这里是否需要判断长连接
        nettyCtx.writeAndFlush(fullResponse)
                .addListener(ChannelFutureListener.CLOSE);
        //将fullRequest对象的引用计数减1。如果该对象引用计数为0，则释放该对象。
        ReferenceCountUtil.release(fullRequest);
    }
    @Trace
    @Override
    public void process(HttpRequestWrapper requestWrapper) {
        ChannelHandlerContext nettyCtx = requestWrapper.getNettyCtx();
        FullHttpRequest fullRequest = requestWrapper.getFullHttpRequest();
        try {
            //解析请求对象，构建该请求在网关中的上下文对象（GatewayContext对象）
            GatewayContext gatewayContext = RequestHelper.doContext(fullRequest, nettyCtx);
            //执行（正常情况的）过滤器链，对网关上下文进行过滤处理，最终通过路由过滤器发送请求和接收响应。
            filterChainFactory.doFilterChain(gatewayContext);
            // TODO: 2024-05-24 此处异常捕获需要完善
        } catch (Throwable t) {
            /*
             * 这里负责捕获的是网关内部异常。
             * 网关发送请求前出现的异常都视为内部异常
             * 网关发送请求后，处理响应的工作线程中的异常都视为后台服务抛出的异常。
             * 后台服务异常在这里是捕获不到的，只有在路由过滤器的complete方法中才能捕获和处理。
             */
            log.error("网关内部出现未知异常", t);
            //构建FullHttpResponse响应对象
            FullHttpResponse fullResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
            //写回响应，并且释放请求对象。
            doWriteAndRelease(nettyCtx, fullRequest, fullResponse);
        }
    }
    @Override
    public void init() {
        //获取过滤器工厂类的唯一实例
        this.filterChainFactory = GatewayFilterChainFactory.getInstance();
    }

    @Override
    public void start() {
        //暂时用不到
    }

    @Override
    public void shutdown() {
        //暂时用不到
    }
}
