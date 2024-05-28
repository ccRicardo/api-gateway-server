package org.wyh.gateway.core.filter.common;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.constant.FilterConst;
import org.wyh.gateway.common.utils.JSONUtil;
import org.wyh.gateway.core.cache.GatewayCacheManager;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.base.FilterAspect;


/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-14 17:10
 * @Description: 网关过滤器的抽象类，是过滤器实现类真正继承的类。
                 其中，泛型C指的是具体过滤器的配置的类型。
                 此外，该类主要实现的是check和filter方法，而doFilter方法是由具体过滤器来实现的。
                 注意，方法中的args参数实际上存放的是过滤器组件的配置类实例
 */
// TODO: 2024-05-15 需要测试一下：在nacos中修改规则的过滤器配置，本地缓存中对应的原配置项会不会失效
@Slf4j
public abstract class AbstractGatewayFilter<C> extends AbstractLinkedFilter {
    //过滤器的注解对象
    protected FilterAspect filterAnnotation;
    //caffeine本地缓存。其中，key由ruleId，filterId加lastModifiedTime构成，value是具体过滤器的配置类对象
    protected Cache<String, C> filterConfigCache;
    //具体过滤器的配置类的Class对象，用于对配置信息反序列化
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
        this.filterConfigCache = GatewayCacheManager.getInstance().getCache(FilterConst.FILTER_CONFIG_CACHE_ID);
        this.filterConfigClass = filterConfigClass;
    }
    /**
     * @date: 2024-05-15 10:05
     * @description: 从上下文中加载本过滤器的配置信息，并以C类型对象的形式返回结果
     * @Param ctx:
     * @return: C
     */
    private C loadFilterConfig(GatewayContext ctx){
        String ruleId = ctx.getRule().getRuleId();
        long lastModifiedTime = ctx.getRule().getLastModifiedTime();
        /*
         * 构造cache key（也就是缓存数据项的id）。
         * cache key由ruleId，filterId加上lastModifiedTime构成
         * 之所以要加上lastModifiedTime，是为了确保当nacos中的规则配置发生变更时，
         * 本地缓存中对应的原配置项会失效
         */
        String cacheKey = ruleId + ":" + filterAnnotation.id() + ":" + lastModifiedTime;
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
    public boolean check(GatewayContext ctx) throws Throwable{
        //根据规则判断是否启用本过滤器组件
        return ctx.getRule().checkFilterExist(filterAnnotation.id());
    }

    @Override
    public void filter(GatewayContext ctx) throws Throwable{
        //从上下文中加载过本滤器配置信息，再调用具体过滤器对象的doFilter方法，完成过滤处理
        C filterConfig = loadFilterConfig(ctx);
        doFilter(ctx, filterConfig);
    }
}
