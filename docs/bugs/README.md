# OttoExtra: Bug-Audit vom 6. September 2026

Nachprüfung vom 7. September für 1.17: zusätzliche Korrekturen am Erststart-Layout
und an der XaeroPlus-Mixin-Auswahl; drei Client-Konfigurationen getestet. Aktueller
Status und noch offene Abnahmen: [Release-Nachprüfung](../release-1.17-review.md).
Die separate Meldung zu veralteten Tablisten-Einträgen bleibt offen.

23 erfasste Befunde: **3 × P1, 18 × P2, 2 × P3**. Alle 23 Befunde haben eine Codekorrektur; MAP-01 sowie die weiteren UI-/Renderpfade benötigen noch ihre Ingame-Abnahme. **0 Befunde bleiben ohne Codefix**. Umsetzung und Testnachweise: [P1-UMSETZUNG.md](P1-UMSETZUNG.md), [P2-UMSETZUNG.md](P2-UMSETZUNG.md), [P3-UMSETZUNG.md](P3-UMSETZUNG.md), [XaeroPlus-Fix](p2-mittel/MAP-01-xaeroplus-zeichnungen-verdeckt.md).

## Prüfstand

- Projekt: OttoExtra `1.16.3-k`, Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Java 21.
- Basis-Commit: `60168aa83a3e6a4a90ae9b1696bf0647c62fd6eb`.
- Geprüft wurde der **aktuelle, bereits umfangreich veränderte Worktree einschließlich unversionierter Quelldateien**. Der Commit allein beschreibt diesen Stand nicht. Quellenlinks und Zeilennummern beziehen sich auf den Auditzeitpunkt.
- Inventar: 262 Client-Javadateien und 72 Testdateien. Risikobasierte Prüfung von Aufrufketten; keine Zusicherung, dass jede Zeile oder jeder mögliche Fehler erfasst wurde.
- Beim ursprünglichen Audit frisch ausgeführt: `.\gradlew.bat test --rerun-tasks --console=plain` → **330 Tests, 0 Fehler, 0 übersprungen**, 72 Testsuites. Einzelne Gegenbeispiele wurden zusätzlich lokal ausgeführt. Die anschließende P1-Umsetzung ergänzt Regressionstests und einen vollständigen Build; siehe [aktueller Stand](P1-UMSETZUNG.md).
- Kein Minecraft-/Multiplayer-Test, kein Live-API-Test. Die meisten Befunde sind durch Codepfade belegt; ausgeführte Algorithmusnachweise sind ausdrücklich markiert. Details und wiederholbare Beispiele: [VALIDIERUNG.md](VALIDIERUNG.md).

## Prioritäten

| Priorität | Bedeutung | Erfasst | Ohne Fix |
| --- | --- | ---: | ---: |
| P0 – kritisch | Unmittelbarer, breit wirksamer Ausfall oder vergleichbarer Notfall; kein belastbarer Befund dieser Stufe | 0 | 0 |
| P1 – hoch | Ungewollter Versand oder Verlust zugesicherter Wiederherstellung | 3 | 0 |
| P2 – mittel | Bedingte Abstürze, falsche Inhalte, Zustandsfehler oder nicht eingehaltene Einstellungen | 18 | 0 |
| P3 – niedrig | Begrenzte Anzeige-/Layoutfehler mit einfachem Umweg | 2 | 0 |

Priorität bewertet Auswirkung und Auslöser gemeinsam, nicht die Beweissicherheit. Innerhalb einer Stufe sind Einträge nach Kennung gruppiert; dort besteht keine zusätzliche Rangfolge. Die Dokumentation enthält keine P0-Datei, weil kein solcher Befund nachgewiesen wurde.

## P1 – hoch

| Kennung | Befund |
| --- | --- |
| BACKUP-01 | Behoben: [Wiederhergestellte Config wird beim Schließen erneut überschrieben](p1-hoch/BACKUP-01-restore-wird-ueberschrieben.md) |
| BACKUP-02 | Behoben: [Vollbackup enthält gespeicherte Briefentwürfe nicht](p1-hoch/BACKUP-02-briefentwuerfe-fehlen.md) |
| LETTER-01 | Behoben: [Briefversand läuft nach schnellem Reconnect unbestätigt weiter](p1-hoch/LETTER-01-versand-ueberlebt-disconnect.md) |

