# Lokale Daten: ansehen, exportieren, löschen

Stand: 11.09.2026. Werkzeug: [tools/local-data.py](../tools/local-data.py), benötigt **Python 3.11+**. Es arbeitet ausschließlich mit Dateien des angegebenen Spielverzeichnisses. Keine Uploads, keine Backend-Löschanträge.

## Ablauf

Alle Minecraft-Instanzen dieses Profils zuerst schließen. OttoExtra hält ab dem P1-Stand bis zum Prozessende eine gemeinsame Dateisperre; das Werkzeug verweigert den Zugriff bei laufendem Client. Ältere Modversionen kennen diese Sperre nicht und müssen ebenfalls geschlossen sein. Keine Datenverwaltung während eines parallelen Backup-Restores oder einer externen Dateisynchronisierung durchführen.

PowerShell im OttoExtra-Repository öffnen. Pfade im Beispiel ersetzen; Plan und Export **außerhalb** des Spielverzeichnisses ablegen. Die Befehle wurden während der Entwicklung ausschließlich gegen isolierte Testdaten ausgeführt.

```powershell
$gameDir = 'C:\Pfad\zum\Minecraft-Profil'
$planFile = 'C:\Pfad\zur\Ablage\ottoextra-loeschplan.json'
$exportFile = 'C:\Pfad\zur\Ablage\ottoextra-export.zip'

# Bestandsliste: Pfad, Kategorie, Größe und Änderungszeit; keine Inhaltsausgabe.
python tools/local-data.py --game-dir $gameDir list

# Vorschau für alte Logs/Caches. Ohne --expired: alle Dateien der gewählten Kategorien.
python tools/local-data.py --game-dir $gameDir plan --categories chat cache --expired --output $planFile
Get-Content -LiteralPath $planFile

# Optionaler lokaler ZIP-Export genau dieser Dateien; bestehende ZIPs werden nicht überschrieben.
python tools/local-data.py --game-dir $gameDir export --plan $planFile --output $exportFile

# Erst nach Prüfung der Vorschau: exakten Plan bestätigen.
$planHash = (Get-FileHash -LiteralPath $planFile -Algorithm SHA256).Hash.ToLowerInvariant()
python tools/local-data.py --game-dir $gameDir delete --plan $planFile --confirm-plan-sha256 $planHash
```

`plan` löscht nichts. Jede Datei im Plan enthält Größe, Änderungszeit und SHA-256. Vor Export/Löschung wird der ganze Plan erneut geprüft. Geänderte/fehlende Dateien, falsches Spielverzeichnis, manipulierte Pfade und Links/Junctions führen zum Abbruch. Gelöscht werden ausschließlich aufgelistete reguläre Dateien, keine rekursiv ausgewählten Verzeichnisse. Fehler mitten im Löschvorgang sind möglich; bereits entfernte Dateien werden nicht zurückgeholt. Es wird dabei **kein neues Backup** der zu löschenden Daten erstellt.

## Getrennte Kategorien

| Auswahl | Enthalten | Besonderheit |
|---|---|---|
| `chat` | `logs/chat/ottoextra-chat-JJJJ-MM-TT-X.log`, alte `logs/ottoextra-chat.log` | Keine Vanilla-Logs und keine beliebigen Dateien im gemeinsamen Logordner. |
| `cache` | `config/ottoextra/cache/`, altes `config/ottoletter-player-cache.json` | Skins, Banner, API-Snapshot, lokale Tieridentitäten. Dateien werden nach erneuter Nutzung/Freigabe ggf. neu erzeugt. |
| `people` | `config/ottoextra/rpnames/` ohne dessen Backups; alte `ottonames.json`, `ottochat-rpnames.json`, `ottochat-rpnames-cache.json` | Enthält Personenbuch, Notizen, lokale Titel und Importzustand. Kein Wegwerfcache. |
| `letters` | `config/ottoextra/letters/`, `drafts/`, `.cache/letters/`, altes `config/ottoletter-draft.json` | Persönliche Briefe, versendete Kopien und Entwürfe. Auch historisch als Cache bezeichnete Briefe gehören hierher. |
| `backups` | `config/ottoextra/backups/` und `config/ottoextra/rpnames/backups/` | Enthält ggf. weitere Kopien persönlicher Daten und Konfigurationen. Nur gesondert auswählen. |

