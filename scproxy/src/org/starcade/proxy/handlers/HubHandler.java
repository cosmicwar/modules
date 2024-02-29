package org.starcade.proxy.handlers;

import org.starcade.proxy.ProxyBootstrap;
import org.starcade.proxy.network.HubServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class HubHandler {
    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        System.out.println("Logging kick: " + event.getPlayer().getUsername() + " from " + event.getServer().getServerInfo().getName());
        if ((event.getServerKickReason().isPresent() && (event.getServerKickReason().get().toString().contains("reboot") || event.getServerKickReason().get().toString().contains("restart") || event.getServerKickReason().get().toString().contains("disconnected"))) || event.getServerKickReason().isEmpty()) {
            if (event.getPlayer().getVirtualHost().isPresent()) {
                var hostname = event.getPlayer().getVirtualHost().get().getHostName().toLowerCase();
                var network = ProxyBootstrap.getNetwork(event.getPlayer().getVirtualHost().get().getHostName());
                if (network != null) {
                    HubServer server = network.getBestHub();
                    if (server != null) {
                        server.players++;
                        event.setResult(KickedFromServerEvent.RedirectPlayer.create(server.info));
                    }
                } else {
                    ProxyBootstrap.getLogger().warning("Nowhere to send player " + event.getPlayer().getUsername() + " with hostname " + hostname);
                }
            } else {
                ProxyBootstrap.getLogger().warning("Nowhere to send player " + event.getPlayer().getUsername() + " with no hostname");
            }
        }
    }

    @Subscribe
    public void onConnect(ServerPreConnectEvent event) {
        System.out.println("Logging send: " + event.getPlayer().getUsername() + " to " + event.getOriginalServer().getServerInfo().getName());
        if (event.getOriginalServer().getServerInfo().getName().equals("dummy")) {
            if (event.getPlayer().getVirtualHost().isPresent()) {
                var hostname = event.getPlayer().getVirtualHost().get().getHostName().toLowerCase();
                var network = ProxyBootstrap.getNetwork(hostname);
                if (network != null) {
                    HubServer server = network.getBestHub();
                    if (server != null) {
                        server.players++;
                        event.setResult(ServerPreConnectEvent.ServerResult.allowed(server.info));
                        ProxyBootstrap.getLogger().info("[sender: " + event.getPlayer().getUsername() + "] Sending to: " + event.getOriginalServer().getServerInfo().getName());
                    } else {
                        event.getPlayer().disconnect(Component.text("§c§lNo Hub servers available."));
                    }
                } else {
                    ProxyBootstrap.getLogger().warning("Nowhere to send player " + event.getPlayer().getUsername() + " with hostname " + hostname);
                }
            } else {
                ProxyBootstrap.getLogger().warning("Nowhere to send player " + event.getPlayer().getUsername() + " with no hostname");
            }
        }
    }

    @Subscribe
    public void onGameProfileRequest(ServerPreConnectEvent event) {
        /*GameProfile profile = event.getGameProfile();
        List<GameProfile.Property> properties = new ArrayList<>(profile.getProperties());
        for (GameProfile.Property property : properties) {
            if (property.getName().equals("secret")) return;
        }
        profile.addProperties(Collections.singletonList(new GameProfile.Property("secret", ProxyBootstrap.getSecret(), "")));
        event.setGameProfile(profile);*/
        ConnectedPlayer player = (ConnectedPlayer) event.getPlayer();
        List<GameProfile.Property> properties = new ArrayList<>(player.getGameProfileProperties());
        for (GameProfile.Property property : properties) {
            if (property.getName().equals("secret")) return;
        }
        properties.add(new GameProfile.Property("secret", ProxyBootstrap.getSecret(), ""));
        player.setGameProfileProperties(properties);
    }
}
