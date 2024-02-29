package org.starcade.proxy.network.redis;

import io.lettuce.core.pubsub.RedisPubSubListener;

public class RedisManagerListener implements RedisPubSubListener<String, String> {
    private RedisManager redis;

    public RedisManagerListener(RedisManager redis) {
        this.redis = redis;
    }

    @Override
    public void message(String channel, String message) {
        var e = redis.getListeners().get(channel);
        if (e != null) e.message(channel, message);
    }

    @Override
    public void message(String pattern, String channel, String message) {
        var e = redis.getListeners().get(pattern);
        if (e != null) e.message(channel, message);
    }

    @Override
    public void subscribed(String s, long l) {
    }

    @Override
    public void psubscribed(String s, long l) {
    }

    @Override
    public void unsubscribed(String s, long l) {
    }

    @Override
    public void punsubscribed(String s, long l) {
    }

}
