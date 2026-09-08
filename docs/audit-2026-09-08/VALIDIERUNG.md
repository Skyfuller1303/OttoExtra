# Prüfstand, Nachweise und Grenzen

[Zur Befundübersicht](README.md)

Dieser Bericht dokumentiert den **Audit vor der Korrektur**. Aktuelle Änderungen
und Testergebnisse stehen in [UMSETZUNG.md](UMSETZUNG.md). Die genannten Skripte
wurden inzwischen auf erfolgreiche Regressionen umgestellt; die hier zitierten
ursprünglichen Fehlerprotokolle bleiben als historische Nachweise erhalten.

## Prüfstand

- Datum: 08.09.2026; OttoExtra `1.16.3-w`.
- Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Fabric Loader `0.19.3`,
  Fabric API `0.141.4+1.21.11`, Loom `1.17.8`, Gradle `9.5.1`, Java `21.0.10`.
- Basis-Commit: `60168aa83a3e6a4a90ae9b1696bf0647c62fd6eb`. Geprüft wurde der
  umfangreich veränderte Worktree einschließlich unversionierter Dateien; der
  Commit allein beschreibt den geprüften Stand nicht.
- Inventar: 290 Produktions-Javadateien, 114 Testdateien. Risikobasierte Prüfung
  von Aufrufketten; keine vollständige Zeilenabnahme aller Dateien.
- Diese Runde ergänzt ausschließlich Audit-Dokumentation und isolierte
  Nachweishilfen. Vorhandene Produktänderungen und ältere Auditdateien bleiben erhalten.

Ein [SHA-256-Inventar aller 290 Produktionsquellen](../../build/audit-2026-09-08/production-source-sha256.json)
identifiziert den lokalen Quellstand unabhängig vom unveränderten Basis-Commit.

Schwerpunkte waren Chatqueue und Mixins, Konfigurationswiederherstellung,
RP-Datenhaltung und Reparatur, asynchrone Kontodarstellung, Ressourcenpaketquellen,
Kartenressourcen und optionale Integrationen sowie die offenen Brief-UI-Punkte.
Modulinventar und bisherige Auditunterlagen wurden zum Abgleich herangezogen.
Aus den zusätzlich geprüften Upload-Widerrufs-/Generationspfaden und der
Client-/Common-Trennung entstand kein weiterer belastbarer Befund.

## Frischer Gesamttest

Aus dem Repositorywurzelverzeichnis mit Java 21 ausgeführt:

```powershell
.\gradlew.bat test --rerun-tasks -x dist --console=plain
```

**Ergebnis: BUILD SUCCESSFUL, 637 Tests, 114 Suites, 0 Fehler, 0 übersprungen.**
Alle sechs beteiligten Aufgaben wurden ausgeführt; Gradle meldete 16 Sekunden.
Vorhandene Hinweise auf veraltete Java-/Gradle-APIs blieben ohne Buildfehler.
`dist` ist ausgeschlossen, da dessen bestehende Aufgabe ältere Distributions-JARs
entfernt. Es wurde kein neuer Release installiert oder veröffentlicht.

Lokale Artefakte:

- [Gradle-Protokoll](../../build/audit-2026-09-08/gradle-test.log)
- [Zählung aus JUnit-XML](../../build/audit-2026-09-08/test-summary.json)
- [Archivierte JUnit-Ergebnisse](../../build/audit-2026-09-08/junit/)

## Gezielte Fehlernachweise

### Langchat: A26-01 und A26-06

```powershell
.\tools\audit-2026-09-08\test-long-chat-sender.ps1
```

Das Skript kompiliert die unveränderten Produktquellen `LongChatSender`,
`LongChatSplitter` und `RpChatSyntax` mit Java 21. Nur MinecraftClient und Handler
sind aufzeichnende Stubs. Ergebnis:

```text
KNOWN_BUG short_disconnect: A=[first], 5 disconnected ticks, B=[second] without enqueue on B.
CONTROL long_disconnect: A=[first], 31 disconnected ticks, B=[] after 31 ticks.
KNOWN_BUG enqueue_delay: first and second sent on consecutive ticks with delayMs=1500 after enqueue(third).
RESULT: 2 known defects reproduced; 1 control passed.
SCOPE: actual production sender source; stub connection boundary; no Minecraft reconnect test.
```

**Exitcode 0 bestätigt hier die erwartete Fehlerreproduktion.** Der Nachweis ist
kein grüner Regressionstest eines Fixes. Ergebnis, Compilerprotokoll und Quellhashes
liegen unter [build/audit-2026-09-08/long-chat-sender](../../build/audit-2026-09-08/long-chat-sender/).

### Config und RP-Store: A26-02 und A26-03

Nach ersten JShell-Proben wurde ein wiederholbarer Nachweis gegen die frisch
kompilierten Produktklassen und den echten lokalen Minecraft-/Loom-Klassenpfad
ausgeführt. Er verwendet keine Stubs und ausschließlich isolierte Testdateien:

```powershell
.\tools\audit-2026-09-08\test-persistence.ps1
```

