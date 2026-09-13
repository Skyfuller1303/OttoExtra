# Eigenes Chatprotokoll

OttoExtra schreibt Chatnachrichten direkt in eigene, sofort lesbare Dateien
unter **`logs/chat/`** im Minecraft-Spielverzeichnis. Die Ablage funktioniert
unabhängig von Minecrafts `latest.log` und dessen `.log.gz`-Archivierung.

Unter **Tweaks → Basis → Chatprotokoll** ist das Speichern standardmäßig **aus**.
Aktivieren öffnet eine Information; erst „Erlauben“ schaltet es ein. Frühere
`true`-Werte ohne dokumentierte Entscheidung werden ebenfalls ausgeschaltet, weil
sie vom damaligen Default stammen können. Eine neue bestätigte Wahl bleibt bei
Neustart erhalten. Abbrechen der Einstellungen und Backup-Restore ändern sie nicht.

Standardumfang bei Aktivierung: **nur Ottonien**. Optional **ALL** für alle Server
und Einzelspieler. Ein Umfangswechsel schaltet das Log aus und benötigt eine neue
Bestätigung. Die Wahl gilt sofort und bleibt auch nach Abbrechen der Einstellungen bestehen.

Die Dateien heißen **`ottoextra-chat-JJJJ-MM-TT-X.log`**, beispielsweise:

```text
logs/chat/ottoextra-chat-2026-09-10-1.log
logs/chat/ottoextra-chat-2026-09-10-2.log
logs/chat/ottoextra-chat-2026-09-11-1.log
```

Mit der ersten Nachricht einer Minecraft-Sitzung wird eine neue Datei angelegt.
`X` ist die nächste Nummer nach der höchsten vorhandenen Nummer dieses Datums.
Bei einem lokalen Datumswechsel oder Erreichen von 8 MiB beginnt mit der nächsten
Nachricht ebenfalls eine neue Datei. Mehrere gleichzeitig laufende Clients überschreiben einander
nicht. Aus- und Einschalten am selben Tag schreibt die aktuelle Datei weiter.

Die Dateien bleiben unkomprimierte UTF-8-Logs. Grenze: **8 MiB je Datei, 64 MiB
insgesamt und 4096 Dateien** unter `logs/chat/`. Bei vollem Budget pausiert das
zusätzliche Log; keine bestehenden Inhalte werden automatisch gelöscht. Einzelne
Einträge über 64 KiB werden ausgelassen. Mehrere gleichzeitig schreibende Clients
können das Gesamtbudget bis zur nächsten Rotation überschreiten; es ist keine
prozessübergreifende Festplattenquote.

Das [Offline-Werkzeug](lokale-daten.md) schlägt mit `--expired` Chatlogs älter als
**30 Tage** zur Bereinigung vor; erst ein bestätigter Löschplan entfernt sie. Eine
vorhandene alte `logs/ottoextra-chat.log` bleibt erhalten, wird nicht weitergeschrieben
und lässt sich separat im selben Löschplan erfassen. Vanilla-Logs werden nie erfasst.

Jeder Eintrag enthält das lokale Datum mit Uhrzeit und den ursprünglichen
Nachrichtentext in UTF-8. Farbcodes werden entfernt. Zeilenumbrüche werden als
`\r`/`\n` und ursprüngliche Backslashes als `\\` geschrieben, damit jede
empfangene Nachricht genau einen eindeutig lesbaren Logeintrag erhält.

Beispiel für einen Eintrag:

```text
[2026-09-10 20:04:15.123] [Sprechen] Matthias: Test
```

Jede Nachricht wird sofort geschrieben und der Dateipuffer geleert. Sie steht
damit schon während des Spiels auf der Festplatte. Ausschalten von
**Chatprotokoll** stoppt neue Einträge; vorhandene Dateien bleiben erhalten.

Erfasst werden alle Nachrichten, die den Minecraft-Chat erreichen: Spielerchat,
System-/Servermeldungen und lokale Modmeldungen. Das gilt innerhalb des ausdrücklich gewählten Serverumfangs, unabhängig von
Debug-Logging und RP-/Chat-Modulschaltern.
OttoExtra protokolliert vor der Umformatierung und vor der Unterdrückung
automatischer Tierinfo-Antworten. Solche Antworten bleiben daher nachlesbar.
Verzögert verarbeitete Nachrichten werden nur beim ursprünglichen Eingang
geschrieben, nicht bei ihrer erneuten Anzeige. Tatsächlich mehrfach empfangene
identische Nachrichten bleiben dagegen einzeln erhalten.

Actionbar, Bossbars und noch nicht gesendete Chat-/Befehlseingaben sind keine
Chatnachrichten und werden von diesem Protokoll nicht erfasst. Die Datei wird
nur lokal geschrieben. Ausschalten greift sofort für neue Nachrichten.

Schreibfehler unterbrechen die Chatdarstellung nicht. Sie werden gedrosselt im
normalen Minecraft-Log gemeldet; nach zehn Sekunden wird ein neuer Schreibversuch
zugelassen. Während eines Dateifehlers können Nachrichten nicht gesichert werden.

Technisch nutzt die Funktion einen eigenen UTF-8-Dateischreiber mit sofortigem
Flush. Dateinamen werden atomar mit `CREATE_NEW` reserviert. Beim Beenden des
Clients wird die Datei geschlossen; nach einem Schreibfehler kann der nächste
zugelassene Versuch eine neue Datei mit der nächsten Nummer öffnen.

Der normale Minecraft-Chatlogger erhält weiterhin `[CHAT]`-Nachrichten. Die
zusätzliche `[OttoExtra-Chat]`-Spiegelung wurde durch die eigenständige Ablage
ersetzt; für die Sicherung zählt ausschließlich die Datei unter `logs/chat/`.
