package org.starcade.proxy.handlers;

import org.starcade.proxy.ProxyBootstrap;
import org.starcade.proxy.Standard;
import org.starcade.proxy.network.redis.OnMessage;
import org.starcade.proxy.utils.Utils;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class SendHandler {
    public SendHandler() {
        for (var network : ProxyBootstrap.getNetworks().values()) {
            OnMessage handler = (channel, message) -> {
                String[] msg = message.split("\\|");
                String sender = msg[0];
                String player = msg[1];
                String address = msg[2];

                ProxyBootstrap.getServer().getPlayer(player).ifPresent(pp -> {
                    ProxyBootstrap.getLogger().info("[sender: " + sender + "] Sending to ip: \"" + address + "\"");
                    try {
                        RegisteredServer info = ServerHandler.getServerInfo(address, Utils.obfuscateIp(address));
                        pp.createConnectionRequest(info).connect().thenAcceptAsync(result -> {
                            if (!result.isSuccessful()) {
                                ProxyBootstrap.getLogger().warning("[sender: " + sender + "] Unable to send player to ip: \"" + address + "\" with status " + result.getStatus().name() + ": " + result.getReasonComponent().toString());
                            }
                        }).exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    } catch (Exception e) {
                        ProxyBootstrap.getLogger().warning("[sender: " + sender + "] Unable to send player to ip: \"" + address + "\", (invalid ip address?)");
                        e.printStackTrace();
                    }
                });
            };
            network.getRedis().subscribe(Standard.PCH_SEND, handler);
            network.getRedis().subscribe(Standard.PCH_SEND + "_echo", handler);
        }
    }
}
