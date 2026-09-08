# CHAT-03: Lange Chatnachrichten zerreißen Emoji-Surrogatpaare

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Originaler Splitter lokal mit Java 21 reproduziert.

## Auslöser und Folge

Eingabe `"x".repeat(253) + "😀" + "abc"`, Chunkgröße 256, Marker `" >"`. Der erste Teil endet vor dem Marker mit einem isolierten High-Surrogat; der zweite beginnt mit dem Low-Surrogat. Beide Teile enthalten damit ungültige Unicode-Sequenzen. Das Emoji kann beim UTF-8-Versand nicht unverändert übertragen werden.

## Codebeleg

- [LongChatSplitter.java:74](../../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L74): `preferredCut()` liefert im harten Schnitt einen UTF-16-Index ohne Codepoint-Prüfung.
- [LongChatSplitter.java:42](../../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L42) und [Zeile 65](../../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L65): Beide Teilstrings übernehmen diese Grenze direkt.

## Lokaler Nachweis

`partCount=2, highBeforeMarker=true, lowAtNextStart=true`. Ausführbares Beispiel unter [Validierung](../VALIDIERUNG.md).

## Fixansatz und Regressionstest

Schnittgrenzen zwischen High- und Low-Surrogat auf eine gültige Codepoint-Grenze verschieben; auch Markerkürzung prüfen. Tests mit Emojis an beiden Seiten der Chunkgrenze müssen das Größenlimit einhalten und ohne isolierte Surrogate auskommen.


## Umsetzung am 06.09.2026

Harte Schnittgrenzen und die Kürzung langer Fortsetzungsmarker berücksichtigen Surrogatpaare. Der Marker lässt Platz für mindestens einen vollständigen Codepoint.

Regression: [ChatP2RegressionTest.java](../../../src/test/java/de/ottoextra/chat/ChatP2RegressionTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
