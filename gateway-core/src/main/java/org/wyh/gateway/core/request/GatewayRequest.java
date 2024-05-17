package org.wyh.gateway.core.request;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.wyh.gateway.common.constant.BasicConst;
import org.wyh.gateway.common.utils.TimeUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;


/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.request
 * @Author: wyh
 * @Date: 2024-01-08 14:01
 * @Description: 请求定义类
 */
@Getter
public class GatewayRequest implements IGatewayRequest{
    //服务唯一id，格式是服务id:版本号
    private final String uniqueId;
    //请求进入网关的时间
    private final long beginTime;
    //请求使用的字符集
    private final Charset charset;
    //客户端ip
    private final String clientIp;
    //请求的主机地址，格式为ip:port
    private final String host;
    //请求的路径（port:ip后面的路径部分，不包括参数）
    private final String path;
    //请求的uri（其实就是路径加上参数）
    private final String uri;
    //请求方式
    private final HttpMethod method;
    //请求内容的格式
    private final String contentType;
    //请求头信息
    private final HttpHeaders headers;
    //参数解析器，用于解析uri，将其分为path部分和键值对参数部分
    private final QueryStringDecoder queryStringDecoder;
    //FullHttpRequest对象
    private final FullHttpRequest fullHttpRequest;
    //请求体
    @Setter
    private String body;
    //用户id。如果没有做用户鉴权处理，那么该值没有意义（为-1）。
    @Setter
    private long userId = -1;
    /*
     * 保存请求的cookie
     * 这里的cookie类型是io.netty.handler.codec.http.cookie.Cookie，
     */
    private Map<String, Cookie> cookieMap;
    //post请求参数集合。由于一个参数名可能对应多个参数值，所以使用List<String>来存储值。
    private Map<String, List<String>> postParameters;
    /*
     * 下面是一些可修改的请求参数
     * modifyHost与前面final修饰的host的不同是：
     * host是客户端原始请求中给定的主机地址，这并不一定是真正的服务主机地址
     * modifyHost则是网关通过注册中心以及负载均衡后，得到的真正的服务主机地址
     * modifyPath与path同理
     */
    //可修改的schema，默认为 http://
    private String modifySchema;
    //可修改的主机地址，用于记录服务的真实地址
    private String modifyHost;
    //可修改的路径
    private String modifyPath;
    //http请求构建器，用于构建网关的转发请求
    private final RequestBuilder requestBuilder;

