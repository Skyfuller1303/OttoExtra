# Umsetzung der zwölf Auditpunkte

Stand: 08.09.2026, OttoExtra `1.16.3-w`. **Alle zwölf Einträge haben eine
Codekorrektur bzw. die beschriebene UI-Verbesserung.** Die gesonderten älteren
Tablisten-/Renderbeobachtungen sind weiterhin außerhalb dieser zwölf Einträge.

**Abschluss: 647 Unit-Tests und 73/73 echte Server-/Clientfälle bestanden.**
Der Build ist erfolgreich; die geprüfte JAR ist unten verlinkt.

## Änderungen

| ID | Ergebnis | Betroffene Stelle |
| --- | --- | --- |
| A26-01 | Queue beim Einreihen an den Netzwerkhandler gebunden; sofortiger Abbruch beim Disconnect und bei jedem Handlerwechsel, auch vor dem ersten Tick. | [LongChatSender](../../src/client/java/de/ottoextra/chat/LongChatSender.java), [ChatModule](../../src/client/java/de/ottoextra/chat/ChatModule.java) |
| A26-02 | `chatLog` wird bei Snapshot-/Backup-Restore in beiden Richtungen übernommen. Nicht veränderbare Konstanten erzeugen keine unnötigen Kopierwarnungen mehr. | [OttoExtraConfig](../../src/client/java/de/ottoextra/config/OttoExtraConfig.java) |
| A26-03 | Leere Erstinstallation bleibt schreibbar. Reparatur einer gesunden Datei erhält wartende Änderungen; beschädigte Daten ohne Sicherung bleiben geschützt. | [LocalRpIdentityStore](../../src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java) |
| A26-04 | Gemeinsame Freigabe schließt zuerst View, dann Textur; beide Referenzen und Größen werden zurückgesetzt. Beide Austauschpfade verwenden sie. | [PaintedMapRenderer](../../src/client/java/de/ottoextra/map/PaintedMapRenderer.java) |
| A26-05 | Screen- und Netzwerk-Einstieg berücksichtigen dieselbe RP-Einstellung und den gewählten Kanal. | [ChatScreenMixin](../../src/client/java/de/ottoextra/mixin/ChatScreenMixin.java), [ChatMessageSplitMixin](../../src/client/java/de/ottoextra/mixin/ChatMessageSplitMixin.java) |
| A26-06 | Weitere Eingaben umgehen keine laufende Sendepause. Auch nach dem letzten Chunk bleibt die verbleibende Pause wirksam. | [LongChatSender](../../src/client/java/de/ottoextra/chat/LongChatSender.java) |
| A26-07 | Generationsprüfung verwirft alte Antworten, einschließlich bereits im Clientexecutor wartender Callbacks. Fehler bleiben mit Altdaten sichtbar; deren Erfolgszeitpunkt wird erhalten. Beim Entfernen des Screens werden ausstehende Antworten ungültig. | [AccountOverviewState](../../src/client/java/de/ottoextra/verify/AccountOverviewState.java), [AccountOverviewScreen](../../src/client/java/de/ottoextra/verify/AccountOverviewScreen.java) |
| A26-08 | Installationszustand speichert `directSource`. Bei Quellenwechsel oder altem Zustand ohne Quellenidentität wird neu geladen. Manifestinstallation entfernt eine frühere Direktzuordnung. | [ResourcePackUpdater](../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java), [PackState](../../src/client/java/de/ottoextra/resourcepack/PackState.java) |
| A26-09 | Neuaufbau nach Reparatur, Backup-Erstellung oder bestätigtem Restore erhält die Aktionsmeldung. Normale Navigation/Suche löscht sie weiterhin. | [OttoExtraSettingsScreen](../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java) |
| A26-10 | Vollständige Langchat-Nachricht wird bei `addToHistory=true` einmal eingetragen und ist per Pfeil-hoch erreichbar; bei `false` erfolgt kein Eintrag. | [ChatScreenMixin](../../src/client/java/de/ottoextra/mixin/ChatScreenMixin.java) |
| A26-11 | Editor zeigt Arbeitsseiten und voraussichtliche Buchseiten, Vorschau zeigt Buchseiten. Beide nutzen dieselbe Planung. Der Editor berechnet bei Änderungen neu und verwendet sonst seinen Cache; ungültige Pläne zeigen einen Fehlerstatus. | [LetterServices](../../src/client/java/de/ottoextra/letter/LetterServices.java), [LetterEditorScreen](../../src/client/java/de/ottoextra/letter/ui/LetterEditorScreen.java), [LetterPreviewScreen](../../src/client/java/de/ottoextra/letter/ui/LetterPreviewScreen.java) |
| A26-12 | Sperrhinweis steht in eigener Kopfzeile. Textbereich und Scrollen berücksichtigen den zusätzlichen Platz. Auch der Nur-Lesen-Hinweis der Vorschau hat eine eigene Zeile. | [LetterEditorScreen](../../src/client/java/de/ottoextra/letter/ui/LetterEditorScreen.java), [LetterPreviewScreen](../../src/client/java/de/ottoextra/letter/ui/LetterPreviewScreen.java) |

