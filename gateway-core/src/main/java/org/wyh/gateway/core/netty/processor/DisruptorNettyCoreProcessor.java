package org.wyh.gateway.core.netty.processor;

import com.lmax.disruptor.dsl.ProducerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.bufferqueue.api.EventListener;
import org.wyh.gateway.core.bufferqueue.impl.DisruptorBufferQueue;
import org.wyh.gateway.core.helper.ResponseHelper;
import org.wyh.gateway.core.request.HttpRequestWrapper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty.processor
 * @Author: wyh
 * @Date: 2024-03-25 15:17
 * @Description: 带Disruptor缓冲队列的请求处理器。（本质上就是对基本实现类NettyCoreProcessor的包装）
                 该类的主要工作其实是对缓冲队列的生命周期进行管理，以及实现事件监听器。
                 而具体的请求处理逻辑则是交给NettyCoreProcessor来实现。
 todo 目前存在的一个问题是，消费者确实是多线程的，但是他们却共用一个NettyCoreProcessor对象
 todo 这会不会成为限制该系统吞吐量的瓶颈？
 */
@Slf4j
public class DisruptorNettyCoreProcessor implements NettyProcessor{
    //静态配置信息
    private Config config;
    //请求处理器
    private NettyCoreProcessor nettyCoreProcessor;
    //缓冲队列
    private DisruptorBufferQueue<HttpRequestWrapper> disruptorBufferQueue;
    /**
     * @date: 2024-03-26 9:22
     * @description: 有参构造器，配合init方法完成初始化工作
     * @Param config:
     * @Param nettyCoreProcessor:
     * @return: null
     */
    public DisruptorNettyCoreProcessor(Config config, NettyCoreProcessor nettyCoreProcessor){
        this.config = config;
        this.nettyCoreProcessor = nettyCoreProcessor;
        //调用init方法完成剩余属性初始化
        init();
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.netty.processor
     * @Author: wyh
     * @Date: 2024-03-26 09:44
     * @Description: 内部类，实现了EventListener接口。
                     负责对缓冲队列中的事件以及事件处理过程中发生的异常进行处理。
     */
    public class DisruptorEventListener implements EventListener<HttpRequestWrapper>{

        @Trace
        @Override
        public void onEvent(HttpRequestWrapper eventValue) {
            //调用NettyCoreProcessor（请求处理器）来真正处理请求事件
            nettyCoreProcessor.process(eventValue);
            // TODO 从以下两句测试代码可知，不同消费者线程共享的是同一个处理器对象。这会不会限制系统吞吐量？
            //System.out.println(Thread.currentThread().getName());
            //System.out.println(nettyCoreProcessor.hashCode());
        }

        @Override
        public void onException(Throwable ex, long sequence, HttpRequestWrapper eventValue) {
            FullHttpRequest httpRequest = eventValue.getFullHttpRequest();
            ChannelHandlerContext nettyCtx = eventValue.getNettyCtx();
            try{
                log.error("Disruptor缓冲队列处理过程中出现错误！请求：{}，错误信息：{}",
                        httpRequest, ex.getMessage(), ex);
                //构建响应对象
                FullHttpResponse httpResponse = ResponseHelper.getHttpResponse(ResponseCode.INTERNAL_ERROR);
                //判断是否为长连接
                if(!HttpUtil.isKeepAlive(httpRequest)){
                    //如果不是长连接，则在写回响应信息后，通过ChannelFutureListener.CLOSE关闭连接/channel
                    nettyCtx.writeAndFlush(httpResponse)
                            .addListener(ChannelFutureListener.CLOSE);
                }else{
                    //如果是长连接，则需要设置一下CONNECTION响应头
                    httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    nettyCtx.writeAndFlush(httpResponse);
                }
            }catch(Exception e){
                log.error("请求响应写回失败！请求：{}，错误信息：{}",
                        httpRequest, e.getMessage(), e);
            }


        }
    }
    @Trace
    @Override
    public void process(HttpRequestWrapper requestWrapper) {
        //将请求事件（的数据）添加到缓冲队列中
        disruptorBufferQueue.add(requestWrapper);
    }
    @Override
    public void init() {
        //创建事件监听器实例
        DisruptorEventListener eventListener = new DisruptorEventListener();
        //创建缓冲队列的建造者实例，设置相应的属性
        DisruptorBufferQueue.Builder<HttpRequestWrapper> builder =
                new DisruptorBufferQueue.Builder<HttpRequestWrapper>()
                        .setBufferSize(config.getBufferSize())
                        .setThreads(config.getThreadCount())
                        //生产者类型默认设置为多生产者，因为netty server的EventLoopGroup部分是多线程的
                        .setProducerType(ProducerType.MULTI)
                        .setNamePrefix(config.getThreadNamePrefix())
                        .setWaitStrategy(config.getTrueWaitStrategy())
                        .setListener(eventListener);
        //通过建造者实例创建缓冲队列实例
        this.disruptorBufferQueue = builder.build();
    }

    @Override
    public void start() {
        nettyCoreProcessor.start();
        //开启缓冲队列
        disruptorBufferQueue.start();
    }

    @Override
    public void shutdown() {
        nettyCoreProcessor.shutdown();
        if(!disruptorBufferQueue.isShutDown()){
            //关闭缓冲队列
            disruptorBufferQueue.shutDown();
        }
    }
}
