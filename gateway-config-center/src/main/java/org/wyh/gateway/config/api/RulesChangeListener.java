package org.wyh.gateway.config.api;

import org.wyh.gateway.common.config.Rule;

import java.util.List;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.gateway.config.center.api
 * @Author: wyh
 * @Date: 2024-01-30 14:42
 * @Description: 规则变更监听器。这是一个函数式接口，可以用lambda表达式来实现。
 */
public interface RulesChangeListener {
    /**
     * @date: 2024-01-30 14:43
     * @description: 回调函数，监听到规则配置变更时调用
     * @Param rules: 变更后的规则配置列表
     * @return: void
     */
    void onRulesChange(List<Rule> rules);
}
