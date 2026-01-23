package com.example.report;

import java.util.List;
import java.util.Map;

final class EquipTemplate {
    private final String tableName;
    private final int excludeEndHour;
    private final Map<Scheme, List<ValueSpec>> schemeSpecs;

    EquipTemplate(String tableName, int excludeEndHour, Map<Scheme, List<ValueSpec>> schemeSpecs) {
        this.tableName = tableName;
        this.excludeEndHour = excludeEndHour;
        this.schemeSpecs = schemeSpecs;
    }

    QueryPlan buildQuery(Scheme scheme) {
        List<ValueSpec> specs = schemeSpecs.get(scheme);
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("No template for scheme " + scheme);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        for (int i = 0; i < specs.size(); i++) {
            ValueSpec spec = specs.get(i);
            sql.append(spec.toSqlExpression("c" + (i + 1)));
            if (i < specs.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" FROM ").append(tableName);
        sql.append(" WHERE EquipId = ? AND SourceTime BETWEEN ? AND ? ");
        sql.append("AND NOT (CONVERT(date, SourceTime) = ? AND DATEPART(HOUR, SourceTime) = ");
        sql.append(excludeEndHour);
        sql.append(") ORDER BY SourceTime");

        return new QueryPlan(sql.toString(), specs.size());
    }
}
