package org.starcade.proxy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;

public class Standard {

    public static final String KEY_PREFIX_BUNGEE = "starcade.bungee.";
    public static final String KEY_PREFIX_SERVER = "starcade.server.";

    public static final String CONFIG_MOTD = "starcade.config.motd";

    public static final String HUB_TYPE = "hub";
    public static final String PCH_SEND = "starcade|send";
    public static final String CMD_REBOOT = "starcade|reboot";


    @Getter@Setter
    @AllArgsConstructor
    public static class Bungee {
        int players;
    }

    @Getter@Setter
    @AllArgsConstructor
    public static class Server {
        String name;
        String type;
        int players;
        int maxPlayers;
    }

    public static InetSocketAddress toAddress(String ip) {
        int port = 25565;
        if (ip.contains(":")) {
            String[] splitIP = ip.split(":");
            port = Integer.parseInt(splitIP[1]);
            ip = splitIP[0];
        }
        return new InetSocketAddress(ip, port);
    }
    @SneakyThrows
    public static InetSocketAddress parseIdentity(String value) {
        return toAddress(value);
    }

    private static final String obfuscationString = "*********************";

    public static String obfuscateIp(String ip) {
        String[] obfuscated = ip.split("\\.");
        return obfuscationString.substring(0, obfuscated[0].length()) + "." + obfuscationString.substring(0, obfuscated[1].length()) + "." + obfuscated[2] + "." + obfuscated[3];
    }
}
