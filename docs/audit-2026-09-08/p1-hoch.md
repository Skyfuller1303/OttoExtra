# P1 – hohe Priorität

[Übersicht](README.md) · [Ursprüngliche Prüfnachweise](VALIDIERUNG.md) · [Umsetzung und aktuelle Tests](UMSETZUNG.md)

Die Fehlerbeschreibungen und Quellzeilen unten dokumentieren den Stand vor den Fixes.

<a id="a26-01"></a>
## A26-01: Langchat kann Resttext an eine neue Verbindung senden

**Status: im Code behoben. Beleg: Original-Sender mit kontrollierten Netzwerkhandlern ausgeführt.**

**Auslöser:** Eine lange Nachricht steht teilweise in der Queue. Während der
Sendepause trennt sich der Client und erhält vor Ablauf der Pause einen neuen
Netzwerkhandler. Der verbleibende Text wird ohne erneute Eingabe über diesen
Handler gesendet. Bei einem Wechsel des Servers kann er dadurch andere Empfänger
erreichen. Auch ein direkter Handlerwechsel ohne beobachteten Null-Tick ist nicht
abgesichert.

**Ursache:** [LongChatSender.java](../../src/client/java/de/ottoextra/chat/LongChatSender.java#L30)
prüft in Zeilen 34–40 zuerst die Sendepause und erst danach eine fehlende
Verbindung. Während der Pause bleibt die Queue erhalten. Der Versand in Zeile 44
verwendet den jeweils aktuellen Handler, ohne die ursprüngliche Verbindung zu
speichern. [ChatModule.java](../../src/client/java/de/ottoextra/chat/ChatModule.java)
tickt den Sender weiter und leert ihn beim Disconnect nicht.

**Ausgeführter Nachweis:** Bei 1500 ms konfigurierter Pause sendet Verbindung A
`first`. Nach fünf Ticks ohne Handler empfängt Verbindung B `second`, obwohl auf B
nichts eingereiht wurde. Die Kontrollprobe mit 31 getrennten Ticks leert die Queue
und sendet auf B nichts. Das zeigt auch die zeitliche Bedingung des Fehlers.

Das [Nachweisskript](../../tools/audit-2026-09-08/test-long-chat-sender.ps1)
kompiliert die tatsächliche Produktquelle; nur Client und Handler sind Stubs. Ein
echter Minecraft-Reconnect für diesen Langchatfall wurde in dieser Runde nicht
ausgeführt. Der früher korrigierte Briefversand nutzt eine andere Queue; dessen
Disconnect-Schutz behebt diesen Fall nicht.

**Begrenzte Korrektur:** Queue an die Verbindung bzw. Sitzung beim Einreihen binden.
Beim Disconnect sofort abbrechen und Resttext verwerfen. Vor jeder Ausgabe die
Verbindung prüfen, auch während einer laufenden Sendepause. Ein bloßes Vorziehen
der Nullprüfung reicht für einen Handlerwechsel ohne Null-Tick nicht aus.

**Abnahme:** Mit null, einem, fünf und mehr als 30 getrennten Ticks darf auf B
kein A-Resttext erscheinen; dasselbe bei direktem Handlerwechsel. Neue Eingaben
auf B müssen normal funktionieren. Anschließend realen lokalen Reconnect mit
offener Queue prüfen. Der Test muss die Nachrichten beider Verbindungen erfassen.
