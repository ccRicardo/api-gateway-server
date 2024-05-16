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
    String LOAD_BALANCE_KEY = "load_balance";
    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
    /*
     * 路由过滤器相关常量
     */
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    //由于路由过滤器必须位于除监控（后置）过滤器之外的所有过滤器之后，所以这里将order值设为int的最大值减1
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
    //下面这两个常量通常配合使用，来控制一段时间内的最大访问次数，即控制访问的流量。
    String FLOW_CTRL_LIMIT_DURATION = "duration"; 
    String FLOW_CTRL_LIMIT_PERMITS = "permits";
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
    String GRAY_IP_SET = "gray_ip_set";
    /*
     * 监控（前置）过滤器相关常量
     */
    String MONITOR_FILTER_ID = "monitor_filter";
    String MONITOR_FILTER_NAME = "monitor_filter";
    int MONITOR_FILTER_ORDER = 0;
    /*
     * 监控（后置）过滤器相关常量
     */
    String MONITOR_END_FILTER_ID = "monitor_end_filter";
    String MONITOR_END_FILTER_NAME = "monitor_end_filter";
    //由于监控（后置）过滤器必须位于过滤器来链的末尾，所以这里将order值设为int的最大值
    int MONITOR_END_FILTER_ORDER = Integer.MAX_VALUE;
//    /*
//     * （已弃用）mock过滤器相关常量
//     */
//    String MOCK_FILTER_ID = "mock_filter";
//    String MOCK_FILTER_NAME = "mock_filter";
//    //mock接口只是个用于联调测试的”模拟“接口，所以不进行真实的过滤器链处理。因此mock过滤器应该放在过滤器链的开头。
//    int MOCK_FILTER_ORDER = -1;
}
