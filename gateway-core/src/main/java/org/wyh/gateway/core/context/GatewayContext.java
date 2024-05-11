package org.wyh.gateway.core.context;

import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.utils.AssertUtil;
import org.wyh.gateway.core.request.GatewayRequest;
import org.wyh.gateway.core.response.GatewayResponse;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.context
 * @Author: wyh
 * @Date: 2024-01-08 13:50
 * @Description: 网关中一次请求的上下文具体实现类
 */
public class GatewayContext extends BasicContext{
    //网关请求对象
    private final GatewayRequest gatewayRequest;
    //网关响应对象
    private GatewayResponse gatewayResponse;
    //规则对象
    private final Rule rule;
    //记录当前的请求重试次数
    @Setter
    @Getter
    private int currentRetryTimes;
    //标识该请求是否属于灰度流量（也就是请求的是否为灰度服务）
    @Setter
    @Getter
    private boolean gray = false;
    //用于统计方法的执行耗时（在本系统中则是统计请求在过滤器链中的处理时间），是监控过滤器的数据采集器。
    @Setter
    @Getter
    private Timer.Sample timerSample;


    /**
     * @date: 2024-01-11 15:11
     * @description: 有参构造器，初始化final修饰的属性
     * @Param protocol:
     * @Param nettyCtx:
     * @Param keepAlive:
     * @Param gatewayRequest:
     * @Param rule:
     * @return: null
     */
    public GatewayContext(String protocol, ChannelHandlerContext nettyCtx, boolean keepAlive,
                          GatewayRequest gatewayRequest, Rule rule, int currentRetryTimes) {
        super(protocol, nettyCtx, keepAlive);
        this.gatewayRequest = gatewayRequest;
        this.rule = rule;
        this.currentRetryTimes = currentRetryTimes;
    }
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.context
     * @Author: wyh
     * @Date: 2024-01-11 15:39
     * @Description: 建造者类，用于构建GatewayContext对象（实际上可以通过@Builder注解简化）
     */
    public static class Builder{
        //转发协议
        private String protocol;
        //netty ChannelHandler上下文
        private ChannelHandlerContext nettyCtx;
        //长连接标识
        private boolean keepAlive;
        //网关请求
        private GatewayRequest gatewayRequest;
        //规则
        private Rule rule;
        /**
         * @date: 2024-01-11 15:45
         * @description: 无参构造器
         * @return: null
         */
        public Builder(){

        };
        /**
         * @date: 2024-01-11 15:46
         * @description: 设置protocol属性。返回对象本身，因此可以使用链式编程。
         * @Param protocol:
         * @return: org.wyh.core.context.GatewayContext.Builder
         */
        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }
        /**
         * @date: 2024-01-11 15:48
         * @description: 设置nettyCtx属性。返回对象本身，因此可以使用链式编程。
         * @Param nettyCtx:
         * @return: org.wyh.core.context.GatewayContext.Builder
         */
        public Builder setNettyCtx(ChannelHandlerContext nettyCtx) {
            this.nettyCtx = nettyCtx;
            return this;
        }
        /**
         * @date: 2024-01-11 15:48
         * @description: 设置keepAlive属性。返回对象本身，因此可以使用链式编程。
         * @Param keepAlive:
         * @return: org.wyh.core.context.GatewayContext.Builder
         */
        public Builder setKeepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }
        /**
         * @date: 2024-01-11 15:49
         * @description: 设置gatewayRequest属性。返回对象本身，因此可以使用链式编程。
         * @Param gatewayRequest:
         * @return: org.wyh.core.context.GatewayContext.Builder
         */
        public Builder setGatewayRequest(GatewayRequest gatewayRequest) {
            this.gatewayRequest = gatewayRequest;
            return this;
        }
        /**
         * @date: 2024-01-11 15:49
         * @description: 设置rule属性。返回对象本身，因此可以使用链式编程。
         * @Param rule:
         * @return: org.wyh.core.context.GatewayContext.Builder
         */
        public Builder setRule(Rule rule) {
            this.rule = rule;
            return this;
        }
        /**
         * @date: 2024-01-11 15:50
         * @description: 构造GatewayContext对象
         * @return: org.wyh.core.context.GatewayContext
         */
        public GatewayContext build() {
            //判断某些属性是否未设置（未设置则为null，系统抛出IllegalArgumentException异常）
            AssertUtil.notNull(protocol, "protocol不能为空");
            AssertUtil.notNull(nettyCtx, "nettyCtx不能为空");
            AssertUtil.notNull(gatewayRequest, "request不能为空");
            AssertUtil.notNull(rule, "rule不能为空");
            return new GatewayContext(protocol, nettyCtx, keepAlive, gatewayRequest, rule, 0);
        }
    }
    /**
     * @date: 2024-01-12 9:59
     * @description: 获取指定的上下文参数，如果不存在则抛出IllegalArgumentException
     * @Param key: 
     * @return: java.lang.Object
     */
    public Object getRequiredAttribute(String key){
        Object value = super.getAttribute(key);
        AssertUtil.notNull(value, "需要的属性'"+key+"'不存在");
        return value;
    }
    /**
     * @date: 2024-01-12 10:03
     * @description: 获取指定的上下文参数，如果不存在则返回指定的默认值
     * @Param key:
     * @Param defaultValue:
     * @return: java.lang.Object
     */
    public Object getAttributeOrDefault(String key, Object defaultValue){
        return super.attributes.getOrDefault(key, defaultValue);
    }
    /**
     * @date: 2024-01-12 10:05
     * @description: 获取指定的过滤器配置信息
     * @Param configId: 
     * @return: org.wyh.common.config.Rule.FilterConfig
     */
    public Rule.FilterConfig getFilterConfig(String configId){
        return rule.getFilterConfig(configId);
    }
    /**
     * @date: 2024-02-21 14:43
     * @description: 从网关上下文中获取请求的后端服务唯一id
     * @return: java.lang.String
     */
    public String getUniqueId(){
        return gatewayRequest.getUniqueId();
    }

    @Override
    public void releaseRequest(){
        //该方法用于真正释放FullHttpRequest请求对象，而父类方法只是改了一下标识
        //如果当前值等于期望值，则将当前值设置为新值并返回true，否则不修改当前值并返回false
        if(requestReleased.compareAndSet(false, true)){
            //将fullRequest对象的引用计数减1。如果该对象引用计数为0，则释放该对象。
            ReferenceCountUtil.release(gatewayRequest.getFullHttpRequest());
        }
    }
    @Override
    public GatewayRequest getRequest() {
        return gatewayRequest;
    }

    @Override
    public void setResponse(Object gatewayResponse) {
        this.gatewayResponse = (GatewayResponse) gatewayResponse;
    }
    @Override
    public GatewayResponse getResponse() {
        return gatewayResponse;
    }
    @Override
    public Rule getRule() {
        return rule;
    }
}
