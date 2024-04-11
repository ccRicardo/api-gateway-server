package org.wyh.gateway.core.filter.common;

import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.toolkit.trace.Trace;
import org.wyh.gateway.core.context.GatewayContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter
 * @Author: wyh
 * @Date: 2024-02-19 14:22
 * @Description: 网关的过滤器链定义类
 */
@Slf4j
public class GatewayFilterChain {
    //过滤器列表
    private List<Filter> filters = new ArrayList<>();
    /**
     * @date: 2024-02-19 14:25
     * @description: 添加单个过滤器
     * @Param filter:
     * @return: org.wyh.core.filter.GatewayFilterChain
     */
    public GatewayFilterChain addFilter(Filter filter){
        filters.add(filter);
        return this;
    }
    /**
     * @date: 2024-02-19 14:26
     * @description: 添加一个过滤器列表
     * @Param filters:
     * @return: org.wyh.core.filter.GatewayFilterChain
     */
    public GatewayFilterChain addFilterList(List<Filter> filters){
        this.filters.addAll(filters);
        return this;
    }
    /**
     * @date: 2024-02-19 14:28
     * @description: 执行过滤器链条
     * @Param ctx:
     * @return: org.wyh.core.context.GatewayContext
     */
    @Trace
    public GatewayContext doFilterChain(GatewayContext ctx) throws Exception{
        //若不设置过滤器，则不对网关上下文做处理
        if(filters.isEmpty()){
            return ctx;
        }
        //依次执行过滤器链中的过滤器，来处理网关上下文
        try{
            for (Filter filter : filters) {
                filter.doFilter(ctx);
                //由于过滤器链中设置了mock过滤器，因此对mock接口的请求会直接返回响应，而不需要执行后续的过滤逻辑
                if(ctx.isTerminated()){
                    break;
                }
            }
        }catch (Exception e){
            log.error("过滤器执行异常，异常信息: {}",e.getMessage());
            throw e;
        }
        //返回处理好的网关上下文
        return ctx;
    }
}