## P2 – mittel

| Kennung | Befund |
| --- | --- |
| CHAT-01 | Behoben: [Verschachtelte Klammern erzeugen eine wiederkehrende Chatqueue](p2-mittel/CHAT-01-klammern-erzeugen-spamqueue.md) |
| CHAT-02 | Behoben: [Chat-Reparatur entfernt legitime Endwiederholungen](p2-mittel/CHAT-02-reparatur-loescht-gueltigen-text.md) |
| CHAT-03 | Behoben: [Lange Chatnachrichten zerreißen Emoji-Surrogatpaare](p2-mittel/CHAT-03-unicode-surrogatpaar-getrennt.md) |
| CONFIG-01 | Behoben: [Ein Lesefehler überschreibt die gesamte ursprüngliche Config](p2-mittel/CONFIG-01-defekte-config-wird-ersetzt.md) |
| CONFIG-02 | Mit P1 behoben: [Verwerfen setzt geänderte Einwilligungen nicht zurück](p2-mittel/CONFIG-02-verwerfen-behaelt-einwilligung.md) |
| HORSES-01 | Behoben: [Fehlgeschlagene Pferdedatenabfrage wird automatisch nicht wiederholt](p2-mittel/HORSES-01-kein-retry-nach-ladefehler.md) |
| HORSES-02 | Behoben: [Alte Pferdenamen-Antwort überschreibt neue Verbindung](p2-mittel/HORSES-02-veraltete-antwort-nach-reconnect.md) |
| LETTER-02 | Behoben: [Verkündungsoptimierung verändert den bereits gestarteten Schreibplan nicht](p2-mittel/LETTER-02-verkuendung-optimierung-zu-spaet.md) |
| MAP-01 | Implementiert, Ingame-Abnahme offen: [Gemalte Karte verdeckt XaeroPlus-Schrift und Zeichnungen](p2-mittel/MAP-01-xaeroplus-zeichnungen-verdeckt.md) |
| NAMETAGS-01 | Behoben: [Benannte Tiere und NPCs erhalten das Profil eines gleichnamigen Spielers](p2-mittel/NAMETAGS-01-tiername-wird-spielerprofil.md) |
| PACK-01 | Behoben: [Fehlende Ressourcenpaket-Datei gilt dauerhaft als aktuell](p2-mittel/PACK-01-fehlende-zip-gilt-als-aktuell.md) |
| PACK-02 | Behoben: [Ersetztes aktives Ressourcenpaket wird nicht neu geladen](p2-mittel/PACK-02-update-ohne-resource-reload.md) |
| PACK-03 | Behoben: [Benutzerdeaktivierung des Ressourcenpakets wird nicht gespeichert](p2-mittel/PACK-03-deaktivierung-wird-nicht-respektiert.md) |
| PLAYERLIST-01 | Behoben: [Spielerlistensortierung verletzt den Comparator-Vertrag](p2-mittel/PLAYERLIST-01-comparator-nicht-transitiv.md) |
| REGIONS-01 | Behoben: [Alter Regions-Sync überschreibt nach Reconnect einen neueren Bootstrap](p2-mittel/REGIONS-01-alter-sync-ueberschreibt-bootstrap.md) |
| RPNAMES-01 | Behoben: [Wartende RP-Namen-Uploads starten noch nach Widerruf](p2-mittel/RPNAMES-01-uploads-nach-widerruf.md) |
| RPNAMES-02 | Behoben: [Wiedervergebener Minecraft-Name übernimmt ein fremdes RP-Profil](p2-mittel/RPNAMES-02-namensfallback-verwechselt-uuid.md) |
| TWEAKS-01 | Behoben: [Low-Health-Unschärfe bleibt trotz ausgeschaltetem Haupteffekt aktiv](p2-mittel/TWEAKS-01-low-health-blur.md) |

## P3 – niedrig

