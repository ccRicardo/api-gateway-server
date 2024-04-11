package org.wyh.gateway.core.context;

import io.netty.channel.ChannelHandlerContext;
import org.wyh.gateway.common.config.Rule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.context
 * @Author: wyh
 * @Date: 2024-01-08 10:13
 * @Description: IContext上下文接口的基础实现类
 */
public class BasicContext implements IContext{
    //设置上下文的基础属性
    //转发协议
    protected final String protocol;
    //上下文的状态，默认为RUNNING状态
    protected volatile int status = IContext.RUNNING;
    //Netty ChannelHandler上下文
    protected final ChannelHandlerContext nettyCtx;
    //上下文参数集合
    protected final Map<String, Object> attributes = new HashMap<>();
    //异常
    protected Throwable throwable;
    //长连接标识
    protected final boolean keepAlive;
    //请求资源释放标识
    protected final AtomicBoolean requestReleased = new AtomicBoolean(false);
    //回调函数集合
    protected List<Consumer<IContext>> completedCallBacks;
    /**
     * @date: 2024-01-08 10:38
     * @description: 构造函数，初始化final修饰的属性
     * @Param protocol:
     * @Param nettyCtx:
     * @Param keepAlive:
     * @return: null
     */
    public BasicContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive) {
        this.protocol = protocol;
        this.nettyCtx = nettyCtx;
        this.keepAlive = keepAlive;
    }

    @Override
    public void setRunning() {
        this.status = IContext.RUNNING;
    }

    @Override
    public void setWritten() {
        this.status = IContext.WRITTEN;
    }

    @Override
    public void setCompleted() {
        this.status = IContext.COMPLETED;
    }

    @Override
    public void setTerminated() {
        this.status = IContext.TERMINATED;
    }

    @Override
    public boolean isRunning() {
        return this.status == IContext.RUNNING;
    }

    @Override
    public boolean isWritten() {
        return this.status == IContext.WRITTEN;
    }

    @Override
    public boolean isCompleted() {
        return this.status == IContext.COMPLETED;
    }

    @Override
    public boolean isTerminated() {
        return this.status == IContext.TERMINATED;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    //该类中并没有request，response和rule属性，因此这里的setter，getter方法都是空的。
    //真正的实现在子类GatewayContext中

    @Override
    public Object getRequest() {
        return null;
    }

    @Override
    public void setResponse(Object response) {

    }

    @Override
    public Object getResponse() {
        return null;
    }

    @Override
    public void setRule() {

    }

    @Override
    public Rule getRule() {
        return null;
    }

    @Override
    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public Throwable getThrowable() {
        return this.throwable;
    }

    @Override
    public void setAttribute(String key, Object obj) {
        this.attributes.put(key, obj);
    }

    @Override
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    @Override
    public ChannelHandlerContext getNettyCtx() {
        return this.nettyCtx;
    }

    @Override
    public boolean isKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public void releaseRequest() {
        //如果requestReleased为false，则置为true
        this.requestReleased.compareAndSet(false, true);
    }

    @Override
    public void setCompletedCallBack(Consumer<IContext> consumer) {
        if(this.completedCallBacks == null){
            completedCallBacks = new ArrayList<>();
        }
        completedCallBacks.add(consumer);
    }

    @Override
    public void invokeCompletedCallBack() {
        if(completedCallBacks != null){
            completedCallBacks.forEach(call->call.accept(this));
        }
    }
}
