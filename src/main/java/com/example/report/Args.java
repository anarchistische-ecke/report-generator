package com.example.report;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

final class Args {
    final Path configPath;
    final String dbUrl;
    final String dbUser;
    final String dbPass;
    final String output;
    final String contract;
    final String from;
    final String to;
    final Integer equipId;
    final String equipName;
    final Map<String, String> paramOverrides;
    final String mode;
    final Path inputPath;
    final String inputSheet;
    final boolean inputHasHeader;

    private Args(Path configPath, String dbUrl, String dbUser, String dbPass, String output,
                 String contract, String from, String to, Integer equipId, String equipName,
                 Map<String, String> paramOverrides, String mode, Path inputPath,
                 String inputSheet, boolean inputHasHeader) {
        this.configPath = configPath;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPass = dbPass;
        this.output = output;
        this.contract = contract;
        this.from = from;
        this.to = to;
        this.equipId = equipId;
        this.equipName = equipName;
        this.paramOverrides = paramOverrides;
        this.mode = mode;
        this.inputPath = inputPath;
        this.inputSheet = inputSheet;
        this.inputHasHeader = inputHasHeader;
    }

    static Args parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        Map<String, String> paramOverrides = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }

            String key = arg.substring(2);
            String value;
            int eq = key.indexOf('=');
            if (eq >= 0) {
                value = key.substring(eq + 1);
                key = key.substring(0, eq);
            } else {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --" + key);
                }
                value = args[++i];
            }

            if (key.startsWith("param")) {
                paramOverrides.put(key, value);
            } else {
                values.put(key, value);
            }
        }

        Path configPath = Paths.get(values.getOrDefault("config", "config/report.properties"));
        Integer equipId = values.containsKey("equip-id") ? Integer.valueOf(values.get("equip-id")) : null;
        String mode = values.getOrDefault("mode", "report");
        Path inputPath = values.containsKey("input") ? Paths.get(values.get("input")) : null;
        String inputSheet = values.get("input-sheet");
        boolean inputHasHeader = Boolean.parseBoolean(values.getOrDefault("input-has-header", "false"));

        return new Args(
                configPath,
                values.get("db-url"),
                values.get("db-user"),
                values.get("db-pass"),
                values.get("output"),
                values.get("contract"),
                values.get("from"),
                values.get("to"),
                equipId,
                values.get("equip-name"),
                paramOverrides,
                mode,
                inputPath,
                inputSheet,
                inputHasHeader
        );
    }
}
