package com.example.report;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

final class Config {
    final String dbUrl;
    final String dbUser;
    final String dbPassword;
    final String outputDir;
    final String contract;
    final String param11;
    final String param12;
    final String param13;
    final String param14;
    final String param15;
    final String param16;
    final String param17;
    final String headerContractLabel;
    final String headerSchemeLabel;
    final String headerTypeLabel;
    final String headerSerialLabel;
    final String headerParam11Label;
    final String headerParam12Label;
    final String headerParam13Label;
    final String headerParam14Label;
    final String headerParam15Label;
    final String headerParam16Label;
    final String headerParam17Label;
    final Map<Scheme, String> schemeHeaders;
    final Map<Scheme, String> schemeUnits;

    private Config(Properties props, Map<Scheme, String> schemeHeaders, Map<Scheme, String> schemeUnits) {
        this.dbUrl = props.getProperty("db.url", "");
        this.dbUser = props.getProperty("db.user", "");
        this.dbPassword = props.getProperty("db.password", "");
        this.outputDir = props.getProperty("report.output", "reports");
        this.contract = props.getProperty("report.contract", "");
        this.param11 = props.getProperty("report.param11", "");
        this.param12 = props.getProperty("report.param12", "");
        this.param13 = props.getProperty("report.param13", "");
        this.param14 = props.getProperty("report.param14", "");
        this.param15 = props.getProperty("report.param15", "");
        this.param16 = props.getProperty("report.param16", "");
        this.param17 = props.getProperty("report.param17", "");
        this.headerContractLabel = props.getProperty("header.contract.label", "");
        this.headerSchemeLabel = props.getProperty("header.scheme.label", "");
        this.headerTypeLabel = props.getProperty("header.type.label", "");
        this.headerSerialLabel = props.getProperty("header.serial.label", "");
        this.headerParam11Label = props.getProperty("header.param11.label", "");
        this.headerParam12Label = props.getProperty("header.param12.label", "");
        this.headerParam13Label = props.getProperty("header.param13.label", "");
        this.headerParam14Label = props.getProperty("header.param14.label", "");
        this.headerParam15Label = props.getProperty("header.param15.label", "");
        this.headerParam16Label = props.getProperty("header.param16.label", "");
        this.headerParam17Label = props.getProperty("header.param17.label", "");
        this.schemeHeaders = schemeHeaders;
        this.schemeUnits = schemeUnits;
    }

    static Config load(Path path) throws IOException {
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            props.load(reader);
        }

        Map<Scheme, String> schemeHeaders = new EnumMap<>(Scheme.class);
        Map<Scheme, String> schemeUnits = new EnumMap<>(Scheme.class);
        for (Scheme scheme : Scheme.values()) {
            schemeHeaders.put(scheme, props.getProperty("scheme." + scheme.code + ".header", ""));
            schemeUnits.put(scheme, props.getProperty("scheme." + scheme.code + ".units", ""));
        }

        return new Config(props, schemeHeaders, schemeUnits);
    }

    Config applyOverrides(Args args) {
        Properties props = new Properties();
        props.setProperty("db.url", firstNonEmpty(args.dbUrl, dbUrl));
        props.setProperty("db.user", firstNonEmpty(args.dbUser, dbUser));
        props.setProperty("db.password", firstNonEmpty(args.dbPass, dbPassword));
        props.setProperty("report.output", firstNonEmpty(args.output, outputDir));
        props.setProperty("report.contract", firstNonEmpty(args.contract, contract));
        props.setProperty("report.param11", firstNonEmpty(args.paramOverrides.get("param11"), param11));
        props.setProperty("report.param12", firstNonEmpty(args.paramOverrides.get("param12"), param12));
        props.setProperty("report.param13", firstNonEmpty(args.paramOverrides.get("param13"), param13));
        props.setProperty("report.param14", firstNonEmpty(args.paramOverrides.get("param14"), param14));
        props.setProperty("report.param15", firstNonEmpty(args.paramOverrides.get("param15"), param15));
        props.setProperty("report.param16", firstNonEmpty(args.paramOverrides.get("param16"), param16));
        props.setProperty("report.param17", firstNonEmpty(args.paramOverrides.get("param17"), param17));
        props.setProperty("header.contract.label", headerContractLabel);
        props.setProperty("header.scheme.label", headerSchemeLabel);
        props.setProperty("header.type.label", headerTypeLabel);
        props.setProperty("header.serial.label", headerSerialLabel);
        props.setProperty("header.param11.label", headerParam11Label);
        props.setProperty("header.param12.label", headerParam12Label);
        props.setProperty("header.param13.label", headerParam13Label);
        props.setProperty("header.param14.label", headerParam14Label);
        props.setProperty("header.param15.label", headerParam15Label);
        props.setProperty("header.param16.label", headerParam16Label);
        props.setProperty("header.param17.label", headerParam17Label);

        return new Config(props, schemeHeaders, schemeUnits);
    }

    String schemeHeader(Scheme scheme) {
        return schemeHeaders.getOrDefault(scheme, "");
    }

    String schemeUnits(Scheme scheme) {
        return schemeUnits.getOrDefault(scheme, "");
    }

    private static String firstNonEmpty(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
