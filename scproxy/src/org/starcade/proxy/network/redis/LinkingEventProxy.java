package org.starcade.proxy.network.redis;

public class LinkingEventProxy implements OnMessage {
    private OnMessage wrapped;
    private OnMessage next;

    public LinkingEventProxy(OnMessage wrapped, OnMessage next) {
        this.wrapped = wrapped;
        this.next = next;
    }

    @Override
    public void message(String channel, String message) {
        wrapped.message(channel, message);
        next.message(channel, message);
    }
}