package org.starcade.runner.environment

import java.util.concurrent.ConcurrentHashMap

class Exports {
    static def map = new ConcurrentHashMap<String, Object>()

    static def ptr(String key) {
        return map.get(key)
    }

    static def ptr(String key, Object obj) {
        return map.put(key, obj)
    }

    static <T> T get(String key, T closure = null) {
        map.get(key) as T ?: closure
    }

    static String lang(String key) {
        map[key] as String ?: "<export missing: $key>"
    }


}