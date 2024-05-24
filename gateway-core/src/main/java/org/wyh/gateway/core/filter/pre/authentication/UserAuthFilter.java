package org.wyh.gateway.core.filter.pre.authentication;

import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClaims;
import io.netty.handler.codec.http.cookie.Cookie;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.exception.FilterProcessingException;
import org.wyh.gateway.common.exception.ResponseException;
import org.wyh.gateway.core.config.ConfigLoader;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.common.base.FilterAspect;
import org.wyh.gateway.core.filter.common.base.FilterConfig;
import org.wyh.gateway.core.filter.common.base.FilterType;
import org.wyh.gateway.core.filter.common.AbstractGatewayFilter;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.authentication
 * @Author: wyh
 * @Date: 2024-03-11 10:24
 * @Description: 用户鉴权过滤器，用于确认用户的身份信息（即用户id）
                 本质上来说，就是先判断用户是否已登录（也就是判断请求是否携带jwt cookie），
                 若已登录，则解析jwt获取用户id等用户信息，并写入请求对象中。（下游的后端服务就不需要重复这些步骤了）
 */
@Slf4j
@FilterAspect(id=USER_AUTH_FILTER_ID,
              name=USER_AUTH_FILTER_NAME,
              type=FilterType.PRE,
              order=USER_AUTH_FILTER_ORDER)
public class UserAuthFilter extends AbstractGatewayFilter<UserAuthFilter.Config> {
    /*
     * jwt是一个基于json的轻量级token，主要用于做用户认证（鉴权）
     * 具体过程大概如下：
     * 1、用户使用用户名和密码来请求用户登录服务（用户登录服务本身是不用做用户鉴权的）
     * 2、用户登陆服务负责验证用户的信息。验证成功（即登录成功）后，服务返回一个相应的jwt cookie。
     * 3、客户端存储该jwt cookie。之后每次在请求其他服务时，都会附上该cookie。
     * 4、服务通过jwt cookie来确认用户的身份。（jwt中包含有用户id等用户信息）
     * jwt分为三个部分，header（头部），payload（负载）和signature（签名）
     * 1、header：存储jwt的元信息
     * 2、payload：存储实际需要传递的数据，默认不加密，一般存储非敏感信息。
     * 3、signature：用指定的签名算法，结合密钥对上述两部分内容使用后得到的签名。作用是验证数据是否被篡改。
     */
    //从静态配置类中获取生成签名时使用的密钥
    private static final String SECRET_KEY = ConfigLoader.getConfig().getSecretKey();
    /**
     * @BelongsProject: my-api-gateway
     * @BelongsPackage: org.wyh.core.filter.authentication
     * @Author: wyh
     * @Date: 2024-05-16 14:36
     * @Description: （静态内部类）该过滤器的配置类。
     */
    @Setter
    @Getter
    public static class Config extends FilterConfig{
        //暂时没有定义任何属性
    }
    /**
     * @date: 2024-05-16 14:41
     * @description: 无参构造器，负责初始化父类的filterConfigClass属性
     * @return: null
     */
    public UserAuthFilter(){
        super(UserAuthFilter.Config.class);
    }
    /**
     * @date: 2024-03-11 15:25
     * @description: 从jwt中解析出用户id信息
     * @Param token:
     * @return: long
     */
    private long parseUserId(String token){
        //解析jwt
        Jwt jwt = Jwts.parser().setSigningKey(SECRET_KEY).parse(token);
        //DefaultClaims的作用是处理jwt中payload部分。其中，subject/sub声明通常用于存放用户id信息。
        return Long.parseLong(((DefaultClaims)jwt.getBody()).getSubject());
    }
    @Override
    public void doFilter(GatewayContext ctx, Object... args) throws Throwable {
        try{
            //从请求对象中获取相应的cookie
            Cookie cookie = ctx.getRequest().getCookie(COOKIE_NAME);
            //若存放jwt的cookie不存在，则说明用户未登录
            if(cookie == null){
                //抛出用户未登录异常
                throw new ResponseException(ResponseCode.UNAUTHORIZED);
            }
            String token = cookie.value();
            //解析jwt，获取用户id
            long userId = parseUserId(token);
            //设置请求对象中的userId属性，方便下游后台服务获取用户身份信息。
            ctx.getRequest().setUserId(userId);
            log.info("【用户鉴权过滤器】用户鉴权成功：{}", userId);
        }catch(Exception e){
            log.error("【用户鉴权过滤器】过滤器执行异常", e);
            throw new FilterProcessingException(USER_AUTH_FILTER_ID, ResponseCode.FILTER_PROCESSING_ERROR);
        } finally {
            /*
             * 调用父类AbstractLinkedFilter的fireNext方法，激发下一个过滤器组件
             * （这是过滤器链能够顺序执行的关键）
             */
            super.fireNext(ctx, args);
        }
    }
}
