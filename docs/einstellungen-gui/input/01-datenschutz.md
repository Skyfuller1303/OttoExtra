# Datenschutz

- Position: Reiter 1 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `privacy`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Verwaltet freiwillige Downloads und Datenübertragungen.
- Umfang: 1 Tab(s), 1 Karte(n), 5 Einträge

## Tab „Basis“

Das Modul hat nur diesen einen Tab; die Tab-Leiste wird dann nicht angezeigt.

### Karte „Einwilligungen“

Kartenbeschreibung im GUI: Freigaben gelten sofort. Widerruf stoppt neue Übertragungen und das spätere Übernehmen laufender Ergebnisse.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Online-Dienste erlauben | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | – | `consent.onlineServices` |
| 2 | Automatische Resourcepack-Downloads | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | – | `consent.automaticResourcePackDownloads` |
| 3 | Externe Skin- und Bannerbilder | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | – | `consent.externalImageDownloads` |
| 4 | GitHub-Versionsprüfung | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | – | `consent.updateChecks` |
| 5 | Beobachtete RP-Namen an API senden | Schalter (An/Aus) | Aus (Zustimmung nötig) |  | – | `consent.communityRpNameUploads` |
