# Backups

- Position: letzter Reiter in der oberen Reiterleiste (nach allen Modul- und Addon-Reitern)
- Kein Modul aus der Einstellungs-Registry, sondern eine fest eingebaute Seite (`OttoExtraSettingsScreen.buildBackupsPage`)
- Keine Tabs, keine Karten mit Rahmen, keine „Zurücksetzen“-Schaltflächen
- Die Einträge dieser Seite werden von der Suche nicht gefunden (die Suche durchsucht nur die Registry)

## Aufbau der Seite (von oben nach unten)

| Nr. | Element | Art | Text im GUI | Wirkung |
|---|---|---|---|---|
| 1 | Überschrift | Text | „Backups“ | – |
| 2 | Beschreibung | Text (grau) | „Sicherungen aller OttoExtra-Daten (Config, Personenbuch, Briefe). Vor jeder Wiederherstellung wird automatisch ein frisches Backup des Ist-Zustands angelegt.“ | – |
| 3 | Überschrift | Text | „RP-Namen-Reparatur“ | – |
| 4 | Beschreibung | Text (grau) | „Prüft die lokale Personendatei. Eine beschädigte Datei bleibt als broken-Kopie erhalten; OttoExtra stellt automatisch die letzte gültige Vorversion oder Sicherung wieder her.“ | – |
| 5 | Reparatur | Schaltfläche | „RP-Namen prüfen und reparieren“ | Prüft die lokale RP-Namensdatei und stellt sie bei Bedarf aus der letzten gültigen Sicherung wieder her |
| 6 | Backup anlegen | Schaltfläche | „Backup jetzt erstellen“ | Legt sofort ein Backup an |
| 7 | Backup-Liste | Liste, eine Zeile je vorhandenem Backup | „<Name>  <n> Dateien“ | – |
| 8 | Wiederherstellen (je Zeile) | Schaltfläche mit Sicherheitsabfrage | „Wiederherstellen“, nach dem ersten Klick rot „Sicher? Klick bestätigt“ | Zweiter Klick stellt das Backup wieder her |

## Statusmeldungen (unten links, grün oder rot)

| Auslöser | Text |
|---|---|
| Backup erstellt | „Backup erstellt.“ |
| Backup wiederhergestellt | „Wiederhergestellt — Neustart empfohlen.“ |
| Backup oder Wiederherstellung fehlgeschlagen | „Fehlgeschlagen — siehe Log.“ |
| Reparatur: Datei in Ordnung | „RP-Namensdatei ist in Ordnung.“ |
| Reparatur: wiederhergestellt | „RP-Namen wurden aus der letzten gültigen Sicherung wiederhergestellt.“ |
| Reparatur: keine Datei | „Noch keine RP-Namensdatei vorhanden.“ |
| Reparatur: keine gültige Sicherung | „Keine gültige Sicherung gefunden. Speichern bleibt zum Schutz der Daten gesperrt.“ |
| Reparatur fehlgeschlagen | „RP-Namen-Reparatur fehlgeschlagen — siehe Log.“ |

## Hinweis außerhalb dieser Seite

Wenn kein Backup vorhanden ist, zeigt der Einstellungsbildschirm auf allen Reitern oben links in Rot: „Kein Backup! Bitte unter Backups eines erstellen.“
