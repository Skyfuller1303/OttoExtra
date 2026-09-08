# OttoExtra 1.17 – Entwurf

Noch nicht veröffentlicht. Die Release-Prüfung und offene Punkte stehen in
[docs/release-1.17-review.md](docs/release-1.17-review.md).

## Neu

- Eigenes, standardmäßig aktives Chatprotokoll mit Zeitstempeln neben den
  Minecraft-Logs. Enthält auch Systemmeldungen und bleibt über Neustarts erhalten.
- RP-Ersteinrichtung nach den grundlegenden Hinweisen: Komfort, Ausgewogen oder
  Immersiv aus drei nebeneinanderliegenden Karten auswählen, elf Anzeigeoptionen
  in den Details mit An/Aus anpassen und übernehmen. Überspringen behält die
  bestehenden Einstellungen bei.
- Pferde-/Tierübersicht mit klappbaren Höfen, gemeinsamen Info-Icons und
  kombinierbarer Sortierung nach Tempo und Sprunghöhe.
- Kampfmodus-Anzeige neben der Hotbar und Benennung von Tieren im eigenen Lehen.

## Verbessert und behoben

- Titelfarben nach dem angezeigten Titel; Adelsfarbe für andere Titel optional.
  Feste Farben für Vorkoster/in, Legende/Legendin und Chronist/in sowie ein
  aktualisierter Titelkatalog.

- Erststart-Hinweise bleiben auch bei hoher GUI-Skalierung bedienbar.
- Der gesamte Erststart-Assistent verwendet Dunkelblau und Gold sowie
  einheitliche Buttons in einer Reihe.
- Karten-Overlays liegen unter Xaeros Buttons, Menüs und Tooltips.
- Tablisten-Sortierdaten werden beim Öffnen aktualisiert; Wappen bleiben beim
  Fallback auf Scoreboard-Teamdaten erhalten.
- XaeroPlus-Minimap-Hooks erkennen die Schnittstelle mit und ohne Zoomargument
  eindeutig und vermeiden den nachgewiesenen Startabsturz beim Dev-Remapping.
- Korrekturen an Briefversand/Recovery, Config-Wiederherstellung, Chat,
  RP-Identitäten, Ressourcenpaket-Lifecycle und Regionsabgleich; Einzelheiten
  stehen im Changelog und im Bug-Audit.

## Kompatibilität

- Minecraft Java 1.21.11, Java 21, Fabric; ausschließlich clientseitig.
- Fabric API erforderlich. Mod Menu und die Xaero-Kartenmods sind optional.
- Netzwerk-/Downloadentscheidungen werden beim Erststart einzeln abgefragt.

## Vor Veröffentlichung

- Offene Tablisten-Nutzermeldung und die dokumentierten Live-Abnahmen bewerten.
- Version in `gradle.properties` auf `1.17` setzen, erneut bauen und Metadaten
  sowie SHA-256 der tatsächlichen Upload-JAR prüfen.
