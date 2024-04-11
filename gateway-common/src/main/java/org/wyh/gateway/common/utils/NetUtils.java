package org.wyh.gateway.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @BelongsProject: my-api-gateway
 * @BelongsPackage: org.wyh.common.utils
 * @Author: wyh
 * @Date: 2024-01-04 10:43
 * @Description: 网络工具类
 */
public class NetUtils {

    /**
     * 	IPv4地址的正则表达式
     */
    public static Pattern pattern =
            Pattern.compile("(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\."
                    + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})\\." + "(1\\d{1,2}|2[0-4]\\d|25[0-5]|\\d{1,2})");
    /**
     * @date: 2024-01-04 10:46
     * @description: 校验ipList中的ip是否全部合法
     * @Param ipList:
     * @return: boolean
     */
    public static boolean validate(List<String> ipList) {
        if (null != ipList) {
            for (String ip : ipList) {
                if (!pattern.matcher(ip).matches()) {
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * @date: 2024-01-04 10:52
     * @description: 对带子网掩码的ip进行合法性校验
     * @Param ipList:
     * @return: boolean
     */
    public static boolean validateRule(List<String> ipList) {
        if (null != ipList) {
            for (String ip : ipList) {
                int nMaskBits = 1;
                if (ip.indexOf('/') > 0) {
                    //分离ip地址和掩码位数
                    String[] addressAndMask = StringUtils.split(ip, "/");
                    ip = addressAndMask[0];
                    nMaskBits = Integer.parseInt(addressAndMask[1]);
                }
                //验证ip是否合法，以及掩码位数是否在[0,32]范围内
                if (!pattern.matcher(ip).matches() ||
                        nMaskBits < 0 ||nMaskBits>32) {
                    return false;
                }
            }
        }
        return true;

    }
    /**
     * @date: 2024-01-04 13:54
     * @description: 校验是否是http url
     * @Param urls:
     * @return: boolean
     */
    public static boolean isHttpUrl(String urls) {
        //设置http url的正则表达式
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";
        Pattern pat = Pattern.compile(regex.trim());
        Matcher mat = pat.matcher(urls.trim());
        return mat.matches();
    }
    /**
     * @date: 2024-01-04 14:06
     * @description: 标准化地址（地址由ip:port构成）
     * @Param address:
     * @return: java.lang.String
     */
    public static String normalizeAddress(String address) {
        //以":"为分隔符拆分字符串
        String[] blocks = address.split("[:]");
        if (blocks.length > 2) {
            throw new IllegalArgumentException(address + " is invalid");
        }
        //主机号
        String host = blocks[0];
        //端口号默认使用80
        int port = 80;
        if (blocks.length > 1) {
            port = Integer.valueOf(blocks[1]);
        } else {
            address += ":" + port; // use default 80
        }
        String serverAddr = String.format("%s:%d", host, port);
        return serverAddr;
    }
    /**
     * @date: 2024-01-04 14:12
     * @description: 获取本机地址
     * @Param address:
     * @return: java.lang.String
     */
    public static String getLocalAddress(String address) {
        //以":"为分隔符拆分字符串
        String[] blocks = address.split("[:]");
        if (blocks.length != 2) {
            throw new IllegalArgumentException(address + " is invalid address");
        }
        //主机号
        String host = blocks[0];
        //端口号
        int port = Integer.valueOf(blocks[1]);
        //主机号为0.0.0.0，表示未指定的地址，需要通过该工具类中的getLocalIp方法获取具体的ip地址
        if ("0.0.0.0".equals(host)) {
            return String.format("%s:%d", NetUtils.getLocalIp(), port);
        }
        return address;
    }
    /**
     * @date: 2024-01-04 14:20
     * @description: 找到与指定ip匹配的第一个前缀的索引
     * @Param ip: 指定ip
     * @Param prefix: 前缀数组
     * @return: int 与指定ip匹配的第一个前缀的索引，若不存在，返回-1
     */
    private static int matchedIndex(String ip, String[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            String p = prefix[i];
            if ("*".equals(p)) { // *, assumed to be IP
                //127开头的是回环地址，10，172，192开头的都可能是私有（内网）地址
                //这里说明不能用通配符*来匹配这些特殊地址
                if (ip.startsWith("127.") ||
                        ip.startsWith("10.") ||
                        ip.startsWith("172.") ||
                        ip.startsWith("192.")) {
                    continue;
                }
                return i;
            } else {
                if (ip.startsWith(p)) {
                    return i;
                }
            }
        }

        return -1;
    }
    /**
     * @date: 2024-01-04 14:38
     * @description: 获取本机的ip地址。基本过程是遍历所有网络接口中所有ip，然后找到满足指定ip优先级的第一个可用ip。
     * @Param ipPreference: ip地址的优先级，以"前缀1>前缀2>..."的形式给出
     *                      之所以要有ip优先级，是因为一台主机可能有多个ip地址，并且不同情况下使用的ip可能不同
     *                      通过指定ip优先级，就可以指定该台主机应该优先使用哪一个ip地址
     * @return: java.lang.String 如果没找到满足条件的ip，则返回127.0.0.1
     */
    public static String getLocalIp(String ipPreference) {
        //若不指定ip优先级，则默认为*>10>172>192>127。*通配符不能匹配10，172，192，127开头的ip
        if (ipPreference == null) {
            ipPreference = "*>10>172>192>127";
        }
        //以">"为分隔符拆分字符串
        String[] prefix = ipPreference.split("[> ]+");
        try {
            //这个正则表达式是用来匹配类似于ipv4地址的字符串，它的匹配规则没有pattern属性那么严，没有做合法性校验
            Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
            /*
             * Enumeration接口和迭代器接口功能类似，都是用于遍历集合中的元素
             * NetworkInterface表示一个网络接口。通过这个类的getNetworkInterfaces方法可以获取本机所有的
             * 物理网络接口和虚拟机等软件利用物理接口创建的逻辑网络接口的信息
             */
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String matchedIp = null;
            int matchedIdx = -1;
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                //	跳过回环接口和虚拟（逻辑）接口
                if(ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                /*
                 * InetAddress类是对IP地址的封装
                 * 通过NetworkInterface类的getInetAddresses方法可以获取绑定到该接口上的所有InetAddress
                 * 注：一个网络接口可以绑定多个IP地址
                 */
                Enumeration<InetAddress> en = ni.getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    //跳过环回地址，site local地址（相当于私有地址）和通配符地址（0.0.0.0）
                    if(addr.isLoopbackAddress() ||
                            !addr.isSiteLocalAddress() ||
                            addr.isAnyLocalAddress()) {
                        continue;
                    }
                    //获取ip地址
                    String ip = addr.getHostAddress();
                    Matcher matcher = pattern.matcher(ip);
                    //找到满足指定ip优先级的第一个可用ip
                    if (matcher.matches()) {
                        int idx = matchedIndex(ip, prefix);
                        if (idx == -1)
                            continue;
                        if (matchedIdx == -1) {
                            matchedIdx = idx;
                            matchedIp = ip;
                        } else {
                            //当且仅当目前ip的优先级比之前找到的更高时，才更新matchedIdx和matchedIp
                            if (matchedIdx > idx) {
                                matchedIdx = idx;
                                matchedIp = ip;
                            }
                        }
                    }
                }
            }
            //如果没有找到满足条件的ip，则返回127.0.0.1作为本机ip
            if (matchedIp != null)
                return matchedIp;
            return "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
    /**
     * @date: 2024-01-04 16:21
     * @description: 获取本机的ip地址，ip优先级默认为*>10>172>192>127
     * @return: java.lang.String
     */
    public static String getLocalIp() {
        //*通配符不能匹配10，172，192，127开头的ip
        return getLocalIp("*>10>172>192>127");
    }
    /**
     * @date: 2024-01-04 16:32
     * @description: 通过SocketChannel对象获取远程主机的地址（ip：port）
     * @Param channel:
     * @return: java.lang.String
     */
    public static String remoteAddress(SocketChannel channel) {
        SocketAddress addr = channel.socket().getRemoteSocketAddress();
        String res = String.format("%s", addr);
        return res;
    }
    /**
     * @date: 2024-01-04 16:36
     * @description: 通过SocketChannel对象获取本地主机的地址（ip：port）
     * @Param channel:
     * @return: java.lang.String
     */
    public static String localAddress(SocketChannel channel) {
        SocketAddress addr = channel.socket().getLocalSocketAddress();
        String res = String.format("%s", addr);
        //res.substring(1)的作用是去除res中第一个无效字符"/"
        return addr == null ? res : res.substring(1);
    }
    /**
     * @date: 2024-01-04 16:48
     * @description: 获取线程id
     * @return: java.lang.String
     */
    public static String getPid() {
        //ManagementFactory类提供了很多获取JVM信息的方法
        //通过getRuntimeMXBean方法可以获取当前正在运行的JVM实例
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        //获取当前线程名称，格式是pid@hostname
        String name = runtime.getName();
        //获取当前线程id
        int index = name.indexOf("@");
        if (index != -1) {
            return name.substring(0, index);
        }
        return null;
    }
    /**
     * @date: 2024-01-04 17:09
     * @description: 获取本地主机名称
     * @return: java.lang.String
     */
    public static String getLocalHostName() {
        try {
            return (InetAddress.getLocalHost()).getHostName();
        } catch (UnknownHostException uhe) {
            String host = uhe.getMessage();
            //如果出现UnknownHostException异常，则提取异常信息中的主机名称，如果无法提取，则默认返回UnknownHost
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    return host.substring(0, colon);
                }
            }
            return "UnknownHost";
        }
    }

}
