package org.wyh.gateway.common.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-05 10:30
 * @Description: 远程交互工具类
 */
@Slf4j
public class RemotingUtil {
	//获取操作系统名称
    public static final String OS_NAME = System.getProperty("os.name");
    //标记是否是Linux系统
    private static boolean isLinuxPlatform = false;
    //标记是否是Windows系统
    private static boolean isWindowsPlatform = false;
    //静态代码块，根据操作系统名称设置相应标记的值
    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }
    /**
     * @date: 2024-01-05 10:35
     * @description: 判断是否是Windows平台
     * @return: boolean
     */
    public static boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }
    /**
     * @date: 2024-01-05 10:38
     * @description: 开启Selector 针对linux平台做了额外处理
     * @return: java.nio.channels.Selector
     */
    public static Selector openSelector() throws IOException {
        Selector result = null;
        //针对linux平台做额外处理
        if (isLinuxPlatform()) {
            try {
                /*
                 * Selector.open底层通过SelectorProvider.openSelector来创建Selector实例
                 * 在linux平台下，SelectorProvider真正的实现类就是sun.nio.ch.EPollSelectorProvider
                 */
                final Class<?> providerClazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
                if (providerClazz != null) {
                    try {
                        //调用静态方法provider获取SelectorProvider实例，然后通过openSelector方法获取Selector实例
                        final Method method = providerClazz.getMethod("provider");
                        if (method != null) {
                            final SelectorProvider selectorProvider = (SelectorProvider) method.invoke(null);
                            if (selectorProvider != null) {
                                result = selectorProvider.openSelector();
                            }
                        }
                    } catch (final Exception e) {
                        log.warn("Open ePoll Selector for linux platform exception", e);
                    }
                }
            } catch (final Exception e) {
                // ignore
            }
        }

        if (result == null) {
            result = Selector.open();
        }

        return result;
    }
    /**
     * @date: 2024-01-05 11:03
     * @description: 判断是否是linux平台
     * @return: boolean
     */
    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }
    /**
     * @date: 2024-01-05 14:13
     * @description: 获取本机的ip地址。基本过程就是遍历网络接口，获取第一个非环回，非私有（192.168开头）的地址。
     *               与NetUtils中的getLocalIp功能相似，不同点主要如下：
     *               1. NetUtils.getLocalIp能够指定ip优先级
     *               2. RemotingUtil.getLocalAddress考虑了ipv4和ipv两种版本的地址
     * @return: java.lang.String
     */
    public static String getLocalAddress() {
        try {
            // Traversal Network interface to get the first non-loopback and non-private address
            /*
             * Enumeration接口和迭代器接口功能类似，都是用于遍历集合中的元素
             * NetworkInterface表示一个网络接口。通过这个类的getNetworkInterfaces方法可以获取本机所有的
             * 物理网络接口和虚拟机等软件利用物理接口创建的逻辑网络接口的信息
             */
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> ipv4Result = new ArrayList<String>();
            ArrayList<String> ipv6Result = new ArrayList<String>();
            while (enumeration.hasMoreElements()) {
                final NetworkInterface networkInterface = enumeration.nextElement();
                /*
                 * InetAddress类是对IP地址的封装
                 * 通过NetworkInterface类的getInetAddresses方法可以获取绑定到该接口上的所有InetAddress
                 * 注：一个网络接口可以绑定多个IP地址
                 */
                final Enumeration<InetAddress> en = networkInterface.getInetAddresses();
                while (en.hasMoreElements()) {
                    final InetAddress address = en.nextElement();
                    if (!address.isLoopbackAddress()) {
                        //与NetUtils.getLocalIp不同的是，这里考虑了Ipv6和Ipv4两种版本
                        if (address instanceof Inet6Address) {
                            ipv6Result.add(normalizeHostAddress(address));
                        } else {
                            ipv4Result.add(normalizeHostAddress(address));
                        }
                    }
                }
            }

            // prefer ipv4
            //优先使用ipv4地址
            if (!ipv4Result.isEmpty()) {
                for (String ip : ipv4Result) {
                    //判断ip地址是否为环回地址或者192.168开头的私有地址
                    if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
                        continue;
                    }

                    return ip;
                }
                //如果没有符合上述条件的ipv4地址，返回最后一个ipv4地址
                return ipv4Result.get(ipv4Result.size() - 1);
            } else if (!ipv6Result.isEmpty()) {
                //返回第一个ipv6地址
                return ipv6Result.get(0);
            }
            //If failed to find,fall back to localhost
            //如果上述步骤没有得到一个可用的ip地址，则调用InetAddress.getLocalHost获取本机ip地址
            final InetAddress localHost = InetAddress.getLocalHost();
            return normalizeHostAddress(localHost);
        } catch (Exception e) {
            log.error("Failed to obtain local address", e);
        }
        //只有当以上步骤全部失败后，才会返回null
        return null;
    }
    /**
     * @date: 2024-01-05 14:43
     * @description: 通过Channel对象，解析出远程主机的地址
     * @Param channel:
     * @return: java.lang.String
     */
    public static String parseRemoteAddress(final Channel channel) {
        if (null == channel) {
            return StringUtils.EMPTY;
        }
        final SocketAddress remote = channel.remoteAddress();
        return doParse(remote != null ? remote.toString().trim() : StringUtils.EMPTY);
    }
    /**
     * @date: 2024-01-05 14:45
     * @description: 截取地址字符串中的有效部分，即ip:port部分
     * @Param addr:
     * @return: java.lang.String
     */
    private static String doParse(String addr) {
        if (StringUtils.isBlank(addr)) {
            return StringUtils.EMPTY;
        }
        //'/'字符后面的才是有效信息部分，下面这段代码都是在找'/'的位置，然后截取子串
        if (addr.charAt(0) == '/') {
            return addr.substring(1);
        } else {
            int len = addr.length();
            for (int i = 1; i < len; ++i) {
                if (addr.charAt(i) == '/') {
                    return addr.substring(i + 1);
                }
            }
            return addr;
        }
    }
    /**
     * @date: 2024-01-05 14:51
     * @description: 标准化主机ip地址的表示
     * @Param localHost: 
     * @return: java.lang.String
     */
    public static String normalizeHostAddress(final InetAddress localHost) {
        //ipv6地址外面要加上中括号
        if (localHost instanceof Inet6Address) {
            return "[" + localHost.getHostAddress() + "]";
        } else {
            return localHost.getHostAddress();
        }
    }


    /**
     * @date: 2024-01-05 14:57
     * @description: 将地址字符串转换为对应的SocketAddress对象
     * @Param addr: 
     * @return: java.net.SocketAddress
     */
    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
        return isa;
    }

    /**
     * @date: 2024-01-05 15:04
     * @description: 将SocketAddress对象转换为相应的地址字符串
     * @Param addr:
     * @return: java.lang.String
     */
    public static String socketAddress2String(final SocketAddress addr) {
        StringBuilder sb = new StringBuilder();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) addr;
        //获取ip地址
        sb.append(inetSocketAddress.getAddress().getHostAddress());
        sb.append(":");
        //获取端口号
        sb.append(inetSocketAddress.getPort());
        return sb.toString();
    }
    /**
     * @date: 2024-01-05 15:08
     * @description: 连接远程服务器，超时时间默认为5000ms
     * @Param remote: 
     * @return: java.nio.channels.SocketChannel
     */
    public static SocketChannel connect(SocketAddress remote) {
        return connect(remote, 1000 * 5);
    }
    /**
     * @date: 2024-01-05 15:09
     * @description: 连接远程服务器，可指定超时时间。设置为0表示无限超时时间
     * @Param remote:
     * @Param timeoutMillis:
     * @return: java.nio.channels.SocketChannel
     */
    public static SocketChannel connect(SocketAddress remote, final int timeoutMillis) {
        SocketChannel sc = null;
        //
        try {
            //创建SocketChannel对象
            sc = SocketChannel.open();
            //将SocketChannel设置为阻塞模式
            sc.configureBlocking(true);
            //设置Socket关闭时的行为，禁用SO_LINGER选项
            sc.socket().setSoLinger(false, -1);
            //关闭Nagle算法
            sc.socket().setTcpNoDelay(true);
            //设置Socket接收缓冲区大小
            sc.socket().setReceiveBufferSize(1024 * 64);
            //设置Socket发送缓冲区大小
            sc.socket().setSendBufferSize(1024 * 64);
            //建立连接
            sc.socket().connect(remote, timeoutMillis);
            //将SocketChannel设置为非阻塞模式
            sc.configureBlocking(false);
            return sc;
        } catch (Exception e) {
            if (sc != null) {
                try {
                    sc.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        return null;
    }
    /**
     * @date: 2024-01-05 15:31
     * @description: 关闭指定的channel
     * @Param channel:
     * @return: void
     */
    public static void closeChannel(Channel channel) {
        //获取字符串形式的远程主机地址（ip+port）
        final String addrRemote = RemotingHelper.parseChannelRemoteAddr(channel);
        //关闭Channel，并添加一个监听器来处理关闭后的操作
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            //Channel关闭后，该对象的operationComplete方法会被自动调用
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                    future.isSuccess());
            }
        });
    }

}
