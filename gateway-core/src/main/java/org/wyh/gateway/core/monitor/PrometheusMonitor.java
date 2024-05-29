package org.wyh.gateway.core.monitor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.config.Config;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.monitor
 * @Author: wyh
 * @Date: 2024-05-17 13:43
 * @Description: 负责监控每个请求的处理时间（准确来说，是对应上下文从创建到销毁所经历的时间），
                 并向外提供一个http api，供Prometheus拉取相关的指标数据。
 */
/*
 * 详细说明：
 * 当一个请求进入网关，并构建好上下文后，便会调用该类的startMonitor方法
 * 该方法先通过Timer.start方法产生Timer.Sample实例，并设置到上下文对象的参数中。
 * 当该上下文对象销毁时（也就是状态被设置为TERMINATED时），便会调用该类的endMonitor方法
 * 该方法会调用上下文中的Timer.Sample实例的stop方法，计算出该上下文的处理时间
 * 然后将该数据记录到注册中心中的指定Timer实例内。（该Timer实例其实充当了数据汇总的角色）
 * 当Prometheus访问api时，注册中心便会将Timer实例中的数据写入到响应体中。
 */
// TODO: 2024-05-17 目前，该类还未启用，可以考虑改成后置过滤器
@Slf4j
public class PrometheusMonitor {
    /*
     * 基础知识：
     * Registry是用于创建和管理Meter实例的。
     * Meter是一组用于数据收集的接口，其具体实现有很多，此类用的是PrometheusMeter。
     * Meter实例的命名通常用"."分隔单词，以体现出层次性。
     * Meter实例还可以设置标签，以进一步指明该Meter实例采集的数据源的详细信息。
     * Timer（计时器）是Meter的一种类型，其静态内部类Timer.Sample可以用于测量某个操作的耗时时间。
     * Timer.start方法会创建一个Timer.Sample实例，该实例会将当前时间记录为监控的开始时间。
     * Timer.Sample.stop方法会停止计时，然后计算当前时间与开始时间的差值，并将其记录到传入的Timer实例中。
     */
    //Prometheus Meter的注册中心，用于创建和管理Prometheus Meter实例
    private final PrometheusMeterRegistry registry;
    //http服务器，用于提供一个rest api，供Prometheus拉取数据
    private  HttpServer httpServer;
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.monitor
     * @Author: wyh
     * @Date: 2024-05-17 14:11
     * @Description: 静态内部类，用于实现单例模式
     */
    private static class SingletonHolder{
        private static final PrometheusMonitor INSTANCE = new PrometheusMonitor();
    }
    /**
     * @date: 2024-05-17 14:11
     * @description: private修饰的无参构造器，主要负责设置和启动http服务器
     * @return: null
     */
    private PrometheusMonitor(){
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        //Prometheus数据拉取api的端口号
        int port = ConfigLoader.getConfig().getPrometheusPort();
        //Prometheus数据拉取api的路径
        String path = ConfigLoader.getConfig().getPrometheusPath();
        try{
            //监听指定端口
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            /*
             * 通过匿名内部类的方式创建HttpHandler对象，并实现handle方法，向Prometheus响应数据，
             * 然后将该HttpHandler对象绑定到"/prometheus"路径上，处理该路径的请求
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
                     * 以下代码的作用就是先获取响应体的输出流对象，然后向响应体中写入数据。
                     */
                    try(OutputStream os = exchange.getResponseBody()){
                        //写入数据
                        os.write(data.getBytes());
                    }
                }
            });
            log.info("正在启动http服务器，端口: {} 路径 :{}", port, path);
            //开启一个单独的线程运行http服务器，并通过上述HttpHandler向Prometheus响应数据
            new Thread(httpServer::start, "监控数据拉取服务").start();
        }catch (IOException e){
            log.error("http服务器状态异常，端口: {} 路径: {}", port, path);
            throw new RuntimeException("http服务器状态异常", e);
        }
    }
    /**
     * @date: 2024-05-17 14:19
     * @description: 获取该类的唯一实例
     * @return: org.wyh.gateway.core.monitor.PrometheusMonitor
     */
    public static PrometheusMonitor getInstance(){
        return SingletonHolder.INSTANCE;
    }
    /**
     * @date: 2024-05-17 15:05
     * @description: 开启监控（即启动数据采集），在上下文对象创建时调用
     * @Param ctx:
     * @return: void
     */
    public void startMonitor(GatewayContext ctx){
        //Timer.start方法会创建一个Timer.Sample实例，该实例会将当前时间记录为监控的开始时间。
        Timer.Sample sample = Timer.start();
        //将上述的数据采集器实例设置到上下文参数中
        ctx.setAttribute(AttributeKey.PROMETHEUS_TIMER_SAMPLE, sample);
        log.info("成功设置请求: {}的上下文的Timer.Sample实例", ctx.getRequest().getPath());
    }
    /**
     * @date: 2024-05-17 15:06
     * @description: 结束监控（即结束数据采集），在上下文对象销毁时调用
     * @Param ctx:
     * @return: void
     */
    public void endMonitor(GatewayContext ctx){
        /*
         * 从注册中心获取对应的Timer实例（若不存在，则创建该实例），
         * 该实例会记录上下文中Timer.Sample实例收集到的数据
         * （也就是该请求对应的上下文对象的处理时间）
         */
        Timer timer = registry.timer("gateway.request",
                //以下为标签信息，用于记录数据源的详细信息
                "uniqueId", ctx.getUniqueId(),
                "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        //Timer.Sample.stop方法会停止计时，然后计算当前时间与开始时间的差值，并将其记录到传入的Timer实例中。
        long duration = ctx.getAttribute(AttributeKey.PROMETHEUS_TIMER_SAMPLE).stop(timer);
        log.info("请求: {}的上下文处理完毕，耗时: {}", ctx.getRequest().getPath(), duration);
    }
}
