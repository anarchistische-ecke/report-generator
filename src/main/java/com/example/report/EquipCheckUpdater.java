package com.example.report;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.text.Normalizer;

final class EquipCheckUpdater {
    private static final DateTimeFormatter[] DATE_FORMATS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            DateTimeFormatter.ofPattern("d.M.yy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
    };

    private static final DateTimeFormatter CSV_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");

    private final Connection connection;
    private final Config config;

    EquipCheckUpdater(Connection connection, Config config) {
        this.connection = connection;
        this.config = config;
    }

    UpdateSummary update(Path inputPath, String ignoredSheetName, boolean hasHeader) throws IOException, SQLException {
        List<SkippedRow> skipped = new ArrayList<>();
        List<InputRow> rows = readInput(inputPath, hasHeader, skipped);
        Map<String, List<DbDevice>> devicesByNormalizedSerial = indexDevicesByNormalizedSerial(loadDevices());

        String updateSql = buildUpdateSql();
        List<MissingDevice> missing = new ArrayList<>();
        int updated = 0;

        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
            for (InputRow row : rows) {
                DbDevice device = findMatchingDevice(row, devicesByNormalizedSerial.get(row.normalizedSerial));
                if (device == null) {
                    if (devicesByNormalizedSerial.containsKey(row.normalizedSerial)) {
                        skipped.add(new SkippedRow(row.rowNumber, "Ambiguous device match"));
                    } else {
                        missing.add(new MissingDevice(row.name, row.serial));
                    }
                    continue;
                }

                stmt.setDate(1, Date.valueOf(row.nextCheckDate));
                stmt.setString(2, device.name);
                stmt.setString(3, device.serial);
                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    missing.add(new MissingDevice(row.name, row.serial));
                } else {
                    updated += affected;
                }
            }
        }

        Path missingCsvPath = writeMissingCsv(inputPath, missing);
        return new UpdateSummary(rows.size(), updated, missing, skipped, missingCsvPath);
    }

    private Path writeMissingCsv(Path inputPath, List<MissingDevice> missing) throws IOException {
        if (missing.isEmpty()) {
            return null;
        }

        Path outputDir = inputPath.toAbsolutePath().getParent();
        if (outputDir == null) {
            outputDir = Path.of(".").toAbsolutePath();
        }

        String fileName = "UPDATE " + LocalDate.now().format(CSV_DATE) + ".csv";
        Path outputPath = outputDir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write("Name,Serial");
            writer.write("\r\n");
            for (MissingDevice device : missing) {
                writer.write(csvEscape(device.name));
                writer.write(',');
                writer.write(csvEscape(device.serial));
                writer.write("\r\n");
            }
        }

        return outputPath;
    }

    private String buildUpdateSql() {
        String table = sqlIdentifier(config.equipTable);
        String nameCol = sqlIdentifier(config.equipNameColumn);
        String serialCol = sqlIdentifier(config.equipSerialColumn);
        String nextCol = sqlIdentifier(config.equipNextCheckColumn);
        String lastCol = sqlIdentifier(config.equipLastCheckColumn);

        return "UPDATE " + table + " SET "
                + lastCol + " = CASE WHEN " + nextCol + " IS NOT NULL THEN " + nextCol + " ELSE " + lastCol + " END, "
                + nextCol + " = ? WHERE " + nameCol + " = ? AND " + serialCol + " = ?";
    }

    private String buildLoadDevicesSql() {
        String table = sqlIdentifier(config.equipTable);
        String nameCol = sqlIdentifier(config.equipNameColumn);
        String serialCol = sqlIdentifier(config.equipSerialColumn);
        return "SELECT " + nameCol + ", " + serialCol + " FROM " + table;
    }

    private List<DbDevice> loadDevices() throws SQLException {
        List<DbDevice> devices = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(buildLoadDevicesSql());
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                String serial = rs.getString(2);
                String normalizedSerial = normalizeLookupValue(serial);
                if (normalizedSerial.isEmpty()) {
                    continue;
                }
                devices.add(new DbDevice(name, serial, normalizeLookupValue(name), normalizedSerial));
            }
        }
        return devices;
    }

    private static Map<String, List<DbDevice>> indexDevicesByNormalizedSerial(List<DbDevice> devices) {
        Map<String, List<DbDevice>> index = new HashMap<>();
        for (DbDevice device : devices) {
            index.computeIfAbsent(device.normalizedSerial, ignored -> new ArrayList<>()).add(device);
        }
        return index;
    }

    static DbDevice findMatchingDevice(InputRow row, List<DbDevice> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        DbDevice exactNormalizedNameMatch = null;
        for (DbDevice candidate : candidates) {
            if (!candidate.normalizedName.equals(row.normalizedName)) {
                continue;
            }
            if (exactNormalizedNameMatch != null) {
                return null;
            }
            exactNormalizedNameMatch = candidate;
        }
        return exactNormalizedNameMatch;
    }

    private static List<InputRow> readInput(Path inputPath, boolean hasHeader, List<SkippedRow> skipped)
            throws IOException {
        if (!Files.exists(inputPath)) {
            throw new IOException("Input file not found: " + inputPath);
        }

        List<InputRow> rows = new ArrayList<>();

        try (BufferedReader reader = openCsvReader(inputPath)) {
            String line;
            int rowNumber = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (hasHeader && rowNumber == 1) {
                    continue;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                String name = columns.size() > 0 ? columns.get(0).trim() : "";
                String serial = columns.size() > 1 ? columns.get(1).trim() : "";
                String rawDate = columns.size() > 2 ? columns.get(2).trim() : "";

                if (name.isEmpty() && serial.isEmpty()) {
                    continue;
                }

                if (name.isEmpty() || serial.isEmpty()) {
                    skipped.add(new SkippedRow(rowNumber, "Missing name or serial"));
                    continue;
                }

                LocalDate nextDate = parseDate(rawDate);
                if (nextDate == null) {
                    skipped.add(new SkippedRow(rowNumber, "Missing or invalid next checking date"));
                    continue;
                }

                rows.add(new InputRow(rowNumber, name, serial, nextDate));
            }
        }

        return rows;
    }

    private static BufferedReader openCsvReader(Path inputPath) throws IOException {
        InputStream input = Files.newInputStream(inputPath);
        PushbackInputStream pushback = new PushbackInputStream(input, 3);
        byte[] bom = new byte[3];
        int read = pushback.read(bom, 0, 3);
        Charset charset;
        int unread;

        if (read == -1) {
            charset = Charset.defaultCharset();
            unread = 0;
        } else if (read >= 3 && (bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            charset = StandardCharsets.UTF_8;
            unread = read - 3;
        } else {
            charset = Charset.defaultCharset();
            unread = read;
        }

        if (unread > 0) {
            pushback.unread(bom, read - unread, unread);
        }

        return new BufferedReader(new InputStreamReader(pushback, charset));
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }

        String normalized = trimmed.replace(',', '.');
        try {
            double value = Double.parseDouble(normalized);
            if (value > 0) {
                long days = (long) Math.floor(value);
                return LocalDate.of(1899, 12, 30).plusDays(days);
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    current.append(c);
                }
            }
        }

        values.add(current.toString());
        return values;
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    static String normalizeLookupValue(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            result.append(canonicalizeConfusableChar(ch));
        }
        return result.toString();
    }

    private static char canonicalizeConfusableChar(char ch) {
        return switch (ch) {
            case 'А' -> 'A';
            case 'В' -> 'B';
            case 'С' -> 'C';
            case 'Е' -> 'E';
            case 'Н' -> 'H';
            case 'К' -> 'K';
            case 'М' -> 'M';
            case 'О' -> 'O';
            case 'Р' -> 'P';
            case 'Т' -> 'T';
            case 'У' -> 'Y';
            case 'Х' -> 'X';
            default -> ch;
        };
    }

    private static String sqlIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        String[] parts = identifier.split("\\.");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append('.');
            }
            result.append('[').append(parts[i]).append(']');
        }
        return result.toString();
    }

    static final class UpdateSummary {
        final int totalRows;
        final int updatedRows;
        final List<MissingDevice> missing;
        final List<SkippedRow> skipped;
        final Path missingCsvPath;

        UpdateSummary(int totalRows, int updatedRows, List<MissingDevice> missing, List<SkippedRow> skipped,
                      Path missingCsvPath) {
            this.totalRows = totalRows;
            this.updatedRows = updatedRows;
            this.missing = missing;
            this.skipped = skipped;
            this.missingCsvPath = missingCsvPath;
        }

        void log() {
            Logger.info("Update finished. Input rows: " + totalRows
                    + ", updated: " + updatedRows
                    + ", missing: " + missing.size()
                    + ", skipped: " + skipped.size());

            if (!missing.isEmpty() && missingCsvPath != null) {
                Logger.info("Missing devices saved to: " + missingCsvPath.toAbsolutePath());
            }

            if (!skipped.isEmpty()) {
                Logger.info("Skipped rows:");
                for (SkippedRow row : skipped) {
                    Logger.info("Row " + row.rowNumber + ": " + row.reason);
                }
            }
        }
    }

    static final class MissingDevice {
        final String name;
        final String serial;

        MissingDevice(String name, String serial) {
            this.name = name;
            this.serial = serial;
        }
    }

    static final class SkippedRow {
        final int rowNumber;
        final String reason;

        SkippedRow(int rowNumber, String reason) {
            this.rowNumber = rowNumber;
            this.reason = reason;
        }
    }

    static final class InputRow {
        final int rowNumber;
        final String name;
        final String serial;
        final LocalDate nextCheckDate;
        final String normalizedName;
        final String normalizedSerial;

        InputRow(int rowNumber, String name, String serial, LocalDate nextCheckDate) {
            this.rowNumber = rowNumber;
            this.name = name;
            this.serial = serial;
            this.nextCheckDate = nextCheckDate;
            this.normalizedName = normalizeLookupValue(name);
            this.normalizedSerial = normalizeLookupValue(serial);
        }
    }

    static final class DbDevice {
        final String name;
        final String serial;
        final String normalizedName;
        final String normalizedSerial;

        DbDevice(String name, String serial, String normalizedName, String normalizedSerial) {
            this.name = name;
            this.serial = serial;
            this.normalizedName = normalizedName;
            this.normalizedSerial = normalizedSerial;
        }
    }
}
