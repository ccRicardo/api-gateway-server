package org.wyh.gateway.core.filter.error;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.BaseException;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.response.GatewayResponse;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.error
 * @Author: wyh
 * @Date: 2024-05-28 9:15
 * @Description: 默认的异常处理过滤器，负责检查上下文中的异常信息，并向客户端写回相应的响应
                 注意：路由过滤器中的complete方法即使出现异常，也不会调用异常处理过滤器
 */
@Slf4j
@FilterAspect(id=DEFAULT_ERROR_FILTER_ID,
              name=DEFAULT_ERROR_FILTER_NAME,
              type= FilterType.ERROR,
              order=DEFAULT_ERROR_FILTER_ORDER)
public class DefaultErrorFilter extends AbstractGatewayFilter<DefaultErrorFilter.Config> {
    /**
     * @BelongsProject: api-gateway-server
     * @BelongsPackage: org.wyh.gateway.core.filter.error
     * @Author: wyh
     * @Date: 2024-05-28 9:39
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig{
        //暂时没用到
    }
    /**
     * @date: 2024-05-28 9:40
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public DefaultErrorFilter(){
        super(DefaultErrorFilter.Config.class);
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        try{
            //args[0]其实就是该过滤器的配置类实例
            DefaultErrorFilter.Config filterConfig =  (DefaultErrorFilter.Config)args[0];
            //异常对应的异常响应码，默认为ResponseCode.INTERNAL_ERROR
            ResponseCode responseCode = ResponseCode.INTERNAL_ERROR;
            Throwable throwable = ctx.getThrowable();
            //判断异常对象是否属于网关定义的异常类型
            if(throwable instanceof BaseException){
                BaseException baseException = (BaseException) throwable;
                responseCode = baseException.getCode();
            }
            //构建并设置网关响应对象
            ctx.setResponse(GatewayResponse.buildGatewayResponse(responseCode));
        }catch (Exception e){
            log.error("【异常处理过滤器】过滤器执行异常", e);
            //构建并设置网关响应对象
            ctx.setResponse(GatewayResponse.buildGatewayResponse(ResponseCode.INTERNAL_ERROR));
        }finally {
            log.info("【异常处理过滤器】正在写回异常响应");
            //需写回异常响应，因此将上下文状态设置为written
            ctx.setWritten();
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，
             * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx);
        }
    }
}
