# PACK-01: Fehlende Ressourcenpaket-Datei gilt dauerhaft als aktuell

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Metadaten- und Dateiprüfpfad statisch bestätigt.

## Auslöser und Folge

Nach einer Installation bleibt `resourcepack/state.json` erhalten, aber `resourcepacks/Ottonien.zip` fehlt, beispielsweise nach einem Profilumzug. Bei unverändertem Manifest lädt der Updater die Datei nicht erneut herunter.

## Codebeleg

- [ResourcePackUpdater.java:92](../../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java#L92): Ein übereinstimmender SHA, ersatzweise die gleiche Version im State, reicht für „aktuell“; die ZIP wird dabei nicht geprüft.
- [PackInstaller.java:63](../../../src/client/java/de/ottoextra/resourcepack/PackInstaller.java#L63): Die vorgemerkte Aktivierung wird wegen fehlender Datei verworfen.

## Reproduktion

In einem Testprofil das Pack installieren. Die ZIP außerhalb des Profilordners sichern und aus dem Ressourcenpaketordner entfernen, den State erhalten. Neustart bei gleichem Manifest: kein Download; weitere Neustarts beheben das ebenfalls nicht.

## Fixansatz und Regressionstest

Ein Pack nur bei vorhandener und plausibel gültiger Datei als aktuell behandeln. Den Fall „State aktuell, Datei fehlt“ mit Fake-Manifest und temporärem Ressourcenpaketordner testen: Es muss heruntergeladen und anschließend aktiviert werden.

## Umsetzung am 06.09.2026

Der Aktualitätsvergleich verlangt zusätzlich eine vorhandene, lesbare ZIP mit nichtleerem `pack.mcmeta`. Fehlende oder strukturell ungültige Dateien durchlaufen wieder den Downloadpfad. Derselbe Dateicheck gilt für den Direktmodus.

Regression: [PackRecoveryTest.java](../../../src/test/java/de/ottoextra/resourcepack/PackRecoveryTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
