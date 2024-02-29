package org.starcade.proxy.handlers;

import org.starcade.proxy.ProxyBootstrap;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;

public class PingHandler {
    @Subscribe
    public void onPing(ProxyPingEvent event) {
        var ping = event.getPing();

        var connection = event.getConnection();
        if (connection.getVirtualHost().isPresent()) {
            var hostname = connection.getVirtualHost().get().getHostName().toLowerCase();
            var network = ProxyBootstrap.getNetwork(hostname);

            if (network != null) {
                event.setPing(ServerPing.builder()
                        .maximumPlayers(Math.max(1000, network.getPlayerCount() + 1))
                        .onlinePlayers(network.getPlayerCount())
                        .version(new ServerPing.Version(ping.getVersion().getProtocol(), "Starcade " + ProtocolVersion.MAXIMUM_VERSION.getMostRecentSupportedVersion()))
                        .description(Component.text(network.getMotd()))
                        .favicon(network.getFavicon())
                        .build());
            } else {
                if (hostname.equals("51.81.107.166")) {
                    var maxCount = 0;
                    for (var server : ProxyBootstrap.getNetworks().values()) maxCount = Math.max(maxCount, server.getPlayerCount());

                    event.setPing(ServerPing.builder()
                            .maximumPlayers(maxCount + 1)
                            .onlinePlayers(maxCount)
                            .version(new ServerPing.Version(ping.getVersion().getProtocol(), "Starcade " + ProtocolVersion.MAXIMUM_VERSION.getMostRecentSupportedVersion()))
                            .description(Component.text(""))
                            .build());
                } else {
                    event.setPing(ServerPing.builder()
                            .maximumPlayers(0)
                            .onlinePlayers(0)
                            .version(new ServerPing.Version(Integer.MAX_VALUE, "NOT FOUND"))
                            .description(Component.text("NOT FOUND"))
                            .build());
                }
            }
        } else {
            var maxCount = 0;
            for (var server : ProxyBootstrap.getNetworks().values()) maxCount = Math.max(maxCount, server.getPlayerCount());

            event.setPing(ServerPing.builder()
                    .maximumPlayers(maxCount + 1)
                    .onlinePlayers(maxCount)
                    .version(new ServerPing.Version(ping.getVersion().getProtocol(), "Starcade " + ProtocolVersion.MAXIMUM_VERSION.getMostRecentSupportedVersion()))
                    .description(Component.text(""))
                    .build());
        }
    }
}