Für persönliche Archive beispielsweise `--categories people letters` wählen und **kein** `--expired`. Die Option `--expired` wählt ausschließlich Chat-/Cache-Dateien, deren letzte Änderung mehr als 30 Tage zurückliegt. Sie nimmt niemals Briefe, Personenbuch oder Backups in eine automatische Altersauswahl auf.

Die aktive `ottoextra.json`, gespeicherte Kontoverknüpfungs-Token, Welten, Minecraft-/andere Modlogs, Resourcepacks und beliebige Import-/Debug-Dateien gehören nicht zum Löschwerkzeug. Backups können Kopien solcher Inhalte enthalten. Die Liste ist deshalb **keine vollständige Auskunft über sämtliche personenbezogenen Daten** auf dem Computer. Externe Exporte, manuelle Kopien, Synchronisationsdienste und Serverdaten bleiben außerhalb der Auswahl. Wiederherstellung alter Backups kann zuvor gelöschte Inhalte wieder einspielen.

ZIPs sind unverschlüsselt und können private Gespräche, Notizen sowie Angaben Dritter enthalten. Das Werkzeug prüft die kopierten Inhalte gegen die Quellhashes; es verschickt sie nicht. Alte Exporte separat verwalten. Lokales Token-Löschen ersetzt kein serverseitiges Unlink/Widerrufen.

## Speichergrenzen

| Bereich | Produktgrenze / Verhalten |
|---|---|
| Neue Chatlogs | 8 MiB je Datei; 64 MiB Gesamtbudget im Chat-Unterordner; höchstens 4096 vorhandene Dateien beim Öffnen. Einzelne Einträge über 64 KiB werden ausgelassen. Bei vollem Budget pausiert das Log. |
| Bild-, Skin-Metadaten-, API-Snapshot- und Tiercache-Schreibpfade | Gemeinsames Budget 256 MiB / 10.000 Dateien unter `config/ottoextra/cache/`; bestehende Dateien werden bei Überschreitung nicht entfernt. Ein Schreibvorgang kann wegen des Budgets ausfallen. |
| Einzelne Cache-Dateien | Skin-Metadaten/API-Snapshot höchstens 8 MiB; Bilder zusätzlich an Download-/PNG-Grenzen gebunden. Tieridentitäten behalten die vorhandenen Grenzen: 1024 Einträge, 14 Tage, 2 MiB Eingabedatei. |
| Texturen im Arbeitsspeicher | Bis 128 Skins (höchstens 256 × 256 Pixel) und ca. 32 Banner (höchstens 1024 × 1024 Pixel; laufende parallele Registrierungen können wenige zusätzliche Einträge erzeugen). Danach Fallback statt unbegrenztem Wachstum. |
| Alter von Chat-/Cache-Dateien | 30 Tage als explizite Bereinigungsauswahl des Werkzeugs; keine stille Löschung vorhandener Altbestände. |
| Personenbuch/Briefe/Backups | Keine automatische Löschung oder künstliche Ablaufzeit. Bewusste Kategorieauswahl erforderlich. |
| Tierbuch (`config/ottoextra/animalbook/<SHA-256 der Serveradresse>.json`) | Hofsymbole, Hofnotizen und Tiernotizen je Server, dazu geteilte Hoflisten mit ihren Codes (der Gruppencode ist ein Passwort). Hof- und Tiernotizen verlassen den Rechner nur für Höfe, die der Nutzer selbst teilt, und nur an den Listendienst auf ottoextra.dev. Höchstens 300 Höfe, 4096 Tiere, Notizen bis 200 Zeichen, 2 MiB Eingabedatei. Löschmarken entfallen nach 30 Tagen. Vom Werkzeug noch keiner Kategorie zugeordnet. |

Budgets sind innerhalb eines Clients synchronisiert, keine Festplattenquote gegenüber anderen Prozessen oder älteren Modversionen. Vorhandene größere Bestände bleiben unangetastet und können neue Cache-Schreibvorgänge verhindern. Nach Offline-Bereinigung und Neustart werden benötigte Inhalte wieder lokal aufgebaut bzw. nur mit gültiger Online-/Downloadfreigabe geladen.

Eine komfortable Ingame-Datenverwaltung bleibt P3. Die geprüfte P1-Verwaltung erfolgt über dieses Offline-Werkzeug; es wird nicht automatisch gestartet und ist nicht in der Mod-JAR enthalten.