```text
KNOWN_BUG config_restore: chatLog remains false for true snapshot, remains true for false snapshot.
CONTROL config_restore: combatModeHud returns to true from the same snapshot.
CONTROL fresh_store: NO_DATA, ensureSeen + saveNow => file exists with 1 player.
KNOWN_BUG fresh_repair: NO_DATA -> NO_VALID_BACKUP; memory=1, file=false after saveNow and shutdown.
KNOWN_BUG healthy_repair: memory 2 -> 1, pending profile lost; repair=HEALTHY; file=1 after saveNow and shutdown; elapsedMs=1.
RESULT: 3 known defects reproduced; 2 controls passed.
```

Damit sind beide RP-Reparaturpfade dynamisch bestätigt. Die zweite Probe hält
kurz den Store-Monitor, um die Folge „Änderung → Reparatur vor verzögertem Flush“
kontrolliert auszuführen; im gemessenen Lauf dauerte sie 1 ms. Auch explizites
Speichern und Shutdown bringen das verworfene Profil nicht zurück.

Zwei Läufe reproduzierten die Fehler. **Exitcode 0 bedeutet auch hier
Fehlerreproduktion.** [Ergebnisprotokoll](../../build/audit-2026-09-08/persistence/result.log)
und [Quell-, Klassen- und Abhängigkeitshashes](../../build/audit-2026-09-08/persistence/source-class-dependency-sha256.json)
sind archiviert. Voraussetzungen und der optionale `-JavaBin`-Parameter stehen in
der [Nachweisanleitung](../../tools/audit-2026-09-08/README.md). Es wurden keine
Benutzerdaten verändert oder Minecraft-Dialoge automatisiert bedient.

Nebenbei protokolliert `restoreFrom` acht Warnungen pro Aufruf für öffentliche
Konstanten, die `copyFields` ebenfalls zu setzen versucht. Das ist ein begrenzter
Logbereinigungspunkt für denselben Config-Fix, kein zusätzlich gezählter Fehler
und keine Ursache des ausgelassenen `chatLog`-Feldes.

### Splittervergleich: A26-05

In JShell wurde die tatsächliche Splitterklasse mit
`"Start (" + "word ".repeat(80) + "end)"`, Chunklänge 256 und Marker `" >"`
ausgeführt. Drei Argumente aktivieren die Klammerergänzung; vier Argumente mit
`false` lassen sie aus. Der Vergleich ergibt unterschiedliche Chunklisten.
Der falsche Aufruf im ChatScreenMixin wurde statisch bis zur Queue verfolgt.

### Übrige Befunde

A26-04 ist mit dem lokal vorhandenen Source-JAR abgeglichen:

```text
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-6dd721cd7d/
1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-v2/
minecraft-clientOnly-6dd721cd7d-1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-v2-sources.jar
```

Der Pfad ist hier zur Lesbarkeit auf drei Zeilen verteilt. Darin erzeugt
`net/minecraft/client/gl/GlBackend.java:211` eine `GlTextureView`. In
`net/minecraft/client/texture/GlTextureView.java:23` erhöht der Konstruktor den
Textur-Referenzzähler; `close()` senkt ihn in Zeile 35. `GlTexture.java:32` gibt
eine geschlossene Textur nur bei Referenzzähler null frei; Zeile 89 holt die
Freigabe bei der letzten View nach. Die konkrete Löschung erfolgt über
`_deleteTexture` in Zeile 39. GPU-Verbrauch wurde nicht gemessen.

A26-07 bis A26-10 beruhen auf
konkreten Codepfaden; deren vorgeschlagene UI-, Future- und HTTP-Testfälle sind
Abnahmearbeit für die Korrektur und wurden in dieser Runde nicht als Tests ausgeführt.
A26-11 ist eine Verbesserung aus dem Seitenmodellvergleich. A26-12 stützt sich
zusätzlich auf die bereits vorhandene Aufnahme aus dem echten Clientlauf.

## Vorhandener V2-Lauf und weitere Grenzen

Der [vorherige Briefmod-Lauf](../briefmod/e2e-v2-2026-09-08.md) desselben Datums
bestand 66/66 Fälle mit lokalem Paper-Server, echter Courier-JAR und Fabric-Client.
Daneben bestanden 2.052 JAR-Protokollvergleiche und 100 vollständige Briefpläne.
Diese Nachweise wurden hier wiederverwendet, nicht erneut ausgeführt.

In dieser erneuten Audit-Runde gab es keinen neuen Minecraft-Client-/Serverlauf,
keine Live-API-Anfrage, keinen realen Serverwechsel für Langchat und keine Messung
mit allen optionalen Mods. Die zwölf Einträge sind keine Behauptung vollständiger
Fehlerfreiheit außerhalb der geprüften Pfade. Bekannte offene visuelle/Tablisten-
Abnahmen bleiben in der Übersicht separat verlinkt.

Die Dateien unter `build/` sind lokale Nachweise und können durch einen späteren
Clean-Build verschwinden. Die Auditbeschreibung und Nachweisskripte unter `docs/`
und `tools/` bleiben im Quellbaum erhalten.
