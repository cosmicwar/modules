package org.starcade.proxy;

import org.starcade.proxy.handlers.ControlHandler;
import org.starcade.proxy.handlers.HubHandler;
import org.starcade.proxy.handlers.PingHandler;
import org.starcade.proxy.handlers.SendHandler;
import org.starcade.proxy.network.NetworkManager;
import org.starcade.proxy.network.redis.RedisManager;
import org.starcade.proxy.utils.Temple;
import org.starcade.proxy.utils.Utils;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import lombok.Getter;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Plugin(id = "scproxy", name = "Starcade Proxy", version = "1.0", url = "https://starcade.org", description = "Proxy plugin for starcade network", authors = {"starcade"})
public class ProxyBootstrap {
    @Getter
    private static Gson gson;
    @Getter
    private static ProxyServer server;
    @Getter
    private static Logger logger;
    @Getter
    private static ProxyBootstrap plugin;
    @Getter
    private static String secret;
    @Getter
    private static RedisManager controlRedis;
    @Getter
    private static ConcurrentHashMap<String, NetworkManager> networks = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, NetworkManager> hosts = new ConcurrentHashMap<>(); // fast lookup

    private static String forcedNetwork = System.getProperty("network", "starcade"); // default to starcade

    public static NetworkManager getNetwork(String hostname) {
        if (forcedNetwork != null) return networks.get(forcedNetwork);
        return hosts.get(hostname);
    }

    @Inject
    public ProxyBootstrap(ProxyServer server, Logger logger) {
        ProxyBootstrap.plugin = this;
        ProxyBootstrap.server = server;
        ProxyBootstrap.logger = logger;
        ProxyBootstrap.gson = new Gson();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        try {
            if (forcedNetwork != null) getLogger().warning("Using forced network: " + forcedNetwork);

            File file = new File(new File("plugins"), "starcade.toml");
            if (!file.exists()) Utils.copyResource("config.toml", file);
            ProxyConfig cfg = new Toml().read(file).to(ProxyConfig.class);
            secret = cfg.secret;

            {
                RedisClient redisClient = RedisClient.create(RedisURI.builder().withHost(cfg.redis_host).withPort(cfg.redis_port).withTimeout(Duration.ofMinutes(1)).withPassword(cfg.redis_password.toCharArray()).build());
                controlRedis = new RedisManager(redisClient);
            }

            cfg.networks.forEach((key, value) -> {
                RedisClient redisClient = RedisClient.create(RedisURI.builder().withHost(value.redis_host).withPort(value.redis_port).withTimeout(Duration.ofMinutes(1)).withPassword(value.redis_password.toCharArray()).build());

                ProxyBootstrap.getLogger().info("Loading network: " + key);
                var manager = new NetworkManager(key, value.favicon, redisClient);
                networks.put(key, manager);

                for (String hostname : value.hostnames) hosts.put(hostname, manager);
            });

            getServer().getScheduler().buildTask(getPlugin(), () -> {
                try {
                    controlRedis.getRedisConnection().sync().setex(Standard.KEY_PREFIX_BUNGEE + Temple.getTemple(), 10, gson.toJson(new Standard.Bungee(getServer().getPlayerCount())));
                } catch (Exception ignore) {}
            }).repeat(2, TimeUnit.SECONDS).schedule();
        } catch (Exception e) {
            e.printStackTrace();
        }

        new ControlHandler();
        server.getEventManager().register(this, new HubHandler());
        server.getEventManager().register(this, new PingHandler());
        server.getEventManager().register(this, new SendHandler());

        server.getCommandManager().unregister("server");
        server.getCommandManager().unregister("velocity");
    }
}
