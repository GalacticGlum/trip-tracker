package com.galacticglum.distancetracker;

public class TimeHelpers {
    public static String getTimeFormatFromSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return  String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