| Kennung | Befund |
| --- | --- |
| LETTER-03 | Behoben: [V2-Briefplaner entfernt führende Leerzeilen](p3-niedrig/LETTER-03-fuehrende-leerzeilen-verschwinden.md) |
| REGIONS-02 | Behoben: [Regionsfenster zeigt nachgeladenen Detailstand erst beim Wiederöffnen](p3-niedrig/REGIONS-02-detailfenster-aktualisiert-sich-nicht.md) |

## Abdeckung und Grenzen

Geprüfte Schwerpunkte: Client-Lifecycle und Server-Gates; Briefplanung, Queue, Recovery und Dialogabläufe; Chat-Splitting, Reparatur und Pipeline; Config, Backup und Consent; API/Auth-Aufrufketten und Uploads; Ressourcenpaket-Installation und Updates; RP-Identitäten; Regionscache und Detailfenster; Tablistensortierung, Nametags und relevante Render-/Kompatibilitätshooks; Pferdedaten, Tierinfo und ausgewählte Tweaks.

Keine zusätzliche belastbare Meldung ergab sich aus den geprüften Update-/Account-Link-Pfaden. Das ist kein vollständiger Fehlerfreiheitsnachweis. Insbesondere RP-UI-/Titelklassen, externe Add-ons und reale Modkombinationen wurden nicht vollständig durchgetestet.

Nicht als Bug aufgenommen: Modulschalter mit dokumentierter Neustartpflicht; unbestätigte Xaero-Button-Erkennung; Nametag-Sichtbarkeitsverhalten mit unklarer beabsichtigter Semantik. Benachbarte Projekte und Add-on-Repositories sind nicht Gegenstand dieses Audits.

## Weiterarbeit

Weitere Nutzermeldung: [Spieler verschwinden erst nach eigenem Reconnect](TABLIST-VERALTETE-EINTRAEGE.md). **Ursache und Fix noch offen.** Der lokale Codepfad und BetterTab wurden geprüft; eine aktivierbare Paketdiagnose ist implementiert und gebaut. Die bestehenden 428 Tests bestehen. Die ursprünglichen 23 Auditbefunde bleiben davon getrennt.

Zusätzliche Nutzermeldung nach dem Audit: [Fehlende Tablisten-Wappen nach Proxy-Rejoin](TABLIST-PROXY-REJOIN.md). Der nachgewiesene Fehler im Scoreboard-Team-Fallback ist korrigiert. Sechs neue Regressionstests und der vollständige Build mit **428 Tests in 95 Suites** sind erfolgreich; der reale Proxy-Rejoin benötigt noch die Ingame-Abnahme. Dieser Nachtrag ist nicht Teil der ursprünglichen 23 Auditbefunde.

XaeroPlus-Nachtest: **Serverseitiges FairPlay als zusätzliche Ursache bestätigt**, über den eindeutigen Live-Tooltip. XaeroPlus unterdrückt damit Zeichnungen trotz aktiver UI und Speicherung; der Renderreihenfolge-Patch allein löst diesen Vorfall nicht. Auf ausdrücklichen Nutzerwunsch ist eine lokale Ausnahme nur für eigene Weltkarten-Linien und Texte implementiert und mit Backup installiert; weitere FairPlay-Sperren bleiben erhalten. 388 Tests erfolgreich, visuelle Ingame-Abnahme offen. Reverse-Engineering, GPU-Gegenprobe, Umsetzung und Installationshash: [XAEROPLUS-ANALYSE.md](XAEROPLUS-ANALYSE.md).

Die 16 zum Start noch offenen P2-Befunde wurden behoben; CONFIG-02 und MAP-01 waren bereits implementiert. Der dokumentierte P2-Build besteht 414 Tests in 93 Suites. Auch die beiden P3-Befunde sind inzwischen korrigiert. Der vollständige P3-Build besteht 422 Tests in 94 Suites; Nachweis unter [P3-UMSETZUNG.md](P3-UMSETZUNG.md). Offen bleiben die dokumentierten Ingame-Abnahmen. Vor jedem weiteren Fix den beschriebenen Auslöser isoliert nachstellen und gezielt absichern. Bei offenen Befunden bleiben die vorgeschlagenen Regressionstests Arbeitsaufträge; ausgeführte Tests der behobenen Einträge sind gesondert dokumentiert. Bereits bestehende Benutzeränderungen wurden beibehalten.
