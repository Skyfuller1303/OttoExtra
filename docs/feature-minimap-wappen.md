# Feature: Lehen-Wappen an der Minimap

## Spieler-Fantasy

Ein Blick auf die Minimap genügt, um zu wissen, in wessen Lehen man gerade
steht — das Wappen des aktuellen Lehens hängt unten rechts an der Minimap,
wie ein Hoheitszeichen am Grenzpfahl.

## Verhalten

- Zeigt das Wappen des Lehens, in dem der Spieler aktuell steht
  (Polygon-Lookup über `MapOverlayRenderer.insidePolygonKey`).
- Wappen-Quelle identisch zur Worldmap: Region-Banner (`banner_path` der
  Region, z. B. Mährstein-Verband) vor Fraktions-Banner; Cache über
  `BannerTextureService`.
- Kein Lehen / kein Wappen → nichts wird gezeichnet (kein Platzhalter).
- Position: unten rechts an der Minimap-Box angedockt, folgt der
  Minimap-Platzierung (Xaero `ModuleSession.getEffectiveX/Y`) inkl.
  fromRight/fromBottom-Verankerungen.
- Sichtbarkeit: gleiche Gates wie die Minimap-Grenzen (Overlay-Hotkey,
  "Nur auf Ottonien", Modul aktiv); zusätzlich eigener Config-Toggle.

## Mehrzeilige Textfelder und Größenänderung (Alpha G)

Unter **Einstellungen → Karte → Minimap → Wappen-Position (Drag&Drop)…** lassen
sich Name, Stand/Hierarchie und Gefolge als Textfelder bearbeiten:

- Im Feld ziehen verschiebt den gesamten Block.
- Den Griff unten rechts ziehen ändert **Breite und Höhe**. Vertikales Ziehen
  erhöht die verfügbare Zeilenzahl; es verändert die Schriftgröße nicht.
- Das Mausrad über einem Textfeld ändert die **Schriftgröße** separat.
- Loslassen speichert Position beziehungsweise Größe; das Mausrad speichert
  die Skalierung direkt. Beim Vergrößern bleibt die linke obere Ecke stehen,
  auch bei rechts oder unten verankerten Feldern.
- Das Wappen bleibt quadratisch und verwendet weiterhin seine eigene Größe.

Längere Texte brechen an Wortgrenzen um, sobald genug Höhe für weitere Zeilen
vorhanden ist. Beispiel bei Schriftgröße 1, Breite 91 und Höhe 20 GUI-Pixeln:

```text
Gefolge in
Gefolge XYZ
```

Die Zeilenhöhe beträgt 10 × Schriftgröße; nur vollständig verfügbare Zeilen
werden verwendet. Reicht das Textfeld insgesamt nicht aus, bleibt überschüssiger
Text ausgeblendet. Die Darstellung verwendet dieselben Grenzen im Editor und
im laufenden HUD. Automatisch angedockte Folgeelemente berücksichtigen die
vergrößerte Höhe.

Unter **Karte → Erweitert → HUD-Feinjustierung** sind die drei Höhen auch numerisch
einstellbar. Gespeichert werden `map.nameHudHeight`, `map.stateHudHeight` und
`map.factionHudHeight`. **0** erhält die bisherige einzeilige Höhe passend zur
Schriftgröße. Vorhandene Konfigurationen bleiben dadurch einzeilig. Der obere
Grenzwert beträgt 160 GUI-Pixel. „Zurücksetzen“ im Editor setzt weiterhin nur
die Positionen auf automatische Andockung zurück.

`UI-MINIMAP-HEIGHT` prüft im echten Minecraft-Client den Eckgriff, zweizeiligen
Text ohne Wortverlust, unabhängige Schriftgröße, Verschieben, erneutes Öffnen
und eine kleinere Fenstergröße. Die Region ist eine isolierte lokale Testvorgabe;
es werden keine produktiven Serverdaten geändert. Unit-Tests prüfen die
Zeilengrenzen, Skalierung und Konfigurationsspeicherung.

[Release-Validierung](releases/0.1.17-g.md).

## Architektur

| Baustein | Aufgabe |
|---|---|
| `OttoExtraConfig.Map.minimapBanner` | Toggle (Default an), Größe `minimapBannerSize` (Default 20 px) |
| `MapOverlayRenderer.bannerForKey(String)` | öffentlicher Banner-Lookup (Region → Fraktion), von Worldmap-Labels mitgenutzt |
| `map/xaero/MinimapBannerOverlay` | Xaero-gebundene Positionslogik + Draw; Klasse wird nur bei installierter `xaerominimap` geladen |
| `MapModule` | registriert `HudRenderCallback`, prüft Gates, ruft Overlay |

### Xaero-Anbindung (kompiliert gegen Minimap wie `XaeroMinimapBorders`)

```java
MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
double scale = HudMod.INSTANCE.getSettings().getMinimapScale();
int x = session.getEffectiveX(scaledWidth, scale);
int y = session.getEffectiveY(scaledHeight, scale);
int half = session.getProcessor().getMinimapSize() / 2 + 18; // Frame-Halbbreite
```

Wappen-Anker: `(x + 2*half - size, y + 2*half + 2)` — rechtsbündig direkt
unter der Minimap-Box. Fehler (Session null, Reflection, Versionsbruch)
deaktivieren das Feature einmalig mit Warn-Log, nie Crash.

## Evals

- [ ] Wappen erscheint beim Betreten eines Lehens, verschwindet außerhalb.
- [ ] Folgt der Minimap bei allen vier Ecken-Platzierungen.
- [ ] Kein Render ohne xaerominimap / auf Fremdservern (bei onlyOnOttonien).
- [ ] Kein Log-Spam bei fehlendem Banner (Fehler-Cache des BannerService).