    /**
     * @date: 2024-01-08 16:51
     * @description: 构造函数，初始化final修饰的属性
     * @Param uniqueId:
     * @Param charset:
     * @Param clientIp:
     * @Param host:
     * @Param uri:
     * @Param method:
     * @Param contentType:
     * @Param headers:
     * @Param fullHttpRequest:
     * @return: null
     */
    public GatewayRequest(String uniqueId, Charset charset, String clientIp,
                          String host, String uri, HttpMethod method,
                          String contentType, HttpHeaders headers, FullHttpRequest fullHttpRequest) {
        this.uniqueId = uniqueId;
        //自动获取当前时间
        this.beginTime = TimeUtil.currentTimeMillis();
        this.charset = charset;
        this.clientIp = clientIp;
        this.host = host;
        this.uri = uri;
        this.method = method;
        //创建参数解析器，解析uri
        this.queryStringDecoder = new QueryStringDecoder(uri, charset);
        //通过解析器queryStringDecoder获取uri中的path部分
        this.path = this.queryStringDecoder.path();
        this.contentType = contentType;
        this.headers = headers;
        this.fullHttpRequest = fullHttpRequest;

        this.modifyHost = this.host;
        this.modifyPath = this.path;
        this.modifySchema = BasicConst.HTTP_PREFIX_SEPARATOR;
        //创建请求构建器，并进行相应的设置
        this.requestBuilder = new RequestBuilder();
        this.requestBuilder.setMethod(this.getMethod().name());
        this.requestBuilder.setHeaders(this.getHeaders());
        //设置GET方式中的参数
        this.requestBuilder.setQueryParams(this.queryStringDecoder.parameters());
        //获取请求体的内容，放入ByteBuf中
        ByteBuf contentBuffer = fullHttpRequest.content();
        //判断请求体是否存在
        if(Objects.nonNull(contentBuffer)){
            this.requestBuilder.setBody(contentBuffer.nioBuffer());
        }


    }
    /**
     * @date: 2024-01-09 9:25
     * @description: 获取请求体
     * @return: java.lang.String
     */
    public String getBody(){
        //如果body为空（第一次获取body），则调用fullHttpRequest中的方法获取body值。body一般是一个JSON串。
        if(StringUtils.isEmpty(body)){
            body = fullHttpRequest.content().toString(charset);
        }
        return body;
    }
    /**
     * @date: 2024-01-09 9:34
     * @description: 获取请求头中的指定cookie
                     此处cookie是io.netty.handler.codec.http.cookie.Cookie类型
     * @Param name:
     * @return: io.netty.handler.codec.http.cookie.Cookie
     */
    public Cookie getCookie(String name){
        //如果cookieMap为空（第一次获取cookieMap），调用相应方法获取请求头中的所有cookie，初始化cookieMap
        if(cookieMap == null){
            cookieMap = new HashMap<>();
            //从请求头中获取字符串形式的cookie信息（有可能为空，所以这里需要先判断一下是否为null）
            String cookieStr = headers.get(HttpHeaderNames.COOKIE);
            if(cookieStr != null){
                //使用严格模式(STRICT)解码字符串形式的cookie，得到cookie集合
                Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(cookieStr);
                //将cookie依次放入cookieMap中
                cookies.forEach(cookie->cookieMap.put(cookie.name(), cookie));
            }
        }
        return cookieMap.get(name);
    }
    /**
     * @date: 2024-01-10 10:21
     * @description: 获取get请求中的指定参数
     * @Param name:
     * @return: java.util.List<java.lang.String>
     */
    public List<String> getQueryParametersMultiple(String name){
        return queryStringDecoder.parameters().get(name);
    }
    /**
     * @date: 2024-01-09 10:31
     * @description: 获取post请求中的指定参数
     * @Param name:
     * @return: java.util.List<java.lang.String>
     */
    public List<String> getPostParametersMultiple(String name){
        /*
         * post请求提交参数的方式，或者说请求体中参数的格式（即content-type）主要分为三种
         * application/x-www-form-urlencoded：
         * 表单提交的默认方式，参数以键值对的形式编码（形如key1=val1，key2=val2）
         * application/json：
         * 以json形式提交参数，参数是一个JSON串
         * multipart/form-data：
         * 表单提交的一种方式，一般是用于上传文件，参数通过boundary分隔
         *
         * 该方法支持以上三种类型参数的解析
         * 但是，此处并不考虑multipart/form-data中的文件上传部分
         */
        if(postParameters == null){
            //解析application/x-www-form-urlencoded或multipart/form-data类型的post请求参数
            if(isFormPost()){
//                //对于第一种表单类型的post请求，其body以key=value形式编码参数，因此可以使用QueryStringDecoder来解析
//                QueryStringDecoder paramDecoder = new QueryStringDecoder(body, false);
//                postParameters = paramDecoder.parameters();
                /*
                 * 由于要解析两种表单类型的post请求，因此只能使用HttpPostRequestDecoder
                 * HttpDataFactory的作用是将body中的数据解析为httpData，httpData有两种类型，Attribute和FileUpload
                 * 前者表示普通属性（key=value形式），后者表示上传的文件。此方法只考虑属性，不考虑文件上传。
                 * DefaultHttpDataFactory中的minSize是一个阈值，小于该阈值则使用内存存储，大于则使用磁盘存储
                 */
                HttpPostRequestDecoder paramDecoder = new HttpPostRequestDecoder(
                        new DefaultHttpDataFactory(charset), fullHttpRequest, charset);
                //通过getBodyHttpDatas方法获取所有的HttpData（HttpData就是对body中数据的一个抽象）
                for (InterfaceHttpData data : paramDecoder.getBodyHttpDatas()) {
                    //不考虑文件上传
                    if(data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute){
                        Attribute attr = (Attribute) data;
                        try {
                            postParameters.put(attr.getName(), Lists.newArrayList(attr.getValue()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                //解析application/json类型的post参数
            }else if(isJsonPost()){
                //这里不能直接使用this.body，因为如果之前没有调用过getBody()，this.body为null
                String body = getBody();
                //使用fastjson中的相应方法将json串转换为map
                Map<String, Object> postParams = (Map<String, Object>)JSON.parse(body);
                if(postParams != null){
                    postParams.forEach((key, value) ->
                            postParameters.put(key, Lists.newArrayList(value.toString())));
                }
            }else{
                //不支持其他类型参数的解析
                return null;
            }
        }
        return postParameters.get(name);
    }
    /**
     * @date: 2024-01-09 10:35
     * @description: 判断post请求是否以表单形式
                    （application/x-www-form-urlencoded或multipart/form-data）提交数据
     * @return: boolean
     */
    public  boolean isFormPost(){
        return HttpMethod.POST.equals(method) &&
                (contentType.startsWith(HttpHeaderValues.FORM_DATA.toString()) ||
                        contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString()));

    }
    /**
     * @date: 2024-01-09 10:35
     * @description: 判断post请求是否以json形式（content-type为application/json）提交数据
     * @return: boolean
     */
    public  boolean isJsonPost(){
        return HttpMethod.POST.equals(method) &&
                contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString());
    }
    @Override
    public void setModifyHost(String host) {
        this.modifyHost = host;
    }

    @Override
    public String getModifyHost() {
        return modifyHost;
    }

    @Override
    public void setModifyPath(String path) {
        this.modifyPath = path;
    }

    @Override
    public String getModifyPath() {
        return modifyPath;
    }

    @Override
    public void addHeader(CharSequence name, String value) {
        requestBuilder.addHeader(name, value);
    }

    @Override
    public void setHeader(CharSequence name, String value) {
        requestBuilder.setHeader(name, value);
    }

    @Override
    public void addQueryParam(String name, String value) {
        requestBuilder.addQueryParam(name, value);
    }

    @Override
    public void addFormParam(String name, String value) {
        if(isFormPost()){
            requestBuilder.addFormParam(name, value);
        }
    }

    @Override
    public void setRequestTimeout(int requestTimeout) {
        requestBuilder.setRequestTimeout(requestTimeout);
    }

    @Override
    public String getFinalUrl() {
        //根据网关解析的服务实例的真实地址和请求中的路径，构造最终的请求url。
        return modifySchema + modifyHost + modifyPath;
    }
    @Override
    public Request build() {
        requestBuilder.setUrl(getFinalUrl());
        //若用户id有效（即该请求经过了用户鉴权处理），则在请求头中进行设置，这样后台服务就能直接获取到用户身份信息。
        if(userId != -1){
            requestBuilder.addHeader("userId", String.valueOf(userId));
        }
        return requestBuilder.build();
    }
}
