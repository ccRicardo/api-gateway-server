package org.wyh.gateway.core.filter.route;

import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterType;

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
public class RouteFilter extends AbstractGatewayFilter<> {

}
