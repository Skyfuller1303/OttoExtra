# BACKUP-01: Wiederhergestellte Config wird beim Schließen erneut überschrieben

Priorität: **P1 – hoch**. Status: behoben; automatisierte Regressionstests grün (06.09.2026). GUI-Ablauf nicht im Spiel ausgeführt.

## Auslöser und Folge

Backup und laufende Config enthalten unterschiedliche Werte. Nach erfolgreichem Restore schließt der Benutzer die Einstellungen mit Esc oder „Speichern & schließen“. Die gerade wiederhergestellte Datei wird sofort mit den alten Werten aus dem Arbeitsspeicher überschrieben.

## Ursprünglicher Codepfad

- [OttoExtraSettingsScreen.java:519](../../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java#L519): Restore kopiert das Backup, aktualisiert aber weder das aktive Config-Objekt noch den GUI-Snapshot.
- [OttoExtraBackupService.java:190](../../../src/client/java/de/ottoextra/config/OttoExtraBackupService.java#L190): Wiederherstellung ersetzt ausschließlich Dateien.
- [OttoExtraSettingsScreen.java:195](../../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java#L195): Sowohl Schließen als auch Speichern rufen anschließend `config.save()` auf.

## Reproduktion

In einem separaten Testprofil ein Backup mit `chat.enabled=false` anlegen. Im laufenden Profil `true` einstellen, Backup wiederherstellen und Esc drücken. Danach steht in der Datei wieder `true`.

## Umsetzung

- [OttoExtraBackupService.java](../../../src/client/java/de/ottoextra/config/OttoExtraBackupService.java) liest und prüft die gesicherte Config vor dem Kopieren. Die bestehende Live-Config übernimmt die Werte im selben synchronisierten Abschnitt wie die Dateiwiederherstellung; gehaltene Modulreferenzen bleiben erhalten.
- [OttoExtraConfig.java](../../../src/client/java/de/ottoextra/config/OttoExtraConfig.java) serialisiert Speichern und Restore am selben Monitor. `restoreFrom` übernimmt auch `consent`, damit beim nächsten Save keine alten Einwilligungen zurückgeschrieben werden.
- [OttoExtraSettingsScreen.java](../../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java) erneuert nach erfolgreichem Restore den Verwerfen-Snapshot und fordert eine Aktualisierung des Chatverlaufs an. Dasselbe gilt nach einem Teilfehler, sobald die Live-Config bereits wiederhergestellt wurde; ein abgelehnter Restore ohne Configänderung behält den bisherigen Snapshot.
- [RpNamesServices.java](../../../src/client/java/de/ottoextra/rpnames/RpNamesServices.java) koordiniert Dateikopie und erneutes Laden der bestehenden Personen- und Titelstores mit deren Schreibzugriffen. [LocalRpIdentityStore.java](../../../src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java) sichert ausstehende Änderungen vorab, stoppt geplante Saves und lädt auch nach einem teilweise fehlgeschlagenen Restore den Dateistand nach.
- Fehler eines nachgelagerten Reloads werden protokolliert und als fehlgeschlagener Restore zurückgegeben; die übrigen Reloads und der GUI-Snapshotabgleich bleiben erreichbar. [TitleCatalogStore.java](../../../src/client/java/de/ottoextra/rpnames/title/TitleCatalogStore.java) prüft restaurierte Katalogdateien vor Übernahme und behält bei ungültigen Daten den bisherigen nutzbaren Katalog samt Suchindex.
- Sicherheitsbackups bekommen bei Zeitstempel-Kollisionen einen freien Suffix. Damit kann ein unmittelbar nach dem Erstellen ausgewähltes Backup nicht durch das Sicherheitsbackup vor Restore überschrieben werden.

## Regression und offene Prüfung

- [OttoExtraBackupServiceTest.java](../../../src/test/java/de/ottoextra/config/OttoExtraBackupServiceTest.java): Restore → nächster Config-Save → erneutes Lesen; unveränderte Modulreferenzen; unveränderte Restorequelle; Zeitstempel-Kollision; fehlende, leere und defekte Configs; Dateikopierfehler nach bereits wiederhergestellter Config. Alle Dateien liegen in `@TempDir`.
- [ConfigSnapshotRestoreTest.java](../../../src/test/java/de/ottoextra/config/ConfigSnapshotRestoreTest.java): sämtliche Consent-Felder in beide Richtungen, inklusive alter Snapshots ohne Einwilligungen.
- [LocalRpIdentityStoreTest.java](../../../src/test/java/de/ottoextra/rpnames/store/LocalRpIdentityStoreTest.java): Restore bei ausstehendem Autosave, nächster Save und Shutdown; Teilfehler; Erhalt noch ungespeicherter Daten bei abgelehntem Restore.
- Gezielter JUnit-Lauf dieser drei Klassen: 14 Tests, 0 Fehler, 0 übersprungen; davon 9 neue Regressionstests für BACKUP-01/BACKUP-02 und die zugehörige Consent-Wiederherstellung.
- Nachreview: [RpNamesBackupRestoreTest.java](../../../src/test/java/de/ottoextra/rpnames/RpNamesBackupRestoreTest.java) und [TitleCatalogStoreRestoreTest.java](../../../src/test/java/de/ottoextra/rpnames/title/TitleCatalogStoreRestoreTest.java) ergänzen vier grüne Regressionstests für werfende Reloads und ungültige Katalogdateien. Insgesamt 13 neue Tests für die Backup-/Config-/RP-Korrekturen.
- Manuell offen: Restore im separaten Spielprofil, anschließend Esc, „Speichern & schließen“ und „Verwerfen“ jeweils prüfen. Personenbuch und Titel anschließend bearbeiten/speichern und neu öffnen. Der Configwert und bereits wiederhergestellte Identitäten/Titel müssen erhalten bleiben.
