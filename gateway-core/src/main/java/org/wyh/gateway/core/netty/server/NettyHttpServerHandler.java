package org.wyh.gateway.core.netty.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.wyh.gateway.core.netty.processor.NettyProcessor;
import org.wyh.gateway.core.request.HttpRequestWrapper;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.netty
 * @Author: wyh
 * @Date: 2024-01-16 15:28
 * @Description: 负责构造请求包装类对象，然后交给请求处理器类进行进一步处理。
 */
@Slf4j
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
    //请求处理器实例
    private final NettyProcessor nettyProcessor;
    /**
     * @date: 2024-01-16 15:42
     * @description: 有参构造器
     * @Param nettyProcessor:
     * @return: null
     */
    public NettyHttpServerHandler(NettyProcessor nettyProcessor){
        this.nettyProcessor = nettyProcessor;
    }
    @Trace
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //监听到Read事件，有数据可读时（即接收到客户端请求时），该方法会被调用
        //判断msg是否是http请求类型
        if(msg instanceof HttpRequest){
            //由于pipeline中有HttpObjectAggregator，所以msg实际上就是FullHttpRequest类型
            FullHttpRequest request = (FullHttpRequest) msg;
            HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
            httpRequestWrapper.setFullHttpRequest(request);
            httpRequestWrapper.setNettyCtx(ctx);
            //调用NettyProcessor对象的方法，完成后续的请求处理工作。
            nettyProcessor.process(httpRequestWrapper);
        }else{
            //实际上，基本不会进入该分支。
            log.error("消息类型不是HttpRequest: {}", msg);
            //释放msg对象
            boolean release = ReferenceCountUtil.release(msg);
            if(!release){
                log.error("消息资源释放失败");
            }
        }

    }
}
