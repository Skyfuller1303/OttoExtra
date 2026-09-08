# LETTER-01: Briefversand läuft nach schnellem Reconnect unbestätigt weiter

Priorität: **P1 – hoch**. Status: behoben; 14 automatisierte Regressionstests grün (2026-09-06). Kein Multiplayer-Versand ausgeführt.

## Auslöser und Folge

Während einer Seitenpause im mehrseitigen Briefversand trennt sich die Verbindung. Erfolgt der Reconnect vor dem nächsten geplanten Befehl, schreibt die alte Queue weiter – gegebenenfalls auf einen anderen Server. Ein zusätzlicher Start über den Recovery-Dialog kann zwei parallele Queues erzeugen, die denselben Fortschrittscache verändern.

## Ursache vor dem Fix

Disconnect setzte lediglich den Versandstatus zurück. Die Versanddienste verwahrten keine aktive, abbrechbare Queue. Jeder Queue-Schritt verwendete den dann aktuellen NetworkHandler, ohne Bindung an die ursprüngliche Verbindung.

## Reproduktion

Mit bedeutungslosem Testtext einen mehrseitigen Brief beginnen. In einer Seitenpause von standardmäßig 3–5,2 Sekunden disconnecten und vor dem nächsten Schritt wieder verbinden. Die Queue sieht einen gültigen neuen Handler und sendet weiter.

## Umgesetzter Fix

- [LetterModule.java](../../../src/client/java/de/ottoextra/letter/LetterModule.java): Disconnect und Client-Stop brechen Brief-/Verkündungsversand und die vorhandene Kalibrierungsqueue ab. Während eines aktiven Versands erscheint kein Recovery-Dialog.
- [LetterServices.java](../../../src/client/java/de/ottoextra/letter/LetterServices.java): Alle Schreib-, Zustellungs- und Recovery-Einstiege prüfen laufenden Versand und Verbindung, bevor sie Fortschritts- oder Entwurfscaches ändern.
- [ActiveCommandSendQueue.java](../../../src/client/java/de/ottoextra/letter/send/ActiveCommandSendQueue.java): Genau eine aktive Versandqueue; Generationsprüfung verhindert, dass alte Fortschritts-, Abschluss- oder Unterbrechungs-Callbacks einen neu gestarteten Versand verändern. Abbruch erhält bereits gespeicherten Fortschritt.
- [CommandSendQueue.java](../../../src/client/java/de/ottoextra/letter/send/CommandSendQueue.java): Bindet den NetworkHandler beim Start und prüft ihn vor jedem Befehl und Abschluss. Abbruch wird vor dem Einreihen auf den Client-Thread und erneut im Client-Callback geprüft. Eine Queue lässt sich nicht doppelt starten. Der öffentliche Konstruktor bleibt mit der Kalibrierung kompatibel.

Verzögerungen und Fortschrittszählung bleiben erhalten. Nur ein regulärer Abschluss löscht den Fortschrittscache. Recovery setzt nach ausdrücklicher Auswahl bei `sentCommands` fort und startet eine neue, an die aktuelle Verbindung gebundene Queue.

## Regressionstests und Prüfgrenzen

[CommandSendQueueTest.java](../../../src/test/java/de/ottoextra/letter/send/CommandSendQueueTest.java) und [ActiveCommandSendQueueTest.java](../../../src/test/java/de/ottoextra/letter/send/ActiveCommandSendQueueTest.java) enthalten 14 deterministische Tests mit steuerbarem Scheduler, getrenntem Client-Dispatcher und austauschbarer Verbindung:

- Reihenfolge, konfigurierte Verzögerungen, einmaliger Abschluss und leere Befehlsliste.
- Handlerwechsel in der Pause sowie nach bereits eingereihtem Client-Callback; keine weiteren Befehle und kein alter Abschluss.
- Abbruch vor Ausführung eines Befehls oder Abschlusses; kein neuer Client-Auftrag aus einem abgebrochenen Timer.
- Handlerbindung beim Start, Start ohne Verbindung sowie Recovery nur der verbleibenden Befehle.
- Ablehnung paralleler Schreib-/Recovery-Starts, Schutz neuer Fortschritts-/Cachezustände vor alten Callbacks sowie Freigabe des Versandstatus bei Verbindungsverlust.

Geprüft mit `./gradlew test --tests de.ottoextra.letter.send.CommandSendQueueTest --tests de.ottoextra.letter.send.ActiveCommandSendQueueTest`: 14 Tests erfolgreich; Client- und Testquellen kompiliert. Vollständiger Test-/Build-Lauf wird über die P1-Gesamtprüfung dokumentiert.

Manuelle Akzeptanzprüfung bleibt offen: mehrseitigen Testbrief beginnen, während der Pause disconnecten, auf gleichen und anderen Server reconnecten; vor Recovery-Auswahl darf kein Befehl folgen. Anschließend einmal fortsetzen und die verbleibenden Befehle prüfen. Dasselbe mit Verkündung und Client-Schließen wiederholen. Keine neuen Common-/Server-Imports; Änderungen liegen ausschließlich im Client-Source-Set.
