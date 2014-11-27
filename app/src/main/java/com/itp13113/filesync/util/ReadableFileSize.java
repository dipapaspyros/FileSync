package com.itp13113.filesync.util;

/**
 * Created by dimitris on 27/11/2014.
 */
public class ReadableFileSize {
    public static String getReadableFileSize(long bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
