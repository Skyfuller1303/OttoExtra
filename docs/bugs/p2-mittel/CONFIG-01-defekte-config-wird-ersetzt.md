# CONFIG-01: Ein Lesefehler überschreibt die gesamte ursprüngliche Config

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Fehlerpfad statisch bestätigt.

## Auslöser und Folge

Ein einzelner ungültiger Feldwert, etwa `requestTimeoutMs="abc"`, verhindert das Einlesen. OttoExtra lädt vollständige Defaults und speichert sie unmittelbar über die Originaldatei. Damit gehen auch alle weiterhin gültigen Einstellungen verloren.

## Codebeleg

- [OttoExtraConfig.java:922](../../../src/client/java/de/ottoextra/config/OttoExtraConfig.java#L922): Der Catch erzeugt eine neue Config; Zeile 930 speichert sie ohne Sicherung des defekten Originals.
- [OttoExtraBackupService.java:83](../../../src/client/java/de/ottoextra/config/OttoExtraBackupService.java#L83): Das vorgeschaltete Migrationsbackup ist einmalig. Bei bereits abgeschlossenem Backup wird der aktuelle Stand nicht erneut gesichert.

## Reproduktion

In einem Wegwerfprofil mit bereits abgeschlossenem Migrationsbackup mehrere Einstellungen ändern. Dann ein Zahlenfeld ungültig machen und neu starten. Auch unbetroffene Änderungen verschwinden aus der Datei.

## Fixansatz und Regressionstest

Das Original unter eindeutigem Fehlerdateinamen sichern, bevor Defaults dauerhaft geschrieben werden. Schlägt die Sicherung fehl, keinen automatischen Ersatz schreiben. Test: ungültiges Einzelfeld führt zu nutzbaren Defaults im Speicher, aber sämtliche Originalbytes bleiben wiederherstellbar.

## Umsetzung am 06.09.2026

Vor dem Speichern von Defaults werden die Originalbytes in eine eindeutige `ottoextra.json.broken-*.json` kopiert und verglichen. Scheitert die Sicherung, sind automatische und spätere Saves dieser Config-Instanz gesperrt; das Original bleibt erhalten.

Regression: [ConfigLoadRecoveryTest.java](../../../src/test/java/de/ottoextra/config/ConfigLoadRecoveryTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
