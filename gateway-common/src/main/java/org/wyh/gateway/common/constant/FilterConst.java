package org.wyh.gateway.common.constant;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.constant
 * @Author: wyh
 * @Date: 2024-02-22 9:27
 * @Description: 定义了一些过滤器相关的常量
 */
public interface FilterConst {
    //过滤器配置缓存的名称/id
    String FILTER_CONFIG_CACHE_ID = "filter_config_cache";
    /*
     * 负载均衡过滤器相关常量
     */
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;
    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
    /*
     * 路由过滤器相关常量
     */
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE - 1;
    /*
     * 流量控制过滤器相关常量
     */
    String FLOW_CTRL_FILTER_ID = "flow_ctrl_filter";
    String FLOW_CTRL_FILTER_NAME = "flow_ctrl_filter";
    int FLOW_CTRL_FILTER_ORDER = 50;
    String FLOW_CTRL_TYPE_PATH = "path";
    String FLOW_CTRL_TYPE_SERVICE = "service";
    String FLOW_CTRL_MODE_DISTRIBUTED = "distributed";
    String FLOW_CTRL_MODE_SINGLETON = "singleton";
    /*
     * 用户鉴权过滤器相关常量
     */
    String USER_AUTH_FILTER_ID = "user_auth_filter";
    String USER_AUTH_FILTER_NAME = "user_auth_filter";
    int USER_AUTH_FILTER_ORDER = 2;
    //携带用户jwt信息的cookie的名称
    String COOKIE_NAME = "user-jwt";
    /*
     * 灰度过滤器相关常量
     */
    String GRAY_FILTER_ID = "gray_filter";
    String GRAY_FILTER_NAME = "gray_filter";
    int GRAY_FILTER_ORDER = 1;
    //请求头中灰度标记参数的key
    String GRAY_FLAG_KEY = "gray";
}
