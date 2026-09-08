# Testläufe und Abnahme

Nachtrag: Der inzwischen ausgeführte automatische Lauf mit echtem Paper-Server und
Fabric-Client ist im [V2-End-to-End-Protokoll](e2e-v2-2026-09-08.md) dokumentiert.
Die folgenden Angaben zum ursprünglichen isolierten Client-Smoke bleiben als
historischer Prüfstand erhalten.

Stand: 08.09.2026. Java 21.0.10, Gradle Wrapper 9.5.1, Loom 1.17.8,
Minecraft 1.21.11, Yarn 1.21.11+build.6, Fabric Loader 0.19.3,
Fabric API 0.141.4+1.21.11.

## Automatisch ausgeführt

| Prüfung | Ergebnis |
| --- | --- |
| Briefmod-Baseline: `gradlew.bat test --tests 'de.ottoextra.letter.*'` | 121 Tests, keine Fehler. |
| Gesamtsuite vor Änderungen: `gradlew.bat test` | 594 Tests, keine Fehler. |
| Regressionsläufe während Korrekturen | Veraltete CourierNew-Erwartungen schlugen nach Vertragskorrektur gezielt fehl und wurden durch 2.1.0-Referenzfälle ersetzt. Neue Editor-/Queue-/Recovery-/Formatfälle ergänzt. |
| `gradlew.bat build -x dist --console=plain` | Erfolgreich; Tests, Kompilierung, Remapping und Sources-JAR. Finaler Zähler siehe Abschluss unten. |
| `tools/test-courier-contract.ps1` | **2.052 Protokollvergleiche + 100 vollständige Briefpläne erfolgreich** gegen die echte `courier-2.1.0.jar`; Seed 2100. |
| Isolierter `runClient` | Minecraft und OttoExtra initialisieren erfolgreich, Texturen/Audio/Rendering starten. Nach Beobachtung gezielt beendet. |

`-x dist` vermeidet den vorhandenen `dist`-Finalizer, der alle dortigen JARs löscht.
Das gebaute Mod-Artefakt liegt unter `build/libs/ottoextra-1.16.3-w.jar`.
Eine eindeutig benannte Audit-Kopie wird unter `build/briefmod-audit/` abgelegt.

Die gesamten JUnit-Ergebnisse liegen nach einem Gesamtlauf in
`build/reports/tests/test/index.html`; maschinenlesbar unter `build/test-results/test/`.
Teiltestläufe überschreiben diese Gradle-Ausgabe, deshalb wird das endgültige
Gesamtergebnis zusätzlich unter `build/briefmod-audit/` gesichert.

## Unabhängiger JAR-Abgleich

```powershell
cd OttoExtra
.\tools\test-courier-contract.ps1
```

Das Skript prüft den SHA-256-Fingerprint, kompiliert die Clientklassen und verwendet
die lokal vorhandenen Paper-/Kyori-/Guava-/Gson-/SnakeYAML-Abhängigkeiten aus dem
Gradle-Cache. Andere Cachepfade lassen sich mit `-DependencyCache` angeben.

Es ruft per Reflection die **echte** private Methode
`LetterCreation.appendMessageToBook` auf, nutzt den **echten** MessageManager und
Papers Argumenttrennung. Nur `BookMeta.getPages/setPages` sind durch eine Liste
ersetzt. Der Test umfasst leere und fast volle Ausgangsseiten, lange Wörter,
mehrfache Leerzeichen, Leerzeilen, Farben, Unicode, Prefix-Ersetzung und zufällige
Payloads. Vollständige Planner-Ausgaben werden gegen die JAR abgespielt und auf
Seitenprognose, Textverlust, Farben und Befehlslänge geprüft.

## Client-Start

Der Test nutzte `build/courier-client-smoke` als neues Spielprofil, Benutzername
`CourierAudit`, deaktiviertes DevAuth und keine Serververbindung. Die temporäre
Gradle-Initdatei liegt unter `build/courier-smoke.init.gradle`.

Beim ersten Start fehlte `xaerolib`, das die vorhandene Dev-Abhängigkeit Xaero World
Map benötigt. Die bereits lokale `libs/xaerolib-fabric-1.21.11-1.1.15.jar` wurde nur
in das Testprofil kopiert. Der zweite Start erreichte `OttoExtra bereit — 12 Modul(e)
aktiv`, Ressourcenladen und Rendering. Das Log wurde unter
`build/courier-client-smoke.log` gesichert. Offline-Realms-Authentifizierungsfehler
und Warnungen über nicht installierte optionale Integrationen sind kein
Briefmod-Absturz.

