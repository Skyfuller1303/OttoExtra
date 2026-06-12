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
