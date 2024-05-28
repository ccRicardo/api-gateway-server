package org.wyh.gateway.core.filter.pre.loadbalance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

import java.util.Set;

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
    //异常消息
    private static final String EXCEPTION_MSG = "【负载均衡过滤器】执行异常: ";
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
            //args[0]其实就是该过滤器的配置类实例
            LoadBalanceFilter.Config filterConfig = (LoadBalanceFilter.Config) args[0];
            String strategy = filterConfig.getLoadBalanceStrategy();
            //获取灰度标记
            Boolean grayFlag = ctx.getAttribute(AttributeKey.GRAY_FLAG);
            //获取请求要访问的服务的唯一id
            String uniqueId = ctx.getUniqueId();
            //从动态配置管理器中获取该唯一id匹配的服务实例集合
            Set<ServiceInstance> matchedInstances = DynamicConfigManager.getInstance()
                    .getServiceInstanceByUniqueId(uniqueId, grayFlag);
            //将该服务实例集合放入对应的上下文参数中，供之后使用
            ctx.setAttribute(AttributeKey.MATCHED_INSTANCES, matchedInstances);
            //获取指定负载均衡策略对应的实例。默认使用随机负载均衡。
            LoadBalance loadBalance = LoadBalanceFactory.getLoadBalance(strategy);
            if(loadBalance == null){
                log.warn("【负载均衡过滤器】不支持该负载均衡策略: {} 默认使用随机负载均衡", strategy);
                loadBalance = LoadBalanceFactory.getLoadBalance(LOAD_BALANCE_STRATEGY_RANDOM);
            }
            //调用负载均衡实例的select方法，选择一个服务实例（该实例就是最后要访问的对象）
            ServiceInstance selectedInstance = loadBalance.select(ctx);
            if(selectedInstance == null){
                throw new ResponseException(ctx.getUniqueId(), ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
            }
            //设置最终服务的地址（这一步非常关键！！！）
            log.info("【负载均衡过滤器】最终访问实例地址: {}", selectedInstance.getAddress());
            ctx.getRequest().setModifyHost(selectedInstance.getAddress());
        } catch (Exception e){
            //过滤器执行过程出现异常，（正常）过滤器链执行结束，将上下文状态设置为terminated
            ctx.setTerminated();
            throw new Exception(EXCEPTION_MSG + e.getMessage(), e);
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，
             * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx);
        }
    }
}
