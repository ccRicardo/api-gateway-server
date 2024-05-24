package org.wyh.gateway.core.response;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import lombok.Getter;
import lombok.Setter;
import org.asynchttpclient.Response;
import org.wyh.gateway.common.enumeration.ResponseCode;
import org.wyh.gateway.common.utils.JSONUtil;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.response
 * @Author: wyh
 * @Date: 2024-01-10 14:34
 * @Description: 响应定义类
                 由于该类主要通过静态方法来构建对象
                 所以不适合设计接口
 */
@Setter
@Getter
public class GatewayResponse {
    //标准响应头，用于存放标准的响应信息
    private HttpHeaders responseHeaders = new DefaultHttpHeaders();
    //额外响应头，可根据实际需求存放额外的响应/特殊的响应信息
    private HttpHeaders extraResponseHeaders = new DefaultHttpHeaders();
    //响应内容（响应体）
    private String content;
    //响应状态码
    private HttpResponseStatus httpResponseStatus;
    //AsyncHttpClient框架的异步响应结果
    private Response futureResponse;


    /**
     * @date: 2024-01-10 14:48
     * @description: 无参构造器。不推荐使用该方法构造对象。
     * @return: null
     */
    public GatewayResponse(){
        //空
    }
    /**
     * @date: 2024-01-10 15:10
     * @description: 设置标准响应头信息
     * @Param key:
     * @Param value:
     * @return: void
     */
    public void putResponseHeader(CharSequence key, CharSequence value) {
        responseHeaders.add(key, value);
    }
    /**
     * @date: 2024-01-10 15:54
     * @description: 设置额外响应头信息
     * @Param key:
     * @Param value:
     * @return: void
     */
    public void putExtraHeader(CharSequence key, CharSequence value){
        extraResponseHeaders.add(key, value);
    }
    /**
     * @date: 2024-01-10 15:57
     * @description: 根据AsyncHttpClient接收的异步响应结果来构建网关响应对象
     * @Param futureResponse: AsyncHttpClient接收的异步响应结果
     * @return: org.wyh.core.response.GatewayResponse
     */
    public static GatewayResponse buildGatewayResponse(Response futureResponse){
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setContent(futureResponse.getResponseBody());
        gatewayResponse.setHttpResponseStatus(HttpResponseStatus.valueOf(futureResponse.getStatusCode()));
        gatewayResponse.setFutureResponse(futureResponse);
        return gatewayResponse;
    }
    /**
     * @date: 2024-01-10 16:00
     * @description: 根据异常响应码，构建一个响应信息为json类型的网关响应对象
     * @Param code: 响应代码
     * @return: org.wyh.core.response.GatewayResponse
     */
    public static GatewayResponse buildGatewayResponse(ResponseCode code){
        //ObjectNode是JsonNode的子类，代表JSON的树模型，主要用于构建json串
        //JSON树模型就是将JSON数据组织成树形结构
        ObjectNode objectNode = JSONUtil.createObjectNode();
        //设置http状态码
        objectNode.put(JSONUtil.STATUS, code.getStatus().code());
        //设置本网关系统自定义的业务状态码，提供更加具体和细粒度的响应状态
        objectNode.put(JSONUtil.CODE, code.getCode());
        //设置本网关系统自定义的业务状态描述信息
        objectNode.put(JSONUtil.MESSAGE, code.getMessage());
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setHttpResponseStatus(code.getStatus());
        //设置响应头信息，主要是content-type
        gatewayResponse.putResponseHeader(HttpHeaderNames.CONTENT_TYPE,
                HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        //设置响应体。失败情况下主要是http状态码，业务状态码和业务状态信息
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));
        return gatewayResponse;
    }
    /**
     * @date: 2024-01-10 16:01
     * @description: 根据响应的数据对象，构建一个响应信息为json类型的网关响应对象。
     * @Param data: 响应数据内容
     * @return: org.wyh.core.response.GatewayResponse
     */
    public static GatewayResponse buildGatewayResponse(Object data){
        ObjectNode objectNode = JSONUtil.createObjectNode();
        //设置成功情况下的状态信息
        objectNode.put(JSONUtil.STATUS, ResponseCode.SUCCESS.getStatus().code());
        objectNode.put(JSONUtil.CODE, ResponseCode.SUCCESS.getCode());
        objectNode.put(JSONUtil.MESSAGE, ResponseCode.SUCCESS.getMessage());
        objectNode.putPOJO(JSONUtil.DATA, data);
        GatewayResponse gatewayResponse = new GatewayResponse();
        gatewayResponse.setHttpResponseStatus(ResponseCode.SUCCESS.getStatus());
        //设置响应头信息，主要是content-type
        gatewayResponse.putResponseHeader(HttpHeaderNames.CONTENT_TYPE,
                HttpHeaderValues.APPLICATION_JSON + ";charset=utf-8");
        //设置响应体。成功情况下主要是成功状态信息，以及响应返回的数据
        gatewayResponse.setContent(JSONUtil.toJSONString(objectNode));
        return gatewayResponse;
    }
}
