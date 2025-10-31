package com.helmx.tutorial.docker.utils;

public class PathUtil {

    /**
     * 清理和验证目录名称
     */
    public static String sanitizeDirectoryName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // 移除危险字符
        String cleaned = name.replaceAll("[/\\\\<>:\"|?*]", "");

        // 移除..序列防止路径遍历
        cleaned = cleaned.replace("..", "");

        // 移除控制字符
        cleaned = cleaned.replaceAll("[\0-\37]", "");

        // 限制长度
        if (cleaned.length() > 100) {
            cleaned = cleaned.substring(0, 100);
        }

        // 不允许以点开头（隐藏文件）
        if (cleaned.startsWith(".")) {
            cleaned = cleaned.substring(1);
        }

        return cleaned.isEmpty() ? null : cleaned;
    }
}
