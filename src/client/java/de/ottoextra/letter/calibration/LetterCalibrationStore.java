package de.ottoextra.letter.calibration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LetterCalibrationStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
            .disableHtmlEscaping().create();

    private final Path root;

    public LetterCalibrationStore(Path root) {
        this.root = root;
    }

    public Path sessionDir(String sessionId) {
        return root.resolve(sessionId);
    }

    public void save(LetterCalibrationSession session) {
        try {
            session.updatedAt = System.currentTimeMillis();
            Path dir = sessionDir(session.sessionId);
            Files.createDirectories(dir.resolve("screenshots"));
            Files.writeString(dir.resolve("session.json"), GSON.toJson(session),
                    StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("report.json"), GSON.toJson(session),
                    StandardCharsets.UTF_8);
            writeReport(session, dir.resolve("report.md"));
        } catch (Exception e) {
            throw new IllegalStateException("Briefkalibrierung konnte nicht gespeichert werden", e);
        }
    }

    public LetterCalibrationSession load(String sessionId) {
        try {
            Path file = sessionDir(sessionId).resolve("session.json");
            LetterCalibrationSession session = Files.exists(file)
                    ? GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    LetterCalibrationSession.class) : null;
            if (session != null && (session.suiteId == null || session.suiteId.isBlank())) {
                session.suiteId = "LEGACY_16";
            }
            return session;
        } catch (Exception e) {
            return null;
        }
    }

    public Path screenshot(String sessionId, String caseId) {
        return sessionDir(sessionId).resolve("screenshots").resolve(caseId + ".png");
    }

    public void saveActiveSessionId(String sessionId) {
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve("active-session.txt"), sessionId,
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Aktive Briefkalibrierung nicht speicherbar", e);
        }
    }

    public void saveActiveSessionId(String suiteId, String sessionId) {
        try {
            Files.createDirectories(root);
            Files.writeString(root.resolve(pointerName(suiteId)), sessionId,
                    StandardCharsets.UTF_8);
            Files.writeString(root.resolve("selected-suite.txt"), normalizedSuite(suiteId),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Aktive Briefkalibrierung nicht speicherbar", e);
        }
    }

    public String loadActiveSessionId() {
        try {
            Path file = root.resolve("active-session.txt");
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String loadActiveSessionId(String suiteId) {
        try {
            Path file = root.resolve(pointerName(suiteId));
            if (!Files.exists(file) && "LEGACY_16".equals(normalizedSuite(suiteId))) {
                file = root.resolve("active-session.txt");
            }
            return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String loadSelectedSuite() {
        try {
            Path file = root.resolve("selected-suite.txt");
            return Files.exists(file) ? normalizedSuite(
                    Files.readString(file, StandardCharsets.UTF_8).trim()) : "LEGACY_16";
        } catch (Exception e) {
            return "LEGACY_16";
        }
    }

    public void clearActiveSession() {
        try {
            Files.deleteIfExists(root.resolve("active-session.txt"));
        } catch (Exception ignored) {
        }
    }

    public void clearActiveSession(String suiteId) {
        try {
            Files.deleteIfExists(root.resolve(pointerName(suiteId)));
            Files.deleteIfExists(root.resolve("selected-suite.txt"));
        } catch (Exception ignored) {
        }
    }

    private static String pointerName(String suiteId) {
        return switch (normalizedSuite(suiteId)) {
            case "V2_HYBRID_8" -> "active-v2-session.txt";
            case "V2_REFINE_6" -> "active-v2-refine-session.txt";
            case "V2_FINAL_8" -> "active-v2-final-session.txt";
            case "V2_BOUNDARY_15" -> "active-v2-boundary-session.txt";
            default -> "active-v1-session.txt";
        };
    }

    private static String normalizedSuite(String suiteId) {
        if ("V2_HYBRID_8".equalsIgnoreCase(suiteId)) {
            return "V2_HYBRID_8";
        }
        if ("V2_REFINE_6".equalsIgnoreCase(suiteId)) {
            return "V2_REFINE_6";
        }
        if ("V2_FINAL_8".equalsIgnoreCase(suiteId)) {
            return "V2_FINAL_8";
        }
        if ("V2_BOUNDARY_15".equalsIgnoreCase(suiteId)) {
            return "V2_BOUNDARY_15";
        }
        return "LEGACY_16";
    }

    private static void writeReport(LetterCalibrationSession session, Path file) throws Exception {
        StringBuilder report = new StringBuilder("# OttoExtra Briefkalibrierung ")
                .append(session.sessionId).append("\n\n")
                .append("Server: `").append(session.serverAddress).append("`  \n")
                .append("Mod: `").append(session.modVersion).append("`  \n")
                .append("Suite: `").append(session.suiteId).append("`  \n")
                .append("Modus: `").append(session.sendMode).append("`\n\n");
        if ("V2_BOUNDARY_15".equals(session.suiteId)) {
            report.append("## Automatische Grenzauswertung\n\n")
                    .append("- Größte bestätigte 9-Zeilen-Breite: `")
                    .append(session.boundaryFitWidth).append(" px`\n")
                    .append("- Kleinste bestätigte 9-Zeilen-Trennung: `")
                    .append(session.boundarySplitWidth).append(" px`\n")
                    .append("- Zeilenumbruchkosten: `")
                    .append(session.inferredLineBreakCost >= 0
                            ? session.inferredLineBreakCost + " px"
                            : session.lineCostMin + "–" + session.lineCostMax + " px")
                    .append("`\n\n");
        }
        for (LetterCalibrationSession.CaseResult result : session.cases) {
            report.append("## ").append(result.id).append(" — ").append(result.status)
                    .append("\n\n").append(result.purpose).append("\n\n");
            if (result.probeKind != null && !result.probeKind.isBlank()) {
                report.append("Messart: `").append(result.probeKind).append("`  \n")
                        .append("Zielbreite: `").append(result.targetRenderedWidth)
                        .append(" px`  \n")
                        .append("Transportbreite: `")
                        .append(result.expectedTransportRenderedWidth).append(" px`  \n")
                        .append("Logische Zeilen / Commands / Serverseiten: `")
                        .append(result.logicalLineCount).append(" / ")
                        .append(result.commandCount).append(" / ")
                        .append(result.observedPageCount).append("`\n\n");
            }
            if (result.differences.isEmpty() && result.status.startsWith("CAPTURED")) {
                report.append("Keine semantische Abweichung.\n\n");
            } else {
                for (String difference : result.differences) {
                    report.append("- ").append(difference).append('\n');
                }
                report.append('\n');
            }
            appendBlock(report, "Quelle", result.sourcePages);
            appendBlock(report, "Intendierte Seiten nach Client-Codec", result.intendedPages);
            appendBlock(report, "Serverbuch", result.observedPages);
            appendBlock(report, "Gesendete Commands", result.commands);
            appendBlock(report, "Transportarten", result.transports);
            appendBlock(report, "Warnungen", result.warnings);
        }
        Files.writeString(file, report.toString(), StandardCharsets.UTF_8);
    }

    private static void appendBlock(StringBuilder report, String heading, java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        report.append("### ").append(heading).append("\n\n");
        for (int i = 0; i < values.size(); i++) {
            report.append("**").append(i + 1).append("**\n\n```text\n")
                    .append(values.get(i).replace("```", "` ` `"))
                    .append("\n```\n\n");
        }
    }
}
