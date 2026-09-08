# Audit-Regressionsnachweise vom 08.09.2026

Die Skripte wurden nach den Korrekturen von Fehlernachweisen auf Regressionen
umgestellt. **Exitcode 0 bedeutet jetzt, dass die erwarteten korrigierten
Verhaltensweisen bestehen.** Die früheren Fehlerprotokolle bleiben unter
`build/audit-2026-09-08/long-chat-sender/` und `persistence/` erhalten.
Die aktuellen Ergebnisse liegen separat unter `build/audit-2026-09-08/fixes/`.

Beide Skripte benötigen ein JDK 21 mit `java` und `javac` im PATH. Alternativ
das bin-Verzeichnis über `-JavaBin` angeben. Aus dem OttoExtra-Repository:

```powershell
.\tools\audit-2026-09-08\test-long-chat-sender.ps1 -JavaBin 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin'
.\tools\audit-2026-09-08\test-persistence.ps1 -JavaBin 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin'
```

Den JDK-Pfad gegebenenfalls anpassen.

## LongChatSender

Kompiliert die aktuellen Produktquellen `LongChatSender.java`,
`LongChatSplitter.java` und `RpChatSyntax.java`. Nur MinecraftClient und
Netzwerkhandler sind aufzeichnende Stubs. Zehn Szenarien prüfen:

- Wechsel mit null, einem, fünf und 31 Ticks ohne Verbindung.
- Wechsel vor dem ersten Versandtick; expliziter Abbruch bei wiederverwendetem Handler.
- Neue Eingabe auf der neuen Verbindung, bevor der nächste Tick läuft.
- Sendepause bei weiteren Eingaben, auch wenn der letzte Chunk bereits gesendet wurde.
- Ablehnung von Eingaben ohne Verbindung.

Der tatsächliche Queuealgorithmus wird ausgeführt; es gibt keinen Netzwerkzugriff.
Compilerprotokoll, Ergebnis und Quellhashes:
`build/audit-2026-09-08/fixes/long-chat-sender/`.

## Konfiguration und Personenpersistenz

Benötigt aktuelle kompilierte Produktklassen, z. B. nach
`gradlew.bat test -x dist`, und den vorhandenen Minecraft-/Bibliotheks-Klassenpfad
aus `.gradle/loom-cache/launch.cfg` und `remapClasspath.txt`.
Kompiliert nur `PersistenceProbe.java`; keine Stubs, kein eigener Gradle-Start.

Drei Regressionen und zwei Kontrollen prüfen:

- `chatLog` wird in beiden Richtungen restauriert; `combatModeHud` bleibt korrekt.
- Eine frische Installation speichert mit und ohne vorausgehende Reparatur.
- Eine gesunde Datei ersetzt bei der Reparatur keine noch ungespeicherten Profile.
  Ein kurzer gemeinsamer Store-Monitor sichert die Reihenfolge vor dem verzögerten
  Flush. Beide Profile müssen danach auch nach Speichern und Shutdown vorliegen.

Alle Testdateien entstehen in einem neuen Unterordner von
`build/audit-2026-09-08/fixes/persistence/`. Quellen, Hauptklassen und
Abhängigkeiten werden mit SHA-256 protokolliert. Veraltete Hauptklassen werden
anhand ihres Zeitstempels abgelehnt.

## Echter Client und lokaler Server

Die zusätzlichen `FIX-*`-Fälle verwenden den vorhandenen
[Courier-E2E-Aufbau](../courier-e2e/README.md):

```powershell
.\tools\courier-e2e\run.ps1 -Filter 'FIX-CHAT-OFF,FIX-CHAT-OOC,FIX-CHAT-RP,FIX-RECONNECT,FIX-GPU,FIX-STATUS,FIX-PAGES'
```

Ohne Filter laufen außerdem sämtliche bisherigen V2-Fälle. Das Testhilfsplugin
zeichnet jetzt auch empfangene Chatnachrichten auf. Die neue Testklasse
`AuditFixClientChecks` prüft reale OpenGL-Texturen, UI-Rückmeldungen und den
Buchseitenzähler. Diese Klassen gehören ausschließlich zum Test-SourceSet.

Ergebnisse und verbleibende Abnahmegrenzen:
[Umsetzungsbericht](../../docs/audit-2026-09-08/UMSETZUNG.md).
