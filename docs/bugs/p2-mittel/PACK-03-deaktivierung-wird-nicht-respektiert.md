# PACK-03: Benutzerdeaktivierung des Ressourcenpakets wird nicht gespeichert

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: State-Schreibstellen und Aktivierungsprüfung statisch gegengeprüft.

## Auslöser und Folge

Mit `autoEnable=true` und `respectUserDisable=true` deaktiviert der Benutzer das Pack im normalen Minecraft-Menü. Beim nächsten erfolgreichen Manifestcheck nach Neustart aktiviert OttoExtra es erneut.

## Codebeleg

- [ResourcePackUpdater.java:167](../../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java#L167): Die Schutzprüfung verwendet ausschließlich `state.enabled`.
- [PackState.java:28](../../../src/client/java/de/ottoextra/resourcepack/PackState.java#L28): Das Feld startet mit `true`.
- [ResourcePackUpdater.java:145](../../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java#L145): Bei Installation wird nur der bisherige State übernommen.
- [SettingsRegistry.java:241](../../../src/client/java/de/ottoextra/config/settings/SettingsRegistry.java#L241): Die Option wird Benutzern angeboten.

Es fehlt eine Schreibstelle, welche eine normale Deaktivierung aus Minecrafts tatsächlicher Packauswahl in diesen State übernimmt.

## Fixansatz und Regressionstest

Benutzerdeaktivierung erkennen und persistieren oder die gespeicherte Packauswahl unter Berücksichtigung einer Erstinstallation auswerten. Test: Pack aktivieren, im Vanilla-Menü deaktivieren, neu starten; mit aktivierter Respekt-Option muss es deaktiviert bleiben.


## Umsetzung am 06.09.2026

Die in Minecraft gespeicherte Packauswahl wird vor einer automatischen Aktivierung ausgewertet. Bei vorheriger Installation bleibt eine deaktivierte Auswahl mit `respectUserDisable=true` aus. Erstinstallation und ausdrücklich ausgeschaltete Respekt-Option erlauben weiterhin Auto-Enable.

Regression: [PackRecoveryTest.java](../../../src/test/java/de/ottoextra/resourcepack/PackRecoveryTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
