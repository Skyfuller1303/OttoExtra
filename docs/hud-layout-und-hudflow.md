# HUD-Layout und HudFlow

Stand: 18.11.2026 (OttoExtra 0.17.1, Tactical Factions 0.1.0). Noch nicht committet.

Diese Grundlage sorgt dafür, dass sich HUD-Panels (Aktivitätsanzeige, Trupp-Anzeige, künftige Addon-Anzeigen) nicht überlagern und dass alle HUD-Elemente in einem gemeinsamen Editor angeordnet werden können. Sie besteht aus zwei Teilen:

1. **HudFlow** – Laufzeit-Logik: Panels weichen einander und festen Hindernissen (Xaero-Minimap, Lehen-Overlay) aus, jeden Frame.
2. **HUD-Layout-Editor** („HUD anordnen“) – Bildschirm, in dem man Elemente verschiebt, skaliert und einrasten lässt. Speichert die *gewünschte* Position jedes Elements.

Die gewünschte Position (Anker + Abstand + Skalierung aus der jeweiligen Config) ist immer die Wahrheit. HudFlow berechnet daraus nur, wo im aktuellen Frame tatsächlich gezeichnet wird; es speichert nie etwas in die Config.

## Dateien

Alle Pfade relativ zu `OttoExtra/src/client/java/de/ottoextra/`.

| Datei | Aufgabe |
|---|---|
| `ui/hudlayout/HudFlow.java` | Öffentliche Laufzeit-API (`place`, `resolved`, `pin`, `isEnabled`). Minecraft-/Xaero-Anbindung, feste Hindernisse, Reset bei Auflösungswechsel. |
| `ui/hudlayout/HudFlowState.java` | Reine Logik ohne Minecraft: Spalten-Layout (`layoutColumn`: Reihenfolge, Packen, Überlauf), Ablauf alter Meldungen. Voll unit-getestet. |
| `ui/hudlayout/HudLayoutElement.java` | Schnittstelle für ein Element im Editor (Bounds, verschieben, skalieren, speichern, Vorschau). |
| `ui/hudlayout/HudLayoutRegistry.java` | Sammelt die Elemente einer Editor-Sitzung: Xaero-Minimap, Lehen-Overlay-Teile, Aktivitätsanzeige, Addon-Elemente. |
| `ui/hudlayout/HudLayoutEditorScreen.java` | Der Editor: Auswahl, Ziehen, Mausrad-Skalierung, Pfeiltasten, Einrasten, Zurücksetzen/Fertig. |
| `ui/hudlayout/HudSnap.java` | Reine Einrast-Mathematik (Bildschirmränder, Mitte, Kanten anderer Boxen) und `Rect`. |
| `ui/hudlayout/HudLayoutSettings.java` | Einstellungseintrag (Knopf + Schalter „HUD Flow“) und Befehl `/ottoextra hud`. |
| `ui/hudlayout/ActivityHudElement.java`, `ActivityHudAnchors.java` | Editor-Element der Aktivitätsanzeige; Umrechnung Position ↔ die sechs Anker (`TOP_LEFT` … `MIDDLE_LEFT` …) plus Offset. |
| `ui/hudlayout/LehenOverlayElement.java` | Editor-Element je Teil des Lehen-Overlays (Wappen, Name, Status, Fraktion); speichert über `MinimapBannerOverlay.savePosition`. Nur verschiebbar. |
| `map/xaero/XaeroMinimapLayoutElement.java` | Editor-Element der Xaero-Minimap. Liest/schreibt Xaeros `ModuleTransform` und speichert mit Xaeros eigenem Settings-Writer. Nur verschiebbar. |
| `map/xaero/XaeroHudLayoutEditorScreen.java` | Editor-Variante als Xaero-`IScreenBase`, damit Xaero die echte Minimap hinter dem Editor weiterzeichnet. |
| `regions/RegionActivityHud.java` | Ruft `HudFlow.place("ottoextra:activity", …, PRIORITY_ACTIVITY_HUD)` vor dem Zeichnen; `lastPanelSize()` liefert die zuletzt gezeichnete Größe für den Editor. |
| `addon/OttoExtraAddon.java`, `addon/OttoExtraAddons.java` | Addon-Haken `hudLayoutElements()` (Default: leere Liste), Sammlung mit try/catch je Addon. |
| `config/OttoExtraConfig.java` | `hudFlowEnabled` (Default `true`); Aktivitätsanzeige: `regions.activityHudPosition`, `activityHudOffsetX/Y`, `activityHudScale`. |