Deutsch und Englisch sind ergänzt. Zeilenbreite und Courier-Pagination bleiben
unverändert; das UI führt keine feste Zeichenspaltenzahl ein. Bei 1280 × 720 und
GUI-Skalierung 3 passen weiterhin alle Editoraktionen in das Fenster. Der
Textbereich zeigt dort bis zu acht Zeilen und scrollt längere Arbeits-/Importseiten.

## Ausgeführte Validierung

| Prüfung | Ergebnis |
| --- | --- |
| Neue Config-/RP-Regressionen vor Korrektur | Drei erwartete Fehler reproduziert; zehn bestehende/Kontrollfälle bestanden. |
| Gezielter Gradle-Lauf nach Korrektur | 22 Tests aus Config, RP-Store, Ressourcenpaket und Kontoübersicht bestanden. |
| Senderprobe gegen aktuelle Produktquellen | Zehn Szenarien bestanden, einschließlich direktem Handlerwechsel ohne Null-Tick. |
| Persistenzprobe mit echten kompilierten Klassen und Minecraft-Klassenpfad | Drei Regressionen und zwei Kontrollen bestanden; beide Profile bleiben nach Reparatur und Shutdown gespeichert. |
| Gezielter echter Server-/Clientlauf | 9/9 bestanden: sieben neue `FIX-*`-Fälle plus Import `I01` und Größenwechsel `UI01`. |
| Vollständiger erweiterter Server-/Clientlauf | **73/73 bestanden**, 0 Fehler; Fälle zusammen 189,259 Sekunden, Gradle einschließlich Clientstart 3 Minuten 38 Sekunden. |
| `gradlew.bat build -x dist --console=plain` | **647 Tests in 115 Suites**, 0 Fehler, 0 übersprungen; Build erfolgreich in 15 Sekunden. |
| JAR-Inhalt und Metadaten | 642 Einträge, keine E2E-/Probe-/Testclipboard-/Vault-Testklassen; weiterhin ausschließlich Client-Mod. |

Die alten Diagnoseprogramme wurden auf Regressionen umgestellt. **Ihr Exitcode 0
bedeutet jetzt korrektes Verhalten.** Die alten Fehlerprotokolle bleiben in ihren
bisherigen Ordnern; neue Ergebnisse liegen unter `build/audit-2026-09-08/fixes/`.

Abschlussprotokolle: [Build](../../build/audit-2026-09-08/fixes/build-final.log),
[JUnit-Zählung](../../build/audit-2026-09-08/fixes/test-summary.json),
[Clientlog](../../build/audit-2026-09-08/fixes/client-full.log),
[73 Einzelergebnisse](../../build/audit-2026-09-08/fixes/client-full-results.json),
[Serverabschluss](../../build/audit-2026-09-08/fixes/server-final.log).
Client und Testserver sind beendet; Ports 25579/25580 sind frei.

Der Offline-Testaccount erhält im Minecraft-Log eine erwartete 401-Antwort beim
Abruf signierter Profilschlüssel. Die Verbindung zum lokalen Testserver und alle
Chat-/Briefprüfungen bestehen. Es wurden keine produktiven Konto-/Economy-Daten
verwendet.

### Inhalt der neuen Clientfälle

- `FIX-CHAT-OFF`, `FIX-CHAT-OOC`, `FIX-CHAT-RP`: tatsächliche ChatScreen-Mixins und
  serverseitig empfangene Teilnachrichten; nur aktives RP ergänzt Klammern.
  Vollständiger Verlaufseintrag und Pfeil-hoch sowie `addToHistory=false` geprüft.
