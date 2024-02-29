package org.starcade.proxy.network.redis;

public interface OnMessage {
    void message(String channel, String message);
}
