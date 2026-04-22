package com.example.report;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EquipCheckUpdaterTest {

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
}
