# Integrierte Module: Resources und Tactical Factions

Ab 0.18.0 sind die früheren Zusatzmods **OttoExtra Resources** und **OttoExtra Tactical Factions** fester Bestandteil von OttoExtra. Du installierst nur noch **eine** Datei: `ottoextra-0.18.0-alpha.jar`.

## Umstieg

1. Minecraft schließen.
2. Im `mods`-Ordner die Dateien `ottoextra_resources…jar` und `ottoextra_tacticalfactions…jar` **löschen**.
3. Die neue OttoExtra-Jar hineinlegen.
4. Minecraft starten.

Konfiguration und gespeicherte Daten bleiben an ihrem Ort (`config/ottoextra-resources.json`, `config/ottoextra-resources/`, `config/ottoextra-tacticalfactions.json`) und werden weiterverwendet. Du musst nichts neu einrichten.

Bleibt ein altes Addon-Jar im Ordner, **verweigert Fabric den Start** und meldet einen Konflikt. Das schützt davor, dass die Module doppelt geladen werden. Entferne das alte Jar, dann startet alles normal.

## Resources

Resources erkennt regenerierende Ressourcen, zeigt Hinweise im Ressourcengebiet und führt die Ressourcenliste.

Einstellungsbuch → **Handwerk & Bauen → Ressourcen-Hinweise**

| Schalter | Standard |
| --- | --- |
| Ressourcen-Hinweise verwenden (Hauptschalter) | an |
| Hinweise im Ressourcengebiet | an |
| Umkreis für Ressourcen (Blöcke), Experte | siehe Einstellungsbuch |
| Daten neu laden nach (Sekunden), Experte | siehe Einstellungsbuch |

Neu in 0.18.0: **Gespeicherte POIs erscheinen wieder in der Liste im Menübuch** (dort mit „POI“ beschriftet, neue legst du mit „+ POI“ an).

Der Befehl `/ottoextra resources` öffnet die Ressourcenliste. Die Taste **R** bestätigt eine Nutzung, wenn du sie nicht umgelegt hast.

## Tactical Factions

Tactical Factions färbt im Gefecht die Namen von Verbündeten und Gegnern, markiert Angreifer und ordnet die Tabliste.

Einstellungsbuch → **Kampf → Verbündete & Gegner**

| Schalter | Standard |
| --- | --- |
| Kampfmodus für Gefechte (Hauptschalter) | aus |
| Weitere Schalter: Accountnamen im Kampf anzeigen, Namen nach Beziehung färben, Politik-Buch (Öffnen…), Angreifer mit rotem „!“ markieren, Auch Neutrale nicht treffen, Farben für Verbündete und Gegner | siehe Einstellungsbuch |

**Schläge auf Verbündete verhindern:** Tactical Factions enthält seit 0.18.0 nur noch die Variante **mit** Friendly-Fire-Schutz. Die Option **Schläge auf Verbündete verhindern** ist vorhanden. Im Spiel steht dazu bei der Option die Warnung, dass diese Funktion zu einem Bann führen kann. Lies sie bitte und schalte die Option nur ein, wenn du die Regeln des Servers dazu kennst.

## FAQ

**Fabric meldet beim Start einen Konflikt mit `ottoextra_resources` oder `ottoextra_tacticalfactions`.**
Es liegt noch ein altes Addon-Jar im `mods`-Ordner. Lösche es und starte neu.

**Sind meine Einstellungen und Daten weg?**
Nein. Die Dateien bleiben am selben Ort und werden übernommen.

**Muss ich Resources oder Tactical Factions extra einschalten?**
Resources ist wie bisher an, der Kampfmodus von Tactical Factions ist wie bisher aus und wird bei Bedarf eingeschaltet.

**Wo finde ich meine gespeicherten POIs?**
In der Ressourcenliste im Menübuch (`/ottoextra resources`). Sie erscheinen dort wieder in der Liste.

**Gibt es Resources und Tactical Factions noch einzeln?**
Die bisherigen Einzel-Jars sind durch die integrierte Fassung ersetzt. Die alten Jars dürfen nicht neben OttoExtra 0.18.0 liegen.

**Ich habe die Zonen-Hinweise oben in der Mitte gesehen. Woher kommen sie?**
Das sind Hinweise von Resources. Sie stapeln sich mit den anderen Anzeigen (siehe [HUD](hud.md)).
