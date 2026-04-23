package tn.esprit.tools;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public final class LanAddress {
    private LanAddress() {}

    public static String pick() {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            for (NetworkInterface nif : Collections.list(nifs)) {
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                String name = nif.getDisplayName() == null ? "" : nif.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("vmware") || name.contains("vbox") || name.contains("hyper-v")) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    String ip = addr.getHostAddress();
                    if (ip.contains(":")) continue; // skip IPv6
                    if (ip.startsWith("192.168.") || ip.startsWith("10.") ||
                        (ip.startsWith("172.") && isPrivate172(ip))) {
                        return ip;
                    }
                    if (fallback == null) fallback = ip;
                }
            }
            if (fallback != null) return fallback;
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static boolean isPrivate172(String ip) {
        try {
            int second = Integer.parseInt(ip.split("\\.")[1]);
            return second >= 16 && second <= 31;
        } catch (Exception e) {
            return false;
        }
    }
}
