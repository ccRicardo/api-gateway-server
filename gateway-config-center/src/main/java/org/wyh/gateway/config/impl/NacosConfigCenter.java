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
 * @Description: 配置中心的nacos实现（利用了nacos的配置服务）
                 每个配置文件存放一个规则对象，其data id（即文件名）默认等于规则id
                 RULES_DATA_ID对应的文件记录了所有规则配置文件的data id
 */
@Slf4j
public class NacosConfigCenter implements ConfigCenter {
    //该data id对应的文件（以json数组的形式）记录了所有规则配置文件的data id
    private static final String RULE_DATA_IDS = "rule_data_ids";
    //配置中心的服务器地址
    private String serverAddr;
    //配置所属环境（本质上是作为nacos配置列表中的组名使用）
    private String env;
    //nacos配置服务实例，提供了一系列api，用于对配置信息进行相关操作
    private ConfigService configService;
    //调用nacos ConfigService api的超时时间
    private long timeout = 5000;
    @Override
    public void init(String serverAddr, String env) {
        this.serverAddr = serverAddr;
        this.env = env;
        try {
            //连接指定的nacos服务器，创建ConfigService实例
            configService = NacosFactory.createConfigService(serverAddr);
        } catch (NacosException e) {
            log.error("【配置中心】nacos配置服务创建失败，服务器地址: {}", serverAddr);
            throw new RuntimeException("【配置中心】nacos配置服务创建失败", e);
        }
    }

    @Override
    public void subscribeRulesChange(ConfigCenterListener listener) {
        try{
            //获取指定配置文件的内容，该内容就是网关系统所有规则配置文件的data id
            String ruleDataIdsStr = configService.getConfig(RULE_DATA_IDS, env, timeout);
            log.info("【配置中心】当前已配置的规则: {}", ruleDataIdsStr);
            //将上述json串形式的配置内容转换为java字符串列表形式
            List<String> ruleDataIds = JSON.parseArray(ruleDataIdsStr, String.class);
            /*
             * 遍历ruleDataIds中每一个data id对应的规则配置文件，
             * 并将其内容转换成相应的Rule对象，然后传给配置中心监听器实例
             * （该监听器实例由网关传入，所以规则对象最后会传入到网关中）
             * 此外，还需给每个规则配置文件添加nacos监听器
             * 注意：修改规则配置文件的内容不需要重启网关，但是添加新的规则配置文件需要重启网关！
             */
            for (String ruleDataId : ruleDataIds) {
                String ruleStr = configService.getConfig(ruleDataId, env, timeout);
                //将json串形式的规则转换成相应的Rule对象
                Rule rule = JSON.parseObject(ruleStr, Rule.class);
                //将该规则对象传给配置中心监听器实例（监听器再将其传给网关）
                listener.onRulesChange(rule);
                //给该规则配置文件添加nacos监听器
                configService.addListener(ruleDataId, env, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        //由于监听器的处理逻辑比较简单，所以没必要定义Executor
                        return null;
                    }
                    @Override
                    public void receiveConfigInfo(String newConfigInfo) {
                        //当指定配置信息发生变更时，nacos会调用该方法。
                        Rule newRule = JSON.parseObject(newConfigInfo, Rule.class);
                        log.info("【配置中心】规则: {} 的配置信息变更为: {}", newRule.getRuleId(), newConfigInfo);
                        //将新的规则对象传给配置中心监听器实例
                        listener.onRulesChange(newRule);
                    }
                });
            }
        }catch(NacosException e){
            log.error("【配置中心】获取规则变更失败");
            throw new RuntimeException("【配置中心】获取规则变更失败", e);
        }
    }
}
