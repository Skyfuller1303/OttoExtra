# Abweichungen und Korrekturen

Ausgangspunkt ist der Arbeitsstand vor dem Audit vom 08.09.2026. „Behoben“ bezeichnet
eine Änderung in OttoExtra. Serverfehler werden ausdrücklich getrennt aufgeführt.

## In OttoExtra behoben

| Befund / Reproduktion | Korrektur | Nachweis |
| --- | --- | --- |
| Altes Modell hängt bei unter 256 Zeichen direkt an; Courier 2.1.0 fügt LF ein und paginiert wortweise. | `CourierNewProtocol` bildet den gelieferten Algorithmus samt Randfällen ab. | Golden Tests und echter JAR-Oracle. |
| Reset-Füllbefehle sollten eine 256er-Seitengrenze erzwingen, erzeugen jetzt unerwartete Zeilen/Seiten. | Keine Füllbefehle; tatsächliche Simulation bestimmt Buchvorschau. | Planner-Tests und 100 vollständige JAR-Replays. |
| Importfortsetzung ignorierte bestehenden Platzbedarf in der Vorschau. | Originalseiten werden als Ausgangszustand simuliert. | Volle Importseite + Zusatz. |
| Farben gingen bei Buchumbrüchen innerhalb von Arbeitsseiten oder logischen Zeilen verloren. | Text wird in intakte Einheiten geplant, Formatstatus an tatsächlichen Übergängen wiederhergestellt; Zusammenfassung von Befehlen nur nach identischem Replay. | Farbtests über mehrere Seiten, schmale Zeichen, Importfortsetzung. |
| Neue manuelle Arbeitsseite konnte beim Zusammenführen im Buch alte Farbe erben. | Eigenständiger Formatstatus pro manueller Quellseite. | Gold → Standard-Test. |
| `trim/isBlank` und Unicode-Kleinschreibung wichen von Paper/Bukkit ab. | ASCII-Space-Argumenttrennung und exakte Bukkit-Farbcodezeichenliste. | JAR-Oracle inkl. Tabs, LF und Kelvinzeichen. |
| Literal `\n` und `%prefix%` verändern den eingegebenen Text auf dem Server. | Planung blockiert verlustbehaftete Inhalte mit Warnung vor dem Versand. | Negative Planner-/Kalibrierungstests. |
| Einige PAGE-/LEGACY-Wege nutzten abweichende Encoder oder ließen zu lange Teilseiten aus. | Alle produktiven Schreibwege verwenden den aktuellen Courier-Vertrag; Warnungen verhindern Teilversand. | Service-/Sendemodus-Tests. |
| Nach Import → neue Seite → zurück ließ Cursor 0 den gesperrten Brieftext ändern. | Präfixschutz an der Mutationsgrenze; Cursor, Auswahl, Paste, Delete, Tab und Formatbereinigung respektieren den Sperrbereich. | Textoperations-Tests; manuelle UI-Matrix. |
| Pfeiltaste innerhalb eines Emojis, danach Delete, beschädigte Surrogatpaare. | Codepoint-Grenzen bei Cursor, Löschen und Umbruch. | Emoji-Regressionsprüfung. |
| Lange Importergänzungen und Cursor verschwanden unterhalb von 14 Zeilen. | Scrollbarer Editor-Ausschnitt mit Cursorverfolgung; Originalpräfix bleibt unverändert. | Viewport-Test; manuelle UI-Matrix. |
| Fett → kursiv beim Import ließ Fett aktiv. | Vollständige Stilübergänge und erforderliche Resets. | Vier Import-Stiltests. |
| Namenssubstring `Ann` traf `Anna`; fremde normale Bücher wurden zur Bearbeitung angeboten. | Servermarker im Buch-Titel und Versand-Lore prüfen; Haupthand verwenden. Keine erfundene Autorenberechtigung. | Shared Guard und Quellenabgleich. |
| Buchvorschau zeigte höchstens zehn statt bis zu vierzehn Buchzeilen. | Buchanzeige nutzt 14 Zeilen; zusätzlicher Überlauf wird angezeigt. | Codeprüfung und Client-Start; visuelle Serverprüfung bleibt vorgesehen. |
| Materialguard fehlte bei Verkündungen und Recovery; zusätzliches Material in früherem Slot konnte den neuen Brief umlenken. | Startprüfung auf allen Schreibwegen; frisches Material inventarweit eindeutig. | ItemGuard-Tests. |
| Laufende Befehle konnten nach Slot-/Buchwechsel fortgesetzt werden. | Vor jedem Dispatch und Schreibabschluss Verbindung, Server, Spieler, Slot und erwartete Buchseiten prüfen. | Queue-/Context-Tests. |
| Recovery war global, ohne Server-/Spieler-/Buchbindung und vollständigen Entwurf. | Persistierter Kontext und Draft; fremder oder nicht übereinstimmender Zustand blockiert. Alte kontextlose Restlisten werden nicht blind abgespielt. | Recovery-Serialisierung, Negativfälle. |
| Neustart/Verwerfen einer Recovery konnte beide gespeicherten Vorgänge löschen. | Nur den angezeigten Store ändern. | Review und Recovery-Tests. |
| Entwürfe wurden schon vor erfolgreichem Schreiben oder nach unbestätigtem Submit gelöscht. | Draft bleibt bei Unterbrechung erhalten; Submit-Dispatch allein löscht ihn nicht. | Context-/Flow-Prüfung. |
| Brief- und Kalibrierqueue konnten gleichzeitig schreiben; unterbrochene Kalibrierqueue blieb belegt. | Gegenseitige Startsperre und Interrupt-Aufräumung. | Codeprüfung, vorhandene Queue-Tests. |
| Optimierung nach abgeschlossenem Schreiben änderte die Vorprüfung, nicht den tatsächlichen Brief. | Vorbereiteten Buchinhalt als unveränderlichen Snapshot prüfen; dafür Optimieren deaktivieren. | Prepared-Preflight-Regression. |
| Nach Schließen der Empfängerwahl versuchte `letter send` den fertigen Brief erneut zu schreiben. | Bei gültigem vorbereiteten Brief direkt Empfängerwahl öffnen. | Prepared-Flow-Prüfung. |

