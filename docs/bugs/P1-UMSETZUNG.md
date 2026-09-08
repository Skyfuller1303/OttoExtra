# P1-Fehlerbehebung vom 6. September 2026

BACKUP-01, BACKUP-02 und LETTER-01 sind umgesetzt und automatisiert geprüft. Die dafür nötige vollständige Config-Wiederherstellung behebt zusätzlich CONFIG-02. Der vollständige Befundindex steht in [README.md](README.md); 18 Befunde aus P2/P3 bleiben offen.

## Ergebnis

- Brief-/Verkündungsqueues sind an die ursprüngliche Verbindung gebunden. Disconnect und Clientende brechen sie ab; alte Callbacks verändern weder neue Queues noch deren Fortschrittscache. Parallele Starts werden vor Cacheänderungen abgewiesen, Recovery erhält den bisher gespeicherten Versandfortschritt.
- Restore übernimmt die Config in die vorhandenen Modulobjekte, gleicht den GUI-Snapshot ab und koordiniert Personenbuch-/Titelreloads mit deren Schreibzugriffen. Teilfehler werden gemeldet; bereits kopierte Configwerte bleiben mit dem Livezustand synchron. Ungültige Titelkataloge ersetzen den bisherigen nutzbaren Katalog nicht.
- Gespeicherte Entwürfe unter `drafts` sind in Backupmanifest und Wiederherstellung enthalten. Gleichzeitige Zeitstempel erzeugen unterschiedliche Backupordner und überschreiben keine ausgewählte Restorequelle.

## Akzeptanzkriterien

| Bereich | Aktion | Erwartetes Verhalten |
| --- | --- | --- |
| Briefqueue | Während einer Seitenpause disconnecten und sofort erneut verbinden | Kein Befehl der alten Queue erreicht die neue Verbindung |
| Client-Callback | Bereits geplanten Schritt erst nach Abbruch ausführen | Kein Versand, Fortschrittsupdate oder Abschlusscallback der alten Queue |
| Recovery | Nach Abbruch den erhaltenen Fortschritt fortsetzen | Genau eine aktive Queue; bestätigter Fortschritt bleibt erhalten |
| Clientende | Client mit laufendem Versand beenden | Queue wird beendet, keine späteren Versandaktionen |
| Config-Restore | Backup laden, Einstellungen schließen und Config erneut lesen | Backupwerte bleiben erhalten; bestehende Config-Referenzen sehen den neuen Stand |
| GUI-Snapshot | Nach erfolgreichem Restore eine weitere Änderung verwerfen | Es gilt der wiederhergestellte Stand, nicht der Stand vor dem Restore |
| Personenbuch | Restore bei vorgemerktem Autosave; anschließend erneut speichern | Wiederhergestellte Identitäten bleiben erhalten |
| Titel | Gesicherte Titeldateien wiederherstellen und später speichern | Laufender Titelstand entspricht der Sicherung |
| Entwürfe | Gespeicherten Brief sichern und in ein Testverzeichnis zurückspielen | Entwurfs-JSON und Inhalt sind in Sicherung und Restore enthalten |
| Backupname | Mehrere Sicherungen unmittelbar nacheinander erstellen | Getrennte Ordner; die ausgewählte Restorequelle wird nicht überschrieben |
| Reload-Fehler | Defekten Titelkatalog wiederherstellen oder einen Reload fehlschlagen lassen | Geregelte Fehlermeldung, übrige Reloads laufen; GUI-Snapshotabgleich bleibt erreichbar |

## Manuelle Prüfung im Testprofil

Diese Schritte werden nicht automatisch auf einem echten Server oder mit produktiven Benutzerdaten ausgeführt.

1. Einen mehrseitigen Brief mit bedeutungslosem Testtext vorbereiten. Während der Seitenpause disconnecten und erneut verbinden. Ohne erneute Freigabe darf nichts weitergeschrieben werden. Danach Recovery starten und prüfen, dass der bestätigte Fortschritt verwendet wird.
2. In einer separaten Profilkopie ein Backup erstellen. Config und Personenbuch ändern, dann das Backup wiederherstellen. Einstellungen sowohl über Esc als auch über „Speichern & schließen“ verlassen. Nach Neustart müssen die gesicherten Werte gelten.
3. Nach einem weiteren Restore eine einzelne Einstellung ändern und „Verwerfen“ wählen. Der wiederhergestellte Wert muss erhalten bleiben.
4. Einen benannten Briefentwurf speichern und ein Backup anlegen. Den Originalentwurf außerhalb des Testprofils sichern, aus dem Testprofil entfernen und wiederherstellen. Der Entwurf muss mit unverändertem Inhalt verfügbar sein.

Der Mod ist in `fabric.mod.json` als clientseitig deklariert. Die Änderungen betreffen Clientdienste und lokale Persistenz; ein Dedicated-Server-Start validiert diese Abläufe nicht.

## Automatisierte Validierung

Abschließender Lauf aus `OttoExtra`:

```powershell
.\gradlew.bat test build -x dist --console=plain
```

Ergebnis: **BUILD SUCCESSFUL in 23s**, **357 Tests in 78 Testsuites**, null Fehler/Failures und null übersprungene Tests. Gegenüber dem Audit kamen **27 Regressionstests** hinzu: 14 für Queue-/Verbindungsverhalten und 13 für Backup, Config, Personenbuch und Titelreloads. Zuvor liefen die betroffenen Queue- und Backup-Testklassen auch gezielt erfolgreich.

Die neuen Tests verwenden steuerbare Scheduler/Verbindungen oder temporäre Verzeichnisse. Sie umfassen auch ausstehende Client-Callbacks, Recovery, Autosave, Fehler nach bereits kopierter Config, kollidierende Backupnamen und ungültige Titeldateien. Es wurden keine produktiven Benutzerdaten wiederhergestellt und keine Befehle an einen echten Server gesendet.

Ein unabhängiges Code-Review fand einen zusätzlichen Reload-Fehlerpfad; dieser wurde behoben und mit vier der genannten Tests abgesichert. Die Nachprüfung ergab keine weiteren Befunde im geprüften Änderungsumfang. Die manuellen Spielprüfungen oben bleiben offen.

Gradle meldet weiterhin vorhandene veraltete API-/Gradle-Verwendungen. Der reguläre `dist`-Finalizer wurde ausgeschlossen, weil er vorhandene Release-JARs entfernt; Paketbau und Remapping liefen vollständig über `build/libs`.

## Gebautes Artefakt

[ottoextra-1.16.3-k.jar](../../build/libs/ottoextra-1.16.3-k.jar)

JAR-Inhalt geprüft: Version `1.16.3-k`, Environment `client`, neuer Queue-Owner enthalten, Classfile-Version 65 (Java 21). Die Modversion wurde nicht angehoben.

SHA-256:

```text
f84b87b95bda73a651b23419280e0b8aec1b777ea82bc191bdf9008967d68686
```
