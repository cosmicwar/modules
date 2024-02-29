package org.starcade.proxy.network;

import org.starcade.proxy.ProxyBootstrap;
import org.starcade.proxy.Standard;
import org.starcade.proxy.handlers.ServerHandler;
import org.starcade.proxy.network.redis.RedisManager;
import org.starcade.proxy.utils.Utils;
import com.google.gson.Gson;
import com.velocitypowered.api.util.Favicon;
import io.lettuce.core.RedisClient;
import lombok.Getter;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NetworkManager implements Runnable {
    private static final Gson gson = new Gson();

    @Getter
    private String name;
    @Getter
    private RedisManager redis;
    @Getter
    private String motd;
    @Getter
    private int playerCount;
    @Getter
    private Favicon favicon;
    @Getter
    private ConcurrentHashMap<String, HubServer> hubs = new ConcurrentHashMap<>();

    public NetworkManager(String name, String favicon, RedisClient redisClient) {
        this.name = name;
        this.redis = new RedisManager(redisClient);

        try {
            this.favicon = Favicon.create(new File(favicon).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ProxyBootstrap.getServer().getScheduler().buildTask(ProxyBootstrap.getPlugin(), this).repeat(2, TimeUnit.SECONDS).schedule();
    }

    @Override
    public void run() {
        this.motd = Utils.translateAlternateColorCodes('&', Optional.ofNullable(redis.getRedisConnection().sync().get(Standard.CONFIG_MOTD)).orElse("loading..."));

        try {
            Set<String> hubIdentities = redis.getKeys(Standard.KEY_PREFIX_SERVER + "*");
            ConcurrentHashMap<String, HubServer> newHubs = new ConcurrentHashMap<>();
            for (String key : hubIdentities) {
                String s = redis.getRedisConnection().sync().get(key);
                try {
                    // key was removed between query and fetch.
                    if (s != null) {
                        Standard.Server hub = gson.fromJson(s, Standard.Server.class);
                        if (!hub.getType().equals(Standard.HUB_TYPE) || !hub.getName().startsWith("hub")) continue;
                        String identity = key.substring(Standard.KEY_PREFIX_SERVER.length());
                        HubServer server = hubs.get(identity);
                        if (server != null) {
                            newHubs.put(identity, server);
                        } else {
                            ProxyBootstrap.getLogger().info("[" + name + "] Added hub server: " + identity + ", " + hub.getName());
                            server = new HubServer();
                        }
                        server.name = hub.getName();
                        server.identity = identity;
                        server.players = hub.getPlayers();
                        server.maxPlayers = hub.getMaxPlayers();
                        server.address = Standard.parseIdentity(identity);
                        server.info = ServerHandler.getServerInfo(server.address, server.name);
                        newHubs.put(identity, server);
                    }
                } catch (Exception e) {
                    ProxyBootstrap.getLogger().severe("Failed to parse: " + s);
                    e.printStackTrace();
                }
            }
            hubs = newHubs;
        } catch (Exception e) {
            e.printStackTrace();
        }

        int players = 0;
        Set<String> bcords = redis.getKeys(Standard.KEY_PREFIX_BUNGEE + "*");
        for (String key : bcords) {
            try {
                String s = redis.getRedisConnection().sync().get(key);
                // was removed
                if (s != null) players += gson.fromJson(s, Standard.Bungee.class).getPlayers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        playerCount = players;
    }

    public HubServer getBestHub() {
        HubServer server = null;
        int players = Integer.MAX_VALUE;
        for (HubServer s : hubs.values()) {
            if (s.players < players) {
                server = s;
                players = s.players;
            }
        }
        return server;
    }
}
