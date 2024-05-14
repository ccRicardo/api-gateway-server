package org.wyh.gateway.core.filter.monitor;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.old_common.Filter;
import org.wyh.gateway.core.filter.old_common.FilterAspect;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.monitor
 * @Author: wyh
 * @Date: 2024-03-14 14:26
 * @Description: 监控（后置）过滤器，设置在过滤器链的末尾，用于结束计时并记录结果（也就是该次请求的处理时间）
                 此外，该方法还会开启一个http服务器线程，提供一个服务api，供Prometheus拉取数据。
 */
@Slf4j
@FilterAspect(id=MONITOR_END_FILTER_ID,
              name=MONITOR_END_FILTER_NAME,
              order=MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {
    /*
     * 基础知识：
     * Registry是用于创建和管理Meter实例的。Meter是一组用于收集应用中度量数据的接口，即数据收集器接口。
     * Meter实例的命名通常用"."分隔单词，以体现出层次性。
     * Meter实例还可以设置标签，以进一步指明该Meter实例采集的数据源的详细信息。
     * Timer（计时器）是Meter的一种类型，其静态内部类Timer.Sample可以统计一个方法的耗时时间。
     * Timer.start方法会创建一个Timer.Sample实例，然后该实例会开始计时。
     * Timer.Sample.stop方法则会结束计时，并将统计的时间间隔数据保存到传入的Timer对象中。
     */
    //（prometheus实现的）Meter注册中心/注册表，用于创建和管理Meter实例（即数据收集器对象）
    private final PrometheusMeterRegistry registry;
    /**
     * @date: 2024-03-15 10:14
     * @description: 无参构造器，用于初始化注册表，并开启一个http服务器线程，提供服务api，供Prometheus拉取数据
     * @return: null
     */
    public MonitorEndFilter(){
        //根据默认配置创建Prometheus注册表实例
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try{
            //创建一个http服务器，并监听指定端口。
            HttpServer server = HttpServer.create(new InetSocketAddress(
                    ConfigLoader.getConfig().getPrometheusPort()), 0);
            /*
             * 通过匿名内部类的方式创建一个HttpHandler对象，用于向Prometheus响应在该系统中采集到的数据，
             * 然后将该HttpHandler对象绑定到"/prometheus"路径上，处理该路径的请求
             * 在HttpServer中，每个http请求及其响应被称为一个交换，用HttpExchange表示。
             * HttpExchange提供了一系列方法，可以用来解析请求以及构建和发送响应。
             * HttpHandler接收的参数就是HttpExchange对象。
             */
            server.createContext("/prometheus", exchange -> {
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
                 * 以下代码的作用就是在获取响应体的输出流对象后，向响应体中写入响应数据。
                 */
                try(OutputStream os = exchange.getResponseBody()){
                    //写入响应数据，即在该系统中采集到的度量数据
                    os.write(data.getBytes());
                }
            });
            //开启一个单独的线程，用于运行http服务器，并通过上述HttpHandler向Prometheus响应采集到的数据
            new Thread(server::start).start();
        }catch(IOException e){
            log.error("【后置监控过滤器】Prometheus数据供给服务器状态异常", e);
            throw new RuntimeException(e);
        }
        log.info("【后置监控过滤器】成功启动Prometheus数据供给服务，端口号: {}",
                ConfigLoader.getConfig().getPrometheusPort());
//        /*
//         * 以下是一段测试代码：
//         * 此处创建了一个线程池来执行一个定时任务，时间间隔为100ms
//         * 该定时任务是在模拟网关系统对一次http请求的过程，其中处理时间被设置为一个0~99之间的随机值
//         */
//        Executors.newScheduledThreadPool(1000).scheduleAtFixedRate(() -> {
//            Timer.Sample sample = Timer.start();
//            try {
//                Thread.sleep(RandomUtils.nextInt(100));
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            Timer timer = registry.timer("gateway_request",
//                    "uniqueId", "http-service:1.0.0",
//                    "protocol", "http",
//                    "path", "/http-service/test" + RandomUtils.nextInt(10));
//            sample.stop(timer);
//        },200, 100, TimeUnit.MILLISECONDS);
    }
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //从注册表中获取采集该请求路径数据的Timer实例（若不存在，则创建相应实例）
        Timer timer = registry.timer("gateway.request",
                //以下为标签信息，用于指明数据源的详细信息
                "uniqueId", ctx.getUniqueId(),
                "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        //停止计时，并将时间间隔数据（也就是该请求的处理时间）保存到上述Timer实例中
        ctx.getTimerSample().stop(timer);
    }
}
