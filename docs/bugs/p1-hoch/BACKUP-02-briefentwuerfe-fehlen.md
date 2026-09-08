# BACKUP-02: Vollbackup enthält gespeicherte Briefentwürfe nicht

Priorität: **P1 – hoch**. Status: behoben; automatisierter Regressionstest grün (06.09.2026). Kein echtes Benutzerbackup verändert.

## Auslöser und Folge

Ein als Entwurf gespeicherter Brief fehlt nach einer Wiederherstellung. Das Backup wird als Sicherung aller OttoExtra-Daten einschließlich Briefen angeboten, berücksichtigt aber den tatsächlichen Entwurfsordner nicht. Beim Verlust des Originals hilft diese Sicherung nicht.

## Ursprünglicher Codepfad

- [OttoExtraBackupService.java:62](../../../src/client/java/de/ottoextra/config/OttoExtraBackupService.java#L62): Quellen enthalten `.cache/letters` und `letters`, jedoch nicht `drafts`.
- [SavedDraftStore.java:24](../../../src/client/java/de/ottoextra/letter/SavedDraftStore.java#L24): Benutzt `OttoExtraPaths.draftsDir()`.
- [OttoExtraPaths.java:40](../../../src/client/java/de/ottoextra/config/OttoExtraPaths.java#L40): Dieser Pfad ist `config/ottoextra/drafts`.
- [de_de.json:579](../../../src/main/resources/assets/ottoextra/lang/de_de.json#L579): Die Backup-Beschreibung schließt Briefe ausdrücklich ein.

## Reproduktion

Einen Testentwurf speichern und ein GUI-Backup erstellen. Im Backupmanifest und Dateibaum nach dessen JSON suchen: Der Entwurf fehlt bereits vor jeder Wiederherstellung.

## Umsetzung und Regression

[OttoExtraBackupService.java](../../../src/client/java/de/ottoextra/config/OttoExtraBackupService.java) nimmt den zentralen Entwurfspfad in die Backupquellen auf. [OttoExtraPaths.java](../../../src/client/java/de/ottoextra/config/OttoExtraPaths.java) verwendet für Laufzeit und temporäre Testpfade dieselbe Pfadbildung (`draftsDir`).

[OttoExtraBackupServiceTest.java](../../../src/test/java/de/ottoextra/config/OttoExtraBackupServiceTest.java) legt ein mehrseitiges `LetterDraft`-JSON mit Namen, Umlauten und Sonderzeichen im tatsächlichen `drafts`-Unterordner einer isolierten Config an. Der Test prüft Backupinhalt und Manifest, entfernt ausschließlich die temporäre Originaldatei und prüft nach Restore Bytegleichheit, Namen und Seiteninhalt.

Manuell offen: Entwurf über den Briefeditor speichern, GUI-Backup erstellen, in separatem Testprofil wiederherstellen und über die Entwurfsliste laden. Sicherungen, die vor dieser Änderung ohne Entwurfsdateien erstellt wurden, gewinnen dadurch keine fehlenden Entwürfe zurück.
