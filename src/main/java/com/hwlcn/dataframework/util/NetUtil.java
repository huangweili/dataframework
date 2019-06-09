package com.hwlcn.dataframework.util;

import java.net.*;
import java.util.Enumeration;

public class NetUtil {

    /**
     * 获取第一个可用的网络地址
     *
     * @return
     */
    public static InetAddress getInet4Address() throws SocketException, UnknownHostException {
        InetAddress candidateAddress = null;
        Enumeration<NetworkInterface> ifcaes = NetworkInterface.getNetworkInterfaces();
        while (ifcaes.hasMoreElements()) {
            NetworkInterface networkInterface = ifcaes.nextElement();
            if (networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue;
            }
            Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
            while (addressEnumeration.hasMoreElements()) {
                InetAddress inetAddress = addressEnumeration.nextElement();
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                    if (!inetAddress.isSiteLocalAddress() && inetAddress != null && inetAddress instanceof Inet4Address) {
                        //外网IP获取
                        return inetAddress;
                    } else if (inetAddress.isSiteLocalAddress() && inetAddress != null && inetAddress instanceof Inet4Address) {
                        //获取内网IP
                        candidateAddress = inetAddress;
                    }
                }
            }
        }
        if (candidateAddress != null) {
            return candidateAddress;
        }
        return InetAddress.getLocalHost();
    }
}
