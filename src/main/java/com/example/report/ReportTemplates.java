package com.example.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ReportTemplates {
    private static final String CYR_T = "\u0422";
    private static final String CYR_V = "\u0412";
    private static final String CYR_V_LOWER = "\u0432";
    private static final String CYR_G = "\u0433";
    private static final String CYR_O = "\u043e";
    private static final String CYR_T_LOWER = "\u0442";
    private static final String CYR_S_LOWER = "\u0441";

    private static final String TV1 = CYR_T + CYR_V_LOWER + "1";
    private static final String TV2 = CYR_T + CYR_V_LOWER + "2";
    private static final String TV1_UPPER = CYR_T + CYR_V + "1";
    private static final String TV2_UPPER = CYR_T + CYR_V + "2";

    private static final String EQUIP_SPT941 = "\u0421\u041f\u0422941.11";
    private static final String EQUIP_SPT943_TV1_TV2 = "\u0421\u041f\u0422943 " + TV1_UPPER + "+" + TV2_UPPER;
    private static final String EQUIP_SPT944 = "\u0421\u041f\u0422944";
    private static final String EQUIP_VKT7 = "\u0412\u041a\u0422-7";
    private static final String EQUIP_TV7 = CYR_T + CYR_V + "7";
    private static final String EQUIP_TSRV_024M = "\u0422\u0421\u0420\u0412-024\u041c";

    private static final String M1_TV1 = "M1_" + TV1;
    private static final String V1_TV1 = "V1_" + TV1;
    private static final String T1_TV1 = "t1_" + TV1;
    private static final String P1_TV1 = "P1_" + TV1;
    private static final String M2_TV1 = "M2_" + TV1;
    private static final String V2_TV1 = "V2_" + TV1;
    private static final String T2_TV1 = "t2_" + TV1;
    private static final String P2_TV1 = "P2_" + TV1;

    private static final String M1_TV2 = "M1_" + TV2;
    private static final String V1_TV2 = "V1_" + TV2;
    private static final String T1_TV2 = "t1_" + TV2;
    private static final String P1_TV2 = "P1_" + TV2;
    private static final String M2_TV2 = "M2_" + TV2;
    private static final String V2_TV2 = "V2_" + TV2;
    private static final String T2_TV2 = "t2_" + TV2;
    private static final String P2_TV2 = "P2_" + TV2;
    private static final String M3_TV2 = "M3_" + TV2;

    private static final String Q_TV1 = "Q_" + TV1;
    private static final String Q_TV2 = "Q_" + TV2;
    private static final String QG_TV1 = "Q" + CYR_G + "_" + TV1;
    private static final String QG_TV2 = "Q" + CYR_G + "_" + TV2;
    private static final String QO_TV1 = "Q" + CYR_O + "_" + TV1;
    private static final String QO_TV2 = "Q" + CYR_O + "_" + TV2;

    private static final String QG_TV2_UPPER = "Q" + CYR_G + "_" + TV2_UPPER;
    private static final String QTV_TV2_UPPER = "Q" + CYR_T_LOWER + CYR_V_LOWER + "_" + TV2_UPPER;
    private static final String WTS1 = "W" + CYR_T_LOWER + CYR_S_LOWER + "1";

    static Map<String, EquipTemplate> buildTemplates() {
        Map<String, EquipTemplate> templates = new HashMap<>();

        templates.put(EQUIP_SPT941, new EquipTemplate(
                "Table_SPT941_11_Present_Hour",
                1,
                Map.of(
                        Scheme.SI_1, schemeSi1("M1", "V1", "t1", null, "M2", "V2", "t2", null, "Q"),
                        Scheme.SI_4, schemeSi4("M1", "V1", "t1", null, "M2", "V2", "t2", null, "Q",
                                null, null, null, null, null),
                        Scheme.SI_5, schemeSi5("M1", "V1", "t1", null, "M2", "V2", "t2", null, "Q",
                                null, null, null, null, null,
                                null, null, null, null, null, null, null)
                )
        ));

        templates.put(EQUIP_SPT943_TV1_TV2, new EquipTemplate(
                "Table_SPT943_Full_Present_Hour",
                1,
                Map.of(
                        Scheme.SI_1, schemeSi1(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, QG_TV1),
                        Scheme.SI_4, schemeSi4(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, Q_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, Q_TV2),
                        Scheme.SI_5, schemeSi5(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, Q_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, null,
                                M2_TV2, V2_TV2, T2_TV2, P2_TV2, null, M3_TV2, Q_TV2)
                )
        ));

        templates.put(EQUIP_SPT944, new EquipTemplate(
                "Table_SPT944_Present_Hour",
                0,
                Map.of(
                        Scheme.SI_1, schemeSi1(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, QG_TV1),
                        Scheme.SI_4, schemeSi4(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, Q_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, Q_TV2),
                        Scheme.SI_5, schemeSi5(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, Q_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, null,
                                M2_TV2, V2_TV2, T2_TV2, P2_TV2, null, M3_TV2, Q_TV2)
                )
        ));

        templates.put(EQUIP_VKT7, new EquipTemplate(
                "Table_VKT_7_Present_Hour",
                1,
                Map.of(
                        Scheme.SI_1, schemeSi1(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, QO_TV1),
                        Scheme.SI_4, schemeSi4(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, QO_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, QO_TV2),
                        Scheme.SI_5, schemeSi5(M1_TV1, V1_TV1, T1_TV1, P1_TV1, M2_TV1, V2_TV1, T2_TV1, P2_TV1, QO_TV1,
                                M1_TV2, V1_TV2, T1_TV2, P1_TV2, null,
                                M2_TV2, V2_TV2, T2_TV2, P2_TV2, null, null, QO_TV2)
                )
        ));

        templates.put(EQUIP_TV7, new EquipTemplate(
                "Table_TV7_Present_Hour",
                1,
                Map.of(
                        Scheme.SI_1, schemeSi1("M1", "V1", "t1", "P1", "M2", "V2", "t2", "P2", null),
                        Scheme.SI_4, schemeSi4("M1", "V1", "t1", "P1", "M2", "V2", "t2", "P2", null,
                                "M3", "V3", "t3", "P3", QG_TV2_UPPER),
                        Scheme.SI_5, schemeSi5("M1", "V1", "t1", "P1", "M2", "V2", "t2", "P2", null,
                                "M3", "V3", "t3", "P3", null,
                                "M2", "V2", "t2", "P2", null, null, QTV_TV2_UPPER)
                )
        ));

        templates.put(EQUIP_TSRV_024M, new EquipTemplate(
                "Table_TSRV_024M_Present_Hour",
                1,
                Map.of(
                        Scheme.SI_1, List.of(
                                ValueSpec.date(),
                                ValueSpec.hour(),
                                ValueSpec.column("M11"),
                                ValueSpec.column("V11"),
                                ValueSpec.column("t11"),
                                ValueSpec.column("P11"),
                                ValueSpec.column("W11"),
                                ValueSpec.column("M12"),
                                ValueSpec.column("V12"),
                                ValueSpec.column("t12"),
                                ValueSpec.column("P12"),
                                ValueSpec.column("W12"),
                                ValueSpec.column("M13"),
                                ValueSpec.column(WTS1),
                                ValueSpec.literal("-"),
                                ValueSpec.literal("-"),
                                ValueSpec.literal("-"),
                                ValueSpec.literal("1"),
                                ValueSpec.literal("-"),
                                ValueSpec.literal("-")
                        )
                )
        ));

        return templates;
    }

    private static List<ValueSpec> schemeSi1(String m1, String v1, String t1, String p1,
                                             String m2, String v2, String t2, String p2,
                                             String dW) {
        return List.of(
                ValueSpec.date(),
                ValueSpec.hour(),
                ValueSpec.columnOrDash(m1),
                ValueSpec.columnOrDash(v1),
                ValueSpec.columnOrDash(t1),
                ValueSpec.columnOrDash(p1),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(m2),
                ValueSpec.columnOrDash(v2),
                ValueSpec.columnOrDash(t2),
                ValueSpec.columnOrDash(p2),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(dW),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("1"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-")
        );
    }

    private static List<ValueSpec> schemeSi4(String m1, String v1, String t1, String p1,
                                             String m2, String v2, String t2, String p2,
                                             String dW, String m1gv, String v1gv,
                                             String t1gv, String p1gv, String w1gv) {
        return List.of(
                ValueSpec.date(),
                ValueSpec.hour(),
                ValueSpec.columnOrDash(m1),
                ValueSpec.columnOrDash(v1),
                ValueSpec.columnOrDash(t1),
                ValueSpec.columnOrDash(p1),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(m2),
                ValueSpec.columnOrDash(v2),
                ValueSpec.columnOrDash(t2),
                ValueSpec.columnOrDash(p2),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(dW),
                ValueSpec.columnOrDash(m1gv),
                ValueSpec.columnOrDash(v1gv),
                ValueSpec.columnOrDash(t1gv),
                ValueSpec.columnOrDash(p1gv),
                ValueSpec.columnOrDash(w1gv),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("1"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-")
        );
    }

    private static List<ValueSpec> schemeSi5(String m1, String v1, String t1, String p1,
                                             String m2, String v2, String t2, String p2,
                                             String dW, String m1gv, String v1gv, String t1gv,
                                             String p1gv, String w1gv, String m2gv, String v2gv,
                                             String t2gv, String p2gv, String w2gv, String dmGv,
                                             String dwGv) {
        return List.of(
                ValueSpec.date(),
                ValueSpec.hour(),
                ValueSpec.columnOrDash(m1),
                ValueSpec.columnOrDash(v1),
                ValueSpec.columnOrDash(t1),
                ValueSpec.columnOrDash(p1),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(m2),
                ValueSpec.columnOrDash(v2),
                ValueSpec.columnOrDash(t2),
                ValueSpec.columnOrDash(p2),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.columnOrDash(dW),
                ValueSpec.columnOrDash(m1gv),
                ValueSpec.columnOrDash(v1gv),
                ValueSpec.columnOrDash(t1gv),
                ValueSpec.columnOrDash(p1gv),
                ValueSpec.columnOrDash(w1gv),
                ValueSpec.columnOrDash(m2gv),
                ValueSpec.columnOrDash(v2gv),
                ValueSpec.columnOrDash(t2gv),
                ValueSpec.columnOrDash(p2gv),
                ValueSpec.columnOrDash(w2gv),
                ValueSpec.columnOrDash(dmGv),
                ValueSpec.columnOrDash(dwGv),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("1"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-"),
                ValueSpec.literal("-")
        );
    }
}
