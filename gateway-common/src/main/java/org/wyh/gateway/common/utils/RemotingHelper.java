package org.wyh.gateway.common.utils;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-05 9:17
 * @Description: 远程交互支持/辅助类
 */
public class RemotingHelper {
    /**
     * @date: 2024-01-05 9:36
     * @description: 获取异常的简单描述信息
     * @Param e:
     * @return: java.lang.String
     */
    public static String exceptionSimpleDesc(final Throwable e) {
        //StringBuffer和StringBuilder相似，都是用于字符串修改，区别在于前者是线程安全，后者不是
        StringBuffer sb = new StringBuffer();
        if (e != null) {
            sb.append(e.toString());
            //通过getStackTrace方法可以获取方法调用栈的信息，即栈追踪StackTrace
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                //StackTrace中第一个元素是产生该异常的方法信息
                StackTraceElement elment = stackTrace[0];
                sb.append(", ");
                sb.append(elment.toString());
            }
        }

        return sb.toString();
    }
    /**
     * @date: 2024-01-05 10:10
     * @description: 将String对象转换为相应的InetSocketAddress对象
     * @Param addr:
     * @return: java.net.SocketAddress
     */
    public static SocketAddress string2SocketAddress(final String addr) {
        String[] s = addr.split(":");
        InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
        return isa;
    }
    /**
     * @date: 2024-01-05 10:13
     * @description: 通过netty channel获取远程主机的地址（ip+port）
     * @Param channel:
     * @return: java.lang.String
     */
    public static String parseChannelRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        //获取远程主机的SocketAddress（IP+端口号）
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";
        //下面这段代码的作用就是截取有效信息部分，即ip:port部分
        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }

            return addr;
        }

        return "";
    }
    /**
     * @date: 2024-01-05 10:23
     * @description: 将SocketAddress对象转换为相应的String对象
     * @Param socketAddress:
     * @return: java.lang.String
     */
    public static String parseSocketAddressAddr(SocketAddress socketAddress) {
        if (socketAddress != null) {
            final String addr = socketAddress.toString();
            //截取有效信息部分
            if (addr.length() > 0) {
                return addr.substring(1);
            }
        }
        return "";
    }

}
