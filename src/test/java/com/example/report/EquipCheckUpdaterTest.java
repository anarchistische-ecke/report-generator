package com.example.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipCheckUpdaterTest {
    @TempDir
    Path tempDir;

    @Test
    void normalizeLookupValueIgnoresWhitespaceAndConfusableLetters() {
        assertEquals(
                EquipCheckUpdater.normalizeLookupValue("xxx A"),
                EquipCheckUpdater.normalizeLookupValue("xxxA")
        );
        assertEquals(
                EquipCheckUpdater.normalizeLookupValue("xxxA"),
                EquipCheckUpdater.normalizeLookupValue("xxxА")
        );
        assertEquals(
                EquipCheckUpdater.normalizeLookupValue(" АВC 123 "),
                EquipCheckUpdater.normalizeLookupValue("ABC123")
        );
    }

    @Test
    void parseCsvLineSupportsSemicolonDelimitedFiles() {
        List<String> columns = EquipCheckUpdater.parseCsvLine("ПРЭМ;523005;31.08.29");

        assertEquals(List.of("ПРЭМ", "523005", "31.08.29"), columns);
    }

    @Test
    void parseCsvLineSupportsQuotedSemicolonDelimitedFiles() {
        List<String> columns = EquipCheckUpdater.parseCsvLine("\"КТСП;Н\";\"25686Г\";31.07.29");

        assertEquals(List.of("КТСП;Н", "25686Г", "31.07.29"), columns);
    }

    @Test
    void readInputSupportsQuotedMultilineCsvRecords() throws Exception {
        Path input = tempDir.resolve("data.csv");
        Files.writeString(input, "ТСП-Н;\"3960\n зип №10285\";25.08.29\nПРЭМ;523005;31.08.29\n");
        List<EquipCheckUpdater.SkippedRow> skipped = new ArrayList<>();

        EquipCheckUpdater.InputData data = EquipCheckUpdater.readInput(input, false, skipped);

        assertEquals(2, data.totalRows);
        assertEquals(2, data.rows.size());
        assertEquals(0, skipped.size());
        assertEquals(1, data.rows.get(0).rowNumber);
        assertEquals("3960\n зип №10285", data.rows.get(0).serial);
        assertEquals(LocalDate.of(2029, 8, 25), data.rows.get(0).nextCheckDate);
        assertEquals(3, data.rows.get(1).rowNumber);
    }

    @Test
    void findMatchingDeviceUsesSerialMatchWhenItIsUnique() {
        EquipCheckUpdater.InputRow row =
                new EquipCheckUpdater.InputRow(2, "CSV Name", "xxxА", LocalDate.of(2026, 4, 23));
        EquipCheckUpdater.DbDevice device =
                new EquipCheckUpdater.DbDevice("DB Name", "xxxA", "DBNAME", "XXXA");

        EquipCheckUpdater.DbDevice matched = EquipCheckUpdater.findMatchingDevice(row, List.of(device));

        assertNotNull(matched);
        assertEquals("xxxA", matched.serial);
    }

    @Test
    void findMatchingDeviceUsesNormalizedNameAsTieBreaker() {
        EquipCheckUpdater.InputRow row =
                new EquipCheckUpdater.InputRow(3, "Device А", "SN001", LocalDate.of(2026, 4, 23));
        EquipCheckUpdater.DbDevice first =
                new EquipCheckUpdater.DbDevice(
                        "Device B",
                        "SN001",
                        EquipCheckUpdater.normalizeLookupValue("Device B"),
                        EquipCheckUpdater.normalizeLookupValue("SN001")
                );
        EquipCheckUpdater.DbDevice second =
                new EquipCheckUpdater.DbDevice(
                        "Device A",
                        "SN 001",
                        EquipCheckUpdater.normalizeLookupValue("Device A"),
                        EquipCheckUpdater.normalizeLookupValue("SN 001")
                );

        EquipCheckUpdater.DbDevice matched = EquipCheckUpdater.findMatchingDevice(row, List.of(first, second));

        assertNotNull(matched);
        assertEquals("Device A", matched.name);
        assertEquals("SN 001", matched.serial);
    }

    @Test
    void findMatchingDeviceReturnsNullWhenMultipleCandidatesRemain() {
        EquipCheckUpdater.InputRow row =
                new EquipCheckUpdater.InputRow(4, "Device A", "SN001", LocalDate.of(2026, 4, 23));
        EquipCheckUpdater.DbDevice first =
                new EquipCheckUpdater.DbDevice(
                        "Device A",
                        "SN001",
                        EquipCheckUpdater.normalizeLookupValue("Device A"),
                        EquipCheckUpdater.normalizeLookupValue("SN001")
                );
        EquipCheckUpdater.DbDevice second =
                new EquipCheckUpdater.DbDevice(
                        "Device А",
                        "SN 001",
                        EquipCheckUpdater.normalizeLookupValue("Device А"),
                        EquipCheckUpdater.normalizeLookupValue("SN 001")
                );

        assertNull(EquipCheckUpdater.findMatchingDevice(row, List.of(first, second)));
    }

    @Test
    void buildUpdateLogContentsIncludesSkippedMissingAndWriteErrors() {
        EquipCheckUpdater.UpdateSummary summary =
                new EquipCheckUpdater.UpdateSummary(Path.of("/tmp/input.csv"));
        summary.totalRows = 5;
        summary.updatedRows = 2;
        summary.skipped.add(new EquipCheckUpdater.SkippedRow(7, "Missing name or serial"));
        summary.missing.add(new EquipCheckUpdater.MissingDevice("Meter A", "SN001"));
        summary.missingCsvPath = Path.of("/tmp/UPDATE 23-04-2026.csv");
        summary.writeErrors.add(new EquipCheckUpdater.WriteError(
                "DB update",
                9,
                "Meter B",
                "SN002",
                "java.sql.SQLException: deadlock victim",
                "java.sql.SQLException: deadlock victim\n\tat test\n"
        ));

        String logContents = EquipCheckUpdater.buildUpdateLogContents(summary);

        assertTrue(logContents.contains("Status: COMPLETED_WITH_ERRORS"));
        assertTrue(logContents.contains("Missing CSV: /tmp/UPDATE 23-04-2026.csv"));
        assertTrue(logContents.contains("Skipped rows"));
        assertTrue(logContents.contains("Row 7: Missing name or serial"));
        assertTrue(logContents.contains("Missing devices"));
        assertTrue(logContents.contains("Name=\"Meter A\", Serial=\"SN001\""));
        assertTrue(logContents.contains("Write errors"));
        assertTrue(logContents.contains("Row 9 | DB update | Name=\"Meter B\" | Serial=\"SN002\" | java.sql.SQLException: deadlock victim"));
    }

    @Test
    void throwIfFailedRethrowsFatalFailure() {
        EquipCheckUpdater.UpdateSummary summary =
                new EquipCheckUpdater.UpdateSummary(Path.of("/tmp/input.csv"));
        SQLException failure = new SQLException("connection lost");

        summary.recordFatalFailure("Updater execution", failure);

        SQLException thrown = assertThrows(SQLException.class, summary::throwIfFailed);
        assertEquals("connection lost", thrown.getMessage());
        assertEquals("FAILED", summary.status());
    }
}