Tests (`OttoExtra/src/test/java/de/ottoextra/`): `ui/hudlayout/HudFlowStateTest.java`, `ui/hudlayout/HudSnapTest.java`, `addon/OttoExtraAddonHooksTest.java`.

Tactical Factions (`OttoExtra-TacticalFactions/src/main/java/de/ottoextra/tacticalfactions/`): `SquadActivityHud.java` (ruft HudFlow), `SquadHudLayoutElement.java` (Editor-Element), `TacticalFactionsAddon.hudLayoutElements()` (Anmeldung), `HudPlacement.java` + `TacticalConfig` (Anker/Offset/Skalierung der Trupp-Anzeige). Test: `HudPlacementTest`.

## HudFlow: Ablauf pro Frame

```
HUD rechnet gewünschtes Rechteck aus (Anker, Offset, Skalierung, aktuelle Größe)
        │
        ▼
int[] pos = HudFlow.place(id, x, y, w, h, pushUp, priority)
        │
        ├─ pos == null  → an der gewünschten Stelle zeichnen
        └─ sonst        → an pos[0], pos[1] zeichnen
```

`place` ist so gebaut, dass es nie wirft und immer sofort antwortet:

- **Koordinaten:** skalierte GUI-Pixel, die eigene Skalierung des HUDs ist schon eingerechnet (`w`, `h` = sichtbare Größe).
- **`pushUp`:** `true` für unten verankerte HUDs (in der Spalte nach ihrer Unterkante einsortiert), sonst `false` (nach ihrer Oberkante). Seitlich wird nie verschoben.
- **`priority`:** `HudFlow.PRIORITY_ACTIVITY_HUD = 100`, `HudFlow.PRIORITY_ADDON = 200`. Nur noch Gleichstands-Entscheider (siehe Rangfolge).
- **Rückgabe `null`:** HudFlow ist ausgeschaltet (`hudFlowEnabled = false`), die Größe ist 0/unbekannt, oder ein Fehler trat auf. Das HUD zeichnet dann an seiner gewünschten Position.

### Kein Render-Reihenfolge-Problem

`HudFlowState` speichert pro HUD das zuletzt gemeldete *Wunsch*-Rechteck (`entries`). Bei jedem `place` wird die ganze Spalte des meldenden HUDs aus diesen Wunsch-Rechtecken neu ausgelegt: die neue Größe des meldenden HUDs zählt sofort, die der anderen mit ihrer letzten Meldung. Die Auslegung hängt nur von der Menge der Wunsch-Rechtecke ab, nicht von der Reihenfolge der Meldungen; alle HUDs einer Spalte bekommen dasselbe Ergebnis, und nichts pendelt von Frame zu Frame.

### Rangfolge: Reihenfolge in der Spalte

Eine **Spalte** sind alle aktiven HUDs, deren x-Bereiche sich (auch über Dritte) überschneiden. Panels links und rechts stören sich nie.

1. **Feste Blöcke** behalten ihr Rechteck: gepinnte HUDs (gerade im Editor gezogen, `HudFlow.pin(id)`, und alle HUDs bei ausgeschaltetem HudFlow) und die festen Hindernisse (unten).
2. **Bewegliche HUDs** werden nach Wunschposition sortiert:
   - oben oder mittig verankert (`pushUp = false`): nach der Oberkante,
   - unten verankert (`pushUp = true`): nach der Unterkante.
3. Gleichstand: niedrigere `priority` zuerst, dann alphabetisch nach `id`.

Dadurch gilt: wächst das obere Panel, rutscht das untere weg, egal ob Aktivitäts- oder Trupp-Anzeige.

### Ausweichen

`HudFlowState.layoutColumn(...)` packt die sortierten HUDs eindimensional, Abstand `HudFlowState.GAP = 2` GUI-Pixel:

