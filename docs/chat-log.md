# Eigenes Chatprotokoll

Unter **Tweaks → Basis → Chatprotokoll** ist das Speichern standardmäßig aktiv,
auch bei vorhandenen Konfigurationen ohne diese neue Option.

Die Datei liegt im Minecraft-Spielverzeichnis unter
**`logs/ottoextra-chat.log`**, neben `latest.log` und gegebenenfalls `debug.log`.
Sie wird mit der ersten Nachricht angelegt und bei jedem Start fortgeschrieben.
Vorhandene Einträge werden weder beim Neustart noch beim Ausschalten gelöscht;
es gibt keine automatische Kürzung oder Aufbewahrungsfrist.

Jeder Eintrag enthält das lokale Datum mit Uhrzeit und den ursprünglichen
Nachrichtentext in UTF-8. Farbcodes werden entfernt. Zeilenumbrüche werden als
`\r`/`\n` und ursprüngliche Backslashes als `\\` geschrieben, damit jede
empfangene Nachricht genau einen eindeutig lesbaren Logeintrag erhält.

Erfasst werden alle Nachrichten, die den Minecraft-Chat erreichen: Spielerchat,
System-/Servermeldungen und lokale Modmeldungen. Das gilt auf allen Servern und
im Einzelspieler, unabhängig von Debug-Logging und RP-/Chat-Modulschaltern.
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

Technisch nutzt die Funktion einen eigenen Log4j-Datei-Appender mit Append und
sofortigem Flush. Er hängt nicht am allgemeinen Logger und bekommt ausschließlich
Chattexte. Beim Beenden des Clients wird er geschlossen.
