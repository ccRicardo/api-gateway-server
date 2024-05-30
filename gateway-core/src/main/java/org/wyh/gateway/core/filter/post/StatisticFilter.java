package org.wyh.gateway.core.filter.post;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.post
 * @Author: wyh
 * @Date: 2024-05-29 16:43
 * @Description: 统计过滤器，负责统计请求的数量以及每个请求的处理时间，便于之后计算qps和平均时延
                 （具体来说，是从创建对应上下文到调用该过滤器所经历的时间）
                 并向外提供一个rest api，供Prometheus拉取相关的指标数据。
 */
/*
 * 详细说明：
 * 当一个请求进入网关，并开始构建上下文时，便会调用该类的startSample方法
 * 该方法会先通过Timer.start方法创建一个Timer.Sample实例，然后设置到对应的上下文对象参数中。
 * 当执行该过滤器的doFilter方法时，便会调用上下文中的Timer.Sample实例的stop方法，计算出该请求的处理时间
 * 然后将该数据上报到指定的Timer实例内。（Timer实例其实充当了数据汇总的角色）
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
 * Timer.Sample.stop方法会停止计时，然后计算当前时间与开始时间的差值，并将其上报到传入的Timer实例中。
 */
@Slf4j
@FilterAspect(id=STATISTIC_FILTER_ID,
              name=STATISTIC_FILTER_NAME,
              type= FilterType.POST,
              order=STATISTIC_FILTER_ORDER)
public class StatisticFilter extends AbstractGatewayFilter<StatisticFilter.Config> {
    //每个过滤器实例在系统中只有一个，所以不需要加static
    //Prometheus Meter的注册中心，用于创建和管理Prometheus Meter实例
    private PrometheusMeterRegistry registry;
    //http服务器，用于提供一个rest api，供Prometheus拉取数据
    private HttpServer httpServer;
    //负责接收统计数据的Timer实例的名称
    private final String TIMER_NAME = "gateway.request";
    //异常信息
    private static final String EXCEPTION_MSG = "【统计过滤器】执行异常: ";

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
        //暂未使用到
    }
    public StatisticFilter(){
        super(StatisticFilter.Config.class);
        initialized();
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        /*
         * 该方法的主要作用是结束测量（该上下文对象对应的）请求对象的处理时间，
         * 并将数据上报到指定的Timer实例中
         */
        try{
            /*
             * 根据请求要访问的服务唯一id和路径，以及访问协议信息，从注册中心获取指定的Timer实例
             * （若对应实例不存在，则创建；若存在，则直接获取），
             * 该实例会接收上下文参数中Timer.Sample实例统计到的数据
             * （也就是对应请求对象的处理时间）
             */
            Timer timer = registry.timer(TIMER_NAME,
                    //以下为标签信息，用于记录数据源的详细信息
                    "uniqueId", ctx.getUniqueId(),
                    "path", ctx.getRequest().getPath(),
                    "protocol", ctx.getProtocol());
            //Timer.Sample.stop方法会停止计时，然后计算当前时间与开始时间的差值，并将其上报到上述Timer实例中。
            long duration = ctx.getAttribute(AttributeKey.PROMETHEUS_TIMER_SAMPLE).stop(timer);
            log.info("请求: {} 处理完毕，耗时: {}", ctx.getRequest().getPath(), duration);
        }catch (Exception e){
            //后置过滤器执行出现异常时，只做简单的日志打印和异常设置，然后继续往下执行
            log.error(EXCEPTION_MSG + e.getMessage());
            ctx.setThrowable(e);
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，
             * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx);
        }
    }
    /**
     * @date: 2024-05-30 9:36
     * @description: 开始统计/测量（该上下文对象对应的）请求对象的处理时间
     * @Param ctx:
     * @return: void
     */
    public static void startSample(GatewayContext ctx){
        //Timer.start方法会创建一个Timer.Sample实例，并将当前时间记录为测量的开始时间。
        Timer.Sample sample = Timer.start();
        //将上述的Prometheus Timer.Sample数据采集器实例设置到相应的上下文参数中
        ctx.setAttribute(AttributeKey.PROMETHEUS_TIMER_SAMPLE, sample);
        log.info("【统计过滤器】成功设置请求: {}的Timer.Sample实例", ctx.getRequest().getPath());
    }
    /**
     * @date: 2024-05-30 9:20
     * @description: 负责初始化注册中心和http服务器
     * @return: void
     */
    private void initialized(){
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        //供Prometheus拉取数据的api的端口号
        int port = ConfigLoader.getConfig().getPrometheusPort();
        //供Prometheus拉取数据的api的路径
        String path = ConfigLoader.getConfig().getPrometheusPath();
        try{
            //指定服务端口
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            /*
             * 通过匿名内部类的方式创建HttpHandler对象，并实现handle方法，
             * 该方法的作用是对访问该api的请求进行处理，在此处具体指向Prometheus响应数据，
             * 然后将该HttpHandler对象绑定到"/prometheus"路径上
             * 在HttpServer中，每个http请求及其响应被称为一个交换，用HttpExchange表示。
             * HttpExchange提供了一系列方法，可以用来解析请求以及构建和发送响应。
             * HttpHandler接收的参数就是HttpExchange对象。
             */
            httpServer.createContext(path, new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    //（以字符串形式）获取在该网关系统中采集到的所有数据
                    String data = registry.scrape();
                    //设置响应头中的响应状态码和响应体长度信息
                    exchange.sendResponseHeaders(200, data.getBytes().length);
                    /*
                     * try(...){...}是java中的try-with-resources语句
                     * 其中，“()”内可以声明要使用的资源对象（可以声明多个）。
                     * （注：任何实现了“java.lang.AutoCloseable”接口的对象，都可以视为一个资源对象。）
                     * 在try代码块执行结束后（即“{}”内包含的代码），声明的资源对象会自动释放/关闭。
                     * 这与finally子句中手动释放资源的效果相似，但是更加简洁，可靠。
                     * 以下代码的作用就是先获取指向响应体的输出流对象，然后向响应体中写入数据。
                     */
                    try(OutputStream os = exchange.getResponseBody()){
                        //写入数据
                        os.write(data.getBytes());
                    }
                }
            });
            log.info("正在启动http服务器，端口: {} 路径 :{}", port, path);
            //开启一个单独的线程运行http服务器，并通过上述HttpHandler向Prometheus响应数据
            new Thread(httpServer::start, "监控数据获取服务").start();
        }catch (IOException e){
            log.error("http服务器状态异常: {}，端口: {} 路径: {}",e.getMessage(), port, path);
            throw new RuntimeException("http服务器状态异常", e);
        }
    }
}
