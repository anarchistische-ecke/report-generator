package com.example.report;

import java.util.Objects;

final class ValueSpec {
    enum Kind {
        DATE,
        HOUR,
        COLUMN,
        LITERAL
    }

    private final Kind kind;
    private final String value;

    private ValueSpec(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    static ValueSpec date() {
        return new ValueSpec(Kind.DATE, null);
    }

    static ValueSpec hour() {
        return new ValueSpec(Kind.HOUR, null);
    }

    static ValueSpec column(String name) {
        Objects.requireNonNull(name, "column name");
        return new ValueSpec(Kind.COLUMN, name);
    }

    static ValueSpec literal(String value) {
        Objects.requireNonNull(value, "literal");
        return new ValueSpec(Kind.LITERAL, value);
    }

    static ValueSpec columnOrDash(String name) {
        return name == null ? literal("-") : column(name);
    }

    String toSqlExpression(String alias) {
        switch (kind) {
            case DATE:
                return "CONVERT(nvarchar(10), SourceTime, 104) AS " + alias;
            case HOUR:
                return "CAST(CASE WHEN DATEPART(HOUR, SourceTime) = 0 THEN 1 "
                        + "WHEN DATEPART(HOUR, SourceTime) > 0 THEN DATEPART(HOUR, SourceTime) + 1 "
                        + "ELSE DATEPART(HOUR, SourceTime) END AS nvarchar(2)) AS " + alias;
            case COLUMN:
                return "COALESCE(CONVERT(nvarchar(50), " + bracket(value) + "), N'-') AS " + alias;
            case LITERAL:
                return "N'" + escapeSql(value) + "' AS " + alias;
            default:
                throw new IllegalStateException("Unsupported kind: " + kind);
        }
    }

    private static String bracket(String column) {
        if (column.startsWith("[") && column.endsWith("]")) {
            return column;
        }
        return "[" + column + "]";
    }

    private static String escapeSql(String input) {
        return input.replace("'", "''");
    }
}
