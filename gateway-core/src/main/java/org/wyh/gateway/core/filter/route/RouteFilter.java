package org.wyh.gateway.core.filter.route;

import com.netflix.hystrix.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.BaseException;
import org.wyh.gateway.common.exception.ConnectException;
import org.wyh.gateway.common.exception.FilterProcessingException;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.helper.AsyncHttpHelper;
import org.wyh.gateway.core.helper.ResponseHelper;
import org.wyh.gateway.core.response.GatewayResponse;

import javax.xml.crypto.dsig.spec.XPathType;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.route
 * @Author: wyh
 * @Date: 2024-05-22 19:15
 * @Description: 路由过滤器，负责发送请求给目标服务，并接收和处理其响应结果。
                 （在此之前，已经通过负载均衡过滤器确定了要访问的服务实例）
                 该过滤器实际上是通过AsyncHttpClient框架向目标异步发送http请求的。
                 注意：发送请求和接收响应的工作都是在工作线程中执行的，因此complete方法不是运行在主线程中的。
 */
@Slf4j
@FilterAspect(id=ROUTER_FILTER_ID,
              name=ROUTER_FILTER_NAME,
              type= FilterType.ROUTE,
              order=ROUTER_FILTER_ORDER)
public class RouteFilter extends AbstractGatewayFilter<RouteFilter.Config> {
    // TODO: 2024-05-22 源码还在相应设置了上下文的RS，RR等属性
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
        // TODO: 2024-05-22 补充完整，并修改rule
        //是否使用hystrix进行熔断降级。只有启用了hystrix，以下与hystrix相关的配置才会生效。
        private boolean useHystrix;
        //HystrixCommand的执行超时时间。若HystrixCommand执行耗时超过该时间，便会进入降级逻辑。
        private int timeoutInMilliseconds;
        //是否强制打开断路器（即让断路器始终处于熔断状态）
        private boolean breakerForceOpen = false;
        //启用断路器的请求量阈值。当窗口时间内的请求数量大于该阈值时，才启用断路器功能（建议保持默认值）
        private int requestVolumeThreshold = 20;
        //断路器熔断/打开的错误率阈值。当窗口时间内请求的错误率高于该阈值时，才对服务进行熔断（建议保持默认值）
        private int errorThresholdPercentage = 50;
        //线程池（此线程池指的是该命令对应分组所使用的线程池）的核心线程数
        private int threadPoolCoreSize;
        //是否开启降级回退逻辑
        private boolean fallbackEnabled;
        //降级回退逻辑中的响应消息
        private String fallbackMessage;
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
        try{
            //args[0]其实就是该过滤器的配置类实例
            RouteFilter.Config filterConfig = (RouteFilter.Config)args[0];
            //判断是否使用hystrix进行熔断降级
            if(filterConfig.isUseHystrix()){
                doRouteWithHystrix(ctx, filterConfig);
            }else{
                doRoute(ctx, filterConfig);
            }
        }catch (Exception e){
            throw new RuntimeException("【路由过滤器】过滤器执行异常", e);
        }
        /*
         * 注意，此处不要设置finally子句，没有意义。
         * 需要在处理响应的工作线程中去设置。
         */
    }
    /**
     * @date: 2024-05-23 14:01
     * @description: 真正执行路由过滤（负责异步发送请求和处理响应）
     * @Param ctx:
     * @Param filterConfig:
     * @return: java.util.concurrent.CompletableFuture<org.asynchttpclient.Response>
     */
    private CompletableFuture<Response> doRoute(GatewayContext ctx, RouteFilter.Config filterConfig){
        //构建AsyncHttpClient的请求对象
        Request request = ctx.getRequest().build();
        /*
         * 通过AsyncHttpHelper封装的AsyncHttpClient发送异步http请求。
         * 注意：发送请求，和后续的响应处理，都是在工作线程（准确来说是AsyncHttpClient线程池中的线程）中执行的。
         */
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        //单/双异步模式的标识。单异步使用whenComplete，双异步使用whenCompleteAsync
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        if(whenComplete){
            //whenComplete使用同一个线程来负责发送请求和接收响应
            future.whenComplete(((response, throwable) -> {
                //调用complete完成响应的处理，并激发下一个过滤器组件
                complete(request, response, throwable, ctx, filterConfig);
            }));
        }else{
            //whenCompleteAsync使用一个线程来发送请求，另一个线程来接收响应
            future.whenCompleteAsync(((response, throwable) -> {
                //调用complete完成响应的处理，并激发下一个过滤器组件
                complete(request, response, throwable, ctx, filterConfig);
            }));
        }
        //返回该异步请求对象
        return future;
    }

    /**
     * @date: 2024-05-23 14:23
     * @description: 执行路由过滤，并附带hystrix进行熔断降级
     * @Param ctx:
     * @Param filterConfig:
     * @return: void
     */
    // TODO: 2024-05-24 hystrix与异步似乎是相冲突的。
    private void doRouteWithHystrix(GatewayContext ctx, RouteFilter.Config filterConfig){
        /*
         * Hystrix基础知识：
         * Hystrix使用的是命令模式，即事先将服务调用逻辑（此处是route方法）封装到命令对象HystrixCommand中，
         * 之后执行该命令对象即可完成服务调用操作。
         * HystrixCommand中有两个需要重写的方法：run和getFallback。
         * 前者包含/定义了服务调用的主要逻辑，后者包含/定义了服务调用失败时的降级回退逻辑。
         * HystrixCommand.Setter，HystrixCommandProperties.Setter以及其他类似Setter的作用都是
         * 设置/配置HystrixCommand的相关属性，没有显式设置的属性均采用默认的配置值。
         */
        //通过Setter配置HystrixCommand实例的相关属性
        HystrixCommand.Setter setter = HystrixCommand.Setter
                /*
                 * 将HystrixCommand分组的key设置为访问服务的uniqueId
                 * 一个HystrixCommand分组对应一个服务，一个线程池（默认使用线程池隔离），以及一个断路器
                 * 一个HystrixCommand实例执行完毕后，其对应的断路器实例并不会跟着释放，
                 * 而是继续运行在内存中，对同一分组的后续HystrixCommand实例的执行进行控制和监控。
                 * （默认情况下分组key也是对应线程池的key）
                 */
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(ctx.getUniqueId()))
                /*
                 * 将HystrixCommand实例的key设为请求的访问路径
                 * 一个HystrixCommand实例对应一个/一次请求。
                 * 综上所属，可知HystrixCommand实例所属的分组，正好对应了该次请求要访问的服务
                 */
                .andCommandKey(HystrixCommandKey.Factory.asKey(ctx.getRequest().getPath()))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        //将服务隔离的方式设置为线程池隔离（也就是不同服务的调用由不同的线程池来负责执行）
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        //给HystrixCommand的执行设置超时时间
                        .withExecutionTimeoutEnabled(true)
                        //设置HystrixCommand执行超时时间的值
                        .withExecutionTimeoutInMilliseconds(filterConfig.getTimeoutInMilliseconds())
                        //HystrixCommand执行超时时中断相应线程
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        //启用断路器
                        .withCircuitBreakerEnabled(true)
                        //是否强制打开断路器
                        .withCircuitBreakerForceOpen(filterConfig.isBreakerForceOpen())
                        //设置启用断路器的请求量阈值（只有当窗口时间内的请求数量大于该阈值时，才启用断路器功能）
                        .withCircuitBreakerRequestVolumeThreshold(filterConfig.getRequestVolumeThreshold())
                        //设置断路器熔断的错误率阈值（当窗口时间内请求的错误率高于该阈值时，断路器对服务进行熔断）
                        .withCircuitBreakerErrorThresholdPercentage(filterConfig.getErrorThresholdPercentage())
                        //是否启用降级处理
                        .withFallbackEnabled(filterConfig.isFallbackEnabled()))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        //设置分组对应的线程池的核心线程数
                        .withCoreSize(filterConfig.getThreadPoolCoreSize()));
        //通过匿名内部类的方式创建HystrixCommand实例，并执行该命令实例。由于不需要返回值，所以泛型指定为Void。
        new HystrixCommand<Void>(setter){
            @Override
            protected Void run() throws Exception {
                /*
                 * 调用doRoute方法来异步发送请求和处理响应
                 * 注意：
                 * 这里要使用get方法，来阻塞等待请求的响应结果（即异步转同步）。
                 * 因为响应结果的接收是异步的，主线程中doRoute方法执行完毕并不意味着工作线程就接收到了响应结果
                 * 如果不加get方法，hystrix实际监控到的是doRoute方法的执行时间，而不是等待响应的时间。
                 * 这样会导致熔断降级功能无法正常触发
                 * 注意：该方法仍然是执行在主线程中的，而complete方法是执行在工作线程中的，两者之间并不会相互干扰。
                 */
                doRoute(ctx, filterConfig).get();
                return null;
            }

            @Override
            protected Void getFallback() {
                /*
                 * 当服务对应的断路器熔断，线程池资源不足；run方法执行超时，出现异常时，会调用该降级回退方法
                 * 触发降级时，网关应该并根据配置值设置响应消息，并写回响应
                 * 注意：该方法仍然是执行在主线程中的，而complete方法是执行在工作线程中的，两者之间并不会相互干扰。
                 */
                log.warn("【路由过滤器】请求: {} 触发降级", ctx.getRequest().getPath());
                //设置网关响应
                ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.SERVICE_UNAVAILABLE,
                        filterConfig.getFallbackMessage()));
                //设置异常信息
                ctx.setThrowable(new ResponseException(ctx.getUniqueId(), ResponseCode.SERVICE_UNAVAILABLE));
                //将该请求的网关上下文状态设置为需写回
                ctx.setWritten();
                ResponseHelper.writeResponse(ctx);
                return null;
            }
        }.execute();

    }
    /**
     * @date: 2024-05-23 14:48
     * @description: 回调方法，负责对响应结果进行处理，并且激发下一个过滤器组件
                     注意：该方法只有当AsyncHttpClient接收到响应时才会被调用，并且是在工作线程中执行
     * @Param request:
     * @Param response:
     * @Param throwable:
     * @Param ctx:
     * @Param filterConfig:
     * @return: void
     */
    private void complete(Request request, Response response, Throwable throwable,
                          GatewayContext ctx, RouteFilter.Config filterConfig) {
        try{
            //释放FullHttpRequest请求对象
            ctx.releaseRequest();
            /*
             * 检查响应结果中是否有存在异常。若有，则进一步判断异常的类型
             * 注意：此处的异常是由后台服务抛出的，而不是由网关本身抛出的
             */
            if(Objects.nonNull(throwable)){
                String url = request.getUrl();
                //超时异常
                if(throwable instanceof TimeoutException){
                    log.warn("【路由过滤器】请求: {} 耗时超过{} ms", url,
                            //获取请求超时时间的配置值
                            (request.getRequestTimeout() == 0 ?
                                    ConfigLoader.getConfig().getHttpRequestTimeout() :
                                    request.getRequestTimeout()));
                    //在上下文中设置异常信息
                    ctx.setThrowable(new ResponseException(ctx.getUniqueId(), ResponseCode.REQUEST_TIMEOUT));
                    // TODO: 2024-05-23 为啥这里不设置一下网关响应
                }else{
                    //其他异常情况
                    log.warn("【路由过滤器】请求: {} 出现响应异常");
                    ctx.setThrowable(new ConnectException(throwable, ctx.getUniqueId(),
                            url, ResponseCode.HTTP_RESPONSE_ERROR));
                }
            }else{
                //正常情况：构建然后设置网关响应对象
                ctx.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        }catch (Exception e){
            /*
             * 此处抛异常没有意义
             * 因为处理器类根本就捕获不到该方法抛出的异常
             */
            log.error("【路由过滤器】出现内部错误，无法正常处理响应");
            ctx.setThrowable(new FilterProcessingException(ROUTER_FILTER_ID, ResponseCode.FILTER_PROCESSING_ERROR));
        }finally {
            // TODO: 2024-05-27 此方法目前还没调用响应写回方法
            try{
                //接收到响应结果，需将其写回，因此将上下文状态设置为written
                ctx.setWritten();
                /*
                 * 调用父类AbstractLinkedFilter的fireNext方法，激发下一个过滤器组件
                 * （这是过滤器链能够顺序执行的关键）
                 */
                // TODO: 2024-05-23 还是同样的疑问：fireNext应该放到finally中吗
                /*
                 * 过滤器链实际上是以异步发送请求为分界点，分为两段执行的：
                 * 前段在主线程中执行，执行完毕后就结束了，并不会激发下一个过滤器组件
                 * 所以此处要负责激发下一个过滤器组件，使过滤器后端执行下去。
                 */
                super.fireNext(ctx, filterConfig);
            }catch (Throwable e){
                /*
                 * 此处抛异常没有意义
                 * 因为处理器类根本就捕获不到该方法抛出的异常
                 */
                log.error("【路由过滤器】后续过滤组件执行异常", e);
                ctx.setThrowable(new FilterProcessingException(ROUTER_FILTER_ID, ResponseCode.FILTER_PROCESSING_ERROR));
            }
        }
    }
}
