package org.starcade.proxy.network.redis;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class RedisManager {
    @Getter
    private RedisClient redisClient;
    @Getter
    private StatefulRedisConnection<String, String> redisConnection;
    @Getter
    private StatefulRedisPubSubConnection<String, String> redisPubSub;

    @Getter
    private Hashtable<String, OnMessage> listeners = new Hashtable<>();

    public RedisManager(RedisClient redisClient) {
        this.redisClient = redisClient;
        redisConnection = redisClient.connect();
        redisPubSub = redisClient.connectPubSub();

        RedisManagerListener listener = new RedisManagerListener(this);
        redisPubSub.addListener(listener);
    }

    @SneakyThrows
    public Set<String> getKeys(String query) {
        ScanArgs sa = new ScanArgs().match(query);
        KeyScanCursor<String> cursor = redisConnection.async().scan(sa).get();
        Set<String> keys = new HashSet<>(cursor.getKeys());
        while (!cursor.isFinished()) {
            cursor = redisConnection.async().scan(cursor, sa).get();
            keys.addAll(cursor.getKeys());
        }
        return keys;
    }

    public RedisFuture<Long> publish(String channel, String message) {
        return redisConnection.async().publish(channel, message);
    }

    public void subscribe(String channel, OnMessage m) {
        redisPubSub.sync().subscribe(channel);
        addListener(channel, m);
    }

    public void psubscribe(String pattern, OnMessage m) {
        redisPubSub.sync().psubscribe(pattern);
        addListener(pattern, m);
    }

    public void psubscribe(String[] patterns, OnMessage m) {
        for (String pattern : patterns) {
            redisPubSub.sync().psubscribe(pattern);
            addListener(pattern, m);
        }
    }

    private void addListener(String channel, OnMessage m) {
        OnMessage old = listeners.get(channel);
        if (old != null) {
            LinkingEventProxy proxy = new LinkingEventProxy(m, old);
            listeners.put(channel, proxy);
        } else {
            listeners.put(channel, m);
        }
    }
}
