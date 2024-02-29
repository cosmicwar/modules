package org.starcade.runner.environment

import java.lang.reflect.Field


static def setField(Class<?> c, String fname, Object value) {
    getField(c, fname).set(null, value)
}

static def setField(Object obj, String fname, Object value) {
    getField(obj.getClass(), fname).set(obj, value)
}

static def setField(Class<?> c, Object obj, String fname, Object value) {
    getField(c, fname).set(obj, value)
}

static Object getField(Object obj, String fname) {
    return getField(obj.getClass(), fname).get(obj)
}

static Field getField(Class<?> clazz, String fname) {
    Field f = null
    try {
        f = clazz.getDeclaredField(fname)
    } catch (Exception e) {
        f = clazz.getField(fname)
    }

    setAccessible(f)
    return f
}

static def setAccessible(Field f) {
    f.setAccessible(true)
    Field modifiers = Field.class.getDeclaredField("modifiers")
    modifiers.setAccessible(true)
    modifiers.setInt(f, f.getModifiers() & -0x11)
}


