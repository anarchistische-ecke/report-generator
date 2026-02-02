package com.example.report;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

public final class Main {
    public static void main(String[] args) {
        try {
            Args parsed = Args.parse(args);
            Config config = Config.load(parsed.configPath).applyOverrides(parsed);

            if ("update-checking".equalsIgnoreCase(parsed.mode)) {
                runUpdateChecking(parsed, config);
                return;
            }

            if (parsed.from == null || parsed.to == null) {
                throw new IllegalArgumentException("--from and --to are required (yyyy-MM-dd)");
            }

            LocalDate from = LocalDate.parse(parsed.from);
            LocalDate to = LocalDate.parse(parsed.to);

            if (config.dbUrl.isBlank() || config.dbUser.isBlank() || config.dbPassword.isBlank()) {
                throw new IllegalArgumentException("Database credentials are missing. Set in config or pass --db-url/--db-user/--db-pass.");
            }

            Logger.info("Using config: " + Path.of(parsed.configPath.toString()).toAbsolutePath());
            try (Connection connection = DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword)) {
                ReportGenerator generator = new ReportGenerator(connection, config);
                int exitCode = generator.generate(from, to, parsed.equipId, parsed.equipName);
                if (exitCode != 0) {
                    System.exit(exitCode);
                }
            }
        } catch (Exception ex) {
            Logger.error("Report generation failed", ex);
            System.exit(1);
        }
    }

    private static void runUpdateChecking(Args parsed, Config config) throws Exception {
        if (parsed.inputPath == null) {
            throw new IllegalArgumentException("--input is required for mode update-checking");
        }

        if (config.dbUrl.isBlank() || config.dbUser.isBlank() || config.dbPassword.isBlank()) {
            throw new IllegalArgumentException("Database credentials are missing. Set in config or pass --db-url/--db-user/--db-pass.");
        }

        Logger.info("Using config: " + Path.of(parsed.configPath.toString()).toAbsolutePath());
        try (Connection connection = DriverManager.getConnection(config.dbUrl, config.dbUser, config.dbPassword)) {
            EquipCheckUpdater updater = new EquipCheckUpdater(connection, config);
            EquipCheckUpdater.UpdateSummary summary = updater.update(parsed.inputPath, parsed.inputSheet, parsed.inputHasHeader);
            summary.log();
        }
    }
}
