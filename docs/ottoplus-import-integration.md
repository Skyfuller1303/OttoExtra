# OttoPlus-Import (RP-Namen, Titel, Farbe)

Integration eines lokalen Datei-Imports aus den OttoPlus/OttoTalk-Caches in das
RP-Namen-Modul von OttoExtra. Ziel: bestehende RP-Identitäten (Charaktername,
Titel, **Titelfarbe**) aus OttoPlus mit dem lokalen Bekanntschafts-Store
abgleichen und überschreiben.

## Quelldateien

Bereitgestellt unter `Mods/docs/ottoplusjson/` (Referenz). Zur Laufzeit liest der
Import aus dem Ordner **`config/ottoextra/import/`**.

### `ottotalk_players.json` (Primärquelle)

Array von OttoTalk-RP-Profilen. Liefert RP-Name, Titel und Titelfarbe.

```json
[
  {
    "accountName": "19BlackAttack81",
    "characterName": "Fronika von Amneesia",
    "characterTitle": "Mundköchin",
    "characterTitleColor": 10781024,
    "locked": false
  }
]
```

| Feld | Typ | Bedeutung | Mapping → LocalRpProfile |
|---|---|---|---|
| `accountName` | String | Minecraft-Account | `accountName` (Match-Schlüssel) |
| `characterName` | String | RP-Name; `"Unbekannt"`/leer = keiner | `rpName` (nur wenn echt) |
| `characterTitle` | String | Titel; leer = keiner | `title` (nur wenn nicht leer) |
| `characterTitleColor` | int | Packed RGB (`0` und `-1` = ungesetzt) | `colors.chatTitleColor` + `tabTitleColor` + `nametagTitleColor` |
| `locked` | bool | In OttoTalk gesperrt | nur informativ, nicht übernommen |

**Farb-Dekodierung:** `characterTitleColor` ist ein vorzeichenbehafteter int.
`0` und `-1` (0xFFFFFF, Default „weiß/unset") gelten als *keine Farbe* und werden
übersprungen. Sonst: `String.format("#%06X", color & 0xFFFFFF)`.

### `ottoletter-player-cache.json` (optional, UUID-Quelle)

Array von OttoLetter-Spielereinträgen. Kein RP-Name (`name` = Account), aber
**UUID** + Titel + Fraktion. Wird nur als `accountName → uuid`-Map genutzt, um beim
Import die UUID nachzutragen (bessere künftige Verknüpfung).

```json
[
  { "uuid": "f76638d5-…", "name": "19BlackAttack81", "title": "Mundköchin",
    "faction_name": "" }
]
```

| Feld | Nutzung |
|---|---|
| `name` | Account (Map-Schlüssel) |
| `uuid` | → `LocalRpProfile.uuid`, falls lokal fehlt |
| `title`, `faction*` | aktuell ignoriert (Fraktion hat OttoExtra kein Feld) |

## Abgleich-Regeln (Reconcile + Overwrite)

OttoPlus ist für `rpName`/`title`/Titelfarbe **autoritativ** und überschreibt den
lokalen Wert — im Gegensatz zum Regions-API-Import (`importApi`), der nur leere
Felder füllt.

Pro `ottotalk_players.json`-Eintrag:

1. `accountName` säubern (Mojibake-Fix wie im API-Importer). Leer → überspringen.
2. UUID aus der OttoLetter-Map (case-insensitive) nachschlagen.
3. Lokales Profil per `uuid` **oder** `accountName` suchen.
4. **Vorhanden:**
   - `locked == true` (lokal manuell gesperrt) → **überspringen** (Schutz bleibt).
   - sonst überschreiben: `rpName` (wenn echt), `title` (wenn nicht leer),
     Titelfarbe (wenn gesetzt), UUID nachtragen (wenn fehlt).
   - `knowledgeState = KNOWN`, `source = IMPORTED_FROM_OTTOPLUS`.
5. **Nicht vorhanden** und Modus „alle": neues Profil anlegen (nur wenn RP-Name
   oder Titel vorhanden). `knowledgeState = KNOWN`.

Vor dem Import wird ein Backup (`store.backup()`) angelegt. Leere/`"Unbekannt"`-
Werte löschen **nie** vorhandene lokale Daten (additive Überschreibung).

### Modi

- **Nur bekannte** (`createMissing = false`): aktualisiert nur lokal vorhandene
  Profile.
- **Alle** (`createMissing = true`): legt zusätzlich fehlende Profile an.

## Komponenten

| Datei | Rolle |
|---|---|
| `rpnames/importer/OttoPlusImporter.java` | Liest beide JSONs, reconcilet, schreibt |
| `rpnames/store/LocalRpIdentityStore#importOttoPlus(...)` | Overwrite-Pfad (locked-sicher) |
| `rpnames/model/RpNameSource.IMPORTED_FROM_OTTOPLUS` | Quellenmarkierung |
| `config/OttoExtraPaths#importDir()` | `config/ottoextra/import/` |
| `rpnames/ui/RpNamesPeopleBookScreen` (Import-Tab) | Buttons „OttoPlus: bekannte/alle" |
| Lang `de_de`/`en_us` | Button-/Status-Texte |

## Ergebnis-Statistik

`OttoPlusImporter.Result(total, updated, created, skippedLocked, error)` —
angezeigt in der Statuszeile des Import-Tabs.

## Fehlerfälle

- `config/ottoextra/import/ottotalk_players.json` fehlt → Statushinweis mit Pfad.
- Defektes JSON → Fehlerstatus, lokale Daten unverändert (Backup existiert).
- `ottoletter-player-cache.json` fehlt → UUID-Anreicherung entfällt, Rest läuft.
