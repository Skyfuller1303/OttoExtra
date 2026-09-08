# Personenbuch: Sperren bewusst setzen

Ab Alpha 0.1.17-l werden Personen beim Kennenlernen ohne automatische Sperre
gespeichert. Auch das Bearbeiten von RP-Name, Farben oder Titel setzt keine
Sperrhäkchen mehr.

- **Titel fest** schützt den Titel vor automatischen Änderungen aus Chat,
  Tablist und API. Das funktioniert auch mit einem ausdrücklich leeren Titel.
- **Gesperrt** schützt das gesamte Profil vor automatischer Übernahme von
  Name und Titel. API-Metadaten wie der zuletzt gemeldete Stand werden weiterhin
  erfasst.
- Ohne diese Sperren bleibt der Titel aktualisierbar. Persönlich eingetragene
  RP-Namen und Farben bleiben trotzdem erhalten.
- Änderungen allein an den Sperrhäkchen werden beim Schließen des Personenbuchs
  gespeichert. Das Entfernen einer Sperre wird jetzt ebenfalls dauerhaft übernommen.

Bereits gespeicherte Sperren bleiben erhalten, weil nachträglich nicht erkennbar
ist, welche davon bewusst gesetzt wurden. Zum Freigeben einer bestehenden Person
im Personenbuch **Gesperrt** und gegebenenfalls **Titel fest** abwählen.

Geprüft mit fünf zusätzlichen Store-Tests und dem automatisierten Clientfall
`UI-PEOPLE-LOCKS`: Kennenlernen, Bearbeiten, Titelaktualisierung, bewusstes Sperren,
Entsperren, Speichern beim Schließen sowie ein gesperrter leerer Titel.
Der Store-Test prüft außerdem das erneute Laden einer aufgehobenen Sperre von Disk.