- `FIX-RECONNECT`: erste Nachricht kommt auf Paper an; Disconnect und echter
  erneuter Login; wartender Rest bleibt verworfen und neue Eingaben funktionieren.
  Für ein klar beobachtbares Zeitfenster setzt die Fixture die Pause auf 5000 ms.
  Die schnellen Grenzfälle bei 1500 ms deckt zusätzlich die Senderprobe ab.
- `FIX-GPU`: echte OpenGL-Texturen in wiederholten Größenwechseln, geschlossene
  Views und Referenzzähler null geprüft. Nach Invalidierung bestätigt `glIsTexture`
  die tatsächliche Löschung; wiederholte Freigabe bleibt sicher.
- `FIX-STATUS`: echter Backup-Klick-Callback über Tastaturaktivierung; Status
  besteht nach dem Neuaufbau und im nächsten Frame. Backups entstehen im isolierten
  Clientprofil. Die Backups-Seite wird für diese Fixture direkt angewählt.
- `FIX-PAGES`: Seitenzähler gegen Vorschau, unveränderten Cache, Texteingabe,
  Undo, tatsächliche Courier-Buchseiten und Fehlerstatus für nicht planbaren Inhalt
  geprüft. Import und Scrollen laufen zusätzlich in `I01`.

Die drei Aufnahmen des gezielten Laufs wurden visuell geprüft:

- [Arbeitsseite und Buchseitenzahl](../../build/audit-2026-09-08/fixes/screenshots-focused/0004_audit-working-and-book-pages.png)
- [Import mit getrenntem Sperrhinweis](../../build/audit-2026-09-08/fixes/screenshots-focused/0000_courier-import-12-lines.png)
- [Sichtbare Backup-Rückmeldung](../../build/audit-2026-09-08/fixes/screenshots-focused/0003_audit-backup-status.png)

## Grenzen

Die Konto-Race-Conditions sind mit steuerbaren Futures und Clientexecutor-Reihenfolge
geprüft; es gab keine Live-Abfrage der produktiven Konto-API. Der Ressourcenpakettest
prüft Quellenwechsel A/B, Migration alter Zustände, serialisierten Zustand und
fehlende/ungültige lokale ZIPs. Ein neuer HTTPS-Download-/Aktivierungslauf gegen eine
externe Paketquelle ist nicht Teil dieser Runde.

Die erfolgreiche Backup-Aktion ist im Client geprüft; ein absichtlich erzeugter
Dateisystemfehler beim Backup wurde nicht zusätzlich im UI ausgelöst. Dieselbe
Erhaltungslogik wird im gemeinsamen Erfolgs-/Fehlercallback verwendet.
Die visuelle Prüfung erfolgte mit englischer Clientsprache; deutsche Übersetzungen
sind ergänzt, aber noch nicht in einem separaten deutschen Clientlauf aufgenommen.

Die bereits bekannte zeitweise fehlende Aktionszeile in einzelnen Import-Screenshots
bleibt eine gesonderte Beobachtung. Auch im neuen Importbild fehlt die Zeile,
während echte Mausklicks und Widgetgrenzen bestehen; sie wird durch die Korrektur
der Kopfzeile nicht als behoben erklärt.

## Wiederholen

Siehe [Nachweisanleitung](../../tools/audit-2026-09-08/README.md) und
[Courier-E2E-Anleitung](../../tools/courier-e2e/README.md). Vorbestehende Änderungen
wurden erhalten. Der Quellstand vor diesen Fixes und ein Diff gegen diesen Stand
sind lokal unter `build/audit-2026-09-08/fixes/before/` bzw. `production.diff` gesichert.

## Geprüftes Artefakt

[ottoextra-1.16.3-w-auditfixes.jar](../../build/audit-2026-09-08/fixes/verified/ottoextra-1.16.3-w-auditfixes.jar)

SHA-256: `5c35202f55fbcb3241dc0ba4e52342d25316f085fd15e13b7ae190c3861ca3de`.
Die JAR wurde aus dem getesteten Quellstand gebaut. JUnit-XML und Screenshots sind
daneben unter `verified/` gesichert. Die Versionskennung bleibt `1.16.3-w`; die
separate Dateibezeichnung unterscheidet dieses Artefakt vom früheren V2-Prüfbuild.
Die vorhandene `dist`-Aufgabe wurde ausgelassen; es wurde keine Benutzerinstallation
ersetzt und nichts veröffentlicht.