Der Start ist ein Laufzeit-Smoke-Test, keine visuell automatisierte Bedienung aller
Briefdialoge. Ein Courier-Ende-zu-Ende-Test auf dem produktiven Server wurde nicht
ausgeführt. Es wurden keine Briefe an andere Spieler geschickt und keine
Verkündungen veröffentlicht.

## Noch auf einem Courier-Testserver auszuführen

Setup: die referenzierte JAR, tatsächlich verwendete MythicItem-Konfiguration,
Permissions, Economy und Ressourcenpaket; für unverfälschten Lesetest
`skills.reading.6`. Zum Versand ein ausdrücklich vorgesehenes Testkonto verwenden.

| Aktion | Erwartung / Negativfall |
| --- | --- |
| Kurzen Brief und mehrseitigen Brief mit Farben, Umlauten, Emoji und Leerzeilen schreiben. | Inventar-Buchseiten entsprechen der Buchvorschau; Farben bleiben über Seiten erhalten. |
| Fertigen unversandten Brief importieren, Seite wechseln, auswählen, tippen, löschen, Tab, Paste und Undo/Redo verwenden. | Originalpräfix bleibt exakt erhalten; Ergänzungen und Cursor bleiben sichtbar, nötigenfalls scrollen. |
| Volle farbige Importseite um weitere Zeilen ergänzen. | Neue Courierseite wird vorhergesagt; aktive Farbe bleibt erhalten, expliziter Reset funktioniert. |
| Während einer Queue Slot/Buch wechseln oder disconnecten. | Queue stoppt vor weiterem Dispatch; Entwurf und Recovery bleiben erhalten. |
| Recovery auf anderem Server/Account oder mit falschem Brief öffnen. | Keine Restbefehle werden verschickt. Passenden Zustand wiederherstellen oder bewusst neu beginnen. |
| Zusätzliches identisches Schreibmaterial im früheren Inventarslot halten. | Start wird blockiert, statt Text auf mehrere Bücher zu verteilen. |
| Schreiben abschließen, Empfängerwahl schließen und `letter send` erneut aufrufen. | Empfängerwahl öffnet sich für den fertigen Brief ohne erneutes Schreiben. |
| Bereits geschriebenen Brief als Verkündung prüfen. | Prüfdialog zeigt tatsächlichen Snapshot, Optimieren ist deaktiviert. |
| Schreib-/Post-/Verkündungspermission, Geld oder Cooldown fehlen. | Serverfehler sichtbar; keine Behauptung einer Zustellung, Entwurf bei kombiniertem Submit bleibt gespeichert. |
| Stark verzögerte Inventarupdates. | Vorsichtiger Halt mit erhaltenem Entwurf; keine automatische Doppelwiederholung. |
| Brief mit niedrigem Leseskill öffnen. | Abweichende Lesedarstellung als Serververhalten einordnen, nicht mit rohen Inventarseiten verwechseln. |

OttoExtra ist laut `fabric.mod.json` ausschließlich clientseitig. Ein Fabric-
Dedicated-Server lädt die Briefmod nicht. Ein `runGameTestServer`-Task war bei der
anfänglichen Taskprüfung nicht konfiguriert; die wesentliche Serverintegration ist
Paper/Courier und wird durch einen Fabric-Serverstart nicht ersetzt.

## Endgültiger Gesamtlauf

`gradlew.bat build -x dist --console=plain` war erfolgreich: **635 Tests, davon 153
Briefmod-Tests; keine Fehler und keine übersprungenen Tests**. Der erneute
JAR-Abgleich bestand ebenfalls alle 2.052 Protokollfälle und 100 kompletten Pläne.

Gesicherte Ausgabe: `build/briefmod-audit/junit/`, `client-smoke.log` und
`summary.json`. Das Audit-JAR heißt
`build/briefmod-audit/ottoextra-1.16.3-w-courier-audit.jar` und enthält weiterhin die
Projektversion 1.16.3-w; es ist kein automatisch veröffentlichtes Release.

SHA-256: `f77caa5a95a58d216380ba4357cd727690a3f3af0e2cc4f3fbb35932b33f59b4`.

JAR-Inhalt geprüft: Client-Environment, Protokoll-/Planner-/Recovery-Klassen und
valide Sprachdateien. `git diff --check` im geänderten Briefmod-/Docs-Bereich meldet
keine Whitespacefehler. Vorhandene Warnungen zu optionalen Xaero-Mappings und
Gradle-Deprecations bleiben außerhalb dieses Briefmod-Fixes.
