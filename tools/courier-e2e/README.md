# Courier-End-to-End-Test

Vom OttoExtra-Verzeichnis aus:

```powershell
./tools/courier-e2e/run.ps1
# Einzelne Fälle wiederholen:
./tools/courier-e2e/run.ps1 -Filter 'V201,I01,P01,A01'
```

Das Skript startet einen isolierten Paper-1.21.11-Server und einen echten Minecraft-Fabric-Client mit OttoExtra aus dem aktuellen Quellstand. Die originale Courier-2.1.0-JAR bleibt unverändert. Details zu Paper, EULA, Abhängigkeiten und Testmaterial stehen in [server/README.md](server/README.md).

Minecraft-Port: `127.0.0.1:25579`; Teststeuerung: `127.0.0.1:25580`. Es werden ausschließlich lokale Testkonten verwendet. Das automatisch gestartete Testspiel wird beendet; ein vom Skript gestarteter Server wird anschließend ebenfalls gestoppt. `-KeepServer` lässt diesen Server weiterlaufen. Ein bereits laufender passender Testserver wird wiederverwendet und nicht vom Skript beendet.

Der testexklusive SourceSet wird nur durch `client.init.gradle` aktiviert. Testmod, HTTP-Steuerung und Zwischenablagen-Mixin gelangen nicht in die OttoExtra-Release-JAR. Die Zwischenablage ist im Testclient intern, weil Windows der automatisierten Instanz den Zugriff auf die native Zwischenablage verweigert. Die eigentlichen Editor-Paste-/Tastaturhandler, Bildschirme, Schreibqueues und Minecraft-Netzwerkpakete laufen unverändert.

Der Server-Helfer stellt Inventar, Permissions und Testguthaben bereit. Er implementiert keine Courier-Befehle und keine Pagination. Die Assertions lesen tatsächliche Bukkit-Buchseiten, Courier-Queues, Befehlsereignisse und gespeicherte Verkündungen. Die Schaltflächen werden mit echten Minecraft-Mausereignissen betätigt; ihre Position muss im sichtbaren Fenster liegen. Nur der atomare Konkurrenztest ruft den ersten Schreibbutton direkt auf, damit zwischen zwei Starts kein Netzwerkpaket verarbeitet wird. Der normale Empfängerdialog wird mit dem lokalen Testkonto bestückt; die Brieftaube wird über ein echtes Client-Interaktionspaket angenommen.

Ergebnisse:

- `build/courier-e2e/results/results.json`: letzter Lauf, pro Fall Status, Laufzeit, Fehler und Serverzustand.
- `build/courier-e2e/results/run-*.json`: dauerhaft getrennte Testläufe.
- `build/courier-e2e/client-*.log`: Client-/Gradle-Logs.
- `build/courier-e2e/client/screenshots/`: gerenderte Screenshots.
- `build/courier-e2e/server/reports/`: unabhängige Serverereignisse und Fehlerbelege.

Für die große Textmatrix sind die konfigurierbaren Wartezeiten auf 500 ms reduziert. V201 verwendet die Standard-PAGE/V2-Wartezeit; Unterbrechungstests verwenden gezielt längere Intervalle. Jede erfolgreiche Queue wartet auf die reguläre abschließende Inhaltsprüfung.

Die Testwelt verwendet Courier-konformes benanntes PAPER statt MythicMobs und einen deterministischen Economy-Testanbieter. Ein vollständiges Produktivserver-Modpack, echte Discord-Veröffentlichungen sowie das Ottonien-Ressourcenpaket sind nicht Bestandteil dieser lokalen Suite. Der diagnostische adaptive Kalibrierungsdialog ist ebenfalls ein eigener Testbereich; die enthaltenen Kalibrierungstexte werden hier durch den normalen V2-Editor geschickt.
