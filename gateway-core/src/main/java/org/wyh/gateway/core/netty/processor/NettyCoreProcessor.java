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
import org.wyh.gateway.common.exception.ConnectException;
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
    @Override
    public void process(HttpRequestWrapper requestWrapper) {
        ChannelHandlerContext nettyCtx = requestWrapper.getNettyCtx();
        FullHttpRequest fullRequest = requestWrapper.getFullHttpRequest();
        try {
            //解析请求对象，构建该请求在网关中的上下文对象（GatewayContext对象）
            GatewayContext gatewayContext = RequestHelper.doContext(fullRequest, nettyCtx);
            //执行（正常情况的）过滤器链，对网关上下文进行过滤处理，最终通过路由过滤器发送请求和接收响应。
            filterChainFactory.doFilterChain(gatewayContext);
            /*
             * 注意：这里只能捕获到路由过滤器异步发送请求之前出现的异常
             * 因为上述过滤器链实际上是以异步发送请求为分界点，分为两段执行的：
             * 前段在主线程中执行，执行完毕后，该处理器类的执行也就结束了，所以捕获不到后端抛出的异常。
             * 只有当AsyncHttpClient接收到响应结果，相应工作线程中的complete方法被调用时，才会开始执行后段
             */
            // TODO: 2024-05-24 此处异常捕获需要完善
        }catch (ConnectException ce){
            log.error("");
        }
        catch (Throwable t) {
            log.error("网关内部出现未知异常", t);
            //写回响应。异常类型为网关内部错误。
            ResponseHelper.writeResponse(requestWrapper, ResponseCode.INTERNAL_ERROR);
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
