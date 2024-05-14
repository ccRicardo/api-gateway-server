package org.wyh.gateway.core.filter.monitor;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.wyh.gateway.core.context.GatewayContext;
import org.wyh.gateway.core.filter.old_common.Filter;
import org.wyh.gateway.core.filter.old_common.FilterAspect;

import static org.wyh.gateway.common.constant.FilterConst.*;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core.filter.monitor
 * @Author: wyh
 * @Date: 2024-03-14 14:23
 * @Description: 监控（前置）过滤器，设置在过滤器链的开头，用于开启计时。
 */
@Slf4j
@FilterAspect(id=MONITOR_FILTER_ID,
              name=MONITOR_FILTER_NAME,
              order=MONITOR_FILTER_ORDER)
public class MonitorFilter implements Filter {
    /*
     * 基础知识：
     * Registry是用于创建和管理Meter实例的。Meter是一组用于收集应用中度量数据的接口，即数据收集器接口。
     * Meter实例的命名通常用"."分隔单词，以体现出层次性。
     * Meter实例还可以设置标签，以进一步指明该Meter实例采集的数据源的详细信息。
     * Timer（计时器）是Meter的一种类型，其静态内部类Timer.Sample可以统计一个方法的耗时时间。
     * Timer.start方法会创建一个Timer.Sample实例，然后该实例会开始计时。
     * Timer.Sample.stop方法则会结束计时，并将统计的时间间隔数据保存到传入的Timer对象中。
     */
    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        //Timer.Sample对象开始计时
        ctx.setTimerSample(Timer.start());
    }
}
