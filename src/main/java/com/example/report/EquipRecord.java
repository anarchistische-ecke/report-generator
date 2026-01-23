package com.example.report;

final class EquipRecord {
    final int equipId;
    final String equipName;
    final String equipType;
    final String schemeMeasure;
    final String equipTypeActual;
    final String schemeMeasureActual;
    final String serialNumber;

    EquipRecord(int equipId, String equipName, String equipType, String schemeMeasure,
                String equipTypeActual, String schemeMeasureActual, String serialNumber) {
        this.equipId = equipId;
        this.equipName = equipName;
        this.equipType = equipType;
        this.schemeMeasure = schemeMeasure;
        this.equipTypeActual = equipTypeActual;
        this.schemeMeasureActual = schemeMeasureActual;
        this.serialNumber = serialNumber;
    }
}
