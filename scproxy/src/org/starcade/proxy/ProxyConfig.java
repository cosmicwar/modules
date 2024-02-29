package org.starcade.proxy;

import java.util.List;
import java.util.Map;

public class ProxyConfig {
    String secret;
    String redis_host;
    int redis_port;
    String redis_password;

    public static class NetworkConfig {
        List<String> hostnames;
        String favicon;

        String redis_host;
        int redis_port;
        String redis_password;
    }

    Map<String, NetworkConfig> networks;
}
