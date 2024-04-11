package org.wyh.gateway.core.filter.loadbalance;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.config.ServiceInstance;
import org.wyh.gateway.common.exception.NotFoundException;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.Filter;
import org.wyh.gateway.core.filter.common.FilterAspect;
import org.wyh.gateway.core.request.GatewayRequest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.wyh.gateway.common.constant.FilterConst.*;
import static org.wyh.gateway.common.enumeration.ResponseCode.SERVICE_INSTANCE_NOT_FOUND;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.loadbalance
 * @Author: wyh
 * @Date: 2024-02-21 15:33
 * @Description: 负载均衡过滤器
 */
@Slf4j
@FilterAspect(id=LOAD_BALANCE_FILTER_ID,
              name=LOAD_BALANCE_FILTER_NAME,
              order=LOAD_BALANCE_FILTER_ORDER)
public class LoadBalanceFilter implements Filter {
    /**
     * @date: 2024-02-22 9:39
     * @description: 从网关上下文的规则配置中获取相应的负载均衡策略
     * @Param ctx:
     * @return: org.wyh.core.filter.loadbalance.IGatewayLoadBalanceRule
     */
    public IGatewayLoadBalanceRule getLoadBalanceRule(GatewayContext ctx){
        IGatewayLoadBalanceRule loadBalanceRule = null;
        Rule rule = ctx.getRule();
        if(rule != null){
            //获取过滤器配置集合
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            //遍历过滤器配置集合，找到负载均衡过滤器的配置信息
            while(iterator.hasNext()){
                filterConfig = iterator.next();
                if(filterConfig == null){
                    continue;
                }
                String filterId = filterConfig.getFilterId();
                //找到负载均衡过滤器的配置信息，并对其进行解析，获取负载均衡策略
                if(filterId.equals(LOAD_BALANCE_FILTER_ID)){
                    String config = filterConfig.getConfig();
                    //默认使用随机负载均衡
                    String strategy = LOAD_BALANCE_STRATEGY_RANDOM;
                    if(StringUtils.isNotEmpty(config)){
                        /*
                         * 配置信息是一个json串，这里将json串转换成了一个map
                         * 其中，key是配置项的名称，value是设定的值
                         */
                        Map<String, String> map = JSON.parseObject(config, Map.class);
                        //找到“负载均衡策略”配置项，读取设定值
                        strategy = map.getOrDefault(LOAD_BALANCE_KEY, strategy);
                    }
                    switch (strategy){
                        case LOAD_BALANCE_STRATEGY_RANDOM:
                            //getInstance方法可以避免创建冗余对象
                            loadBalanceRule = RandomLoadBalanceRule.getInstance(rule.getServiceId());
                            break;
                        case LOAD_BALANCE_STRATEGY_ROUND_ROBIN:
                            //getInstance方法可以避免创建冗余对象
                            loadBalanceRule = RoundRobinLoadBalanceRule.getInstance(rule.getServiceId());
                            break;
                        default:
                            //目前仅支持随机和轮询策略，其他情况都默认使用随机策略
                            log.warn("【负载均衡过滤器】暂不提供此负载均衡策略: {}", strategy);
                            //getInstance方法可以避免创建冗余对象
                            loadBalanceRule = RandomLoadBalanceRule.getInstance(rule.getServiceId());
                            break;
                    }
                    //读取完负载均衡过滤器的配置后，退出循环
                    break;
                }
            }
        }
        return loadBalanceRule;
    }
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        String serviceId = ctx.getUniqueId();
        //通过网关上下文，获取相应的负载均衡策略
        IGatewayLoadBalanceRule loadBalanceRule = getLoadBalanceRule(ctx);
        //根据指定的负载均衡策略，获取相应的服务实例
        ServiceInstance serviceInstance = loadBalanceRule.choose(serviceId, ctx.isGray());
        log.info("服务实例ip: {} port: {}", serviceInstance.getIp(), serviceInstance.getPort());
        GatewayRequest request = ctx.getRequest();
        if(serviceInstance != null && request != null){
            //设置服务的真实地址
            String host = serviceInstance.getIp() + ":" + serviceInstance.getPort();
            request.setModifyHost(host);
        }else{
            log.warn("【负载均衡过滤器】无可用的服务实例: {}",serviceId);
            throw new NotFoundException(SERVICE_INSTANCE_NOT_FOUND);
        }
    }
}
