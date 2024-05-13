package org.wyh.gateway.core.helper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.config.ServiceDefinition;
import org.wyh.gateway.common.config.ServiceInvoker;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.constant.GatewayConst;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.NotFoundException;
import org.wyh.gateway.common.exception.PathNoMatchedException;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.common.utils.AntPathMatcher;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.request.GatewayRequest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.wyh.gateway.common.enumeration.ResponseCode.PATH_NO_MATCHED;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.helper
 * @Author: wyh
 * @Date: 2024-01-17 9:45
 * @Description: 处理请求对象的辅助类
 */
public class RequestHelper {
    //ANT路径规则匹配器
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    /**
     * @date: 2024-01-17 9:55
     * @description: 解析请求对象（FullHttpRequest对象），构建对应的上下文对象（GatewayContext对象）
     * @Param request:
     * @Param nettyCtx:
     * @return: org.wyh.core.context.GatewayContext
     */
    public static GatewayContext doContext(FullHttpRequest request, ChannelHandlerContext nettyCtx){
        //构建GatewayRequest请求对象
        GatewayRequest gatewayRequest = doRequest(request, nettyCtx);
        //根据请求对象里的uniqueId，从动态配置管理器中获取服务定义信息
        ServiceDefinition serviceDefinition =
                DynamicConfigManager.getInstance().getServiceDefinition(gatewayRequest.getUniqueId());
        //将服务定义中的ANT风格（匹配）规则与请求中的请求路径进行匹配，如果匹配失败，则直接抛出相应异常（这就是快速失败策略）
        if(!antPathMatcher.match(serviceDefinition.getPatternPath(), gatewayRequest.getPath())){
            throw new PathNoMatchedException(PATH_NO_MATCHED);
        }
        //根据请求对象中的路径信息，获取对应的方法调用对象
        ServiceInvoker serviceInvoker = getServiceInvoker(gatewayRequest, serviceDefinition);
        //根据方法调用对象中的ruleId，从动态配置管理器中获取对应的规则对象
        String ruleId = serviceInvoker.getRuleId();
        Rule rule = DynamicConfigManager.getInstance().getRule(ruleId);
        //构建该请求的GatewayContext上下文对象
        GatewayContext gatewayContext = new GatewayContext.Builder()
                .setProtocol(serviceDefinition.getProtocol())
                .setNettyCtx(nettyCtx)
                .setKeepAlive(HttpUtil.isKeepAlive(request))
                .setGatewayRequest(gatewayRequest)
                .setRule(rule)
                .build();
        return gatewayContext;
    }
    /**
     * @date: 2024-01-17 9:58
     * @description: 根据FullHttpRequest请求对象，构建内部的GatewayRequest请求对象
     * @Param request:
     * @Param nettyCtx:
     * @return: org.wyh.core.request.GatewayRequest
     */
    private static GatewayRequest doRequest(FullHttpRequest request, ChannelHandlerContext nettyCtx){
        /*
         * 以下一系列操作主要是在获取构建GatewayRequest对象需要的参数
         */
        HttpHeaders headers = request.headers();
        String uniqueId = headers.get(GatewayConst.UNIQUE_ID);
        //请求头中必须带有uniqueId属性
        if(StringUtils.isBlank(uniqueId)){
            throw new ResponseException(ResponseCode.REQUEST_PARSE_ERROR_NO_UNIQUEID);
        }
        String host = headers.get(HttpHeaderNames.HOST);
        HttpMethod method = request.method();
        String uri = request.uri();
        String clientIp = getClientIp(request, nettyCtx);
        //HttpUtil.getMimeType方法能够获取HttpMessage及其子类的contentType值
        String contentType = HttpUtil.getMimeType(request) == null ? null : HttpUtil.getMimeType(request).toString();
        Charset charset = HttpUtil.getCharset(request, StandardCharsets.UTF_8);

        //根据上述操作获得的参数值，构建GatewayRequest对象
        GatewayRequest gatewayRequest = new GatewayRequest(uniqueId, charset, clientIp,
                host, uri, method, contentType, headers, request);
        return gatewayRequest;
    }
    /**
     * @date: 2024-01-17 9:59
     * @description: 获取客户端ip
     * @Param request:
     * @Param nettyCtx:
     * @return: java.lang.String
     */
    private static String getClientIp(FullHttpRequest request, ChannelHandlerContext nettyCtx){
        /*
         * X-Forwarded-For(XFF)是一个http请求头字段，通过它可以获取以代理方式连接服务器的客户端的真实IP地址
         * 例如，如果一个http请求到达服务器之前，经过了三个代理Proxy1、Proxy2、Proxy3，IP分别为IP1、IP2、IP3，
         * 用户真实IP为IP0，那么该请求的X-Forwarded-For字段值应为: IP0, IP1, IP2
         */
        String xForwardedValue = request.headers().get(BasicConst.HTTP_FORWARD_SEPARATOR);
        String clientIp = null;
        //如果客户端通过代理方式连接服务器，那么就需要找到客户端的真实ip
        if(StringUtils.isNotEmpty(xForwardedValue)){
            List<String> values = Arrays.asList(xForwardedValue.split(", "));
            if(values.size() >= 1 && StringUtils.isNotBlank(values.get(0))){
                //第一个值就是客户端的原始ip地址
                clientIp = values.get(0);
            }
        }
        //如果客户端没使用代理，那么直接获取的ip就是其真实ip
        if(clientIp == null){
            InetSocketAddress inetSocketAddress = (InetSocketAddress)nettyCtx.channel().remoteAddress();
            clientIp = inetSocketAddress.getAddress().getHostAddress();
        }
        return clientIp;
    }
    /**
     * @date: 2024-05-13 16:03
     * @description: 根据请求对象中的路径信息，获取对应的方法调用对象
     * @Param request:
     * @Param serviceDefinition:
     * @return: org.wyh.gateway.common.config.ServiceInvoker
     */
    private static ServiceInvoker getServiceInvoker(GatewayRequest request,
                                                    ServiceDefinition serviceDefinition){
        Map<String, ServiceInvoker> invokerMap = serviceDefinition.getInvokerMap();
        //根据请求对象中的路径信息，获取对应的方法调用对象
        ServiceInvoker serviceInvoker = invokerMap.get(request.getPath());
        if(serviceInvoker == null){
            throw new NotFoundException(ResponseCode.SERVICE_INVOKER_NOT_FOUND);
        }
        return serviceInvoker;
    }
}
