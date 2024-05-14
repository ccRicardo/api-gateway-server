package org.wyh.gateway.core.filter.old_common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.cache.GatewayCacheManager;
import org.wyh.gateway.core.context.GatewayContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.wyh.gateway.common.constant.FilterConst.FILTER_CHAIN_CACHE_ID;
import static org.wyh.gateway.common.constant.FilterConst.ROUTER_FILTER_ID;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter
 * @Author: wyh
 * @Date: 2024-02-19 14:48
 * @Description: 过滤器链工厂的实现类
                 该类使用了caffeine本地缓存来保存过滤器链实例，以进一步提高系统的请求处理效率。
 */
@Slf4j
public class GatewayFilterChainFactory implements FilterChainFactory{
    //保存/维护过滤器id与过滤器对象之间的映射
    private Map<String,Filter> processorFilterIdMap = new ConcurrentHashMap<>();
    //缓存管理器，用于管理过滤器链缓存（原因详见构造器内的相关注释）
    private GatewayCacheManager cacheManager;
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter
     * @Author: wyh
     * @Date: 2024-02-19 15:08
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonInstance{
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }
    /**
     * @date: 2024-02-19 15:09
     * @description: 获取GatewayFilterChainFactory类的单例对象
     * @return: org.wyh.core.filter.GatewayFilterChainFactory
     */
    public static GatewayFilterChainFactory getInstance(){
        return SingletonInstance.INSTANCE;
    }
    /**
     * @date: 2024-02-19 15:16
     * @description: 无参构造器，主要作用是初始化processorFilterIdMap和cacheManager属性
     * @return: null
     */
    public GatewayFilterChainFactory(){
        /*
         * 以下这段代码的作用是通过java SPI机制来加载/构建各过滤器插件的实例
         * SPI是JDK内置的一种服务提供发现机制，可以动态获取/发现接口的实现类
         * 具体来说，服务提供者在提供了一种接口实现后，
         * 需要在resources/META-INF/services目录下创建一个以接口（全类名）命名的文件
         * 文件的内容就是该接口具体实现类的全类名
         * 之后通过java.util.ServiceLoader，就可以根据文件中的实现类全类名，来构建/加载相应的实例
         * 将ServiceLoader转换成流后，每个元素的类型是java.util.ServiceLoader.Provider<S>，S是接口的类型
         * 再通过get方法，就可以获取服务提供者（也就是接口的实现类）的实例。
         */
        ServiceLoader<Filter>  serviceLoader = ServiceLoader.load(Filter.class);
        //以下这段代码的作用是：获取每个过滤器插件（除了路由过滤器）的实例，并添加到processorFilterIdMap集合中
        serviceLoader.stream().forEach(filterProvider -> {
            //获取过滤器插件的实例
            Filter filter = filterProvider.get();
            //获取过滤器对象上的过滤器注解，以获得过滤器的描述信息
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            if(annotation != null){
                String filterId = annotation.id();
                log.info("成功加载过滤器组件: {},{},{},{}", filter.getClass(),
                        filterId, annotation.name(), annotation.order());
                //如果未设置过滤器id，则以全类名作为该过滤器的id
                if(StringUtils.isEmpty(filterId)){
                    filterId = filter.getClass().getName();
                }
                //将过滤器id以及对应的过滤器实例添加到processorFilterIdMap集合中
                processorFilterIdMap.put(filterId, filter);
            }
        });
        /*
         * 以下代码的主要作用是获取缓存管理器实例，并创建过滤器链缓存，然后纳入缓存管理器的管理
         * 为什么要缓存过滤器链实例：规则的变更频率低，因此过滤器链的变更频率也很低
         * （实际上，只有在规则中添加/删除过滤器组件时才会导致过滤器链发生变更，而只是修改参数配置的话并不会。）
         * 如果把过滤器链进行缓存，那么每次请求处理时就不需要重复地创建相同的过滤器链实例
         * 因此能够节省大量开销，减少请求的平均响应时间
         */
        cacheManager = GatewayCacheManager.getInstance();
        Cache<String, GatewayFilterChain> chainCache = Caffeine.newBuilder()
                //开启统计数据的收集
                .recordStats()
                //设置（在最后一次写操作后）缓存项的过期时间
                .expireAfterWrite(10, TimeUnit.MINUTES)
                //设置缓存的初始容量
                .initialCapacity(16)
                //设置缓存的最大容量
                .maximumSize(256)
                .build();
        cacheManager.add(FILTER_CHAIN_CACHE_ID, chainCache);
    }
    /**
     * @date: 2024-03-28 10:51
     * @description: 根据规则构建过滤器链实例。
     * @Param rule:
     * @return: org.wyh.core.filter.GatewayFilterChain
     */
    public GatewayFilterChain doBuildFilterChain(Rule rule) {
        GatewayFilterChain filterChain = new GatewayFilterChain();
        List<Filter> filters = new ArrayList<>();
        if(rule != null){
            //获取规则中的过滤器配置集合
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator iterator = filterConfigs.iterator();
            //保存单个的过滤器配置
            Rule.FilterConfig filterConfig;
            while(iterator.hasNext()){
                filterConfig = (Rule.FilterConfig) iterator.next();
                if(filterConfig == null){
                    continue;
                }
                //获取过滤器的id
                String filterId = filterConfig.getFilterId();
                //先判断过滤器id是否为空，再判断对应的过滤器实例是否存在
                if(StringUtils.isNotEmpty(filterId) && getFilter(filterId) != null){
                    Filter filter = getFilter(filterId);
                    filters.add(filter);
                }
            }
        }
        //由于路由过滤器是必定存在的，所以单独在这里添加
        filters.add(getFilter(ROUTER_FILTER_ID));
        //将过滤器插件按照执行优先级排序，默认为升序排序
        filters.sort(Comparator.comparingInt(Filter::getOrder));
        //将排好序的过滤器链表添加到过滤器链中
        filterChain.addFilterList(filters);
        return filterChain;
    }
    @Override
    public GatewayFilterChain buildFilterChain(GatewayContext ctx) throws Exception{
        return cacheManager.get(FILTER_CHAIN_CACHE_ID,
                /*
                 * 过滤器链缓存项的key是对应规则的id与最后修改时间的拼接
                 * 这样就确保了当规则发生变更时，对应的原过滤器链缓存项就会失效。
                 * （因为当规则发生变更并写入动态配置管理器时，其“最后修改时间”属性会被修改为当前时间）
                 */
                ctx.getRule().getRuleId() + ctx.getRule().getLastModifiedTime(),
                /*
                 * 此处lambda表达式的入参key就是get方法中的第二个实参（也就是规则id与最后修改时间的拼接）
                 * 该lambda表达式会在key对应的缓存项不存在时被调用
                 * （实际上就是调用doBuildFilterChain方法来生成过滤器链实例）
                 * 其调用结果将会作为get方法的结果返回，并且还会被保存到缓存中，供之后使用。
                 */
                key->doBuildFilterChain(ctx.getRule()));
    }
    @Override
    public Filter getFilter(String filterId) {
        //根据过滤器id，从processorFilterIdMap获取对应的过滤器实例
        return processorFilterIdMap.get(filterId);
    }
}
