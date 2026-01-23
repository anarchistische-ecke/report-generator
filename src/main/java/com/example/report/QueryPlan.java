package com.example.report;

final class QueryPlan {
    final String sql;
    final int columnCount;

    QueryPlan(String sql, int columnCount) {
        this.sql = sql;
        this.columnCount = columnCount;
    }
}
