package org.wyh.gateway.core.netty;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.netty.client.NettyHttpClient;
import org.wyh.gateway.core.netty.server.NettyHttpServer;
import org.wyh.gateway.core.netty.processor.DisruptorNettyCoreProcessor;
import org.wyh.gateway.core.netty.processor.NettyCoreProcessor;
import org.wyh.gateway.core.netty.processor.NettyProcessor;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core
 * @Author: wyh
 * @Date: 2024-01-19 14:32
 * @Description: 网关容器类，封装了网关系统的核心组件NettyHttpServer，NettyHttpClient，NettyCoreProcessor
                 主要负责核心组件对象的创建和管理。
 */
@Slf4j
public class Container implements LifeCycle{
    //网关的核心静态配置
    private final Config config;
    //网关的netty httpServer
    private NettyHttpServer nettyHttpServer;
    //网关的netty httpClient
    private NettyHttpClient nettyHttpClient;
    //网关的请求处理器
    private NettyProcessor nettyProcessor;
    /**
     * @date: 2024-01-19 14:39
     * @description: 有参构造器，负责初始化final属性
     * @Param config:
     * @return: null
     */
    public Container(Config config) {
        this.config = config;
        init();
    }

    @Override
    public void init() {
        //创建请求处理器的基本实现实例
        NettyCoreProcessor nettyCoreProcessor = new NettyCoreProcessor(config);
        //判断是否启用了缓冲队列
        if(config.isUseBufferQueue()){
            //使用带缓冲队列的请求处理器（本质上就是对基本实现类NettyCoreProcessor的包装）
            this.nettyProcessor = new DisruptorNettyCoreProcessor(config, nettyCoreProcessor);
        }else{
            //使用请求处理器的基本实现类
            this.nettyProcessor = nettyCoreProcessor;
        }
        nettyHttpServer = new NettyHttpServer(config, nettyProcessor);
//        //以下代码中，NettyHttpClient与NettyHttpServer共用了一个worker EventLoopGroup（理论上是应该是分开的）
//        nettyHttpClient = new NettyHttpClient(config,
//                nettyHttpServer.getEventLoopGroupWorker());
        //以下代码中，NettyHttpClient与NettyHttpServer都有各自的worker EventLoopGroup
        nettyHttpClient = new NettyHttpClient(config);
    }

    @Override
    public void start() {
        nettyHttpServer.start();
        nettyProcessor.start();
        nettyHttpClient.start();
        log.info("API网关容器启动");
    }

    @Override
    public void shutdown() {
        nettyHttpServer.shutdown();
        nettyProcessor.shutdown();
        nettyHttpClient.shutdown();
    }
}
