package de.ottoextra.letter.calibration;

import java.util.List;

public final class LetterCalibrationCorpus {

    public record TestCase(String id, String purpose, List<String> pages) {
        public TestCase(String id, String purpose, String page) {
            this(id, purpose, List.of(page));
        }
    }

    private static final List<TestCase> CASES = List.of(
            new TestCase("L01", "Basistransport", "OE-L01\nKurzer Brief."),
            new TestCase("L02", "Unicode", "OE-L02\nGrüße aus Südhafen: ÄÖÜ äöü ß – „Zitat“."),
            new TestCase("L03", "Glyphenbreiten",
                    "OE-L03\nIIIIIIII WWWWWWWW llllllll mmmmmmmm Ende."),
            new TestCase("L04", "Wortumbruch",
                    "OE-L04\nEin schmaler Satz läuft genau bis an den rechten Rand und danach weiter."),
            new TestCase("L05", "Langes Wort",
                    "OE-L05\nDonaudampfschifffahrtsgesellschaftskapitän"),
            new TestCase("L06", "Leerraum",
                    "OE-L06\nErste Zeile.\n\nDritte Zeile.  Zwei Leerzeichen.\n"),
            new TestCase("L07", "Escapes",
                    "OE-L07\nPfad C:\\Ottonien\\Briefe; sichtbar: \\n und \\\\ ."),
            new TestCase("L08", "Literale Codes",
                    "OE-L08\nHandel & Gewinn; Codes: &a und &z; einzelnes & bleibt sichtbar."),
            new TestCase("L09", "Farben und Reset",
                    "§4OE-L09 ROT §aGRÜN §9BLAU§r normal."),
            new TestCase("L10", "Alle Stile",
                    "OE-L10 §lfett§r §okursiv§r §nunterstrichen§r §mdurchgestrichen§r §kmagisch§r."),
            new TestCase("L11", "Stil über automatischen Umbruch",
                    "§6§lOE-L11 Gold fett läuft automatisch über mehrere sichtbare Zeilen "
                            + "und bleibt formatiert§r normal."),
            new TestCase("L12", "Farbcode setzt Stil zurück",
                    "§lOE-L12 fett §crot setzt fett zurück §lrot-fett§r Ende."),
            new TestCase("L13", "Stil über echte Umbrüche",
                    "§e§oOE-L13 Gelb kursiv\nzweite Zeile erbt den Stil\n§rDritte Zeile normal."),
            new TestCase("L14", "Formatierte Leerzeile",
                    "§bOE-L14 Erste Zeile§r\n\n§dDritte Zeile§r"),
            new TestCase("L15", "Explizite Seitengrenzen", List.of(
                    "OE-L15 SEITE 1\nDiese Seite muss allein bleiben.",
                    "OE-L15 SEITE 2\nAuch diese Seite bleibt getrennt.")),
            new TestCase("L16", "Realistischer RP-Brief", List.of(
                    "§6§lOE-L16 Einladung§r\n\nWerte Freundin,\nwir laden Euch am Abend zur "
                            + "Zusammenkunft im Ratshaus von Südhafen ein.",
                    "Bitte bringt Eure Unterlagen mit. Für Speis und Trank ist gesorgt.\n\n"
                            + "§oHochachtungsvoll§r\nMatthias"))
    );

    private static final List<TestCase> V2_CASES = List.of(
            new TestCase("V201", "V2-Basistransport",
                    "OE-V201\nErste normale Zeile.\nZweite normale Zeile."),
            new TestCase("V202", "Einzelne hybride Leerzeile",
                    "OE-V202\nOberer Absatz.\n\nUnterer Absatz."),
            new TestCase("V203", "Mehrfache und empfindliche Leerzeichen",
                    "\nOE-V203\n\n\nZwei  Leerzeichen.\nAbschluss.\n"),
            new TestCase("V204", "Formatierung über Transportwechsel",
                    "§6§lOE-V204 Gold fett\nbleibt im ersten Absatz\n\n"
                            + "und nach der Leerzeile§r normal."),
            new TestCase("V205", "Unicode, Glyphenbreiten und Langwort",
                    "OE-V205\nGrüße aus Südhafen – „Willkommen“. "
                            + "IIII WWWW Donaudampfschifffahrtsgesellschaftskapitän"),
            new TestCase("V206", "Backslashes und Escape-Grenze",
                    "OE-V206\nPfad C:\\Ottonien\\Briefe\nWörtlich \\n bleibt kritisch."),
            new TestCase("V207", "Automatische Serverpagination",
                    "OE-V207\nDieser fortlaufende Brief prüft die automatische Seitengrenze des Servers. "
                            + "Die Zeilen werden nach ihrer sichtbaren Breite umgebrochen und nicht nach "
                            + "einer Entwurfsseite. Weitere Wörter füllen den verfügbaren Raum, bis der "
                            + "Server selbstständig eine neue Buchseite beginnt. Danach muss der Text ohne "
                            + "Verlust und ohne doppelte Zeile auf der Folgeseite weiterlaufen. Ende V207."),
            new TestCase("V208", "Realistischer hybrider RP-Brief", List.of(
                    "§6§lOE-V208 Einladung§r\n\nWerte Freundin,\nwir laden Euch am Abend zur "
                            + "Zusammenkunft im Ratshaus von Südhafen ein.",
                    "Bitte bringt Eure Unterlagen mit. Für Speis und Trank ist gesorgt.\n\n"
                            + "§oHochachtungsvoll§r\nMatthias"))
    );

