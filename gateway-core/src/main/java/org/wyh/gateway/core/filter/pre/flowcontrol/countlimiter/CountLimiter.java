package org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter;

/**
 * @BelongsProject: api-gateway-server
 * @BelongsPackage: org.wyh.gateway.core.filter.pre.flowcontrol.countlimiter
 * @Author: wyh
 * @Date: 2024-05-16 20:36
 * @Description: 限流器的接口类
 */
public interface CountLimiter {
    /**
     * @date: 2024-05-16 20:38
     * @description: 判断本次对限流对象的访问是否需要限流。true表示不要限流（正常处理/放行）。
     * @Param value: 限流对象的值。若限流类型为path，则该值就是方法调用的路径；若为service，就是服务的唯一id。
     * @Param duration: 设定的基本时间间隔/时间窗口
     * @Param permits: 在上述时间间隔/窗口内，设定的最大允许访问次数
     * @return: boolean
     */
    boolean doFlowCtrl(String value, int duration, int permits);
}
