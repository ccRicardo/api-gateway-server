package org.wyh.gateway.core.filter.mock;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.old_common.Filter;
import org.wyh.gateway.core.filter.old_common.FilterAspect;
import org.wyh.gateway.core.helper.ResponseHelper;
import org.wyh.gateway.core.response.GatewayResponse;

import java.util.Map;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.mock
 * @Author: wyh
 * @Date: 2024-03-18 10:43
 * @Description: mock过滤器，用于解析规则中的mock配置。若请求的服务api是mock接口，则直接返回配置中的预设值。
                 mock是一种用于模拟真实对象的测试技术：
                 它通常能够根据需求模拟出真实对象的行为，例如返回预设值，抛出预设异常等。
                 该技术通常应用于api开发过程中，以提前与前端或其他依赖api进行联调测试，提高团队的软件开发效率。
                 mock接口只是一个用于联调测试的模拟接口，不需要进行真实的过滤器链处理。
                 因此该mock过滤器应该放在过滤器链的开头。
 */
@Slf4j
@FilterAspect(id=MOCK_FILTER_ID,
              name=MOCK_FILTER_NAME,
              order=MOCK_FILTER_ORDER)
public class MockFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //获取mock过滤器的配置信息
        Rule.FilterConfig config = ctx.getRule().getFilterConfig(MOCK_FILTER_ID);
        //如果未进行mock配置，则进行后续的过滤器链处理。否则，按照mock配置直接返回相应的响应。
        if(config == null){
            return;
        }
        //配置信息是一个json串，因此需要反序列化为一个map集合对象。
        Map<String, String> map = JSON.parseObject(config.getConfig(), Map.class);
        /*
         * mock接口配置项是一个键值对，其中：
         * 键是由该接口的请求方式（GET/POST）和请求路径拼接而成
         * 值是该接口应该响应的信息
         * 如果该条请求要访问的是一个mock接口，则该mock过滤器直接返回响应，不需要经过后续过滤器链的处理
         */
        String key = ctx.getRequest().getMethod()+" "+ctx.getRequest().getPath();
        String value = map.get(key);
        if(value != null){
            //构造响应对象，并将请求上下文状态设置为写回
            ctx.setResponse(GatewayResponse.buildGatewayResponse(value));
            ctx.setWritten();
            //写回响应对象。响应写回完成后，将请求上下文状态设置为结束
            ResponseHelper.writeResponse(ctx);
            log.info("【Mock过滤器】成功调用mock接口: {} {} {}",
                    ctx.getRequest().getMethod(), ctx.getRequest().getPath(), value);
            ctx.setTerminated();
        }
    }
}
