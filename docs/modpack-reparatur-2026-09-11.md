# Bestands-Modpack: Reparatur und Prüfung

Stand: **11.09.2026**. Zunächst angepasst wurde das lokale Entwicklungs-Modpack unter
`OttoExtra/run/mods`. Es verwendet OttoExtra **0.1.17-l** aus dem Loom-Quellstand.
Anschließend wurden die beiden unten genannten Builds auch im Launcher-Profil
**Ottonien 1.0.0** installiert; siehe Nachtrag. Veröffentlichte Downloads wurden nicht verändert.

## Nachtrag: Installation im Launcher-Profil

Am **11.09.2026, 15:38 Uhr** wurden in
`%APPDATA%/ModrinthApp/profiles/Ottonien 1.0.0/mods` der aktuelle getestete Core
`ottoextra-0.1.17-l.jar` und `OttoExtra-AnimalBreeding-0.5.16.jar` installiert.
Die bisherige `ottoextra-0.1.17-m.jar` wurde aus dem Modordner in
`OttoExtra/modpack-backups/20260911-153808-ottonien-1.0.0/` verschoben.
Das dortige `manifest.json` dokumentiert Originalpfade und SHA-256.
Installations-/Build-Hashes stimmen überein; beide Mod-IDs sind jeweils genau einmal
aktiv vorhanden. AnimalBreeding war zuvor nicht installiert.

Die übrigen Mods und Einstellungen dieses Profils blieben erhalten. Es enthält
andere Fremdmod-Versionen als das oben getestete Entwicklungs-Modpack; die
Installation ist daher noch kein Laufzeitnachweis für diese Launcher-Kombination.
Die neuen Dateien werden beim nächsten Spielstart geladen.

## Änderungen

| Bereich | Vorher | Jetzt / Ort |
|---|---|---|
| AnimalBreeding | Installiert: 0.5.9, verlangt Core ≥1.16.3; nicht mit 0.1.17-l startbar. | **0.5.16** aus dem vorhandenen Add-on-Quellstand gebaut und in `run/mods` installiert. Abhängigkeit zentral aus `ottoextra_version=0.1.17-l`: `>=0.1.17-l <0.2.0`. Keine Loader-Übersteuerung. |
| AnimalBreeding-Zustimmung | Eigener Sync prüfte nur Ottonien/Züchtermodus; Core sperrte unautorisierte HTTP-Aufrufe. | Add-on verwendet zusätzlich die Online-Freigabe des Core. Widerruf macht die Sitzung ungültig; spätere Freigabe startet eine neue Sync-Generation. DE-/EN-Status nennt die Zustimmung. |
| Alte Core-JAR | `run/mods/ottoextra-0.1.15.jar` zusätzlich zum aktuellen Core aus Loom. | Aus dem Modordner in die Sicherung verschoben. Bei `runClient` liefert Loom den aktuellen Core; keine zweite Core-JAR dort ablegen. |
| Java | Clientstart mit Java 21; C2ME-Natives verlangt ≥22. | `runClient` und `runClient2` verwenden **Java 25**, konfiguriert über `modpack_java_version`. Compilerziel, Tests und Server bleiben Java 21. |
| Xaero | World Map 1.40.16 im Modpack; zusätzliche 1.41.0 im Gradle-Runtime-Classpath. XaeroPlus verlangt genau 1.40.16. | Zusätzliche Runtime-Abhängigkeit entfernt. **World Map 1.40.16 + Minimap 25.3.12 + XaeroPlus 2.31.5** bleiben zusammen. XaeroLib 1.1.15 ist in den Xaero-JARs eingebettet. |

Add-on-Dateien: [Version](../../OttoExtra-AnimalBreeding/gradle.properties),
[Build](../../OttoExtra-AnimalBreeding/build.gradle),
[Metadaten](../../OttoExtra-AnimalBreeding/src/main/resources/fabric.mod.json),
[Client-Sitzung](../../OttoExtra-AnimalBreeding/src/main/java/de/ottoextra/animalbreeding/AnimalBreedingClient.java),
[Regression](../../OttoExtra-AnimalBreeding/src/test/java/de/ottoextra/animalbreeding/sync/AnimalBreedingSyncServiceTest.java).
Da das Add-on-Verzeichnis kein Git-Repository besitzt, ist der gesamte Reparaturdiff
zusätzlich als [Patch](../tools/consent-smoke/animalbreeding-0.5.16.patch) abgelegt.

## Prüfungen

