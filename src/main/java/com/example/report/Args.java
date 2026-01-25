package com.example.report;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "config",
            "db-url",
            "db-user",
            "db-pass",
            "output",
            "contract",
            "from",
            "to",
            "equip-id",
            "equip-name"
    );

    private static final Set<String> PARAM_KEYS = buildParamKeys();

    private static final String USAGE = """
Usage:
  --from yyyy-MM-dd --to yyyy-MM-dd [options]

Options:
  --config PATH
  --db-url JDBC_URL
  --db-user USER
  --db-pass PASS
  --output DIR
  --contract NUMBER
  --equip-id ID
  --equip-name LIKE
  --param11..--param17 VALUE
  --help | -h
""";

    private Args(Path configPath, String dbUrl, String dbUser, String dbPass, String output,
                 String contract, String from, String to, Integer equipId, String equipName,
                 Map<String, String> paramOverrides) {
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
    }

    static Args parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        Map<String, String> paramOverrides = new HashMap<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("--help".equals(arg) || "-h".equals(arg)) {
                throw new IllegalArgumentException(USAGE);
            }

            if (!arg.startsWith("--")) {
                errors.add("Unexpected argument: " + arg);
                continue;
            }

            String key = arg.substring(2);
            if (key.isEmpty()) {
                errors.add("Empty option: " + arg);
                continue;
            }

            String value;
            int eq = key.indexOf('=');
            if (eq >= 0) {
                value = key.substring(eq + 1);
                key = key.substring(0, eq);
            } else {
                if (i + 1 >= args.length) {
                    errors.add("Missing value for --" + key);
                    continue;
                }
                value = args[++i];
            }

            if (PARAM_KEYS.contains(key)) {
                paramOverrides.put(key, value);
            } else if (ALLOWED_KEYS.contains(key)) {
                values.put(key, value);
            } else if (key.startsWith("param")) {
                errors.add("Unknown param key: --" + key + " (allowed: param11..param17)");
            } else {
                errors.add("Unknown option: --" + key);
            }
        }

        String from = values.get("from");
        String to = values.get("to");
        if (from == null || from.isBlank()) {
            errors.add("Missing required option: --from (yyyy-MM-dd)");
        }
        if (to == null || to.isBlank()) {
            errors.add("Missing required option: --to (yyyy-MM-dd)");
        }

        Integer equipId = null;
        String equipIdValue = values.get("equip-id");
        if (equipIdValue != null) {
            try {
                equipId = Integer.valueOf(equipIdValue);
            } catch (NumberFormatException ex) {
                errors.add("Invalid --equip-id value (must be an integer): " + equipIdValue);
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder("Invalid arguments:");
            for (String error : errors) {
                message.append(System.lineSeparator()).append("- ").append(error);
            }
            message.append(System.lineSeparator()).append(System.lineSeparator()).append(USAGE);
            throw new IllegalArgumentException(message.toString());
        }

        Path configPath = Paths.get(values.getOrDefault("config", "config/report.properties"));

        return new Args(
                configPath,
                values.get("db-url"),
                values.get("db-user"),
                values.get("db-pass"),
                values.get("output"),
                values.get("contract"),
                from,
                to,
                equipId,
                values.get("equip-name"),
                paramOverrides
        );
    }

    private static Set<String> buildParamKeys() {
        Set<String> keys = new HashSet<>();
        for (int i = 11; i <= 17; i++) {
            keys.add("param" + i);
        }
        return keys;
    }
}
