# CHAT-02: Chat-Reparatur entfernt legitime Endwiederholungen

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Originale Suffix-Heuristik lokal per Reflection reproduziert; Pipeline-Aufruf statisch geprüft.

## Auslöser und Folge

Ein Chatbody beginnt mit einem Emote oder einer Klammer und endet absichtlich mit wiederholtem Text. Die Korruptionsreparatur deutet diese Wiederholung als Serverfehler:

- `*nickt*??` wird zu `*nickt*?`.
- `*lacht* hahaha` wird zu `*lacht* haha`.
- `(1+1)=22` wird zu `(1+1)=2`.

Damit ändern sich gültige Aussagen und Zahlen.

## Codebeleg

- [RpChatCorruptionRepair.java:56](../../../src/client/java/de/ottoextra/chat/RpChatCorruptionRepair.java#L56): Ein wiederholtes Suffix und geschlossene RP-Syntax genügen für das Entfernen. Ein Nachweis tatsächlicher Serverkorruption fehlt.
- [ChatMessagePipeline.java:94](../../../src/client/java/de/ottoextra/chat/ChatMessagePipeline.java#L94): Die Reparatur läuft ohne eigenen Kanal-, Sprecher- oder RP-Formatierungsfilter. Die Erkennung eines Nachrichtenbodys innerhalb der Reparatur bleibt Voraussetzung.

## Fixansatz und Regressionstest

Mehrdeutige Suffix-Kürzung entfernen oder auf eindeutige Protokollmerkmale beschränken. Die genannten legitimen Bodies müssen unverändert bleiben; bekannte echte Fehlerfälle separat absichern. Der lokale Nachweis betrifft den Originalalgorithmus, nicht einen empfangenen Live-Chat.


## Umsetzung am 06.09.2026

Die mehrdeutige Suffix-Heuristik wurde entfernt. Wiederholungen, Zahlen, Satzzeichen und Textstile bleiben erhalten. Auch zuvor als Serverkorruption eingestufte, allein anhand des Textes nicht beweisbare Duplikate werden jetzt unverändert angezeigt.

Regression: [ChatP2RegressionTest.java](../../../src/test/java/de/ottoextra/chat/ChatP2RegressionTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.

## Ergänzung am 09.09.2026: Originalabgleich statt Suffix-Heuristik

Ein erneut gemeldeter OOC-/Emote-Fehler war bereits im unveränderten
`ottoextra-chat.log` vorhanden. Minecraft kennzeichnete die Nachricht als
`Modified`. Dessen `MessageIndicator.modified(signedContent)` enthält den
signierten Originaltext als separates Textobjekt; die Struktur wurde gegen
die lokalen Minecraft-1.21.11-Sources geprüft.

Die Reparatur greift jetzt nur bei signiertem RP-Chat mit diesen unveränderten
Originalmetadaten und aktiver RP-Formatierung. Der Anhang muss eine exakte
Wiederholung ab einem inneren Emote bis zum Ende eines abgeschlossenen
OOC-Blocks sein. Der verbleibende Inhalt muss mit dem Original übereinstimmen;
äußere OOC-Klammerpaare und deren Randabstände dürfen in der Serverdarstellung
abweichen. Ohne Originalabgleich wird nichts entfernt, auch nicht beim bekannten
Fehlermuster. Übersetzte/gefilterte Inhalte und absichtliche Wiederholungen werden
nicht durch den Originaltext ersetzt.

Nur eine Kopie der Anzeige wird gekürzt. Kopf-/Wappen-Komponenten, Stile,
Signatur, Original-Hinweis und Empfangslogs bleiben erhalten. Unbekannte
Komponentenstrukturen fallen unverändert zurück.

Regression: [RpChatCorruptionRepairTest.java](../../../src/test/java/de/ottoextra/chat/RpChatCorruptionRepairTest.java).
Zusätzlich bestätigt ein isolierter Fabric-Client den tatsächlichen ChatHud-Pfad,
Verlaufsaktualisierung, verzögerte Addon-Verarbeitung und unverändertes Logging.
