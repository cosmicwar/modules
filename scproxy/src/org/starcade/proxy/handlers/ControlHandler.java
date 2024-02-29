package org.starcade.proxy.handlers;

import org.starcade.proxy.ProxyBootstrap;
import org.starcade.proxy.Standard;
import org.starcade.proxy.network.redis.OnMessage;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;

public class ControlHandler {
    static String hostName;

    @SneakyThrows
    public ControlHandler() {
        hostName = InetAddress.getLocalHost().getHostName();

        OnMessage handler = (channel, message) -> {
            if (channel.equals(Standard.CMD_REBOOT)) {
                if (message.equals(hostName)) ProxyBootstrap.getServer().shutdown(Component.text("Scheduled Reboot."));
            }
        };
        ProxyBootstrap.getControlRedis().subscribe(Standard.PCH_SEND, handler);
        ProxyBootstrap.getControlRedis().subscribe(Standard.PCH_SEND + "_echo", handler);
    }
}
