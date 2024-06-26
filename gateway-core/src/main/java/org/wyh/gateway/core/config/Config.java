package org.wyh.gateway.core.config;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import lombok.Data;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.core
 * @Author: wyh
 * @Date: 2024-01-15 9:52
 * @Description: 网关配置类
 */
@Data
public class Config {
    /*
     * 以下是网关系统的基础配置参数
     */
    //网关服务名称
    private String applicationName = "my-api-gateway";
    //网关服务版本号
    private String version = "1.0.0";
    //网关服务的端口号
    private int port = 8888;
    //提供给Prometheus拉取数据的api接口的端口号
    private int prometheusPort = 17777;
    //提供给Prometheus拉取数据的api接口的路径
    private String prometheusPath = "/prometheus";
    //nacos注册中心地址
    private String registryAddress = "127.0.0.1:8848";
    //nacos配置中心地址
    private String configAddress = "127.0.0.1:8848";
    //环境（其实就是命名空间）
    private String env = "dev";
    //用户鉴权过滤器中，生成jwt签名使用的密钥
    private String secretKey = "amknvqo390j0oinxbhw9u10jlg3nikbn";
    //netty server的boss eventLoopGroup的数量。Boss负责处理连接请求，Worker负责处理数据读写。
    private int serverBossEventLoopGroupNum = 1;
    //netty server的worker eventLoopGroup的数量
    private int serverWorkerEventLoopGroupNum = Runtime.getRuntime().availableProcessors() / 2 + 1;
    //AsyncHttpClient的worker eventLoopGroup的数量
    private int clientWorkerEventLoopGroupNum = Runtime.getRuntime().availableProcessors() / 2 + 1;
    //http报文中数据的最大长度
    private int maxContentLength = 64 * 1024 * 1024;
    /*
     * 单/双异步模式，默认单异步模式。
     * 实际上这里的单/双异步指的是使用whenComplete还是whenCompleteAysnc来处理请求的响应结果
     * 前者使用同一个工作线程来负责发送请求和接收响应
     * 后者使用一个线程负责发送，另一个线程负责接收
     */
    private boolean whenComplete = true;

    /*
     * 以下是AsyncHttpClient的配置参数
     */
    //连接超时时间（AsyncHttpClient连接主机时可以等待的最大毫秒数）
    private int httpConnectTimeout = 3 * 1000;
    //请求超时时间（AsyncHttpClient等待响应完成的最大毫秒数）
    private int httpRequestTimeout = 3 * 1000;
    //重定向的最大次数
    private int httpMaxRedirects = 2;
    //AsyncHttpClient可以处理的最大连接数
    private int httpMaxConnections = 10000;
    //对于每台（远程）主机，AsyncHttpClient可以处理的最大连接数
    private int httpConnectionsPerHost = 8000;
    //空闲连接超时时间（AsyncHttpClient连接池中的连接在空闲状态下可以保持多长时间）
    private int httpPooledConnectionIdleTimeout = 60 * 1000;
    /*
     * 以下是（流量控制组件中使用到的）redis的配置参数
     */
    //要使用的redis库
    private int redisDatabase = 0;
    //redis服务的地址
    private String redisHost = "127.0.0.1";
    //redis服务的端口号
    private int redisPort = 6379;
    //redis连接超时时间
    private int redisTimeout = 2000;
    //redis最大总连接数
    private int redisMaxTotal = 10;
    //redis最大空闲连接数
    private int redisMaxIdle = 10;
    //redis最小空闲连接数
    private int redisMinIdle = 3;

    /*
     * 以下是Disruptor缓冲队列的配置参数
     */
    //标识是否在netty server和processor之间使用缓冲队列
    private boolean useBufferQueue = true;
    //缓冲队列的大小
    private int bufferSize = 16 * 1024;
    //处理请求的消费者线程的数量
    private int threadCount = Runtime.getRuntime().availableProcessors() / 2 + 1;
    //消费者线程的名字前缀
    private String threadNamePrefix = "缓冲队列消费者线程";
    //等待策略，默认为阻塞（目前Disruptor只针对消费者等待生产者的情况应用了等待策略）
    private String waitStrategy = "blocking";
    /**
     * @date: 2024-03-25 14:14
     * @description: 根据配置信息获取对应的Disruptor等待策略实例
     * @return: com.lmax.disruptor.WaitStrategy
     */
    public WaitStrategy getTrueWaitStrategy(){
        switch (waitStrategy){
            //阻塞等待。
            case "blocking":
                return new BlockingWaitStrategy();
            //忙等待。消费者线程会一直循环运行，检测是否有事件可消费。
            case "busySpin":
                return new BusySpinWaitStrategy();
            //睡眠指定时间。
            case "sleeping":
                return new SleepingWaitStrategy();
            //暂不支持其他等待策略。默认使用阻塞等待。
            default:
                return new BlockingWaitStrategy();
        }
    }
}
