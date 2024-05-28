package org.wyh.gateway.core.filter.pre.gray;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.core.context.AttributeKey;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.gray
 * @Author: wyh
 * @Date: 2024-03-12 15:07
 * @Description: 灰度过滤器，作用是对请求进行分流，区分正常流量和灰度流量。
                 灰度发布是指在api新，旧版本之间进行平滑过渡的一种发布方式。
                 在新版本api正式发布之前，可以划出小部分灰度流量先进行测试，经验证后无问题，反馈良好后便可全面上线。
                 实现灰度发布的具体策略可以分为以下三种：
                 1、基于http请求头中的灰度标记进行灰度分流
                 2、基于用户jwt令牌中的灰度标记进行灰度分流
                 3、基于客户端ip进行灰度分流
                 1与2相似，因此本系统只实现了1，3，并且将它们合并到了一个方法中
 */
@Slf4j
@FilterAspect(id=GRAY_FILTER_ID,
              name=GRAY_FILTER_NAME,
              type=FilterType.PRE,
              order=GRAY_FILTER_ORDER)
public class GrayFilter extends AbstractGatewayFilter<GrayFilter.Config> {
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.gray
     * @Author: wyh
     * @Date: 2024-05-17 09:13
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig{
        //灰度ip集合。ip之间通过";"符号分隔
        private String grayIpSet;
    }
    /**
     * @date: 2024-05-17 9:16
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public GrayFilter(){
        super(GrayFilter.Config.class);
    }

    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        /*
         * 灰度过滤的基本流程大致如下：
         * 1、先检查http请求头中是否携带灰度标记。若携带，则直接标记为灰度流量，并跳过第二步
         * 2、再从规则配置中获取灰度ip集合，判断发出请求的客户端ip是否包含在内。若在内，则标为灰度流量。
         */
        try{
            //将灰度标记设置到上下文参数中，默认为false
            ctx.setAttribute(AttributeKey.GRAY_FLAG, false);
            //尝试从请求头中获取灰度标记参数
            String grayFlag = ctx.getRequest().getHeaders().get(GRAY_FLAG_KEY);
            if("true".equals(grayFlag)){
                ctx.setAttribute(AttributeKey.GRAY_FLAG, true);
            }else{
                //获取发出请求的客户端的ip地址
                String clientIp = ctx.getRequest().getClientIp();
                log.info("【灰度过滤器】客户端ip地址: {}", clientIp);
                //args[0]其实就是该过滤器的配置类实例
                GrayFilter.Config filterConfig = (GrayFilter.Config) args[0];
                if(filterConfig == null){
                    log.warn("【灰度过滤器】未设置配置信息");
                }else{
                    String grayIpSetStr = filterConfig.getGrayIpSet();
                    if(StringUtils.isNotEmpty(grayIpSetStr)){
                        String[] grapIpArray = grayIpSetStr.split(";");
                        // TODO: 2024-05-17 ip地址匹配这一块感觉要重做，并且最好再加一个ip格式验证
                        //判断当前ip是否在灰度ip数组中
                        for (String grayIp : grapIpArray) {
                            if(clientIp == grayIp){
                                ctx.setAttribute(AttributeKey.GRAY_FLAG, true);
                                break;
                            }
                        }
                    }
                }
            }
            if(ctx.getAttribute(AttributeKey.GRAY_FLAG)){
                log.info("【灰度过滤器】当前流量属于灰度流量");
            }
        }catch (Exception e){
            log.error("【灰度过滤器】过滤器执行异常", e);
            //过滤器执行过程出现异常，（正常）过滤器链执行结束，将上下文状态设置为terminated
            ctx.setTerminated();
            throw e;
        }finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，
             * 根据上下文的当前状态做出相关操作，然后触发/激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx);
        }

    }
}
