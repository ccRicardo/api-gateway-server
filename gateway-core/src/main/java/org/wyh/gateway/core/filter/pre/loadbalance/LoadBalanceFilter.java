package org.wyh.gateway.core.filter.pre.loadbalance;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.DynamicConfigManager;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.FilterProcessingException;
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
                loadBalance = LoadBalanceFactory.getLoadBalance(LOAD_BALANCE_STRATEGY_RANDOM);
            }
            //调用负载均衡实例的select方法，选择一个服务实例（该实例就是最后要访问的对象）
            ServiceInstance selectedInstance = loadBalance.select(ctx);
            if(selectedInstance == null){
                // TODO: 2024-05-22 此时将上下文状态设置终止合理吗，与其他部分冲突吗
                //不存在对应的服务实例，网关抛出异常，将上下文状态设置为written
                ctx.setWritten();
                throw new ResponseException(ResponseCode.SERVICE_INSTANCE_NOT_FOUND);
            }
            //设置最终服务的地址（这一步非常关键！！！）
            ctx.getRequest().setModifyHost(selectedInstance.getAddress());
        }catch (Exception e){
            log.error("【负载均衡过滤器】过滤器执行异常", e);
            throw new FilterProcessingException(LOAD_BALANCE_FILTER_ID, ResponseCode.FILTER_PROCESSING_ERROR);
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            // TODO: 2024-05-23 总感觉这句代码应该写在try中，当该过滤器出现异常，就不再往下执行了 
            super.fireNext(ctx, args);
        }
    }
}
