# OttoExtra

## Deutsch

OttoExtra ist eine clientseitige Fabric-Mod für den deutschsprachigen
Minecraft-Rollenspielserver **Ottonien** (`ottonien.com`). Sie bündelt
Komfort-, Karten- und Rollenspielfunktionen in einer gemeinsamen,
konfigurierbaren Mod.

### Funktionen

- RP-Namen in Chat, Tabliste und Namensschildern
- lokales RP-Personenbuch mit Notizen, Titeln, Farben und Imports
- Chatkanal-Auswahl und Versand längerer Chatnachrichten
- Brief- und Verkündungseditor mit Vorschau, Entwürfen und Recovery
- Regionen-Einblendungen, Lehensinformationen und politische Kartenansichten
- optionale Integration in Xaero's Minimap und Xaero's World Map
- Schutz vor versehentlichem Werkzeugbruch
- lokale Backups wichtiger OttoExtra-Daten
- optionale Verknüpfung des Minecraft-Kontos mit dem OttoExtra-Konto
- optionaler Downloader für das offizielle Ottonien-Resourcepack

Die enthaltenen Ottonien-Kartenbilder werden mit ausdrücklicher Genehmigung
der Rechteinhaber verbreitet.

### Datenschutz, Downloads und Netzwerkzugriffe

Beim ersten Start erscheint vor dem Minecraft-Hauptmenü ein geführter
Einwilligungsassistent. Keine Erlaubnis ist vorausgewählt. OttoExtra kann
vollständig gestartet werden, wenn alle optionalen Punkte abgelehnt werden.
Alle Einwilligungen lassen sich später in den Datenschutz-Einstellungen
widerrufen.

Der Assistent fragt getrennt nach:

- automatischem Download, Speichern und Aktivieren des offiziellen
  Ottonien-Resourcepacks von GitHub; Dateigrößenlimit: 64 MB
- Download und lokalem Zwischenspeichern von Skin- und Regionsbanner-PNGs
- Versionsprüfung über die GitHub-API
- Übertragung beobachteter RP-Namen an `api.ottoextra.dev`

Beim optionalen Teilen beobachteter RP-Namen werden der eigene
Minecraft-Accountname oder die eigene UUID sowie RP-Name, Accountname und,
falls verfügbar, UUID des beobachteten Spielers übertragen. Ohne Zustimmung
bleiben diese Beobachtungen lokal.

Die Versionsprüfung lädt keine Mod-Datei herunter. OttoExtra ersetzt oder
aktualisiert seine eigene JAR niemals automatisch. Der Update-Hinweis öffnet
die öffentliche GitHub-Release-Seite erst nach einem Klick.

Die optionale Konto-Verknüpfung und Kontoübersicht kontaktieren
`ottoextra.dev` erst nach einem ausdrücklichen Spielerbefehl.

### Abhängigkeiten

**Erforderlich**

- Fabric Loader `0.18.1` oder neuer
- Fabric API für Minecraft `1.21.11`
- Java 21

**Optional**

- Mod Menu – komfortabler Zugriff auf OttoExtra-Einstellungen
- Xaero's Minimap – Minimap-Grenzen und Bannerintegration
- Xaero's World Map – Weltkarten-Overlay und Lehensansichten

XaeroLib muss unter Fabric nicht separat installiert werden; es ist in den
aktuellen Xaero-Kartenmods enthalten.

### Kompatibilität

- Minecraft Java Edition `1.21.11`
- Fabric
- clientseitig; keine Installation auf dem Server erforderlich

Quellcode, Fehlerberichte und Releases:
[GitHub](https://github.com/Skyfuller1303/OttoExtra)

---

## English

OttoExtra is a client-side Fabric mod for the German-language **Ottonien**
Minecraft role-play server (`ottonien.com`). It combines role-play, map,
convenience, and safety features in one configurable mod.

### Features

- RP names in chat, the player list, and name tags
- local RP people book with notes, titles, colors, and imports
- chat-channel selection and long-message handling
- letter and announcement editor with previews, drafts, and recovery
- region notifications, fief information, and political map views
- optional integration with Xaero's Minimap and Xaero's World Map
- protection against accidentally breaking tools
- local backups of important OttoExtra data
- optional Minecraft/OttoExtra account linking
- optional downloader for the official Ottonien resource pack

The bundled Ottonien map images are distributed with explicit permission from
their rights holders.

### Privacy, downloads, and network access

Before the first Minecraft main menu, OttoExtra displays a guided consent
screen. No permission is preselected. The mod can be started with every
optional item declined. Every permission can later be revoked in Privacy
settings.

The guide asks separately about:

- automatically downloading, storing, and enabling the official Ottonien
  resource pack from GitHub; maximum file size: 64 MB
- downloading and locally caching skin and region-banner PNG files
- checking the GitHub API for new OttoExtra versions
- sending observed RP names to `api.ottoextra.dev`

Optional RP-name sharing sends the user's Minecraft account name or UUID and
the observed player's RP name, account name, and UUID when available. Without
consent, these observations remain local.

The version check never downloads a mod file. OttoExtra never automatically
replaces or updates its own JAR. The update notice opens the public GitHub
release page only after a user click.

Optional account linking and account information contact `ottoextra.dev` only
after an explicit player command.

### Dependencies

**Required**

- Fabric Loader `0.18.1` or newer
- Fabric API for Minecraft `1.21.11`
- Java 21

**Optional**

- Mod Menu – convenient access to OttoExtra settings
- Xaero's Minimap – minimap borders and banner integration
- Xaero's World Map – world-map overlay and fief views

XaeroLib does not need to be installed separately on Fabric; current Xaero map
mods include it.

### Compatibility

- Minecraft Java Edition `1.21.11`
- Fabric
- client-side; no server installation required

Source code, issue tracker, and releases:
[GitHub](https://github.com/Skyfuller1303/OttoExtra)
