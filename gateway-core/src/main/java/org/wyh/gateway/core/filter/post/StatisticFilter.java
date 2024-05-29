package org.wyh.gateway.core.filter.post;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.Setter;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.monitor.PrometheusMonitor;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.post
 * @Author: wyh
 * @Date: 2024-05-29 16:43
 * @Description: 统计过滤器，负责统计请求的数量以及每个请求的处理时间
                 （具体来说，是从创建对应上下文到调用该过滤器所经历的时间）
                 并向外提供一个rest api，供Prometheus拉取相关的指标数据。
 */
/*
 * 详细说明：
 * 当一个请求进入网关，并开始构建上下文时，便会调用该类的startSample方法
 * 该方法会先通过Timer.start方法创建一个Timer.Sample实例，然后设置到对应的上下文对象参数中。
 * 当执行该过滤器的doFilter方法时，便会调用上下文中的Timer.Sample实例的stop方法，计算出该请求的处理时间
 * 然后将该数据记录到注册中心中的指定Timer实例内。（该Timer实例其实充当了数据汇总的角色）
 * 当Prometheus访问api时，注册中心便会将Timer实例中的数据写入到响应体中。
 */
/*
 * 基础知识：
 * Registry是注册中心，负责创建和管理Meter实例的。
 * Meter是一组用于数据统计和收集的接口，其具体实现有很多，此类用的是PrometheusMeter。
 * Meter实例的命名通常用"."分隔单词，以体现出层次性。
 * Meter实例还可以设置标签，以进一步指明该Meter实例采集的数据源的详细信息。
 * Timer（计时器）是Meter的一种类型，其静态内部类Timer.Sample可以用于测量某个操作的耗时时间。
 * Timer.start方法会创建一个Timer.Sample实例，并将当前时间记录为测量的开始时间。
 * Timer.Sample.stop方法会停止计时，然后计算当前时间与开始时间的差值，并将其记录到传入的Timer实例中。
 */
public class StatisticFilter extends AbstractGatewayFilter<StatisticFilter.Config> {
    // TODO: 2024-05-29 过滤器实例在系统中好像只有一个，不需要加static
    //Prometheus Meter的注册中心，用于创建和管理Prometheus Meter实例
    private static PrometheusMeterRegistry registry;
    //http服务器，用于提供一个rest api，供Prometheus拉取数据
    private static HttpServer httpServer;
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.post
     * @Author: wyh
     * @Date: 2024-05-29 16:57
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig {
        //
    }
    public StatisticFilter(){
        super(StatisticFilter.Config.class);
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        // TODO: 2024-05-29 完成该类
    }

}
