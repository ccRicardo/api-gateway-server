package org.wyh.gateway.core.filter.common.chainfactory;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.filter.common.AbstractLinkedFilter;
import org.wyh.gateway.core.filter.common.Filter;

import java.util.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common.chainfactory
 * @Author: wyh
 * @Date: 2024-05-15 16:56
 * @Description: 网关的过滤器链工厂类，主要负责实现过滤器实例的SPI加载和过滤器链实例的构建，以及过滤器链的执行。
                 注意：每个过滤器组件只加载一次，所以网关系统中每个过滤器实际上都是单例的。
 */
@Slf4j
public class GatewayFilterChainFactory extends AbstractFilterChainFactory{
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.common.chainfactory
     * @Author: wyh
     * @Date: 2024-05-15 17:02
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder {
        private static final GatewayFilterChainFactory INSTANCE = new GatewayFilterChainFactory();
    }
    /**
     * @date: 2024-05-15 17:02
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.filter.common.chainfactory.GatewayFilterChainFactory
     */
    public static GatewayFilterChainFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-15 17:03
     * @description: private修饰的无参构造器，负责通过java spi来加载过滤器实例，并将其添加到相应过滤器链中
     * @return: null
     */
    private GatewayFilterChainFactory(){
        //保存过滤器类型及其对应的过滤器实例集合。其中，key指的是过滤器类型的描述代码。
        Map<String, List<AbstractLinkedFilter>> filterMap = new LinkedHashMap<>();
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
        ServiceLoader<Filter> serviceLoader = ServiceLoader.load(Filter.class);
        //以下这段代码的作用是：获取每个过滤器组件的实例，并添加到filterMap集合中
        serviceLoader.stream().forEach(filterProvider -> {
            //获取过滤器组件实例
            Filter filter = filterProvider.get();
            FilterAspect annotation = filter.getClass().getAnnotation(FilterAspect.class);
            if(annotation != null){
                //获取该过滤器类型的描述代码，并以此为key，将对应实例加入到相应的list集合中
                String filterTypeCode = annotation.type().getCode();
                List<AbstractLinkedFilter> filterList = filterMap.get(filterTypeCode);
                if(filterList == null){
                    filterList = new ArrayList<AbstractLinkedFilter>();
                }
                filterList.add((AbstractLinkedFilter) filter);
                filterMap.put(filterTypeCode, filterList);
            }
        });
        /*
         * 对于各类型的过滤器集合，先按照优先级对其中的过滤器实例进行排序，
         * 然后再将有序集合添加到相应的过滤器链中。
         * 注意：枚举类元素的遍历顺序与其定义顺序是一致的
         */
        for (FilterType filterType : FilterType.values()) {
            //获取该类型对应的过滤器集合
            List<AbstractLinkedFilter> filterList = filterMap.get(filterType.getCode());
            if(filterList == null || filterList.isEmpty()){
                continue;
            }
            //将集合中的过滤器实例按照优先级进行排序，优先级数字越小，执行位置越靠前
            Collections.sort(filterList, new Comparator<AbstractLinkedFilter>() {
                @Override
                public int compare(AbstractLinkedFilter o1, AbstractLinkedFilter o2) {
                    //优先级数字越小，执行位置越靠前
                    return o1.getClass().getAnnotation(FilterAspect.class).order() -
                            o2.getClass().getAnnotation(FilterAspect.class).order();
                }
            });
            try{
                //将排好序的过滤器集合添加到相应的过滤器链中
                super.buildFilterChain(filterType, filterList);
            }catch (Throwable e){
                log.error("过滤器链构建异常: {}", e.getMessage());
            }
        }

    }
    @Override
    public void doFilterChain(GatewayContext ctx) {
        try{
            //启动正常情况下的过滤器链。
            super.defaultFilterChain.start(ctx);
        }catch (Throwable e){
            /*
             * 这里只负责捕获前置和路由过滤器（不包括complete方法）中的异常
             * 对于后置过滤器中的异常，在complete方法中做简单处理即可
             */
            log.error("过滤器链执行异常: {}", e.getMessage());
            //在网关上下文中设置异常信息
            ctx.setThrowable(e);
            //正常过滤器链执行结束后，开始执行异常过滤器链，所以要更改上下文状态
            if(ctx.isTerminated()){
                ctx.setRunning();
            }
            //执行异常情况下的过滤器链
            doErrorFilterChain(ctx);
        }
    }

    @Override
    public void doErrorFilterChain(GatewayContext ctx) {
        try{
            //启动异常情况下的过滤器链
            super.errorFilterChain.start(ctx);
        }catch (Throwable e){
            //实际上，异常和后置过滤器都不会向上抛异常，所以理论上该分支永远也不会执行
            log.error("异常处理过滤器链执行异常: {}", e.getMessage());
            ctx.setThrowable(e);
        }
    }
}
