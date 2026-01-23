package com.example.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Logger {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static void info(String message) {
        System.out.println(prefix("INFO") + message);
    }

    static void error(String message, Throwable throwable) {
        System.err.println(prefix("ERROR") + message);
        throwable.printStackTrace(System.err);
    }

    private static String prefix(String level) {
        return "[" + level + "] " + TS.format(LocalDateTime.now()) + " ";
    }
}