- **Von oben nach unten:** Jedes HUD steht an seiner Wunsch-y, aber unter allen vorher platzierten HUDs, mit denen es sich in x überschneidet, und springt über feste Blöcke, die es berührt.
- **Von unten nach oben (Überlauf):** Ragt ein HUD über den unteren Bildschirmrand oder über das nächste HUD darunter, wird es hochgezogen (`y = min(y, next.y - GAP - h)`, `y <= screenH - h`); dabei springt es über feste Blöcke nach oben. Nach unten geschoben wird in diesem Schritt nie.
- **Kein Platz:** Nur wenn die Spalte wirklich höher als der Bildschirm ist, wird oben bei `y = 0` geklemmt und es überlappt; das zuerst sortierte HUD bleibt dann vollständig sichtbar.
- Seitlich wird nie verschoben.

Beispiel (Dev-Konfiguration): Aktivitäts-HUD `MIDDLE_RIGHT` und Trupp-HUD `BOTTOM_RIGHT` überlappen sich in x. Das Aktivitäts-HUD (Oberkante weiter oben) steht zuerst, das Trupp-HUD kommt darunter; passt es dort nicht mehr auf den Bildschirm, bleibt es am unteren Rand und das Aktivitäts-HUD rückt nach oben.

### Feste Hindernisse

`HudFlow.obstacles(now)` sammelt alle 200 ms (`OBSTACLE_REFRESH_MS`) neu:

- Xaero-Minimap: Bounds aus `XaeroMinimapLayoutElement.bounds(...)`.
- Lehen-Overlay: `MinimapBannerOverlay.computeElements(...)` (nur wenn die Minimap-Session aktiv ist).

Nur wenn `xaerominimap` geladen ist; Xaero-Klassen stecken in der inneren Klasse `XaeroObstacles`, damit ohne Xaero nichts geladen wird. Ändert sich Xaeros API, gibt es einfach keine festen Hindernisse.

### Stabilität

- **Ablauf:** Meldet ein HUD sich `HudFlowState.EXPIRY_MS = 250` ms nicht (ausgeblendet, F1, leer), wird es entfernt; die anderen springen an ihre gewünschte Position zurück.
- **Größe 0 / unbekannt** (z. B. kurz während eines Daten-Refreshs): `place` meldet nichts und gibt `null` zurück; die letzte gültige Meldung bleibt für die anderen bestehen.
- **Auflösungs- oder GUI-Skalierungswechsel:** alle gespeicherten Rechtecke werden verworfen (`STATE.clear()`), weil sie im alten Pixelraum liegen.
- **HudFlow aus:** HUDs melden ihre Position trotzdem (als gepinnt), damit der Editor sie kennt, werden aber nie verschoben.

## HUD-Layout-Editor

Öffnen: `/ottoextra hud`, OttoExtra-Einstellungen (HUD-Bereich), oder in Tactical Factions „HUD anordnen“ im Trupp-Reiter. Mit Xaero wird `XaeroHudLayoutEditorScreen` verwendet, damit die echte Minimap sichtbar bleibt; der Editor dunkelt den Hintergrund nicht ab (`renderBackground` leer), das echte HUD ist also dahinter zu sehen.

Bedienung:

- Klick wählt ein Element, Ziehen verschiebt es. Das gezogene Element wird über `HudFlow.pin(flowId)` gepinnt und folgt exakt der Maus; die anderen reagieren live.
- Mausrad über einem skalierbaren Element: Skalierung 50–200 %.
- Pfeiltasten: 1 px, mit Shift 10 px.
- Einrasten (`HudSnap`): an Bildschirmränder, Bildschirmmitte und Kanten anderer Elemente innerhalb von ca. 4 px, mit Hilfslinie. Alt gedrückt = kein Einrasten.
- „Zurücksetzen“ setzt nur das ausgewählte Element zurück; „Fertig“/Esc schließt.
- Wird ein Element von HudFlow verschoben, zeigt der Editor zusätzlich, wo es gerade tatsächlich gezeichnet wird.

