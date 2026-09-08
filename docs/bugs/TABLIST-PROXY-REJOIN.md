# Fehlende Tablisten-Wappen nach Proxy-Rejoin

Nutzermeldung vom 6. September 2026, zusätzlich zum ursprünglichen Bug-Audit.

Status: **Nachgewiesener Codefehler korrigiert, Regressionstests erfolgreich; realer Proxy-Rejoin noch im Spiel zu prüfen.**

## Ursache und Korrektur

Minecraft 1.21.11 verwendet in `PlayerListHud.getPlayerName()` den separaten Anzeigenamen eines Spielers, sofern dieser vorhanden ist. Bei `null` dekoriert Vanilla stattdessen den Kontonamen mit Präfix, Suffix und Farbe des aktuellen Scoreboard-Teams. Ein Wappen kann als `ObjectTextContent` in diesem Präfix liegen.

OttoExtras `PlayerListEntryMixin` formatierte bereits `PlayerListEntry.getDisplayName()`. Ohne separaten Anzeigenamen nahm `TablistNameFormatter` nur den nackten Kontonamen als Basis. Der zurückgegebene RP-Anzeigename war anschließend nicht mehr `null`: Vanilla übersprang den Team-Fallback und damit dessen Wappen. Nachträgliche Team-Updates änderten daran nichts.

Der Mixin übergibt jetzt bei fehlendem Anzeigenamen und aktivem RP-Modul das aktuelle Team an den Formatter. Dieser verwendet denselben `Team.decorateName()`-Fallback wie Vanilla, bevor er Namen und Titel anpasst. Ein vorhandener Server-Anzeigename behält Vorrang; Wappen werden nicht doppelt angefügt. Es gibt keinen neuen Wappen-Cache: Teamwechsel, entfernte Zuordnungen und verspätete Präfix-Updates werden bei jedem Aufruf neu berücksichtigt. Ohne Namensanpassung bleibt der Rückgabewert `null` und Vanilla übernimmt weiterhin die Darstellung.

Bei übersetzten Titelargumenten wurde außerdem der Text vor der Abstandsermittlung getrimmt. Die Abstandsermittlung verwendet dort jetzt den Originaltext des objektfreien Arguments, damit Wappen und nachfolgender Titel/Name getrennt bleiben.

Geänderte Produktionsdateien:

- [PlayerListEntryMixin.java](../../src/client/java/de/ottoextra/mixin/PlayerListEntryMixin.java)
- [TablistNameFormatter.java](../../src/client/java/de/ottoextra/rpnames/tablist/TablistNameFormatter.java)

## Testnachweis

Sechs neue Tests in [TablistNameFormatterTest.java](../../src/test/java/de/ottoextra/rpnames/tablist/TablistNameFormatterTest.java) prüfen:

1. Fehlender Anzeigename: echtes Atlas-Wappen, Team-Suffix, Teamfarbe, persönliche Namensfarbe und Komponenten-Styles bleiben erhalten; der Servertext wird nicht verändert.
2. Fehlendes/später eintreffendes Team, später eintreffendes Wappen, Präfixwechsel, neu angelegtes Team und entfernte Zuordnung verwenden jeweils den aktuellen Stand.
3. Ein expliziter Anzeigename hat Vorrang und verdoppelt keine Wappen.
4. Lokaler Titel und ausgeblendete Titel erhalten strukturierte Wappen in Übersetzungsargumenten sowie den Abstand zum Namen.
5. Unbekannte Spieler behalten Wappen bei sichtbaren und versteckten Titeln.
6. Deaktivierte Anpassungen überlassen Vanilla den Fallback.

Vor der Team-Korrektur schlugen vier der sechs Tests wegen fehlender Objektkomponenten fehl. Nach der Team-Korrektur machte die Formatierungsprüfung zusätzlich den fehlenden Abstand in Übersetzungsargumenten sichtbar; beide Fehler sind korrigiert.

- Minecraft `1.21.11`, Yarn `1.21.11+build.6`, Java 21, Loom `1.17.8`. Die verwendeten Methoden und der Vanilla-Fallback wurden in den lokal generierten Minecraft-Quellen geprüft.
- Fokussierter Lauf: `.\gradlew.bat test --tests de.ottoextra.rpnames.tablist.TablistNameFormatterTest --console=plain` erfolgreich.
- Vollständiger Lauf: `.\gradlew.bat test build -x dist --console=plain` → **BUILD SUCCESSFUL**, 20 Sekunden, **428 Tests in 95 Suites, 0 Fehler, 0 übersprungen**.
- Produktionsdiff gegen den vor Beginn gesicherten Worktree geprüft; vorhandene Änderungen beibehalten. Keine Änderungen an Versionsnummer, Abhängigkeiten oder Mixin-Injektionspunkt.
- [Remappte JAR](../../build/tablist-proxy-fix/ottoextra-1.16.3-k.jar): `1.16.3-k`, `environment=client`, 30.815.233 Bytes.
- SHA-256: `eee133bbd76f72729cee0ba0f5152912fb90d62e3c619a7cc578885606aa7d83`.

Die JAR enthält den aktuellen Worktree einschließlich der vorherigen P1-/P2-/P3-Fixes. Der vorhandene `dist`-Finalizer wurde wegen seines Löschschritts ausgeschlossen; das Prüfartefakt liegt als separate Kopie vor. Keine Installation in ein Minecraft-Profil.

## Ingame-Abnahme

Auf dem Server einen bekannten und einen unbekannten Spieler über den Proxy aus- und wieder einloggen lassen. Wappen vor und nach dem Rejoin vergleichen, Titelanzeige an-/ausschalten und einen eigenen Serverwechsel prüfen. Auch nach verzögerten Team-Updates muss das aktuelle Wappen ohne erneuten eigenen Login erscheinen.

Die Tests verwenden echte Minecraft-Text- und Teamobjekte, führen aber weder den Mixin in einem gestarteten Client noch Proxy-Pakete aus. Es wurde kein Minecraft gestartet und kein Server kontaktiert. Der beschriebene Fehlerpfad ist belegt; ob zusätzlich bereits vom Server unvollständige Wappendaten eintreffen, lässt sich ohne den realen Ablauf nicht entscheiden.
