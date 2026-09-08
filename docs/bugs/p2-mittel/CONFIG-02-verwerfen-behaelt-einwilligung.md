# CONFIG-02: Verwerfen setzt geänderte Einwilligungen nicht zurück

Priorität: **P2 – mittel**. Status: im Zuge von BACKUP-01 behoben (06.09.2026). Vollständiges Config-Restore benötigt auch die Consent-Sektion.

## Auslöser und Folge

In den Einstellungen eine Einwilligung ändern und anschließend „Verwerfen“ wählen. Die Änderung bleibt im laufenden Client aktiv. Beispielsweise können RP-Namen-Uploads weiter erlaubt sein, obwohl ihr Einschalten verworfen wurde.

## Ursache vor dem Fix

- [SettingsRegistry.java:215](../../../src/client/java/de/ottoextra/config/settings/SettingsRegistry.java#L215): Consent-Schalter ändern direkt das aktive Config-Objekt.
- [OttoExtraSettingsScreen.java:183](../../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java#L183): „Verwerfen“ ruft `config.restoreFrom(snapshot)` auf.
- [OttoExtraConfig.java:1140](../../../src/client/java/de/ottoextra/config/OttoExtraConfig.java#L1140): Restore kopiert die Funktionssektionen, lässt `consent` jedoch vollständig aus.

## Reproduktion

Upload-Einwilligung zunächst ausschalten, Einstellungen öffnen, einschalten und verwerfen. Das aktive Feld `communityRpNameUploads` bleibt `true`. Auch der umgekehrte Ablauf stellt den Ausgangswert nicht wieder her.

## Umsetzung und Regressionstest

`OttoExtraConfig.restoreFrom()` kopiert jetzt sämtliche Consent-Felder in das bestehende Consent-Objekt. Damit bleiben Referenzen gültig und „Verwerfen“ übernimmt wieder den ursprünglichen Wert.

[ConfigSnapshotRestoreTest.java](../../../src/test/java/de/ottoextra/config/ConfigSnapshotRestoreTest.java) prüft alle Einwilligungsfelder in beide Richtungen, einschließlich alter Snapshots ohne Consent-Sektion. Gezielter Testlauf grün. Die UI übernimmt Einstellungen weiterhin sofort; eine zusätzliche Änderung des Aktivierungszeitpunkts war nicht Teil des Fixes.
