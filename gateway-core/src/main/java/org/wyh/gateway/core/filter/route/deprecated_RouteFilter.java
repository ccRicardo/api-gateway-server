package org.wyh.gateway.core.filter.route;

import com.netflix.hystrix.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceCrossThread;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.ConnectException;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.old_common.Filter;
import org.wyh.gateway.core.filter.old_common.FilterAspect;
import org.wyh.gateway.core.helper.AsyncHttpHelper;
import org.wyh.gateway.core.helper.ResponseHelper;
import org.wyh.gateway.core.response.GatewayResponse;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.router
 * @Author: wyh
 * @Date: 2024-02-22 14:01
 * @Description: 路由过滤器，负责将请求（异步地）发送给相应的后台服务，并对响应进行相关处理。
                 在前面的负载均衡过滤器中，已经通过serviceId和负载均衡算法确定了要访问的服务实例的真实地址
                 因此，该过滤器本质上就是通过AsyncHttpClient框架向该服务实例发送异步http请求。（目前只支持http协议）
                 此外，过滤器还包括请求重发和服务熔断部分的内容。
 */
@Slf4j
@FilterAspect(id=ROUTER_FILTER_ID,
              name=ROUTER_FILTER_NAME,
              order=ROUTER_FILTER_ORDER)
public class deprecated_RouteFilter implements Filter {
    //访问日志的日志记录器
    private static Logger accessLog = LoggerFactory.getLogger("accessLog");
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.router
     * @Author: wyh
     * @Date: 2024-03-29 16:05
     * @Description: 内部类。
                     主要作用是封装complete方法，供route方法中的异步回调函数whenComplete使用。
                     Q：为什么要将complete方法封装为一个类供route的回调函数使用？不能直接使用吗？
                     A：complete方法可以直接被route种的回调函数调用。
                     这样的话，该方法就会在回调线程（或者说辅助线程）中执行。
                     这种情况下，就不能再使用@Trace来跟踪该方法的调用信息，因为@Trace不能跨线程跟踪
                     而@TraceCrossThread虽然能够跨线程跟踪，但是只能作用在类上
                     因此，只能用一个内部类去封装complete方法。

