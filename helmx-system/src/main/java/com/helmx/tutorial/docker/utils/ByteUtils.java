package com.helmx.tutorial.docker.utils;

public class ByteUtils {
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";

        int unit = 1024;
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        exp = Math.min(exp, units.length - 1);

        double value = bytes / Math.pow(unit, exp);
        return String.format("%.2f %s", value, units[exp]);
    }
}
