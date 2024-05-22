package org.wyh.gateway.core.netty.client;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.wyh.gateway.common.utils.RemotingUtil;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.netty.LifeCycle;
import org.wyh.gateway.core.helper.AsyncHttpHelper;

import java.io.IOException;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty
 * @Author: wyh
 * @Date: 2024-01-19 9:56
 * @Description: netty http客户端。主要作用是将请求转发给相应的后台服务。
                 由于项目底层使用了AsyncHttpClient来发送异步请求，
                 所以这个类本质上就是用来初始化和关闭AsyncHttpClient对象的。
                 （初始化后的AsyncHttpClient对象会传入
 */
@Slf4j
public class NettyHttpClient implements LifeCycle {
    //网关配置信息
    private final Config config;
    //该类维护的AsyncHttpClient对象，用于发送异步http请求
    private AsyncHttpClient asyncHttpClient;
    //提供AsyncHttpClient对象需要用到的eventLoopGroup
    private final EventLoopGroup eventLoopGroupWorker;
    /**
     * @date: 2024-01-19 10:10
     * @description: 有参构造器，需要提供一个worker EventLoopGroup实例
     * @Param config:
     * @Param eventLoopGroupWorker:
     * @return: null
     */
    public NettyHttpClient(Config config, EventLoopGroup eventLoopGroupWorker) {
        this.config = config;
        this.eventLoopGroupWorker = eventLoopGroupWorker;
        //初始化
        init();
    }
    /**
     * @date: 2024-03-27 10:04
     * @description: 有参构造器，无需提供worker EventLoopGroup实例
     * @Param config:
     * @return: null
     */
    public NettyHttpClient(Config config){
        this.config = config;
        //根据操作系统判断是否使用epoll。具体说明见NettyHttpServer中的相关注释
        if(RemotingUtil.isLinuxPlatform() && Epoll.isAvailable()){
            //使用epoll提高性能
            this.eventLoopGroupWorker = new EpollEventLoopGroup((config.getEventLoopGroupWorkerNum()),
                    new DefaultThreadFactory("netty-client-worker"));
        }else{
            this.eventLoopGroupWorker = new NioEventLoopGroup((config.getEventLoopGroupWorkerNum()),
                    new DefaultThreadFactory("netty-client-worker"));
        }
        //初始化
        init();
    }

    @Override
    public void init() {
        //DefaultAsyncHttpClientConfig使用了建造者模式，通过静态内部类Builder的build方法来创建对象
        DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder()
                //提供AsyncHttpClient对象需要用到的eventLoopGroup
                .setEventLoopGroup(eventLoopGroupWorker)
                //设置连接超时时间
                .setConnectTimeout(config.getHttpConnectTimeout())
                //设置请求超时时间
                .setRequestTimeout(config.getHttpRequestTimeout())
                //设置重定向的最大次数
                .setMaxRedirects(config.getHttpMaxRedirects())
                //设置ByteBuf分配器。这里使用了池化的ByteBuf分配器，用于提升性能。
                .setAllocator(PooledByteBufAllocator.DEFAULT)
                //设置是否强制执行HTTP压缩
                .setCompressionEnforced(true)
                //设置最大连接数
                .setMaxConnections(config.getHttpMaxConnections())
                //设置每台（远程）主机的最大连接数
                .setMaxConnectionsPerHost(config.getHttpConnectionsPerHost())
                //设置空闲连接超时时间
                .setPooledConnectionIdleTimeout(config.getHttpPooledConnectionIdleTimeout());
        //通过传入上述DefaultAsyncHttpClientConfig配置对象来创建相应的AsyncHttpClient对象
        this.asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
    }

    @Override
    public void start() {
        //将本类维护的AsyncHttpClient对象传入AsyncHttpHelper对象。
        AsyncHttpHelper.getInstance().initialized(asyncHttpClient);
    }

    @Override
    public void shutdown() {
        if (asyncHttpClient != null) {
            try {
                //释放AsyncHttpClient对象
                this.asyncHttpClient.close();
            } catch (IOException e) {
                log.error("NettyHttp客户端关闭异常", e);
            }
        }
    }
}
