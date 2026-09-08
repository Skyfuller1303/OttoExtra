# REGIONS-02: Regionsfenster zeigt nachgeladenen Detailstand erst beim Wiederöffnen

Priorität: **P3 – niedrig**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Screen- und Service-Aufrufkette statisch bestätigt; kein UI-Test.

## Auslöser und Folge

Das Regionsfenster öffnet mit unvollständigen oder veralteten Cachedaten. Der anschließende Detailrequest liefert bessere Daten, das bereits offene Fenster bleibt jedoch beim vorherigen Stand. Notizen, Fähigkeiten und Fraktionsinformationen fehlen weiter. Schließen und Wiederöffnen hilft.

## Codebeleg

- [RegionInfoScreen.java:101](../../../src/client/java/de/ottoextra/regions/RegionInfoScreen.java#L101): Nur `init()` ruft `loadData()` auf.
- [RegionInfoScreen.java:130](../../../src/client/java/de/ottoextra/regions/RegionInfoScreen.java#L130): Die Methode übernimmt zuerst Cachewerte in eigene Felder und startet danach den Detailrequest.
- [RegionDataService.java:563](../../../src/client/java/de/ottoextra/regions/RegionDataService.java#L563): Dessen Antwort aktualisiert die Service-Indizes, nicht die Screenfelder.
- [RegionInfoScreen.java:140](../../../src/client/java/de/ottoextra/regions/RegionInfoScreen.java#L140): Bei zunächst fehlender Fraktion wird auch der Spieler-Tab nicht geladen.

## Fixansatz und Regressionstest

Nach erfolgreichem Request die Daten des weiterhin passenden Screens auf dem Clientthread aktualisieren. Test: Fenster mit Grunddaten öffnen, Antwort mit neuen Details verzögert liefern; die Anzeige muss ohne Wiederöffnen wechseln. Antworten für inzwischen geschlossene oder andere Regionen ignorieren.


## Umsetzung am 06.09.2026

Der Service bietet zusätzlich einen abwartbaren Detailrequest an. Laufende Prefetches werden gemeinsam verwendet; die Completion erfolgt erst nach dem Indexupdate. Das Fenster übernimmt anschließend Region, Fraktion und Vasallen auf dem Clientthread und aktualisiert Tab-Beschriftungen sowie Zeilenbuttons. Ein bereits ausgewählter Spieler-Tab lädt seine Liste nach, sobald eine Fraktion verfügbar ist.

Callbacks prüfen die aktive Screeninstanz und ihre Lebensdauer. Beim Fraktionswechsel sind auch alte Spielerlistenantworten ungültig. Verspätete Detailantworten alter Verbindungen überschreiben keine neuen Cachedaten. Der bisherige öffentliche Prefetch-Aufruf und der Cooldown nach erfolgreichem Laden bleiben erhalten; fehlgeschlagene Requests können erneut versucht werden.

Fünf Tests in [RegionDetailRefreshTest.java](../../../src/test/java/de/ottoextra/regions/RegionDetailRefreshTest.java) prüfen verzögerte Fake-API-Antworten, die Übernahme neuer Notizen/Fähigkeiten/Fraktion/Vasallen erst auf dem Client-Executor, gemeinsame Prefetches, geschlossene und wiedergeöffnete Views, andere ausgewählte Regionen, Fehler/Retry und alte Verbindungen. Das ist eine isolierte Service-/Callback-Prüfung, kein gerenderter UI-Test.

Buildnachweis und verbleibende Ingame-Abnahme: [P3-UMSETZUNG.md](../P3-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor dem Fix.