| Element | Verschieben | Skalieren | Gespeichert in | Zurücksetzen auf |
|---|---|---|---|---|
| Aktivitätsanzeige | ja | ja | `OttoExtraConfig.regions.activityHud*` | `MIDDLE_LEFT`, 0/0, 100 % |
| Lehen-Overlay (4 Teile) | ja | nein | `MinimapBannerOverlay.savePosition` | Overlay-Default |
| Xaero-Minimap | ja | nein | Xaeros eigene Settings (`ModuleTransform` + `saveSettings()`) | Position beim Öffnen des Editors |
| Trupp-Anzeige (TF) | ja | ja | `TacticalConfig` (Ecke, Offset, Skalierung) | oben rechts, 0, 100 % |

Positionsspeicherung: Elemente mit Ankern (Aktivitäts- und Trupp-Anzeige) bestimmen beim Ablegen die nächstgelegene Ecke bzw. den nächsten Anker und speichern den Abstand dazu. So bleibt die Position bei anderer Auflösung oder GUI-Skalierung sinnvoll.

Xaero-Minimap: Verschoben wird über dieselben Xaero-Objekte, die Xaeros eigener Edit-Modus nutzt (`HudModule.getConfirmedTransform()` / `setTransform(...)`, `HudMod.INSTANCE.getSettings().saveSettings()`). Keine Mixins. Größe der Minimap ist eine separate Xaero-Option und wird hier nicht verändert.

## Neue Anzeige anbinden

### Laufzeit (Ausweichen)

Im Render-Code des HUDs, vor dem Zeichnen:

```java
int w = Math.round(panelWidth * scale);
int h = Math.round(panelHeight * scale);
int drawX = wantedX, drawY = wantedY;
try {
    int[] flowed = de.ottoextra.ui.hudlayout.HudFlow.place(
            "meinaddon:mein_hud", wantedX, wantedY, w, h,
            /* pushUp = unten verankert */ anchoredBottom,
            de.ottoextra.ui.hudlayout.HudFlow.PRIORITY_ADDON);
    if (flowed != null) {
        drawX = flowed[0];
        drawY = flowed[1];
    }
} catch (Throwable ignored) {
    // Ältere OttoExtra-Version ohne HudFlow: an der gewünschten Stelle zeichnen.
}
```

Regeln:

- Jeden Frame aufrufen, in dem das HUD sichtbar ist; nicht aufrufen, wenn es ausgeblendet ist (dann läuft es nach 250 ms ab).
- Eindeutige, stabile `id` im Format `modid:name`.
- In Addons den Aufruf in try/catch kapseln (`NoClassDefFoundError` bei älterem OttoExtra). Tactical Factions macht das in `SquadActivityHud` und fällt dann auf die alte Regel „gleiche Ecke wie Aktivitätsanzeige → vertikal spiegeln“ zurück.

### Editor

`HudLayoutElement` implementieren und über `OttoExtraAddon.hudLayoutElements()` zurückgeben (Vorlage: `SquadHudLayoutElement` in Tactical Factions, `ActivityHudElement` in OttoExtra):

- `id()`, `label()`, `bounds(screenW, screenH)` (gewünschte Position, `null` wenn nicht verfügbar).
- `place(x, y, w, h, screenW, screenH)`: in eigene Anker/Offset-Werte umrechnen.
- Optional `scalable()`, `scale()`, `setScale(...)`, `reset()`, `save()`.
- `flowId()` = dieselbe id wie bei `HudFlow.place`, damit der Editor die echte Position kennt und das Element pinnen kann.
- `drawPreview(...)` für eine echte Vorschau, sonst zeichnet der Editor eine beschriftete Box.
- `rendersLive()` = `true`, wenn das Spiel-HUD das Element selbst hinter dem Editor zeichnet (dann keine Kopie).

## Bekannte Grenzen

- Ausweichen nur vertikal; eine Spalte, die höher als der Bildschirm ist, überlappt oben.
- Andere Xaero-Module (Koordinaten, Effekte, …) sind keine Hindernisse; nur Minimap und Lehen-Overlay.
- Xaero-Minimap: nicht skalierbar im Editor; Zurücksetzen nur auf die Position beim Editor-Start. Im Spiel noch nicht getestet, ob die neue Position einen Neustart übersteht.
- `MinimapBannerEditScreen` (alter Lehen-Overlay-Editor) existiert weiter parallel.
- Der Test `RegionDataServiceBootstrapTest` schlägt unter Windows gelegentlich mit „Failed to delete temp directory“ fehl; hat nichts mit HudFlow zu tun.
