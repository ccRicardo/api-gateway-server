package org.wyh.gateway.core.netty.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.utils.RemotingUtil;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.netty.LifeCycle;
import org.wyh.gateway.core.netty.processor.NettyProcessor;

import java.net.InetSocketAddress;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty
 * @Author: wyh
 * @Date: 2024-01-16 9:36
 * @Description: netty http服务端。
                 主要负责接收并解析客户端请求，然后交给请求处理器进行进一步处理，最后将结果返回给客户端。
 */
@Slf4j
public class NettyHttpServer implements LifeCycle {
    //网关的核心静态配置
    private final Config config;
    //请求处理类。
    private final NettyProcessor nettyProcessor;
    //netty服务器引导类（负责初始化netty服务器并监听端口）
    private ServerBootstrap serverBootstrap;
    //boss eventLoopGroup，负责监听和处理accept事件（客户端请求连接）
    private EventLoopGroup eventLoopGroupBoss;
    //worker eventLoopGroup，负责监听和处理read/write事件（客户端连接的I/O操作）
    @Getter
    private EventLoopGroup eventLoopGroupWorker;
    /**
     * @date: 2024-01-16 15:59
     * @description: 有参构造器
     * @Param config: 网关核心静态配置
     * @Param nettyProcessor:
     * @return: null
     */
    public NettyHttpServer(Config config, NettyProcessor nettyProcessor){
        this.config = config;
        this.nettyProcessor = nettyProcessor;
        //初始化
        init();
    }
    @Override
    public void init() {
        this.serverBootstrap = new ServerBootstrap();
        /*
         * netty中Selector的实现类主要分为NioSelector和EpollSelector
         * 前者基于Java NIO的Selector来实现I/O多路复用，可在所有支持Java NIO的系统中使用（Windows，Linux等）
         * 在Windows和旧版的Linux系统中，Java NIO Selector使用的是select或poll
         * 后者基于Linux的epoll来实现I/O多路复用，只能在Linux系统中使用
         * select，poll和epoll都是I/O多路复用技术（I/O事件通知机制），不过epoll效率最高
         * 因此，这里的优化就是判断能否使用epoll来提高性能
         */
        if(useEpoll()){
            //使用epoll相关的api，提高性能
            //DefaultThreadFactory是线程工厂，用于创建新的线程
            this.eventLoopGroupBoss = new EpollEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-server-boss"));
            this.eventLoopGroupWorker = new EpollEventLoopGroup((config.getEventLoopGroupWorkerNum()),
                    new DefaultThreadFactory("netty-server-worker"));
        }else{
            this.eventLoopGroupBoss = new NioEventLoopGroup(config.getEventLoopGroupBossNum(),
                    new DefaultThreadFactory("netty-server-boss"));
            this.eventLoopGroupWorker = new NioEventLoopGroup((config.getEventLoopGroupWorkerNum()),
                    new DefaultThreadFactory("netty-server-worker"));
        }

    }
    /**
     * @date: 2024-01-16 10:18
     * @description: 判断能否使用epoll
     * @return: boolean
     */
    private boolean useEpoll(){
        return RemotingUtil.isLinuxPlatform() && Epoll.isAvailable();
    }
    @Override
    public void start() {
        //初始化netty服务器端，并绑定端口监听请求
        this.serverBootstrap
                //设置两个EventLoopGroup，boss group和worker group。
                .group(eventLoopGroupBoss, eventLoopGroupWorker)
                //设置用于接入和处理客户端连接的Server Channel的类型
                .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                //设置服务器监听的端口
                .localAddress(new InetSocketAddress(config.getPort()))
                //设置每个已经接入的连接的channel pipeline。pipeline中包含了处理I/O操作的channelHandler。
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        /*
                         * HttpServerCodec是http编解码器，用于解析http GET请求
                         * 然而HttpServerCodec无法完全解析Http POST请求，
                         * 因为HttpServerCodec只能获取uri中参数，所以需要加上HttpObjectAggregator
                         *
                         * HttpObjectAggregator的主要作用是解析http POST请求，
                         * 并且可以将分块的消息体聚合成一个完整的请求或响应对象。
                         * （也就是FullHttpRequest和FullHttpResponse）
                         *
                         * NettyServerConnectManagerHandler是连接管理器，
                         * 负责定义连接对象在特定情况下的操作，主要是打印相应日志信息
                         *
                         * NettyHttpServerHandler负责构造请求包装类对象，并交给请求处理器进行处理
                         */
                        channel.pipeline().addLast(
                                new HttpServerCodec(),
                                new HttpObjectAggregator(config.getMaxContentLength()),
                                new NettyServerConnectManagerHandler(),
                                new NettyHttpServerHandler(nettyProcessor)

                        );
                    }
                });
        try{
            //bind()作用是启动netty服务器，并绑定指定端口
            //sync()作用是阻塞当前线程，直到netty服务器相关初始化操作完成
            this.serverBootstrap.bind().sync();
            log.info("NettyHttp服务端运行端口: {}", this.config.getPort());
        }catch (Exception e){
            throw new RuntimeException();
        }
    }

    @Override
    public void shutdown() {
        //优雅关机
        if(eventLoopGroupBoss != null){
            eventLoopGroupBoss.shutdownGracefully();
        }
        if(eventLoopGroupWorker != null){
            eventLoopGroupBoss.shutdownGracefully();
        }

    }
}
