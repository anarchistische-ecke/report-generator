package com.example.report;

enum Scheme {
    SI_1("SI-1", "\u0421\u0418-1"),
    SI_4("SI-4", "\u0421\u0418-4"),
    SI_5("SI-5", "\u0421\u0418-5");

    final String code;
    final String dbValue;

    Scheme(String code, String dbValue) {
        this.code = code;
        this.dbValue = dbValue;
    }

    static Scheme fromDb(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (Scheme scheme : values()) {
            if (scheme.dbValue.equals(trimmed)) {
                return scheme;
            }
        }
        return null;
    }
}
