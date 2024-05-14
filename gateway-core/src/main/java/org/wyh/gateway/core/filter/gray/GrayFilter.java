package org.wyh.gateway.core.filter.gray;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.old_common.Filter;
import org.wyh.gateway.core.filter.old_common.FilterAspect;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
              order=GRAY_FILTER_ORDER)
public class GrayFilter implements Filter {
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        /*
         * 灰度过滤的基本流程大致如下：
         * 1、先检查http请求头中是否携带灰度标记。若携带，则直接标记为灰度流量，并跳过第二步
         * 2、再从规则配置中获取灰度ip集合，判断发出请求的客户端ip是否包含在内。若在内，则标为灰度流量。
         */
        //尝试从请求头中获取灰度标记
        String grayFlag = ctx.getRequest().getHeaders().get("gray");
        if("true".equals(grayFlag)){
            ctx.setGray(true);
            //直接返回
            return;
        }
        //获取发出请求的客户端的ip地址
        String clientIp = ctx.getRequest().getClientIp();
        log.info("【灰度过滤器】客户端ip地址：{}", clientIp);
        Rule rule = ctx.getRule();
        if(rule != null){
            //获取过滤器配置集合
            Set<Rule.FilterConfig> filterConfigs = rule.getFilterConfigs();
            Iterator<Rule.FilterConfig> iterator = filterConfigs.iterator();
            Rule.FilterConfig filterConfig;
            //遍历过滤器配置集合，找到灰度过滤器的配置信息
            while(iterator.hasNext()){
                filterConfig = iterator.next();
                if(filterConfig == null){
                    continue;
                }
                String filterId = filterConfig.getFilterId();
                //找到灰度过滤器的配置信息，并对其进行解析，获取灰度ip集合
                if(filterId.equals(GRAY_FILTER_ID)){
                    String config = filterConfig.getConfig();
                    if(StringUtils.isNotEmpty(config)){
                        /*
                         * 配置信息是一个json串，这里先将json串转换成了一个JSONObject对象
                         * 然后使用JSONObject对象中的getString方法，获取字符串形式的“灰度ip集合”配置值
                         */
                        JSONObject jsonObject = JSONObject.parseObject(config);
                        String grayIpInfo = jsonObject.getString(GRAY_IP_SET);
                        if(StringUtils.isNotEmpty(grayIpInfo)){
                            //此时的“灰度ip集合”值是一个json数组，因此要将其转换为一个set集合（先转list，再转set）
                            Set<String> grayIpSet = new HashSet(JSON.parseArray(grayIpInfo, String.class));
                            //判断当前ip是否属于灰度ip
                            if(grayIpSet.contains(clientIp)){
                                ctx.setGray(true);
                            }
                        }
                    }
                    //读取完灰度过滤器的相关配置后，退出循环
                    break;
                }
            }
        }

    }
}
