package org.starcade.proxy.handlers;

import org.starcade.proxy.ProxyBootstrap;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler {
	private static ConcurrentHashMap<String, RegisteredServer> infoMap = new ConcurrentHashMap<>(); // basically just an ip cache
	private static ProxyServer server;

	public static void send(Player player, String ip, String name) {
		RegisteredServer info = getServerInfo(ip, name);
		player.createConnectionRequest(info);
	}

	public static RegisteredServer getServerInfo(InetSocketAddress ip, String name) {
		RegisteredServer info = infoMap.get(name);
		if (info == null) {
			info = ProxyBootstrap.getServer().registerServer(new ServerInfo(name, ip));
			infoMap.put(name, info);
		}
		return info;
	}

	public static RegisteredServer getServerInfo(String ip, String name) {
		RegisteredServer info = infoMap.get(ip.toLowerCase());
		if (info == null) {
			String[] ipArgs = ip.split(":");
			info = ProxyBootstrap.getServer().registerServer(new ServerInfo(name, new InetSocketAddress(ipArgs[0], Integer.parseInt(ipArgs[1]))));
			infoMap.put(ip, info);
		}
		return info;
	}
}
