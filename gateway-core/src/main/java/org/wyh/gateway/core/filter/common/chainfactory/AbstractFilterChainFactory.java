package org.wyh.gateway.core.filter.common.chainfactory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.filter.common.GatewayFilterChain;
import org.wyh.gateway.core.filter.common.AbstractLinkedFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common
 * @Author: wyh
 * @Date: 2024-05-15 15:47
 * @Description: 过滤器链工厂的抽象类。主要负责实现过滤器链的构建和过滤器集合的维护，以及指定过滤器实例的获取
 */
@Slf4j
@Setter
@Getter
public abstract class AbstractFilterChainFactory implements FilterChainFactory {
    //默认/正常情况下的过滤器链，由pre，route和post依次构成
    protected GatewayFilterChain defaultFilterChain =
            new GatewayFilterChain("default_filter_chain");
    //异常情况下的过滤器链，由error和post依次构成
    protected GatewayFilterChain errorFilterChain =
            new GatewayFilterChain("error_filter_chain");
    /*
     * 保存过滤器类型及其对应过滤器集合的集合。
     * 其中，外层key指的是过滤器类型的描述代码，外层value指的是对应的过滤器集合
     * 内层（也就是上述提到的对应过滤器集合）key指的是过滤器id，内存value指的是过滤器实例
     * 此外，LinkedHashMap可以保证插入和访问的顺序一致
     */
    protected Map<String, Map<String, AbstractLinkedFilter>> filterTypeMap = new LinkedHashMap<>();
    /*
     * 保存过滤器id及其对应实例的集合
     * 其中，key指的是过滤器id，value指的是对应的过滤器实例
     * 此外，LinkedHashMap可以保证插入和访问的顺序一致
     */
    protected Map<String, AbstractLinkedFilter> filterIdMap = new LinkedHashMap<>();
    /**
     * @date: 2024-05-15 16:23
     * @description: 将单个过滤器添加到指定过滤器链中
     * @Param filterChain:
     * @Param filter:
     * @return: void
     */
    private void addFilter(GatewayFilterChain filterChain, AbstractLinkedFilter filter){
        FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
        if(annotation != null){
            filterChain.addLast(filter);
            log.info("过滤器{}已添加到过滤器链{}中", annotation.name(), filterChain.getFilterChainId());
            //以下一系列操作就是将该过滤器实例添加到filterTypeMap和filterIdMap集合中
            String filterId = annotation.id();
            String filterTypeCode = annotation.type().getCode();
            //获取该类型对应的过滤器集合，若不存在，则创建
            Map<String, AbstractLinkedFilter> filterMap = filterTypeMap.get(filterTypeCode);
            if(filterMap == null){
                filterMap = new LinkedHashMap<>();
            }
            filterMap.put(filterId, filter);
            //维护filterTypeMap和filterIdMap集合
            filterTypeMap.put(filterTypeCode, filterMap);
            filterIdMap.put(filterId, filter);
        }
    }
    /**
     * @date: 2024-05-15 16:19
     * @description: 将过滤器列表添加到指定过滤器链中
     * @Param filterChain:
     * @Param filters:
     * @return: void
     */
    private void addFilters(GatewayFilterChain filterChain, List<AbstractLinkedFilter> filters) throws Throwable{
        for (AbstractLinkedFilter filter : filters) {
            //过滤器初始化
            filter.init();
            addFilter(filterChain, filter);
        }
    }
    @Override
    public void buildFilterChain(FilterType filterType, List<AbstractLinkedFilter> filters) throws Throwable{
        /*
         * pre，rout会添加到正常过滤器链中
         * error会添加到异常过滤器连中
         * post既会添加到正常过滤器链，也会添加到异常过滤器链
         */
        switch (filterType){
            case PRE:
            case ROUTE:
                addFilters(defaultFilterChain, filters);
                break;
            case ERROR:
                addFilters(errorFilterChain, filters);
                break;
            case POST:
                addFilters(defaultFilterChain, filters);
                addFilters(errorFilterChain, filters);
                break;
            default:
                throw new RuntimeException("不支持的过滤器类型");
        }
    }

    @Override
    public <T> T getFilter(Class<T> filterClass) {
        FilterAspect annotation = filterClass.getAnnotation(FilterAspect.class);
        if(annotation != null){
            String filterId = annotation.id();
            return getFilter(filterId);
        }
        return null;
    }

    @Override
    public <T> T getFilter(String filterId) {
        AbstractLinkedFilter filter = null;
        if(!filterIdMap.isEmpty()){
            filter = filterIdMap.get(filterId);
        }
        return (T)filter;
    }
}