## Verbleibende Grenzen in OttoExtra

Im anschließenden [echten V2-Client-/Serverlauf](e2e-v2-2026-09-08.md) wurden drei
weitere Fehler korrigiert: das vom Server an einer Seitengrenze weggelassene
Transport-Schlussleerzeichen führte zum Verwerfen vollständiger Zeilen; außerdem
passten Editor und Vorschau bei 240 logischen Pixeln Höhe nicht vollständig ins
Fenster. Der abschließende Lauf besteht mit 66/66 Fällen. Die folgenden Grenzen
beschreiben weiterhin das Produktivverhalten und werden dadurch nicht aufgehoben.

- `/post` und Verkünden besitzen hier keinen belastbaren Erfolgs-/Zustell-ACK.
  Geld-, Permission- und Cooldownfehler bleiben im Serverchat sichtbar. Entwürfe
  kombinierter Schreib-/Submit-Vorgänge bleiben vorsorglich gespeichert.
- Dauert die Inventaraktualisierung länger als das Befehlsintervall, stoppt der
  Inhaltsvergleich. Das verhindert weiteres Schreiben auf einen ungeklärten Zustand,
  kann aber eine bewusste Wiederaufnahme erfordern. Keine automatische Wiederholung
  eines möglicherweise schon ausgeführten Befehls.
- Die sichtbare Materialbezeichnung ist keine vollständige MythicItem-Prüfung.
  Serverseitig zählt `isSimilar` zur tatsächlichen Materialkonfiguration.
- Die Marker entsprechen der mitgelieferten `messages.yml`. Bei geänderten
  Live-Titeln/Loremarkern muss der Vertrag erneut abgeglichen werden.
- Arbeitsseiten im Editor sind keine erzwungenen Buchseiten. Verbindlich für den
  vorhergesagten Serverinhalt ist die Buchvorschau.
- Oracle-BookMeta ist ein Test-Proxy. CraftMetaBook-Limits, Netzwerk,
  Serverressourcenpaket und Leseskill sind zusätzliche Integrationsgrenzen.
- RGB-Farben außerhalb der vorhandenen Legacy-Palette sind weiterhin nicht
  verlustfrei über die Briefeditor-Formatierung repräsentiert.

## Im gelieferten Serverplugin verbleibend

Diese Befunde wurden dokumentiert; `input/` wurde nicht verändert.

| Serverbefund | Quelle / Wirkung |
| --- | --- |
| Lore-Seitenzahl nach Bearbeiten ist tatsächliche Seitenzahl + 1. | `LetterCreation.editBook`, Zeile 227. OttoExtra zählt Buchseiten statt Lore. |
| Kosten und Fehlermeldungen widersprechen sich: 5 vs. 3 beim Versand, 20 vs. 10 beim Verkünden. | `config.yml` vs. `messages.yml`. OttoExtra kann den serverseitigen Preis nicht korrigieren. |
| Langes Einzelwort wird auch bei Überlauf auf leerer Seite übernommen; zusätzliche leere Seite möglich. | `appendLogicalLineWordSafe` / `commitLine`. Das Modell bildet dies ab; reguläre Clientplanung vermeidet solche Transporte. |
| `Too_Many_Pages` behauptet eine Ein-Seiten-Grenze, aber `Announcements.announce` prüft sie nicht. | Meldung ungenutzt in diesem Pfad; lokale Komfortgrenzen sind keine Servergarantie. |
| Versandkosten werden vor einigen Empfängerberechtigungen abgezogen. | `LetterSender.send`; möglicher Geldverlust bei späterer Ablehnung. |
| Versand-Erfolgsmeldung liegt vor asynchroner Speicherung. | `LetterSender.send`; kein Ende-zu-Ende-Zustellnachweis. |
| Leseskill verändert die geöffnete Buchansicht. | `BookManager`; sichtbare Textabweichung kann absichtliches Serververhalten sein. |
| Verkündungen benötigen JDA-Klassen auch bei deaktiviertem Discord. | Im lokalen Lauf löste `new Announcement` nach Geldabbuchung und YAML-Speicherung einen `NoClassDefFoundError` aus. Mit echter JDA-Runtime erfolgreich; Courier-JAR unverändert. |

Details und Referenzlinks stehen im [Serververtrag](courier-2.1.0-contract.md).
