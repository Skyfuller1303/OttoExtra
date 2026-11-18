# Integrierte Module (Resources und Tactical Factions)

Ab 0.18.0 sind die früheren Addons "OttoExtra Resources" (`ottoextra_resources`) und "OttoExtra Tactical Factions" (`ottoextra_tacticalfactions`) fester Bestandteil der OttoExtra-Jar. Es wird nur noch eine Datei installiert.

## Was wohin gezogen ist

- Quellcode: `src/client/java/de/ottoextra/resources/` und `src/client/java/de/ottoextra/tacticalfactions/` (unveränderte Pakete).
- Tests: `src/test/java/de/ottoextra/resources/` und `src/test/java/de/ottoextra/tacticalfactions/`.
- Assets: `src/main/resources/assets/ottoextra_resources/` und `assets/ottoextra_tacticalfactions/`. Die Namespaces sind unverändert, damit Identifier, Übersetzungsschlüssel und Texturpfade weiter funktionieren. Die Resource-Icons aus dem früheren `input/`-Ordner liegen jetzt fest unter `assets/ottoextra_resources/textures/gui/resources/`.
- Mixins: `ottoextra_resources.mixins.json` und `ottoextra_tacticalfactions.mixins.json` bleiben getrennte Konfigurationen und sind in `fabric.mod.json` neben `ottoextra.mixins.json` eingetragen.
- Registrierung: Die Addon-Klassen bleiben bestehen und sind als `ottoextra`-Entrypoints (plus je ein `client`-Entrypoint) in der `fabric.mod.json` von OttoExtra eingetragen. Sie laufen über dieselbe Addon-API wie externe Addons.
- Tactical Factions liest seine Build-Variante aus `ottoextra_tacticalfactions.variant.properties`. In der integrierten Fassung ist das fest die Variante mit Friendly-Fire-Schutz (`friendlyFireProtection=true`).

## Konfiguration

Die Konfigurationsdateien haben sich nicht geändert, vorhandene Nutzerdaten werden weiterverwendet: `config/ottoextra-resources.json`, `config/ottoextra-resources/`, `config/ottoextra-tacticalfactions.json`.

## Alte Jars: `breaks`-Regel

Die `fabric.mod.json` enthält:

```json
"breaks": { "ottoextra_resources": "*", "ottoextra_tacticalfactions": "*" }
```

Liegt neben OttoExtra 0.18.0 noch eine alte Addon-Jar im `mods`-Ordner, verweigert Fabric den Start mit einer Fehlermeldung. Die alte Jar muss dann entfernt werden. Dadurch werden die Module nie doppelt registriert. Bewusst kein `provides`, weil es mit `breaks` kollidieren würde.

## Entwicklung

Im Dev-Run (`run/mods/`) dürfen `OttoExtra-Resources-dev.jar` und `OttoExtra-TacticalFactions-dev.jar` nicht mehr liegen. Die Ordner `OttoExtra-Resources` und `OttoExtra-TacticalFactions` bleiben nur noch als Archiv bestehen.

Zusätzliche Compile-Abhängigkeiten (nur `modCompileOnly`): Xaero World Map (`libs/xaeroworldmap-fabric-1.21.11-1.41.0.jar`, wie die anderen Xaero-Jars nicht in Git) sowie Litematica und MaLiLib über Modrinth-Maven.
