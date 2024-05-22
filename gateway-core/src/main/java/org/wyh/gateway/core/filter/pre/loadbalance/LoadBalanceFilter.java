package org.wyh.gateway.core.filter.pre.loadbalance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
 * @Author: wyh
 * @Date: 2024-05-22 16:20
 * @Description: 负载均衡过滤器。
                 主要作用是根据指定的负载均衡策略，从匹配的服务实例集合中，选择一个服务实例
                 （该实例就是最终的访问对象）
 */
@Slf4j
@FilterAspect(id=LOAD_BALANCE_FILTER_ID,
              name=LOAD_BALANCE_FILTER_NAME,
              type=FilterType.PRE,
              order=LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter extends AbstractGatewayFilter<LoadBalanceFilter.Config> {
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.pre.loadbalance
     * @Author: wyh
     * @Date: 2024-05-22 16:27
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig{
        //采用的负载均衡策略（目前支持随机）
        private String loadBalanceStrategy;
    }
    /**
     * @date: 2024-05-22 16:38
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public LoadBalanceFilter(){
        super(LoadBalanceFilter.Config.class);
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        try{

        }catch (Exception e){
            throw new RuntimeException("【负载均衡过滤器】过滤器执行异常", e);
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx, args);
        }
    }
}
