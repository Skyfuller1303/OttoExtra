# XaeroPlus: persönliche Zeichnungen auf der Minimap

Stand: 6. September 2026. Der Nutzer bestätigt die Funktion des vorigen
Weltkarten-Fixes und beauftragt die Erweiterung auf die Minimap.

## Ursache und Änderung

Der vorhandene `XaeroPlusDrawManagerMixin` injizierte ausschließlich in
`drawWorldMapFeatures`. XaeroPlus beendet auch `drawMinimapFeatures` bei aktivem
FairPlay vor dem Rendern der Features. Der bisherige Adapter erzeugte zudem immer
einen Weltkarten-Kontext mit Tiefenversatz 1.

Der optionale Mixin enthält jetzt zwei Minimap-Hooks mit getrennten Signaturen:

| XaeroPlus | Minimap-Argumente | Kontext |
| --- | --- | --- |
| 2.34.5 | sechs Positionsanteile, Zoom, MatrixStack, BufferProvider | Zoom aus dem Aufruf, `worldMap=false`, Tiefenversatz 0 |
| 2.31.5 | sechs Positionsanteile, MatrixStack, BufferProvider | Skalierung 1 wie im Original, `worldMap=false`, Tiefenversatz 0 |

Der Kameraursprung wird pro Achse wie in XaeroPlus aus
`chunk * 64 + tile * 16 + inside` rekonstruiert. Matrix, Kameraursprung und Zoom
stammen aus dem jeweiligen Aufruf; Rotation und Clipping bleiben in Xaeros
Minimap-Pipeline. Die aktuelle XaeroPlus-Version ruft diesen DrawManager sowohl
für den Oberflächen- als auch den Höhlenpfad auf.

Der gemeinsame Adapter unterscheidet jetzt Weltkarte und Minimap. Exakte
Feature-Allowlist: `Drawing-lines-saved`, `Drawing-lines-in-progress`,
`Drawing-text`. Ohne FairPlay zeichnet weiterhin allein XaeroPlus. Das
FairPlay-Flag und andere Features werden nicht verändert. Der Matrixstack wird
auch bei Fehlern wiederhergestellt.

## Nachweise

- Tatsächliche JARs geprüft: XaeroPlus 2.34.5 aus dem Modrinth-Profil
  `Ottonien 1.0.0`, XaeroPlus 2.31.5 aus `run/mods`.
- `javap`-Abgleich der remappten OttoExtra-JAR: für jede Version exakt ein
  passender Minimap-Selektor; `MatrixStack` korrekt nach
  `net/minecraft/class_4587` remappt. Beide optionalen Hooks verwenden `require=0`
  und `@Coerce Object` für den BufferProvider.
- 16 Zeichnungsfälle: beide Kartenpfade, FairPlay/Normalbetrieb, exakte Allowlist,
  beide Kontextversionen, Minimap-Rotation/Zoom/negative Weltkoordinaten,
  Stack-Restore bei Fehlern, fehlende optionale APIs.
- `.\gradlew.bat test build -x dist --console=plain`: **BUILD SUCCESSFUL**,
  21 Sekunden. Gesamtlauf: **414 Tests, 93 Suites, keine Fehler, keine
  übersprungenen Tests**. `dist` wurde zum Erhalt vorhandener Artefakte ausgelassen.
- Arbeitskopie auf Diff-Whitespacefehler geprüft. Die zahlreichen vorhandenen
  Änderungen anderer Funktionen wurden beibehalten.

Keine vollständige Mixin-Transformation oder visuelle Ingame-Abnahme durch den
Unit-Testlauf. Nach Neustart prüfen: gespeicherte Linie und Text auf der Minimap,
Zoom und Rotation, gemalte Karte an/aus, anschließend Weltkarte als Regression.
Der Fix ergänzt den FairPlay-Zeichenpfad; die bestehende Reihenfolge der
gemalten Minimap-Flächen wird nicht verändert.

## Build und Installation

- Build: `build/reports/xaeroplus-minimap/ottoextra-1.16.3-k-minimap-fix.jar`.
- Neue SHA-256: `f19fb9a35a5f519df03edc17d44def85e636b9727cc22a19d952144255ddeb42`.
- Ziel: `C:\Users\Mattis\AppData\Roaming\ModrinthApp\profiles\Ottonien 1.0.0\mods\ottoextra-1.16.3-k.jar`.
- Gesichertes Original: `C:\Users\Mattis\AppData\Roaming\ModrinthApp\profiles\Ottonien 1.0.0\ottoextra-backups\jar-20260906-152648-minimap-drawings\ottoextra-1.16.3-k.jar`.
- Original und Backup identisch:
  `32180e48bf2462c243c6afdfc6e1fb648f7c812b9a1fdd572573d2e309c4a921`.
- Stand 15:27 Uhr: Austausch durch laufendes Minecraft gesperrt. Original-JAR
  unverändert; geprüfter Build liegt außerhalb des Mods-Ordners als
  `ottoextra-1.16.3-k-minimap.pending` neben dem Backup bereit. Installation folgt
  nach normalem Beenden des Spiels.
- Maschinenlesbarer Installationsstand:
  `build/reports/xaeroplus-minimap/installation.json`.

Versionsname bleibt wie beim vorigen lokalen Testbuild `1.16.3-k`. Der Hash
unterscheidet die Builds.
