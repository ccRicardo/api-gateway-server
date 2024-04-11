package org.wyh.gateway.core.helper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.core.context.IContext;
import org.wyh.gateway.core.response.GatewayResponse;

import java.util.Objects;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.helper
 * @Author: wyh
 * @Date: 2024-01-17 9:47
 * @Description: 处理响应对象的辅助类
 */
public class ResponseHelper {
    /**
     * @date: 2024-01-18 10:33
     * @description: 请求失败时调用（准确来说，是接收异步请求的响应之前出现了异常）。
                     根据ResponseCode对象，构建FullHttpResponse响应对象
     * @Param responseCode:
     * @return: io.netty.handler.codec.http.FullHttpResponse
     */
    public static FullHttpResponse getHttpResponse(ResponseCode responseCode){
        GatewayResponse gatewayResponse = GatewayResponse.buildGatewayResponse(responseCode);
        //Unpooled.wrappedBuffer的作用是创建相应的ByteBuf对象
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes()));
        //设置响应头内容
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        return httpResponse;
    }
    /**
     * @date: 2024-01-18 10:34
     * @description: 请求成功时调用（准确来说，是接收异步请求的响应之前不出现异常）。
                     根据上下文对象和GatewayResponse对象，构建FullHttpResponse响应对象
     * @Param ctx:
     * @Param gatewayResponse:
     * @return: io.netty.handler.codec.http.FullHttpResponse
     */
    private static FullHttpResponse getHttpResponse(IContext ctx, GatewayResponse gatewayResponse){
        ByteBuf content;
        /*
         * 优先从futureResponse属性中获取响应体
         * 如果该属性为空，再从content属性中获取
         * 若该属性也为空，则说明响应体内容为空
         */
        if(Objects.nonNull(gatewayResponse.getFutureResponse())) {
            //Unpooled.wrappedBuffer的作用是创建相应的ByteBuf对象
            content = Unpooled.wrappedBuffer(gatewayResponse.getFutureResponse()
                    .getResponseBodyAsByteBuffer());
        }
        else if(gatewayResponse.getContent() != null) {
            content = Unpooled.wrappedBuffer(gatewayResponse.getContent().getBytes());
        }
        else {
            content = Unpooled.wrappedBuffer(BasicConst.BLANK_SEPARATOR_1.getBytes());
        }
        /*
         * 先判断futureResponse属性是否为空
         * 若不为空，则通过该异步响应对象来构建响应对象
         * 具体来说，就是使用org.asynchttpclient.Response的api来获取响应状态码，响应头信息
         * 否则通过GatewayResponse对象构建响应对象，使用GatewayResponse的api来获取相关信息
         */
        if(Objects.isNull(gatewayResponse.getFutureResponse())) {
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    gatewayResponse.getHttpResponseStatus(),
                    content);
            //设置标准响应头信息
            httpResponse.headers().add(gatewayResponse.getResponseHeaders());
            //设置额外响应头信息
            httpResponse.headers().add(gatewayResponse.getExtraResponseHeaders());
            // TODO: 2024-01-18 好奇这里为什么没有设置CONTENT_TYPE字段，是因为在其他环节已经设置了吗
            httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            return httpResponse;
        } else {
            //设置额外响应头信息
            gatewayResponse.getFutureResponse().getHeaders().add(gatewayResponse.getExtraResponseHeaders());
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(gatewayResponse.getFutureResponse().getStatusCode()),
                    content);
            //设置标准响应头信息
            httpResponse.headers().add(gatewayResponse.getFutureResponse().getHeaders());
            return httpResponse;
        }
    }
    /**
     * @date: 2024-01-18 10:34
     * @description: 请求成功时调用（准确来说，是接收异步请求的响应之前不出现异常）。向客户端写回响应信息
     * @Param ctx:
     * @return: void
     */
    public static void writeResponse(IContext ctx){
        //释放请求对象
        ctx.releaseRequest();
        //判断上下文状态是否是需写回
        if(ctx.isWritten()) {
            //构建响应对象，并写回数据（注意，这里是调用的是请求成功情况下的响应构建方法）
            FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ctx, (GatewayResponse)ctx.getResponse());
            //判断是否为长连接
            //如果不是长连接，则添加一个监听器，当write和flush操作完成后，关闭该连接
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
        } else if(ctx.isCompleted()){
            //如果上下文状态已经为写回完成，则调用回调函数，完成相关的后处理
            ctx.invokeCompletedCallBack();
        }
    }
}
