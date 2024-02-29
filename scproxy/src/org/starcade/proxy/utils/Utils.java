package org.starcade.proxy.utils;

import lombok.SneakyThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

public class Utils {
    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; ++i) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    public static boolean copyResource(String resource, File destination) {
        try (InputStream in = Utils.class.getClassLoader().getResourceAsStream(resource)) {
            try (FileOutputStream out = new FileOutputStream(destination, false)) {
                int i;
                byte[] buffer = new byte[512];
                while ((i = in.read(buffer)) != -1)
                    out.write(buffer, 0, i);
                out.flush();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SneakyThrows
    public static void destroySecurity() {
        final Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        final Field[] allFields = (Field[]) getDeclaredFields0.invoke(System.class, false);
        for (final Field field : allFields) {
            if (field.getName().equalsIgnoreCase("security")) {
                field.setAccessible(true);
                field.set(null, null);
            }
        }
    }

    public static byte[] write(byte[] array, int index, short value) {
        array[index] = (byte) (value & 0xff);
        array[index + 1] = (byte) ((value >> 8) & 0xff);
        return array;
    }

    public static byte[] write(byte[] array, int index, int value) {
        array[index] = (byte) (value >> 24);
        array[index + 1] = (byte) (value >> 16);
        array[index + 2] = (byte) (value >> 8);
        array[index + 3] = (byte) (value /*>> 0*/);
        return array;
    }

    public static String base64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public static <T> T first(Iterable<T> i) {
        Iterator<T> it = i.iterator();
        if (it.hasNext())
            return it.next();
        return null;
    }

    @SneakyThrows
    public static InetAddress getIpAddress() {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                if (inetAddress.isLoopbackAddress()
                        || inetAddress instanceof Inet6Address)
                    continue;
                return inetAddress;
            }
        }
        return null;
    }

    public static Integer parseInt(String string) {
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            return null;
        }
    }

    public static InetSocketAddress toAddress(String ip) {
        int port = 25565;
        if (ip.contains(":")) {
            String[] splitIP = ip.split(":");
            port = Integer.parseInt(splitIP[1]);
            ip = splitIP[0];
        }
        return new InetSocketAddress(ip, port);
    }


    @SneakyThrows
    public static InetSocketAddress parseIdentity(String value) {
        return toAddress(value);
    }

    private static final String obfuscationString = "*********************";

    public static String obfuscateIp(String ip) {
        String[] obfuscated = ip.split("\\.");
        return obfuscationString.substring(0, obfuscated[0].length()) + "." + obfuscationString.substring(0, obfuscated[1].length()) + "." + obfuscated[2] + "." + obfuscated[3];
    }
}
