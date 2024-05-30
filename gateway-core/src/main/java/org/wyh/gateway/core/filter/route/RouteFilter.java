package org.wyh.gateway.core.filter.route;

import com.netflix.hystrix.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.ConnectException;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.helper.AsyncHttpHelper;
import org.wyh.gateway.core.response.GatewayResponse;

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
                 注意：发送请求和接收响应的工作都是在AsyncHttpClient的线程池中执行的。
                 而处理响应的complete方法具体执行在哪个线程，取决于是否使用hystrix
                 若使用hystrix，则complete执行在disruptor消费者线程池，否则执行在AsyncHttpClient线程池。
 */
@Slf4j
@FilterAspect(id=ROUTE_FILTER_ID,
              name=ROUTE_FILTER_NAME,
              type= FilterType.ROUTE,
              order=ROUTE_FILTER_ORDER)
public class RouteFilter extends AbstractGatewayFilter<RouteFilter.Config> {
    // TODO: 2024-05-22 源码还在相应设置了上下文的RS，RR等属性
    //异常消息
    private static final String EXCEPTION_MSG = "【路由过滤器】执行异常: ";
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
        private boolean fallbackEnabled = true;
        //降级回退逻辑中的响应消息
        private String fallbackMessage = "降级消息";
    }
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.route
     * @Author: wyh
     * @Date: 2024-05-27 18:55
     * @Description: AsyncHttpClient响应的包装类，
                     包含了AsyncHttpClient接受的响应对象，请求过程中的异常和响应数据
                     （其中只有一个属性不为空）
     */
    @Setter
    @Getter
    public static class ResponseWrapper{
        //AsyncHttpClient接收到的响应对象
        private Response response = null;
        //AsyncHttpClient请求过程中的异常
        private Throwable throwable = null;
        //响应数据（主要用于保存降级回退时的返回消息）
        private Object data = null;
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
            //构建AsyncHttpClient的请求对象
            Request request = ctx.getRequest().build();
            /*
             * 通过AsyncHttpHelper封装的AsyncHttpClient发送异步http请求。
             * 注意：发送请求，和后续的响应接收，都是在AsyncHttpClient线程池中执行的。
             */
            CompletableFuture<Response> futureResponse = AsyncHttpHelper.getInstance().executeRequest(request);
            /*
             * 根据过滤器配置判断是否要使用hystrix进行熔断降级
             * 注意：
             * 若不使用hystrix，则需要将complete设置为异步回调方法。这种情况下，complete运行在AsyncHttpClient线程池中。
             * 若使用hystrix，则在其run方法中，需要阻塞等待请求的响应结果，然后再调用complete方法。
             * 这种情况下，complete运行在disruptor的消费者线程池中
             */
            if(!filterConfig.isUseHystrix()){
                //单/双异步模式的标识。单异步使用whenComplete，双异步使用whenCompleteAsync
                boolean whenComplete = ConfigLoader.getConfig().isWhenComplete();
                //若请求成功，则throwable为空；若请求失败，则response为空。
                if(whenComplete){
                    //whenComplete使用同一个线程来负责发送请求和接收响应
                    futureResponse.whenComplete(((response, throwable) -> {
                        //调用complete完成响应的处理，并激发下一个过滤器组件
                        complete(request, response, throwable, ctx, null);
                    }));
                }else{
                    //whenCompleteAsync使用一个线程来发送请求，另一个线程来接收响应
                    futureResponse.whenCompleteAsync(((response, throwable) -> {
                        //调用complete完成响应的处理，并激发下一个过滤器组件
                        complete(request, response, throwable, ctx, null);
                    }));
                }
            }else{
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
                     * 将HystrixCommand分组的key设置为要访问的服务实例的id/真实地址
                     * 一个HystrixCommand分组对应一个服务实例，一个线程池（默认使用线程池隔离），以及一个断路器
                     * 一个HystrixCommand实例执行完毕后，其对应的断路器实例并不会跟着释放，
                     * 而是继续运行在内存中，对同一分组的后续HystrixCommand实例的执行进行控制和监控。
                     * （默认情况下分组key也是对应线程池的key）
                     */
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(ctx.getAttribute(AttributeKey.SELECTED_INSTANCE).getServiceInstanceId()))
                    /*
                     * 将HystrixCommand实例的key设为请求的访问路径
                     * 一个HystrixCommand实例对应一个/一次请求。
                     * 综上所属，可知HystrixCommand实例所属的分组，正好对应了该次请求要访问的服务实例
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
                //通过匿名内部类的方式创建HystrixCommand实例，并执行该命令实例。
                HystrixCommand<ResponseWrapper> hystrixCommand = new HystrixCommand<>(setter){
                    @Override
                    protected ResponseWrapper run() throws Exception {
                        ResponseWrapper wrapper = new ResponseWrapper();
                        /*
                         * 使用get方法，来阻塞等待请求的响应结果（即异步转同步）。
                         * 若请求失败，该方法会返回一个异常对象
                         */
                        try{
                            log.info("【路由过滤器】正在执行hystrix command run方法");
                            Response response = futureResponse.get();
                            wrapper.setResponse(response);
                        }catch (Throwable t){
                            wrapper.setThrowable(t);
                        }finally{
                            return wrapper;
                        }
                    }
                    @Override
                    protected ResponseWrapper getFallback() {
                        ResponseWrapper wrapper = new ResponseWrapper();
                        //当服务对应的断路器熔断，线程池资源不足；run方法执行超时，出现异常时，会调用该降级回退方法
                        log.error("【路由过滤器】请求: {} 触发降级: {}",
                                ctx.getRequest().getPath(), filterConfig.getFallbackMessage());
                        wrapper.setData(filterConfig.getFallbackMessage());
                        //注：降级回退默认不属于异常
                        return wrapper;
                    }
                };
                ResponseWrapper responseWrapper = hystrixCommand.execute();
                //调用complete方法完成响应的处理，并激发下一个过滤器组件
                complete(request, responseWrapper.getResponse(),
                        responseWrapper.getThrowable(), ctx, responseWrapper.getData());
            }
        }catch (Exception e){
            //过滤器执行过程出现异常，（正常）过滤器链执行结束，将上下文状态设置为terminated
            ctx.setTerminated();
            throw new Exception(EXCEPTION_MSG + e.getMessage(), e);
        }
        /*
         * 注意，此处不要设置finally子句，没有意义。
         * 需要在处理响应的complete方法中去设置。
         */
    }
    /**
     * @date: 2024-05-23 14:48
     * @description: 负责对响应结果进行处理，并且激发下一个过滤器组件
                     注意：该方法具体执行在哪个线程，取决于是否使用了hystrix。
                     若未使用hystrix，则该方法执行在AsyncHttpClient线程池中，并且只有当AsyncHttpClient接收到响应时才会被调用
                     此时，complete方法无法把异常抛给disruptor消费者线程中的相应方法（具体指FilterChainFactory.doFilterChain），
                     所以complete不应该向上层抛出任何异常，也不应该去执行异常过滤器链
                     由于不会去执行异常处理过滤器，所以出现异常时，需要自己构建对应的网关响应对象

     * @Param request:
     * @Param response:
     * @Param throwable:
     * @Param ctx:
     * @Param data:
     * @return: void
     */
    private void complete(Request request, Response response, Throwable throwable,
                          GatewayContext ctx, Object data) {
        try{
            log.info("接收到请求: {} 的响应结果", request.getUri());
            //释放FullHttpRequest请求对象
            ctx.releaseRequest();
            String url = request.getUrl();
            /*
             * 检查请求过程中是否存在异常。若有，则进一步判断异常的类型。（getCause的作用是获取原始异常）
             * 注意：此处的异常是请求过程中产生的，而不是由网关本身抛出的
             */
            if(Objects.nonNull(throwable)){
                //超时异常（hystrix会对原始异常进行封装，所以需要使用getCause获取原始异常）
                if(throwable.getCause() instanceof TimeoutException || throwable instanceof  TimeoutException){
                    log.warn("【路由过滤器】请求: {} 耗时超过{} ms", url,
                            //获取请求超时时间的配置值
                            (request.getRequestTimeout() == 0 ?
                                    ConfigLoader.getConfig().getHttpRequestTimeout() :
                                    request.getRequestTimeout()));
                    //在上下文中设置异常信息
                    ctx.setThrowable(new ResponseException(throwable.getCause(), ctx.getUniqueId(),
                            ResponseCode.REQUEST_TIMEOUT));
                    ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.REQUEST_TIMEOUT));
                }else{
                    //其他异常情况

                    log.warn("【路由过滤器】请求: {} 出现响应异常", url);
                    ctx.setThrowable(new ConnectException(throwable.getCause(), ctx.getUniqueId(),
                            url, ResponseCode.HTTP_RESPONSE_ERROR));
                    ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.HTTP_RESPONSE_ERROR));
                }
            }else if(Objects.nonNull(response)){
                ctx.setResponse(GatewayResponse.buildGatewayResponse(response));
            }else{
                //降级回退默认不属于异常
                ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.SUCCESS, data));
            }
        }catch (Exception e){
            //注：此处不要去执行异常过滤器链
            log.error("【路由过滤器】出现内部错误，无法正常处理响应");
            ctx.setThrowable(e);
            ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
        }finally {
            try{
                //需将响应结果写回客户端，将上下文状态设置为written
                ctx.setWritten();
                /*
                 * 调用父类AbstractLinkedFilter的fireNext方法，
                 * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
                 * （这是过滤器链能够顺序执行的关键）
                 */
                super.fireNext(ctx);
            }catch (Throwable t){
                /*
                 * 实际上，后续的写回操作和后置过滤器都不会向上抛异常，
                 * 所以理论上该分支永远也不会执行
                 */
                log.error("【路由过滤器】fireNext执行异常: {}", t.getMessage());
                ctx.setThrowable(t);
            }
        }
    }
}
