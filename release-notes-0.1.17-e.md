# OttoExtra 0.1.17-e – Alpha

8. September 2026. Geschlecht und Namensfarbe im Tier-Nametag einstellen.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil durch
**`ottoextra-0.1.17-e.jar`** ersetzen; nur eine OttoExtra-Version gleichzeitig laden.

## Einstellungen

Unter **Einstellungen → Tierinfo → Grundlagen → Tier-Nametag** gibt es zwei
unabhängige Schalter:

- **Geschlecht im Nametag anzeigen**: rosa ♀ oder blaues ♂ hinter dem Namen.
- **Tiernamen nach Geschlecht färben**: den Namen selbst rosa bzw. blau darstellen,
  auch wenn das Symbol ausgeblendet bleibt.

Beide Optionen sind zunächst aus. Bei unbekanntem Geschlecht bleibt die bisherige
Darstellung erhalten. Verwendet werden bereits bekannte Daten zum registrierten
Tier; durch das Rendern erfolgen keine zusätzlichen Serverabfragen.

Das gilt für OttoExtras Tier-Nametags. Das ältere separate AnimalBreeding-Addon
verwendet weiterhin seine eigene Pferde-Nametag-Darstellung.

Die kompakten Toast-Symbole aus [Alpha D](release-notes-0.1.17-d.md) und alle
bisherigen Briefkorrekturen sind enthalten.

**671 Unit-Tests** sowie der gezielte Test im echten Minecraft-Client bestanden.
Dieser prüft den Render-State einschließlich Mixin, die ausgegebenen Labels,
Sichtbarkeitsregeln und beide Menüschalter mit isolierten Tierdaten.

[Verhalten und Beispiele](docs/animal-nametags.md),
[Release-Validierung](docs/releases/0.1.17-e.md).
