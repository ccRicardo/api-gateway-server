package org.wyh.gateway.common.utils;

import java.util.concurrent.TimeUnit;
/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-04 10:32
 * @Description: 时间工具类，用于获取当前时间
 */
public final class TimeUtil {

    private static volatile long currentTimeMillis;

    static {
        currentTimeMillis = System.currentTimeMillis();
        //开启一个线程，每隔1ms更新一次currentTimeMillis
        Thread daemon = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis();
                    try {
                        TimeUnit.MILLISECONDS.sleep(1);
                    } catch (Throwable e) {

                    }
                }
            }
        });
        //设置为守护线程
        daemon.setDaemon(true);
        daemon.setName("common-fd-time-tick-thread");
        daemon.start();
    }

    public static long currentTimeMillis() {
        //此处获得的时间其实是一个时间戳
        return currentTimeMillis;
    }
}
