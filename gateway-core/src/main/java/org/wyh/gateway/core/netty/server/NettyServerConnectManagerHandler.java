package org.wyh.gateway.core.netty.server;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.utils.RemotingHelper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty
 * @Author: wyh
 * @Date: 2024-01-16 13:56
 * @Description: 连接管理器，作用是当channel中发生某些特殊事件时，打印相应的日志信息。
                 ChannelDuplexHandler是ChannelInboundHandler和ChannelOutboundHandler的结合，
                 它可以同时处理入站和出站数据/事件

 */
@Slf4j
public class NettyServerConnectManagerHandler extends ChannelDuplexHandler {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //当Channel注册到相应的EventLoop，并且能处理I/O时被调用
        //获取远程主机（客户端）的地址，并打印相应日志
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.info("NETTY SERVER PIPLINE: channelRegistered {}", remoteAddr);
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        //当Channel从相应的EventLoop中注销，并且无法处理任何I/O时被调用
        //获取远程主机（客户端）的地址，并打印相应日志
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.info("NETTY SERVER PIPLINE: channelUnregistered {}", remoteAddr);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //当Channel变为活动状态（建立连接）时被调用
        //获取远程主机（客户端）的地址，并打印相应日志
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.info("NETTY SERVER PIPLINE: channelActive {}", remoteAddr);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //当Channel变为非活动状态时（断开连接）被调用
        //获取远程主机（客户端）的地址，并打印相应日志
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.info("NETTY SERVER PIPLINE: channelInactive {}", remoteAddr);
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //用户事件被触发时（具体来说，是ChannelInboundHandler.fireUserEventTriggered()被调用时），会被调用
        //判断用户事件是否为IdleStateEvent类型，该类型事件主要用于检测远程连接是否存活/活跃
        if(evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;
            //判断事件状态是否为读写空闲（IdleState.ALL_IDLE），如果是，则关闭该空闲连接
            if(event.state().equals(IdleState.ALL_IDLE)){
                //获取远程主机（客户端）的地址，并打印相应日志
                final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
                log.warn("NETTY SERVER PIPLINE: userEventTriggered: IDLE {}", remoteAddr);
                //关闭channel
                ctx.channel().close();
            }
        }
        /*
         * 初看下面这句代码，可能会以为这是一个死循环，因为fireUserEventTriggered会触发userEventTriggered
         * 而userEventTriggered又会调用fireUserEventTriggered，然后无限重复上述过程
         * 但实际上userEventTriggered方法内是有退出条件的，即：
         * evt instanceof IdleStateEvent并且event.state().equals(IdleState.ALL_IDLE
         * 然后channel会被关闭，所有Handler被移除，不会再触发userEventTriggered方法
         * 因此，这样写实际上是在不断地触发用户事件，然后判断连接是否空闲，一旦空闲则释放
         * 但是这种轮询会不会降低系统的性能呢？这点目前还不清楚。
         */
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //当ChannelHandler在处理过程中出现异常时被调用
        //获取远程主机（客户端）的地址，并打印相应日志
        final String remoteAddr = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
        log.warn("NETTY SERVER PIPLINE: remoteAddr: {}, exceptionCaught {}", remoteAddr, cause);
        //关闭channel
        ctx.channel().close();
    }
}
