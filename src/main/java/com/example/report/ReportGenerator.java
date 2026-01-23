package com.example.report;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class ReportGenerator {
    private static final String DATA_START = "$$$data_start$$$";
    private static final String DATA_END = "$$$data_end$$$";
    private static final Pattern SCI_NOTATION = Pattern.compile(
            "^[+-]?(?:\\d+\\.?\\d*|\\d*\\.\\d+)[eE][+-]?\\d+$"
    );

    private final Connection connection;
    private final Config config;
    private final Map<String, EquipTemplate> templates;

    ReportGenerator(Connection connection, Config config) {
        this.connection = connection;
        this.config = config;
        this.templates = ReportTemplates.buildTemplates();
    }

    int generate(LocalDate from, LocalDate to, Integer equipId, String equipNameFilter) throws SQLException, IOException {
        List<EquipRecord> equipment = loadEquipment(equipId, equipNameFilter);
        if (equipment.isEmpty()) {
            Logger.info("No equipment matched the filter.");
            return 0;
        }

        Path outputDir = resolveOutputDir(config.outputDir);
        Files.createDirectories(outputDir);

        int errors = 0;
        int written = 0;

        for (EquipRecord record : equipment) {
            try {
                generateForEquipment(record, from, to, outputDir);
                written++;
            } catch (Exception ex) {
                errors++;
                Logger.error("Failed for EquipID=" + record.equipId + " (" + record.equipName + ")", ex);
            }
        }

        Logger.info("Finished. Files written: " + written + ", errors: " + errors);
        return errors == 0 ? 0 : 1;
    }

    private void generateForEquipment(EquipRecord record, LocalDate from, LocalDate to, Path outputDir)
            throws SQLException, IOException {
        EquipTemplate template = templates.get(record.equipType);
        if (template == null) {
            throw new IllegalArgumentException("No template for equip type: " + record.equipType);
        }

        Scheme scheme = Scheme.fromDb(record.schemeMeasure);
        if (scheme == null) {
            throw new IllegalArgumentException("Unknown scheme: " + record.schemeMeasure);
        }

        QueryPlan plan = template.buildQuery(scheme);
        Path outputFile = outputDir.resolve(buildFileName(record.equipName));

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writeHeader(writer, record, scheme);
            writeLine(writer, DATA_START);
            writeLine(writer, config.schemeHeader(scheme));
            writeLine(writer, config.schemeUnits(scheme));
            writeData(writer, plan, record.equipId, from, to);
            writeLine(writer, DATA_END);
        }

        Logger.info("Wrote " + outputFile.toAbsolutePath());
    }

    private void writeData(BufferedWriter writer, QueryPlan plan, int equipId, LocalDate from, LocalDate to)
            throws SQLException, IOException {
        try (PreparedStatement stmt = connection.prepareStatement(plan.sql)) {
            stmt.setInt(1, equipId);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));
            stmt.setDate(4, Date.valueOf(to));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 1; i <= plan.columnCount; i++) {
                        String value = normalizeValue(rs.getString(i));
                        if (i > 1) {
                            line.append('\t');
                        }
                        line.append(value);
                    }
                    writeLine(writer, line.toString());
                }
            }
        }
    }

    private void writeHeader(BufferedWriter writer, EquipRecord record, Scheme scheme) throws IOException {
        writeLine(writer, config.headerContractLabel + "  $$$1$$$" + sanitizeHeaderValue(config.contract));
        writeLine(writer, config.headerSchemeLabel + "  $$$2$$$" + sanitizeHeaderValue(record.schemeMeasureActual));
        writeLine(writer, config.headerTypeLabel + "  $$$3$$$" + sanitizeHeaderValue(record.equipTypeActual));
        writeLine(writer, config.headerSerialLabel + "  $$$4$$$" + sanitizeHeaderValue(record.serialNumber));
        writeLine(writer, config.headerParam11Label + "  $$$11$$$" + config.param11);
        writeLine(writer, config.headerParam12Label + "  $$$12$$$" + config.param12);
        writeLine(writer, config.headerParam13Label + "  $$$13$$$" + config.param13);
        writeLine(writer, config.headerParam14Label + "  $$$14$$$" + config.param14);
        writeLine(writer, config.headerParam15Label + "  $$$15$$$" + config.param15);
        writeLine(writer, config.headerParam16Label + "  $$$16$$$" + config.param16);
        writeLine(writer, config.headerParam17Label + "  $$$17$$$" + config.param17);
    }

    private List<EquipRecord> loadEquipment(Integer equipId, String equipNameFilter) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT eqTGK.EquipID, eq.Name AS EquipName, eqt.Name AS EquipType, ")
                .append("eqTGK.SchemeMeasure, eqTGK.EquipTypeActual, eqTGK.SchemeMeasureActual, eqTGK.SerialNumber ")
                .append("FROM EquipTGK01 eqTGK ")
                .append("JOIN Equip eq ON eq.Id = eqTGK.EquipID ")
                .append("JOIN EquipType eqt ON eq.EquipTypeId = eqt.Id ");

        List<Object> params = new ArrayList<>();
        if (equipId != null) {
            sql.append("WHERE eqTGK.EquipID = ? ");
            params.add(equipId);
        } else if (equipNameFilter != null && !equipNameFilter.isBlank()) {
            sql.append("WHERE eq.Name LIKE ? ");
            params.add(equipNameFilter);
        }

        sql.append("ORDER BY eqTGK.EquipID");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<EquipRecord> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(new EquipRecord(
                            rs.getInt("EquipID"),
                            rs.getString("EquipName"),
                            rs.getString("EquipType"),
                            rs.getString("SchemeMeasure"),
                            rs.getString("EquipTypeActual"),
                            rs.getString("SchemeMeasureActual"),
                            rs.getString("SerialNumber")
                    ));
                }
                return results;
            }
        }
    }

    private static Path resolveOutputDir(String output) {
        Path path = Paths.get(output);
        if (path.isAbsolute()) {
            return path;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }

    private static String buildFileName(String equipName) {
        String cleaned = safe(equipName).trim()
                .replace(' ', '_')
                .replace('.', '_')
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        if (cleaned.isEmpty()) {
            cleaned = "report";
        }
        return cleaned + ".txt";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sanitizeHeaderValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return "-";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "-".equals(trimmed)) {
            return "-";
        }
        if (!SCI_NOTATION.matcher(trimmed).matches()) {
            return trimmed;
        }
        try {
            return new BigDecimal(trimmed).toPlainString();
        } catch (NumberFormatException ex) {
            return trimmed;
        }
    }

    private static void writeLine(BufferedWriter writer, String line) throws IOException {
        writer.write(line);
        writer.write("\r\n");
    }
}
