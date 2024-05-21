package org.wyh.gateway.config.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.common.config.Rule;
import org.wyh.gateway.config.api.ConfigCenter;
import org.wyh.gateway.config.api.ConfigCenterListener;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.config.center
 * @Author: wyh
 * @Date: 2024-01-31 10:33
 * @Description: 配置中心的nacos实现
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {
    // TODO: 2024-05-21 使用一个文件记录所有规则配置文件的data id 
    //配置的唯一标识符
    private static final String DATA_ID = "api-gateway";
    //配置中心服务器地址
    private String serverAddr;
    //环境
    private String env;
    //nacos核心api，用于对配置进行相关操作
    private ConfigService configService;
    @Override
    public void init(String serverAddr, String env) {
        this.serverAddr = serverAddr;
        this.env = env;
        try {
            //创建ConfigService实例，用于从nacos配置中心获取规则配置
            configService = NacosFactory.createConfigService(serverAddr);
        } catch (NacosException e) {
            log.error("【配置中心】nacos配置服务创建失败，服务器地址: {}", serverAddr);
            throw new RuntimeException("【配置中心】nacos配置服务创建失败", e);
        }
    }

    @Override
    public void subscribeRulesChange(ConfigCenterListener listener) {
        try{
            //获取网关系统的动态规则配置
            String config = configService.getConfig(DATA_ID, env, 5000);
            log.info("【配置中心】nacos配置信息: {}", config);
            /*
             * 由于规则配置是一个json串，所以这里需要将其转换为Rule对象的列表
             * parseObject方法的作用是将json串转换成一个JSONObject对象
             * getJSONArray方法的作用是从JSONObject对象中提取指定的JSONArray对象
             * toJavaList方法的作用是将JSONArray对象转换为指定类型的实例列表
             */
            List<Rule> rules = JSON.parseObject(config).getJSONArray("rules").toJavaList(Rule.class);
            //将规则配置传给（自定义）监听器
            listener.onRulesChange(rules);
            //添加nacos监听器，监听配置信息的变更
            configService.addListener(DATA_ID, env, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("【配置中心】更新nacos配置信息: {}", configInfo);
                    //将规则配置json串转换为一个Rule对象的列表
                    List<Rule> rules = JSON.parseObject(configInfo)
                            .getJSONArray("rules").toJavaList(Rule.class);
                    //将更新后的规则配置传给（自定义）监听器
                    listener.onRulesChange(rules);
                }
            });
        }catch(NacosException e){
            throw new RuntimeException(e);
        }
    }
}
