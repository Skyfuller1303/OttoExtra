# Inventar-Crash mit Mouse Wheelie im Yarn-Devclient

Stand: 8. September 2026. Der Crash um 17:11:13 beim Öffnen des Inventars ist
ein `StackOverflowError` in `RecipeBookWidget.getLeft()`. Das betroffene Profil
`run2` verwendete Minecraft 1.21.11, Java 21.0.10, Loader 0.19.3, OttoExtra
0.1.17-f und Mouse Wheelie 1.16.2+mc1.21.11.

## Ursache

Die lokale Mouse-Wheelie-JAR enthält in `MixinRecipeBookWidget` die privaten
Hilfsmethoden `getLeft()` und `getTop()`. Sie delegieren an die Minecraft-Methoden
`method_64367()` und `method_64366()`. In den verwendeten Yarn-Mappings heißen
diese Minecraft-Methoden ebenfalls `getLeft()` und `getTop()`.

Beim Einfügen des Mixins in den benannten Dev-Runtime kollidiert die Hilfsmethode
mit der Minecraft-Methode. Die Delegation wird dadurch zum Selbstaufruf. Der
Original-Crash sowie der isolierte Vorher-Test zeigen dieselbe Endlosschleife.
OttoExtra injiziert keinen Code in `RecipeBookWidget`.

## Lokale Korrektur

[`prepare-mousewheelie.py`](../../tools/dev-client/prepare-mousewheelie.py) erstellt
eine Kopie mit zwei eindeutigen privaten Methodennamen:

- `getLeft` → `mouseWheelie$devGetLeft`
- `getTop` → `mouseWheelie$devGetTop`

Die Umbenennung betrifft die entsprechenden UTF-8-Konstanten dieser einzelnen
Mixin-Klasse, einschließlich ihrer Aufrufstellen. Delegation, Minecraft-Symbole,
sonstige Klassen, Metadaten, Ressourcen und eingebettete Abhängigkeiten bleiben
unverändert. Das Skript akzeptiert ausschließlich die per SHA-256 geprüfte
Original-JAR, schreibt nie auf den Eingabepfad und überschreibt keine abweichende
Ausgabedatei. Die Archivprüfung bestätigt genau einen geänderten Eintrag.

| Datei | SHA-256 |
| --- | --- |
| Original Mouse Wheelie | `37a55be6874104a82ec2b06954ffc77e0ec5bbbd09a007dbcae688455e3a9214` |
| Lokale Dev-Kopie | `789692b3499270d64d268ad6ce704c96ccac298c34025ae093422aaa60f1e33e` |

Installiert im vorhandenen Dev-Profil:

- Aktive Datei: `run2/mods/mouse-wheelie-1.16.2+mc1.21.11-yarn-devfix.jar`
- Unveränderte Sicherung: `run2/disabled-mods/mouse-wheelie-1.16.2+mc1.21.11-original.jar`

Die Korrektur ist ausschließlich für das lokale benannte Entwicklungsprofil.
Die reguläre OttoExtra-JAR 0.1.17-g wird dadurch nicht verändert und enthält
Mouse Wheelie weiterhin nicht. Die korrigierte Drittanbieter-JAR wird nicht in
Git eingecheckt oder als Release-Artefakt verteilt.

Zum erneuten Erzeugen aus der Sicherung, im Projektverzeichnis:

```powershell
python tools/dev-client/prepare-mousewheelie.py `
  'run2/disabled-mods/mouse-wheelie-1.16.2+mc1.21.11-original.jar' `
  'build/inventory-crash/mouse-wheelie-1.16.2+mc1.21.11-yarn-devfix.jar'
```

Für ein Zurücksetzen bei geschlossenem Client die Dev-Kopie aus `run2/mods/`
entfernen und die gesicherte Original-JAR zurückkopieren. Im Modordner darf
jeweils nur eine der beiden Dateien liegen.

## Validierung

Der neue Clientfall `UI-INVENTORY` öffnet das normale Spielerinventar dreimal,
mit geöffnetem und geschlossenem Rezeptbuch, schaltet das Rezeptbuch insgesamt
sechsmal um und ändert die Fenstergröße. Die Kreativ-Einstellung des lokalen
Testspielers wird für die UI-Prüfung vorübergehend im Client deaktiviert und
anschließend wiederhergestellt. Inventarslots werden nicht angeklickt.

| Lauf | Ergebnis |
| --- | --- |
| Original-JAR, sonst identisches Testprofil | **FAIL**, `StackOverflowError` in `RecipeBookWidget.getLeft()` |
| Korrigierte JAR, gleicher Test | **PASS**, 34 Sekunden einschließlich Clientstart |
| Geladene Integration | `mousewheelie=true` im Testlog |
| Prüfung des ZIP-Inhalts | Nur `MixinRecipeBookWidget.class` geändert; CRC-Prüfung bestanden |
| Originaldatei | SHA-256 unverändert, außerhalb des aktiven Modordners gesichert |

Die automatischen Läufe verwenden eine lokale Paper-Welt und ein separates
Profil mit Mouse Wheelie sowie den benötigten Abhängigkeiten. Das vollständige
Modpaket bleibt im manuellen Profil `run2`. Nach dem bestandenen Vergleichstest
wurde dieses Profil mit OttoExtra 0.1.17-g, DevAuth und dauerhaftem Debug-Logging
erneut zu `ottonien.com` gestartet.

Nachweise: [Original-Crash](../../build/inventory-crash/original-crash.txt),
[Vorher-Ergebnis](../../build/inventory-crash/before-results.json),
[Vorher-Log](../../build/inventory-crash/before.log),
[Nachher-Ergebnis](../../build/inventory-crash/after-results.json),
[Nachher-Log](../../build/inventory-crash/after.log),
[Patchbericht](../../build/inventory-crash/patch-report.json),
[Inventaransicht](../../build/inventory-crash/0000_inventory-recipebook-open.png).
