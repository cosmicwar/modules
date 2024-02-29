package org.starcade.proxy.network;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;

import java.net.InetSocketAddress;

public class HubServer {
    public RegisteredServer info;
    public InetSocketAddress address;
    public String name;
    public String identity;
    public int maxPlayers;
    public int players;
}