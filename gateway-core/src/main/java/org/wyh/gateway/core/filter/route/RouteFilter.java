package org.wyh.gateway.core.filter.route;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.helper.AsyncHttpHelper;

import javax.xml.crypto.dsig.spec.XPathType;

import java.util.concurrent.CompletableFuture;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.route
 * @Author: wyh
 * @Date: 2024-05-22 19:15
 * @Description: 路由过滤器，负责发送请求给目标服务，并接收和处理其响应结果。
                 （在此之前，已经通过负载均衡过滤器确定了要访问的服务实例）
                 注：该过滤器是通过AsyncHttpClient框架向目标发送http请求的。
 */
@Slf4j
@FilterAspect(id=ROUTER_FILTER_ID,
              name=ROUTER_FILTER_NAME,
              type= FilterType.ROUTE,
              order=ROUTER_FILTER_ORDER)
public class RouteFilter extends AbstractGatewayFilter<RouteFilter.Config> {
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.route
     * @Author: wyh
     * @Date: 2024-05-22 20:13
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig{
        // TODO: 2024-05-22 修改rule
    }
    /**
     * @date: 2024-05-22 20:16
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public RouteFilter(){
        super(RouteFilter.Config.class);
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        //构建AsyncHttpClient的请求对象
        Request request = ctx.getRequest().build();
        // TODO: 2024-05-22 源码这里设置了一下上下文的RS属性
        //通过AsyncHttpHelper封装的AsyncHttpClient发送异步http请求
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        //单/双异步模式的标识。单异步使用whenComplete，双异步使用whenCompleteAsync
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        if(whenComplete){
            // TODO: 2024-05-22 完成该类 
        }

    }
}
