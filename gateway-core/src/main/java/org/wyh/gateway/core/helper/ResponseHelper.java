package org.wyh.gateway.core.helper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Response;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.utils.AssertUtil;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.context.IContext;
import org.wyh.gateway.core.response.GatewayResponse;

import java.util.Objects;
import java.util.Optional;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.helper
 * @Author: wyh
 * @Date: 2024-01-17 9:47
 * @Description: 该辅助类主要负责构建FullHttpResponse响应对象，以及将FullHttpResponse响应写回客户端
                 注意：构建FullHttpResponse响应对象前，一般都要先构建GatewayResponse网关响应对象
 */
@Slf4j
public class ResponseHelper {
    //http协议的版本号
    private static final HttpVersion HTTP_VERSION = HttpVersion.HTTP_1_1;
    /**
     * @date: 2024-01-18 10:33
     * @description: 根据ResponseCode异常响应码对象，构建FullHttpResponse响应对象
     * @Param responseCode:
     * @return: io.netty.handler.codec.http.FullHttpResponse
     */
    private static FullHttpResponse getHttpResponse(ResponseCode responseCode){
        //先构建GatewayResponse网关响应对象
        GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(responseCode);
        //Unpooled.wrappedBuffer的作用是创建相应的ByteBuf对象
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_VERSION,
                responseCode.getStatus(),
                //创建一个包含响应内容的ByteBuffer对象
                Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes()));
        //设置响应头内容
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }
    /**
     * @date: 2024-01-18 10:34
     * @description: 根据GatewayResponse网关响应对象，构建对应的FullHttpResponse响应对象
     * @Param gatewayResponse:
     * @return: io.netty.handler.codec.http.FullHttpResponse
     */
    private static FullHttpResponse getHttpResponse(GatewayResponse gatewayResponse){
        ByteBuf content;
        /*
         * 优先从网关响应对象的futureResponse属性中获取响应体
         * 如果该属性为空，再从content属性中获取
         * 若该属性也为空，则说明响应体内容为空
         */
        if(Objects.nonNull(gatewayResponse.getFutureResponse())) {
            //Unpooled.wrappedBuffer的作用是创建包含指定内容的ByteBuf对象
            content = Unpooled.wrappedBuffer(gatewayResponse.getFutureResponse()
                    .getResponseBodyAsByteBuffer());
        } else if(gatewayResponse.getContent() != null) {
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes());
        } else {
            //响应体内容设为空
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes());
        }
        /*
         * 先判断futureResponse属性是否为空
         * 若为空，则通过GatewayResponse对象的其他属性来构建FullHttpResponse对象
         * 若不为空，则通过该属性（也就是AsyncHttpClient框架接收的原始响应对象）来构建。
         *
         */
        if(Objects.isNull(gatewayResponse.getFutureResponse())) {
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_VERSION,
                    gatewayResponse.getHttpResponseStatus(),
                    content);
            //将标准响应头信息添加到FullHttpResponse对象的响应头中
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            //将额外响应头信息添加到FullHttpResponse对象的响应头中
            httpResponse.headers().add(gatewayResponse.getExtraResponseHeaders());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8")
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            return httpResponse;
        } else {
            //AsyncHttpClient框架接收的原始响应对象
            Response futureResponse = gatewayResponse.getFutureResponse();
            //设置将额外响应头信息添加到org.asynchttpclient.Response对象的响应头中
            futureResponse.getHeaders().add(gatewayResponse.getExtraResponseHeaders());
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_VERSION,
                    HttpResponseStatus.valueOf(futureResponse.getStatusCode()),
                    content);
            //设置FullHttpResponse对象的响应头信息
            httpResponse.headers().add(futureResponse.getHeaders());
            return httpResponse;
        }
    }
    /**
     * @date: 2024-05-24 14:45
     * @description: 根据上下文和异常响应码（可选），向客户端写回FullHttpResponse响应对象
                     注意：异常响应码参数是可选的！
     * @Param ctx:
     * @Param codeOptional: 异常响应码，它是一个可选参数，默认为null
                             （Optional类的最佳应用场景就是作为方法的可选参数）
     * @return: void
     */
    public static void writeResponse(GatewayContext ctx, Optional<ResponseCode> codeOptional){
        try{
            //释放请求对象
            ctx.releaseRequest();
            //判断上下文状态是否是写回
            if(ctx.isWritten()) {
                FullHttpResponse httpResponse;
                //如果codeOptional可选参数存在，则优先使用异常响应码来构建FullHttpResponse响应对象
                if(codeOptional.isPresent()){
                    httpResponse = ResponseHelper.getHttpResponse(codeOptional.get());
                }else{
                    //如果可选参数不存在，则使用上下文中的网关响应对象，来构建对应的FullHttpResponse响应对象
                    AssertUtil.notNull(ctx.getResponse(), "上下文的网关响应对象为空");
                    httpResponse = ResponseHelper.getHttpResponse(ctx.getResponse());
                }
                /*
                 * 判断是否为长连接
                 * 如果不是长连接，则添加一个监听器，当write和flush操作完成后，自动关闭该连接
                 */
                if(!ctx.isKeepAlive()) {
                    ctx.getNettyCtx()
                            .writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
                }
                //如果是长连接，则在响应头中加上相应信息，并且不关闭连接
                else {
                    httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.getNettyCtx().writeAndFlush(httpResponse);
                }
                //将上下文状态设置为写回完成
                ctx.setCompleted();
                //调用回调函数，完成相关的后处理
                ctx.invokeCompletedCallBack();
                // TODO: 2024-05-24 这里的逻辑是否合理？
            } else {
                log.warn("请求: {} 的结果已写回！");
            }
        }catch (Exception e){
            log.error("写回失败！请求: {} 写回过程出现异常", e);
            // TODO: 2024-05-24 该异常属于网关内部异常，其实可以考虑返回一个网关异常响应
        }
    }
}
