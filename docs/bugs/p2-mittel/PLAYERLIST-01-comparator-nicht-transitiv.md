# PLAYERLIST-01: Spielerlistensortierung verletzt den Comparator-Vertrag

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Vergleichszyklus statisch bestätigt; TimSort-Ausnahme lokal mit einem Java-Logikmodell reproduziert.

## Auslöser und Folge

Lehensortierung ist aktiv und Sortierdaten sind geladen. Normale Spieler und Spectators besitzen unterschiedliche serverseitige `listOrder`-Werte. Die Sortierung erzeugt dann widersprüchliche Reihenfolgen. Bei geeigneter Eingangsfolge kann das Öffnen der Tabliste ungefangen mit `java.lang.IllegalArgumentException: Comparison method violates its general contract!` scheitern.

## Codebeleg

- [PlayerListSortingService.java:163](../../../src/client/java/de/ottoextra/playerlist/PlayerListSortingService.java#L163): Bei unterschiedlichem Spectatorstatus wird vollständig Vanilla verglichen; bei gleichem Status entscheidet zunächst die eigene Lehen-/Titelordnung.
- [PlayerListHudMixin.java:22](../../../src/client/java/de/ottoextra/mixin/PlayerListHudMixin.java#L22) bindet die Sortierung in die Tabliste ein.
- Lokale Minecraft-1.21.11-Quellen, `PlayerListHud.java:66`: Vanilla sortiert zuerst absteigend nach `listOrder`, erst danach nach Spectatorstatus.

## Konkreter Gegenbeweis

A = normal, Lehen A, Order 0; B = normal, Lehen B, Order 2; S = Spectator, Order 1. Daraus folgt `A < B`, `B < S`, aber `S < A`. Ein Logikmodell mit 96 eindeutigen Einträgen und Shuffle-Seed 49 reproduziert die Ausnahme; siehe [Validierung](../VALIDIERUNG.md). Kein Minecraft-Integrationstest.

## Fixansatz und Regressionstest

Eine einheitliche Hierarchie von Vergleichsschlüsseln verwenden, etwa Spectatorstatus als konsistentes erstes Kriterium. Keine paarabhängige Auswahl zweier Gesamtordnungen. Transitivität über gemischte Status-/Order-/Lehendaten sowie Sortieren der Gegenbeispielliste testen.


## Umsetzung am 06.09.2026

Die Sortierung verwendet eine durchgehende Schlüsselreihenfolge: Spectatorstatus, RP-Sortierschlüssel, Vanilla als letzter Gleichstandsvergleich. Ein Test prüft Antisymmetrie und Transitivität über 96 gemischte Einträge mit verschränkten Server-Orders und sortiert die mit Seed 49 gemischte Liste.

Regression: [PlayerListComparatorTest.java](../../../src/test/java/de/ottoextra/playerlist/PlayerListComparatorTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
