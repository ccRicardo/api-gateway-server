package org.wyh.gateway.core.filter.common.chainfactory;

import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.filter.Filter;

import java.util.ServiceLoader;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.common.chainfactory
 * @Author: wyh
 * @Date: 2024-05-15 16:56
 * @Description: 网关的过滤器链工厂类，主要负责实现过滤器实例的SPI加载，以及过滤器链的执行。
 */
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
     * @description: private修饰的无参构造器，主要负责通过java spi机制来加载过滤器链实例
     * @return: null
     */
    private GatewayFilterChainFactory(){
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
        // TODO: 2024-05-15 完成该类 
    }
    @Override
    public void doFilterChain(GatewayContext ctx, Object... args) {

    }

    @Override
    public void doErrorFilterChain(GatewayContext ctx, Object... args) {

    }
}
