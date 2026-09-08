# PACK-02: Ersetztes aktives Ressourcenpaket wird nicht neu geladen

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Codepfad bestätigt; abhängig von erfolgreichem ZIP-Austausch auf dem jeweiligen Dateisystem.

## Auslöser und Folge

Minecraft hat das alte Pack bereits geladen. Danach ersetzt der Updater die ZIP durch eine neue Version. Die Pack-ID ist weiterhin `file/Ottonien.zip`; deshalb wird kein Ressourcenreload ausgelöst. Bis zu manuellem Reload oder Neustart bleiben alte Texturen und Modelle sichtbar.

## Codebeleg

- [ResourcePackUpdater.java:131](../../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java#L131): Installiert die neue Datei, aktualisiert den State und fordert Aktivierung an.
- [PackInstaller.java:73](../../../src/client/java/de/ottoextra/resourcepack/PackInstaller.java#L73): Bereits im Manager und in den Optionen aktivierte Pack-ID führt zum vorzeitigen Return vor `client.reloadResources()`.

## Reproduktion

Mit aktivem alten Pack starten; den neuen Download erst nach dem initialen Ressourcenladen abschließen lassen. Nach erfolgreichem ZIP-Austausch wird die neue Version im State geführt, ihre Inhalte erscheinen aber erst nach Reload.

## Fixansatz und Regressionstest

Aktivierung und erforderlichen Reload getrennt vormerken. Nach Austausch eines aktiven Packs muss ebenfalls ein Reload erfolgen. Unter Windows kann bereits der ZIP-Austausch an einer Dateisperre scheitern; dieser konkrete Befund setzt erfolgreichen Austausch voraus.


## Umsetzung am 06.09.2026

Ein erfolgreicher Dateiaustausch merkt einen Reload unabhängig von der Aktivierung vor. Bereits aktive Packs werden deshalb auch bei unveränderter Pack-ID neu geladen; dies gilt ebenfalls bei ausgeschaltetem Auto-Enable. Laufende Ressourcenreloads werden abgewartet.

Regression: [PackRecoveryTest.java](../../../src/test/java/de/ottoextra/resourcepack/PackRecoveryTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
