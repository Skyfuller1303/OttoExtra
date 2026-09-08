# CHAT-01: Verschachtelte Klammern erzeugen eine wiederkehrende Chatqueue

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Splitter lokal mit Java 21 reproduziert; erneuter Sendepfad statisch bestätigt.

## Auslöser und Folge

Im RP-Modus eine Eingabe aus 300 öffnenden Klammern teilen: `"(".repeat(300)`, Chunkgröße 256, Marker `" >"`. Das Ergebnis enthält 1.000 künstliche Mininachrichten und als letzten Teil wieder die ursprünglichen 300 Zeichen. Dieser Rest wird beim Versand erneut geteilt. Die Queue kann sich wiederholt erneuern, normales Chatten verzögern und Spam-Sanktionen auslösen.

## Codebeleg

- [LongChatSplitter.java:32](../../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L32): Ein Guard begrenzt nur den lokalen Schleifendurchlauf.
- [LongChatSplitter.java:65](../../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L65): `reopen` ergänzt die abgeschnittenen Klammern wieder; der Rest schrumpft nicht. Zeile 69 hängt ihn unverändert an.
- [LongChatSender.java:44](../../../src/client/java/de/ottoextra/chat/LongChatSender.java#L44) → [ChatMessageSplitMixin.java:22](../../../src/client/java/de/ottoextra/mixin/ChatMessageSplitMixin.java#L22): Der überlange Rest erreicht beim Versand erneut den Splitter.

## Lokaler Nachweis

`parts=1001, firstLength=4, lastLength=300, lastEqualsInput=true`. Reproduktionsskript unter [Validierung](../VALIDIERUNG.md). Es wurden keine Nachrichten an einen Server gesendet.

## Fixansatz und Regressionstest

Strikten Fortschritt pro Iteration und das Größenlimit für jeden Ausgabeteil erzwingen. Kann ein Syntaxpräfix nicht sinnvoll fortgesetzt werden, kontrolliert ohne Wiedereröffnung teilen. Regression: tiefe Klammerung terminiert mit begrenzter Teilzahl und keinem überlangen Rest.


## Umsetzung am 06.09.2026

LongChatSplitter erzwingt in jedem Schritt Fortschritt und das Größenlimit. Bei nicht fortsetzbarer Klammerung wird der Rest ohne künstliches Wiederöffnen geteilt.

Regression: [ChatP2RegressionTest.java](../../../src/test/java/de/ottoextra/chat/ChatP2RegressionTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