    private static final List<TestCase> V2_REFINE_CASES = List.of(
            new TestCase("R01", "Budget 179", budgetDocument("R01", 179, false)),
            new TestCase("R02", "Budget 180", budgetDocument("R02", 180, false)),
            new TestCase("R03", "Budget 181", budgetDocument("R03", 181, false)),
            new TestCase("R04", "Budget 180 mit Formatcodes",
                    budgetDocument("R04", 180, true)),
            new TestCase("R05", "Server-Leerzeichenmatrix",
                    "OE-R05\nNormal:[A  B]\nNBSP:[A\u00a0B]\nFigure:[A\u2007B]"
                            + "\nSchmal:[A\u202fB]\nVorne:[\u00a0A]\nHinten:[A\u00a0]"),
            new TestCase("R06", "Realistischer Brief mit 180er-Pagination", List.of(
                    "§6§lOE-R06 Einladung§r\n\nWerte Freundin,\nwir laden Euch am Abend zur "
                            + "Zusammenkunft im Ratshaus von Südhafen ein.",
                    "Bitte bringt Eure Unterlagen mit. Für Speis und Trank ist gesorgt.\n\n"
                            + "§oHochachtungsvoll§r\nMatthias"))
    );

    private static final List<TestCase> V2_FINAL_CASES = List.of(
            new TestCase("F01", "Renderbudget 816 px exakt",
                    pixelBoundaryDocument("F01", 10, false)),
            new TestCase("F02", "Renderbudget 818 px",
                    pixelBoundaryDocument("F02", 11, false)),
            new TestCase("F03", "816 px mit Farbe und Kursiv",
                    "§6§o" + pixelBoundaryDocument("F03", 10, false) + "§r"),
            new TestCase("F04", "816 px mit fetter Kennung",
                    pixelBoundaryDocument("F04", 7, true)),
            new TestCase("F05", "818 px mit fetter Kennung",
                    pixelBoundaryDocument("F05", 8, true)),
            new TestCase("F06", "816 px einschließlich hybrider Leerzeile",
                    hybridBoundaryDocument("F06", 2)),
            new TestCase("F07", "818 px einschließlich hybrider Leerzeile",
                    hybridBoundaryDocument("F07", 3)),
            new TestCase("F08", "Finaler realistischer V2-Brief", List.of(
                    "§6§lOE-F08 Einladung§r\n\nWerte Freundin,\nwir laden Euch am Abend zur "
                            + "Zusammenkunft im Ratshaus von Südhafen ein.",
                    "Bitte bringt Eure Unterlagen mit. Für Speis und Trank ist gesorgt.\n\n"
                            + "§oHochachtungsvoll§r\nMatthias"))
    );

    private static final List<TestCase> V2_BOUNDARY_CASES = java.util.stream.IntStream
            .rangeClosed(1, LetterBoundaryCalibration.CASE_COUNT)
            .mapToObj(number -> {
                String id = "B" + String.format(java.util.Locale.ROOT, "%02d", number);
                String purpose;
                if (number <= 7) {
                    purpose = "Adaptive Rohgrenze " + number + "/7 bei neun Zeilen";
                } else if (number <= 11) {
                    purpose = "Adaptive Zeilenkostenprobe " + (number - 7) + "/4";
                } else {
                    purpose = switch (number) {
                        case 12 -> "Kontrolle: ein PAGE-Command";
                        case 13 -> "Kontrolle: gleicher Text in drei PAGE-Commands";
                        case 14 -> "Kontrolle: hybride Leerzeile";
                        default -> "Kontrolle: Farbe und Fettdruck";
                    };
                }
                return new TestCase(id, purpose,
                        "OE-" + id + "\nWird beim Start adaptiv erzeugt.");
            }).toList();

    private LetterCalibrationCorpus() {
    }

    public static List<TestCase> all() {
        return CASES;
    }

    public static List<TestCase> v2() {
        return V2_CASES;
    }

    public static List<TestCase> v2Refine() {
        return V2_REFINE_CASES;
    }

    public static List<TestCase> v2Final() {
        return V2_FINAL_CASES;
    }

    public static List<TestCase> v2Boundary() {
        return V2_BOUNDARY_CASES;
    }

    public static TestCase byId(String id) {
        return java.util.stream.Stream.of(CASES, V2_CASES, V2_REFINE_CASES,
                        V2_FINAL_CASES, V2_BOUNDARY_CASES)
                .flatMap(List::stream)
                .filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Brieftest: " + id));
    }

    private static String budgetDocument(String id, int targetEffectiveChars,
                                         boolean formatted) {
        String marker = "OE-" + id;
        StringBuilder out = new StringBuilder(formatted
                ? "§6§l" + marker + "§r" : marker);
        for (int i = 0; i < 9; i++) {
            out.append('\n').append("i".repeat(16));
        }
        int visibleEffective = marker.length() + 9 * 18;
        int finalLength = targetEffectiveChars - visibleEffective - 2;
        if (finalLength > 0) {
            out.append('\n').append("x".repeat(finalLength));
        }
        return out.toString();
    }

    private static String pixelBoundaryDocument(String id, int finalNarrowChars,
                                                boolean boldMarker) {
        String marker = "OE-" + id;
        StringBuilder out = new StringBuilder(boldMarker
                ? "§l" + marker + "§r" : marker);
        for (int i = 0; i < 7; i++) {
            out.append('\n').append("x".repeat(18));
        }
        return out.append('\n').append("i".repeat(finalNarrowChars)).toString();
    }

    private static String hybridBoundaryDocument(String id, int finalNarrowChars) {
        StringBuilder out = new StringBuilder("OE-").append(id);
        for (int i = 0; i < 3; i++) {
            out.append('\n').append("x".repeat(18));
        }
        out.append("\n\n");
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append("x".repeat(18));
        }
        return out.append('\n').append("i".repeat(finalNarrowChars)).toString();
    }
}
