package com.helmx.tutorial.docker.utils;

public class TimeUtils {

    /**
     * 将纳秒转换为人性化的时间格式 (时:分:秒.毫秒)
     *
     * @param nanoseconds 纳秒数
     * @return 格式化后的时间字符串
     */
    public static String formatNanoseconds(long nanoseconds) {
        // 转换为毫秒
        long milliseconds = nanoseconds / 1_000_000;

        // 计算各时间单位
        long hours = milliseconds / (3600 * 1000);
        long minutes = (milliseconds % (3600 * 1000)) / (60 * 1000);
        long seconds = (milliseconds % (60 * 1000)) / 1000;
        long millis = milliseconds % 1000;

        if (hours > 0) {
            return String.format("%dh%dm%ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm%ds", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%ds%dms", seconds, millis);
        } else {
            return String.format("%dms", millis);
        }
    }

    /**
     * 更详细的纳秒转换方法
     *
     * @param nanoseconds 纳秒数
     * @return 格式化后的时间字符串
     */
    public static String formatNanosecondsDetailed(long nanoseconds) {
        long totalMillis = nanoseconds / 1_000_000;
        long days = totalMillis / (24 * 3600 * 1000);
        long hours = (totalMillis % (24 * 3600 * 1000)) / (3600 * 1000);
        long minutes = (totalMillis % (3600 * 1000)) / (60 * 1000);
        long seconds = (totalMillis % (60 * 1000)) / 1000;
        long millis = totalMillis % 1000;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d");
        }
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s");
        }
        if (millis > 0 && sb.isEmpty()) { // 只有在没有更大单位时才显示毫秒
            sb.append(millis).append("ms");
        }

        return sb.toString().isEmpty() ? "0ms" : sb.toString();
    }
}

