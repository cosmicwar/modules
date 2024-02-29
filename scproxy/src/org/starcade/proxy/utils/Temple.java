package org.starcade.proxy.utils;

import java.io.File;
import java.nio.file.Files;

public class Temple {
    private static String temple;

    public static String getTemple() {
        if (temple == null) {
            try {
                temple = Files.readAllLines(new File(".temple").toPath()).get(0).split("\\.")[2];
            } catch (Exception e) {

            }
        }
        return temple;
    }
}
