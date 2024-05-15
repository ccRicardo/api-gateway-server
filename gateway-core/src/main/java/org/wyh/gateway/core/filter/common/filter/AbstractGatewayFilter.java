package org.wyh.gateway.core.filter.common.filter;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.constant.FilterConst;
import org.wyh.gateway.common.utils.JSONUtil;
import org.wyh.gateway.core.cache.GatewayCacheManager;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.FilterAspect;
import org.wyh.gateway.core.filter.common.filter.AbstractLinkedFilter;


/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 17:10
 * @Description: 网关过滤器的抽象类，是过滤器实现类真正继承的类。
                 其中，泛型C指的是具体过滤器的配置的类型。
                 由于该类过滤处理的内容是网关上下文，因此其父类中的泛型被指定为了GatewayContext
                 此外，该类主要实现的是check和filter方法，而doFilter方法是由具体过滤器来实现的。
 */
// TODO: 2024-05-15 目前存在的一个问题：在nacos中修改规则的过滤器配置，本地缓存中对应的原配置项不会失效
@Slf4j
public abstract class AbstractGatewayFilter<C> extends AbstractLinkedFilter<GatewayContext> {
    //过滤器的注解对象
    protected FilterAspect filterAnnotation;
    //caffeine本地缓存。其中，key由ruleId加filterId构成，value是具体过滤器的配置类对象
    protected Cache<String, C> filterConfigCache;
    //具体过滤器的配置类的Class对象
    protected Class<C> filterConfigClass;
    /**
     * @date: 2024-05-15 9:39
     * @description: 有参构造器。负责属性初始化，并指定/实例化泛型参数。
     * @Param filterConfigClass: 具体过滤器的配置类的Class对象
     * @return: null
     */
    public AbstractGatewayFilter(Class<C> filterConfigClass){
        //获取FilterAspect注解信息
        this.filterAnnotation = this.getClass().getAnnotation(FilterAspect.class);
        //通过网关缓存管理器创建过滤器配置缓存对象
        this.filterConfigCache = GatewayCacheManager.getInstance().create(FilterConst.FILTER_CONFIG_CACHE_ID);
        this.filterConfigClass = filterConfigClass;
    }
    /**
     * @date: 2024-05-15 10:05
     * @description: 从上下文中加载本过滤器的配置信息，并以C类型对象的形式返回结果
     * @Param ctx:
     * @Param args:
     * @return: C
     */
    private C loadFilterConfig(GatewayContext ctx, Object... args){
        String ruleId = ctx.getRule().getRuleId();
        //构造cache key。
        String cacheKey = ruleId + ":" + filterAnnotation.id();
        //先尝试从缓存中获取本过滤器的（C类型的）配置对象
        C filterConfig = filterConfigCache.getIfPresent(cacheKey);
        //若缓存中不存在对应项，则将上下文规则中的相应配置信息解析为C类型的配置对象，并放入缓存中
        if(filterConfig == null){
            //从上下文规则中获取本过滤器的（Rule.FilterConfig类型的）配置信息
            Rule.FilterConfig configInfo = ctx.getRule().getFilterConfig(filterAnnotation.id());
            if(configInfo != null && StringUtils.isNotEmpty(configInfo.getConfig())){
                //获取json字符串形式的过滤器配置信息
                String configInfoStr = configInfo.getConfig();
                try{
                    //将json串形式的配置信息反序列化为C类型的配置对象
                    filterConfig = JSONUtil.parse(configInfoStr, filterConfigClass);
                    //将该配置对象放入缓存中
                    filterConfigCache.put(cacheKey, filterConfig);
                }catch (Exception e){
                    log.error("过滤器: {}的配置信息: {}反序列化失败",
                            filterAnnotation.id(), configInfoStr, e);
                }
            }

        }
        return filterConfig;
    }
    @Override
    public boolean check(GatewayContext ctx) {
        //根据规则判断是否启用本过滤器组件
        return ctx.getRule().checkFilterExist(filterAnnotation.id());
    }

    @Override
    public void filter(GatewayContext ctx, Object... args) {
        //从上下文中加载过本滤器配置信息，并作为参数传递给父类filter方法，父类filter再调用具体过滤器对象的doFilter方法，完成过滤处理
        C filterConfig = loadFilterConfig(ctx, args);
        //这里把参数args给丢了。但实际上，args通常是空的，所以没有影响。
        super.filter(ctx, filterConfig);
    }
}