- [x] Core: **783 Java-Tests**, Build erfolgreich; keine Fehler oder ausgelassenen Tests.
- [x] AnimalBreeding: **52 Tests**, Build erfolgreich. Neue Regression: verzögerte Namensantwort bleibt nach Widerruf/erneuter Freigabe wirkungslos; erst ein neuer Abgleich übernimmt aktuelle Namen.
- [x] Gradle-Laufzeitmodell geprüft: beide Clienttasks Java 25.0.2; Server/Test Java 21.0.10; beide Java-Compilerziele 21; keine zusätzliche World-Map-Runtime-Abhängigkeit.
- [x] Vollständiger Clientlauf mit reparierter Modkombination und AnimalBreeding 0.5.16: Start, sieben englische Dialoge, Bilddownload/Widerruf, echter Pack-Reload, Low-Health-Effekte, Weltwechsel und Reconnect bestanden.
- [x] AnimalBreeding-Hofübersicht über `J` geöffnet und gerendert. Manueller Klick auf **Sync** bei verweigerter Online-Zustimmung bleibt `LOCAL_ONLY`; keine OttoExtra-HTTP-Anfrage beim simulierten Ottonien-Join/Reconnect.
- [x] Xaero-Karte geöffnet; tatsächlicher OttoExtra-Overlay-Hook ausgeführt. Hofübersicht und Karte zusätzlich im Screenshot geprüft.
- [x] Installierte Add-on-JAR entspricht dem Build-Hash; keine Testklassen enthalten, Client-Metadaten und Java-21-Bytecode geprüft. Original-Sicherungen anhand ihrer Hashes verifiziert.

Logs: `build/modpack-repair/core-build.log`, `animalbreeding-build.log`,
`modpack-fixed-run.log`. Inventar der **71 aktiven JAR-Dateien**:
`build/modpack-repair/mods-inventory.json`. Verschachtelte Mods und Fabric-Module
erhöhen die Zahl der vom Loader gemeldeten Mods.

Testprofil: `build/privacy-qa/modpack-fixed/`, mit `privacy-qa-results.txt` und
Screenshots. Es kopiert die installierten Add-ons, lädt World Map direkt aus
deren Modordner und ergänzt den Core/Fabric über Loom. Die Tests benutzen eine
separate Konfiguration und synthetische Daten. Sie prüfen weder private
Bestandsdaten noch die Erreichbarkeit/Berechtigungen des produktiven Backends.

## Start und Wiederholung

Aus `OttoExtra/`:

```powershell
# Entwicklungs-Modpack mit dem aktuellen Core starten.
.\gradlew.bat runClient -x dist --console=plain

# Automatisierter Clienttest ohne öffentlichen Serverbeitritt.
.\gradlew.bat -I tools/consent-smoke/privacy.init.gradle -PprivacyQaMode=modpack-fixed runClientGameTest -x dist --console=plain
```

Java 25 muss als Gradle-Toolchain verfügbar sein; auf diesem Rechner ist es bereits
installiert. Die Systemeinstellung `JAVA_HOME` wurde nicht geändert. Ein getrenntes
Produktionslauncher-Profil benötigt den fertig gebauten Core zusätzlich zur
Add-on-JAR und eine passende Java-Laufzeit; dieser Launcher-Lauf bleibt offen.

## Artefakte und Rückweg

| Artefakt | Bytes | SHA-256 |
|---|---:|---|
| `OttoExtra/build/libs/ottoextra-0.1.17-l.jar` | 31.085.673 | `db910ae1fc74da3c6d1317c33de9ed71236c30b91c4f5e5323e63117cafdc81a` |
| `OttoExtra-AnimalBreeding/build/libs/OttoExtra-AnimalBreeding-0.5.16.jar` | 123.520 | `08e4d0006196d0d1c57dcbb47549a3e5c70e4a7c66e365b107dcb437b6e4a297` |

Sicherung: `OttoExtra/modpack-backups/20260911-145246/`.
`manifest.json` enthält Originalpfade, Sicherungspfade und SHA-256; `mods/` enthält
die alten Core-/Add-on-JARs, `sources/` die vorherigen bearbeiteten Dateien.
`dist` wurde nicht ausgeführt, da der Task ältere Release-JARs entfernt.

Rückweg bei beendetem Client: neue Add-on-JAR aus `run/mods` herausnehmen,
die beiden alten JARs aus `mods/` zurückkopieren und die betroffenen Quelldateien
gezielt aus `sources/` wiederherstellen. Spätere eigene Änderungen vorher sichern.
Dies stellt den vorherigen, mit dem aktuellen Core inkompatiblen Zustand wieder her.

## Verbleibende Grenzen

AmbientSounds 6.3.5/CreativeCore 2.14.11 melden weiterhin fehlerhafte Pack-Metadaten;
die Mods initialisieren und die geprüften Szenarien bestehen trotzdem. Ihre JARs
wurden nicht verändert. Weitere optionale Integrationswarnungen, malilibs Meldung
zur fehlenden Erststart-Konfiguration und ein BetterF3/Fabric-Test-Accessor-Konflikt
sind im Log erhalten. Das ist keine vollständige Abnahme aller Fremdmod-Funktionen.

Die offenen Betreiber-, Datenschutz-, Rechte- und Produktionsprüfungen aus dem
[Fahrplan](release-datenschutz-fahrplan.md) bleiben offen. Die Modpack-Reparatur
erteilt keine Modrinth-Freigabe. Dokumentation und lokale Änderungen wurden mit
generativer KI erstellt.