     */
    @TraceCrossThread
    public class CompleteBiConsumer implements BiConsumer<Response, Throwable>{
        //请求
        private Request request;
        //响应
        private Response response;
        //异常
        private Throwable throwable;
        //上下文
        private GatewayContext ctx;
        //Hystrix熔断器配置
        private Optional<Rule.HystrixConfig> hystrixConfig;
        /**
         * @date: 2024-03-29 16:35
         * @description: 有参构造器，用于初始化剩余属性
         * @Param request:
         * @Param ctx:
         * @Param hystrixConfig:
         * @return: null
         */
        public CompleteBiConsumer(Request request, GatewayContext ctx,
                                  Optional<Rule.HystrixConfig> hystrixConfig){
            this.request = request;
            this.ctx = ctx;
            this.hystrixConfig = hystrixConfig;
        }
        /**
         * @date: 2024-03-29 16:37
         * @description: 封装complete方法。@TraceCrossThread会跟踪该方法的调用信息
         * @return: void
         */
        public void run(){
            log.info("接收到{}的响应信息", request.getUrl());
            complete(request, response, throwable, ctx, hystrixConfig);
        }
        @Override
        public void accept(Response response, Throwable throwable) {
            //该重写方法的作用是初始化响应和异常属性。
            this.response = response;
            this.throwable = throwable;
            //调用封装了complete方法的run方法
            this.run();
        }
    }
    /**
     * @date: 2024-02-22 14:28
     * @description: 请求成功时调用（准确来说，是接收异步请求的响应之前不出现异常）。对响应结果进行处理。
     * @Param request:
     * @Param response:
     * @Param throwable:
     * @Param gatewayContext:
     * @Param hystrixConfig:
     * @return: void
     */
    private void complete(Request request, Response response,
                          Throwable throwable, GatewayContext ctx,
                          Optional<Rule.HystrixConfig> hystrixConfig){
        // TODO: 2024-03-07 感觉这个资源释放的位置不对，应该在请求成功或者重试次数耗尽后再释放资源。
        //释放请求对象
        ctx.releaseRequest();
        //获取当前的请求重发/重试次数
        int currentRetryTimes = ctx.getCurrentRetryTimes();
        //获取配置的请求重发/重试次数
        int confRetryTimes = ctx.getRule().getRetryConfig().getTimes();
        //判断是否需要进行请求重发/重试
        if((throwable instanceof TimeoutException
                || throwable instanceof IOException)
                && currentRetryTimes<=confRetryTimes
                //若设置了断路器，则不启用重发机制。因为HystrixCommand中一次run方法的执行应该对应一次请求。
                && !hystrixConfig.isPresent()){
            doRetry(ctx, currentRetryTimes);
            //注意：这个return是必须要加的，不然会重复执行下面的try-catch代码块，造成不必要的开销。
            return;
        }
        try {
            //判断异步返回的响应结果中是否存在异常
            if (Objects.nonNull(throwable)) {
                String url = request.getUrl();
                //判断异常类型
                if (throwable instanceof TimeoutException) {
                    //超时异常
                    log.warn("【路由过滤器】超时异常: {}", url);
                    //设置异常信息
                    ctx.setThrowable(new ResponseException(ResponseCode.REQUEST_TIMEOUT));
                    //设置请求失败时的响应结果
                    ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                } else {
                    //其他异常默认为服务连接异常。设置异常信息。
                    ctx.setThrowable(new ConnectException(throwable,
                            ctx.getRequest().getUniqueId(),
                            url, ResponseCode.HTTP_RESPONSE_ERROR));
                    //设置请求失败时的响应结果
                    ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            } else {
                //设置请求成功时的网关响应对象
                ctx.setResponse(GatewayResponse.buildGatewayResponse(response));
            }
        } catch (Throwable t) {
            //如果执行上述代码时出现异常，则设置网关内部服务异常
            ctx.setThrowable(new ResponseException(ResponseCode.INTERNAL_ERROR));
            //设置请求失败时的响应结果
            ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
            log.error("【路由过滤器】网关内部服务异常", t);
        } finally {
            //将该请求的网关上下文状态设置为需写回
            ctx.setWritten();
            //向客户端写回响应信息
            ResponseHelper.writeResponse(ctx);
            //记录访问日志
            accessLog.info("{} {} {} {} {} {} {}",
                    System.currentTimeMillis() - ctx.getRequest().getBeginTime(),
                    ctx.getRequest().getClientIp(),
                    ctx.getRequest().getUniqueId(),
                    ctx.getRequest().getMethod(),
                    ctx.getRequest().getPath(),
                    ctx.getResponse().getHttpResponseStatus().code(),
                    ctx.getResponse().getFutureResponse().getResponseBodyAsBytes().length);
        }
    }
    /**
     * @date: 2024-02-26 14:40
     * @description: 进行请求重发/重试
     * @Param ctx:
     * @Param retryTimes:
     * @return: void
     */
    private void doRetry(GatewayContext ctx, int retryTimes){
        System.out.println("当前重试次数："+retryTimes);
        ctx.setCurrentRetryTimes(retryTimes+1);
        try{
            //请求重发/重试
            doFilter(ctx);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    /**
     * @date: 2024-03-07 10:02
     * @description: 执行路由转发，不设置熔断器。
                     基本过程就是先构建网关对服务的请求对象，然后通过AsyncHttpClient框架发送该请求。
     * @Param ctx:
     * @Param hystrixConfig:
     * @return: java.util.concurrent.CompletableFuture<org.asynchttpclient.Response>
     */
    private CompletableFuture<Response> route(GatewayContext ctx, Optional<Rule.HystrixConfig> hystrixConfig){
        //构建异步请求对象（在之前的过程中已经获取了真实的主机地址，再拼上请求路径，就获得了最终的url）
        Request request = ctx.getRequest().build();
        log.info("成功发送请求{}", request.getUrl());
        //TODO 感觉这里改成从NettyHttpClient获取AsyncHttpClient实例，再发送异步http请求比较好，不然体现不出客户端的作用
        //使用AsyncHttpClient，发送异步http请求
        CompletableFuture<Response> future = AsyncHttpHelper.getInstance().executeRequest(request);
        //单/双异步标识
        boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
        //这里可以看出，单/双异步的差异就是异步请求中的回调函数不同
        if (whenComplete) {
            //异步回调函数，使用封装了complete方法的内部类，对响应结果进行处理。
            future.whenComplete(new CompleteBiConsumer(request, ctx, hystrixConfig));
        } else {
            //异步回调函数，使用封装了complete方法的内部类，对响应结果进行处理。
            future.whenCompleteAsync(new CompleteBiConsumer(request, ctx, hystrixConfig));
        }
        return future;
    }
    /**
     * @date: 2024-03-07 10:42
     * @description: 设置熔断器，执行路由转发
     * @Param ctx:
     * @Param hystrixConfig:
     * @return: void
     */
    private void routeWithHystrix(GatewayContext ctx, Optional<Rule.HystrixConfig> hystrixConfig){
        /*
         * HystrixCommand.Setter、HystrixCommandProperties.Setter以及其他类似Setter的作用都是
         * 设置/配置HystrixCommand的相关属性
         * 没有显式设置的属性均采用默认的配置值。
         * Hystrix使用的是命令模式，即事先将服务调用逻辑封装到命令对象HystrixCommand中，
         * 之后执行该命令对象即可完成服务调用操作。
         * HystrixCommand中有两个需要重写的方法：run和getFallback。
         * 前者包含/定义了服务调用的主要逻辑，后者包含/定义了服务调用失败时的降级回退逻辑。
         */
        //通过HystrixCommand.Setter设置/配置HystrixCommand的基本属性
        HystrixCommand.Setter setter = HystrixCommand.Setter
                //将该HystrixCommand分组的key设为对应服务的uniqueId
                //一个HystrixCommand分组对应一个服务，一个线程池（默认使用线程池隔离），以及一个断路器
                //一个HystrixCommand实例执行完毕后，其对应的断路器实例并不会跟着释放，
                //而是继续运行在内存中，对同一分组后续HystrixCommand实例的执行进行控制和监控。
                // （默认情况下也是该服务线程池的key）
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(ctx.getUniqueId()))
                //一个HystrixCommand对应一个/一次请求。
                //综上所属，可知HystrixCommand所属的分组，正好对应了请求要访问的服务
                //将该HystrixCommand的key设为对应的请求路径
                .andCommandKey(HystrixCommandKey.Factory.asKey(ctx.getRequest().getPath()))
                //通过HystrixCommandProperties.Setter设置/配置HystrixCommand的行为属性
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        //将服务/依赖隔离的方式设置为线程池隔离（也就是不同服务的调用由不同的线程池来负责执行）
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        //是否给HystrixCommand的执行设置超时时间
                        .withExecutionTimeoutEnabled(true)
                        //设置超时时间的值
                        .withExecutionTimeoutInMilliseconds(hystrixConfig.get().getTimeoutInMilliseconds())
                        //HystrixCommand执行超时时是否中断相应线程
                        .withExecutionIsolationThreadInterruptOnTimeout(true)
                        //是否启用断路器
                        .withCircuitBreakerEnabled(true)
                        //设置启用断路器的请求量阈值（只有当窗口时间内的请求数量大于该阈值时，才启用断路器功能）
                        .withCircuitBreakerRequestVolumeThreshold(20)
                        //设置开启断路器的错误率阈值（当窗口时间内请求的错误率高于该阈值时，开启断路器，进行服务熔断）
                        .withCircuitBreakerErrorThresholdPercentage(50))
                //通过HystrixThreadPoolProperties.Setter设置/配置线程池相关的属性
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        //设置线程池的核心线程数（也就是常驻线程数）
                        .withCoreSize(hystrixConfig.get().getThreadCoreSize()));
        //通过匿名内部类的方式实现HystrixCommand抽象类。由于不需要返回值，所以泛型指定为Void
        new HystrixCommand<Void>(setter){
            @Override
            protected Void run() throws Exception {
                /*
                 * 调用路由转发方法，向相应服务发送异步请求。
                 * 注意，这里一定要使用get方法，将异步转同步，即同等待异步请求完成，并返回响应结果。
                 * 不然，由于异步任务的不确定性，
                 * （即不确定什么时候执行完，有可能run方法结束了，异步请求还没处理完）
                 * Hystrix框架捕获不到请求过程中的异常（也就是无法判断请求是否真正成功），无法正常工作。
                 */
                route(ctx, hystrixConfig).get();
                return null;
            }
            @Override
            protected Void getFallback() {
                /*
                 * 当断路器开启、线程池资源不足，run方法执行超时或出现异常时，会调用该降级回退方法
                 * 降级回退方法的处理流程与complete方法相似：
                 * 释放请求（此处忽略），判断异常类型（此处忽略），设置异常和响应信息，设置状态，最后返回响应
                 */
                //这里做了简化处理，只要走了降级回退逻辑，一律认为是“服务不可用”异常
                ctx.setThrowable(new ResponseException(ctx.getUniqueId(), ResponseCode.SERVICE_UNAVAILABLE));
                //设置请求失败时的响应结果
                ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.SERVICE_UNAVAILABLE));
                //将该请求的网关上下文状态设置为需写回
                ctx.setWritten();
                //向客户端写回响应信息
                ResponseHelper.writeResponse(ctx);
                System.out.println(hystrixConfig.get().getFallbackResponse());
                return null;
            }
            //执行该HystrixCommand实例，完成带断路器的路由转发
        }.execute();
    }
    /**
     * @date: 2024-03-07 10:20
     * @description: 静态方法，获取网关上下文中的熔断降级配置。
     * @Param ctx:
     * @return: java.util.Optional<org.wyh.common.config.Rule.HystrixConfig>
     */
    private static Optional<Rule.HystrixConfig> getHystrixConfig(GatewayContext ctx){
        Rule rule = ctx.getRule();
        /*
         * Optional是一个可以保存null或者非null值的容器，主要用来处理/保存值可能为null的对象。
         * Optional提供了很多有用的方法，这样就不用显式进行空值检测。
         * 以下这段代码的作用是根据请求路径找到匹配的熔断降级配置项
         * stream方法将集合对象转换为流
         * filter方法的作用是过滤，具体来说，它会遍历流中的每一个元素，并检查表达式结果是否为真。
         * 只有当结果为真，对应元素才会被保留在流中。
         * findFirst方法的作用是返回流中的第一个元素。
         */
        Optional<Rule.HystrixConfig> hystrixConfig = rule.getHystrixConfigs().stream()
                .filter(c-> StringUtils.equals(c.getPath(), ctx.getRequest().getPath()))
                .findFirst();
        return hystrixConfig;
    }
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //获取熔断降级的相关配置。Optional是一个可以保存null或者非null值的容器，主要用来处理/保存值可能为null的对象。
        Optional<Rule.HystrixConfig> hystrixConfig = getHystrixConfig(ctx);
        if(hystrixConfig.isPresent()){
            //执行带熔断器的路由方法
            routeWithHystrix(ctx, hystrixConfig);
        }else{
            //执行不带熔断器的路由方法
            route(ctx, hystrixConfig);
        }
    }
}
