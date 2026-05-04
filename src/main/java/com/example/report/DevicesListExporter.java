package com.example.report;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PageMargin;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class DevicesListExporter {
    private static final String OUTPUT_FILE = "devices.xlsx";
    private static final String SHEET_NAME = "Sheet";
    private static final String[] HEADERS = {
            "Модель",
            "Исполнение прибора",
            "Заводской номер",
            "Объект учета",
            "Последняя поверка",
            "Следующая поверка"
    };
    private static final double[] COLUMN_WIDTHS = {
            13.6640625,
            24.0,
            9.5,
            38.33203125,
            10.6640625,
            11.6640625
    };
    private static final String LOAD_DEVICES_SQL = """
            WITH AddressPath AS (
                SELECT
                    Id,
                    AddressId,
                    CAST(NULLIF(LTRIM(RTRIM(Name)), N'') AS nvarchar(max)) AS FullName
                FROM dbo.Address
                WHERE AddressId IS NULL

                UNION ALL

                SELECT
                    a.Id,
                    a.AddressId,
                    CAST(CONCAT_WS(N', ', ap.FullName, NULLIF(LTRIM(RTRIM(a.Name)), N'')) AS nvarchar(max)) AS FullName
                FROM dbo.Address a
                JOIN AddressPath ap ON ap.Id = a.AddressId
            ),
            DeviceRows AS (
                SELECT
                    e.Id AS EquipId,
                    et.Name AS Model,
                    etm.Name AS DeviceExecution,
                    e.SerialNumber,
                    NULLIF(CONCAT_WS(N', ', NULLIF(LTRIM(RTRIM(ap.FullName)), N''), NULLIF(LTRIM(RTRIM(n.Name)), N'')), N'') AS ObjectName,
                    CAST(e.TimeLastChecking AS date) AS TimeLastChecking,
                    CAST(e.TimeNextChecking AS date) AS TimeNextChecking
                FROM dbo.Equip e
                LEFT JOIN dbo.EquipType et ON et.Id = e.EquipTypeId
                LEFT JOIN dbo.EquipTypeModification etm ON etm.Id = e.EquipTypeModificationId
                LEFT JOIN dbo.NodeEquip ne ON ne.EquipId = e.Id
                LEFT JOIN dbo.Node n ON n.Id = ne.NodeId
                LEFT JOIN AddressPath ap ON ap.Id = n.AddressId
            )
            SELECT
                Model,
                DeviceExecution,
                SerialNumber,
                ObjectName,
                TimeLastChecking,
                TimeNextChecking
            FROM DeviceRows
            ORDER BY
                CASE WHEN ObjectName IS NULL THEN 0 ELSE 1 END,
                ObjectName,
                EquipId
            OPTION (MAXRECURSION 32767)
            """;

    private final Connection connection;
    private final Config config;

    DevicesListExporter(Connection connection, Config config) {
        this.connection = connection;
        this.config = config;
    }

    ExportSummary export() throws SQLException, IOException {
        Path outputDir = resolveOutputDir(config.outputDir);
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve(OUTPUT_FILE);
        int rowCount;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(SHEET_NAME);
            configureSheet(sheet);

            Styles styles = Styles.create(workbook);
            writeHeader(sheet, styles.header);
            rowCount = writeDevices(sheet, styles);

            try (OutputStream output = Files.newOutputStream(outputFile)) {
                workbook.write(output);
            }
        }

        return new ExportSummary(outputFile, rowCount);
    }

    private int writeDevices(XSSFSheet sheet, Styles styles) throws SQLException {
        int rowIndex = 1;
        try (PreparedStatement stmt = connection.prepareStatement(LOAD_DEVICES_SQL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                row.setHeightInPoints(15.75f);

                writeText(row, 0, rs.getString("Model"), styles.text);
                writeText(row, 1, rs.getString("DeviceExecution"), styles.text);
                writeText(row, 2, rs.getString("SerialNumber"), styles.text);
                writeText(row, 3, rs.getString("ObjectName"), styles.text);
                writeDate(row, 4, rs.getDate("TimeLastChecking"), styles.date);
                writeDate(row, 5, rs.getDate("TimeNextChecking"), styles.date);
            }
        }
        return rowIndex - 1;
    }

    private static void configureSheet(XSSFSheet sheet) {
        sheet.setDisplayGridlines(false);
        sheet.setZoom(117);

        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            sheet.setColumnWidth(i, (int) Math.round(COLUMN_WIDTHS[i] * 256));
        }

        sheet.setMargin(PageMargin.LEFT, 0.79000002145767212);
        sheet.setMargin(PageMargin.RIGHT, 0.38999998569488525);
        sheet.setMargin(PageMargin.TOP, 0.38999998569488525);
        sheet.setMargin(PageMargin.BOTTOM, 0.38999998569488525);

        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        printSetup.setLandscape(true);
    }

    private static void writeHeader(XSSFSheet sheet, XSSFCellStyle headerStyle) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(12.75f);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(HEADERS[i]);
        }
    }

    private static void writeText(Row row, int column, String value, XSSFCellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null && !value.isBlank()) {
            cell.setCellValue(value);
        }
    }

    private static void writeDate(Row row, int column, Date value, XSSFCellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value.toLocalDate());
        }
    }

    private static Path resolveOutputDir(String output) {
        Path path = Paths.get(output);
        if (path.isAbsolute()) {
            return path;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
    }

    static final class ExportSummary {
        final Path outputFile;
        final int rowCount;

        ExportSummary(Path outputFile, int rowCount) {
            this.outputFile = outputFile;
            this.rowCount = rowCount;
        }
    }

    private static final class Styles {
        final XSSFCellStyle header;
        final XSSFCellStyle text;
        final XSSFCellStyle date;

        private Styles(XSSFCellStyle header, XSSFCellStyle text, XSSFCellStyle date) {
            this.header = header;
            this.text = text;
            this.date = date;
        }

        static Styles create(XSSFWorkbook workbook) {
            Font font = workbook.createFont();
            font.setFontName("Tahoma");
            font.setFontHeightInPoints((short) 8);

            CreationHelper helper = workbook.getCreationHelper();
            XSSFCellStyle header = baseStyle(workbook, font, true);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setDataFormat(helper.createDataFormat().getFormat("@"));

            XSSFCellStyle text = baseStyle(workbook, font, false);
            text.setDataFormat(helper.createDataFormat().getFormat("@"));

            XSSFCellStyle date = baseStyle(workbook, font, false);
            date.setDataFormat(helper.createDataFormat().getFormat("dd\\.mm\\.yyyy"));

            return new Styles(header, text, date);
        }

        private static XSSFCellStyle baseStyle(XSSFWorkbook workbook, Font font, boolean header) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(header ? rgb(0xD3, 0xD3, 0xD3) : rgb(0xFF, 0xFF, 0xFF));
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            XSSFColor border = rgb(0xA9, 0xA9, 0xA9);
            style.setLeftBorderColor(border);
            style.setRightBorderColor(border);
            style.setTopBorderColor(border);
            style.setBottomBorderColor(border);
            style.setAlignment(HorizontalAlignment.LEFT);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            return style;
        }

        private static XSSFColor rgb(int red, int green, int blue) {
            return new XSSFColor(
                    new byte[]{(byte) red, (byte) green, (byte) blue},
                    new DefaultIndexedColorMap()
            );
        }
    }
}
