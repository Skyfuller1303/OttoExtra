# OttoLetter PAGE-Modus — Server-Anforderungen

Der Client (OttoExtra) unterstützt zwei Versandmodi für `/letter`
(Config `letter.sendMode`):

- **LEGACY** — eine `/letter <Zeile>`-Nachricht pro sichtbarer Zeile (altes Verhalten, Leerzeile = Figure Space ` `).
- **PAGE** — eine `/letter <kodierte Seite>`-Nachricht pro Buchseite. Default ist LEGACY; PAGE ist in den Einstellungen aktivierbar.

Diese Datei beschreibt, was der **Server** für den PAGE-Modus leisten muss.
Der Server-Code liegt nicht in diesem Repo (Client-only Mod).

## Payload-Format (PAGE)

Pro Buchseite sendet der Client genau einen Command:

```
/letter <encoded>
```

`<encoded>` ist die komplette Seite, kodiert:

| Im Buch | Im Payload |
|---|---|
| echter Zeilenumbruch | `\n` (Backslash + n) |
| echter Backslash `\` | `\\` |
| Formatcode `§a` | `&a` (Client normalisiert §→& vor dem Senden) |

## Server-Dekodierung (Pflicht, exakt rückwärts)

```
\n  -> echter Zeilenumbruch
\\  -> echter Backslash
```

Reihenfolge beachten: erst `\\` und `\n` als Escape-Sequenzen parsen, damit ein
sichtbar getipptes `\n` (vom Client als `\\n` kodiert) NICHT zum Umbruch wird.

## Validierung (Server)

- max. 14 Zeilen pro Seite
- max. 256 effektive Buchzeichen (Zeilenumbruch zählt als 2)
- `&`-Formatcodes interpretieren, falls gewünscht

## Invariante für Verkündungen

```
1 /letter-Command == 1 Minecraft-Buchseite == 1 Discord-Verkündungsnachricht
```

Der Discord-Crawler darf eine Minecraft-Seite NICHT auf mehrere Nachrichten
aufteilen und nicht selbst raten, wo Seiten enden — der Client liefert die
Seitengrenzen bereits eindeutig.

## Quelle

Spezifikation: `Mods/docs/ottoletter-page-rework-docs/`.
