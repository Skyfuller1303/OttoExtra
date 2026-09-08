# Tier-Glow nach Geschlecht

Ab Alpha **0.1.17-h** unter **Einstellungen → Tierinfo → Grundlagen → Tier-Glow**
den Schalter **Tiere nach Geschlecht umranden** aktivieren.

- Weiblich: rosa Umriss (`#FF79B8`).
- Männlich: blauer Umriss (`#68B8FF`).
- Unbekanntes Geschlecht: keine zusätzliche Markierung.
- Ein ausgewähltes Finder-Tier behält seinen goldenen Umriss.

Unter **Minecraft → Optionen → Steuerung → Tastenbelegung → Verschiedenes**
lässt sich **Tier-Glow nach Geschlecht umschalten** frei belegen. Ab Alpha I ist
**F8** die Standardtaste. Sie schaltet denselben gespeicherten Wert wie das Menü um und
zeigt den neuen Zustand in der Aktionsleiste. Bei geöffnetem Menü oder außerhalb
einer aktiven Ottonien-Verbindung schaltet sie nichts um.

Bereits gespeicherte eigene Tastenbelegungen bleiben erhalten. Falls Alpha H
die Taste schon als unbelegt gespeichert hat, in der Tastenbelegung einmal
**Zurücksetzen** neben dem Tier-Glow wählen oder **F8** zuweisen.

Der Glow ist standardmäßig aus (`animalInfo.genderGlowEnabled=false`) und
unabhängig von den Nametag-Schaltern. Er gilt für lebende, geladene Tiere in
weniger als 64 Blöcken Entfernung vom Spieler. Die Umrisse sind wie der
Minecraft-Leuchteffekt auch durch Wände sichtbar.

## Bereits bekannte Tierdaten

Auch Tiere ohne Register-Eintrag werden markiert, sobald ihre normale Tierinfo
ein bekanntes Geschlecht geliefert hat. Einmal anvisieren und die Tierinfo
abwarten genügt; das Tier muss danach nicht mehr im Fadenkreuz bleiben.
Es werden keine Geschlechter geraten und keine zusätzlichen Abfragen gestartet.

Die vollständigen Tierbeobachtungen bleiben bis zum Disconnect im Arbeitsspeicher.
Ab Alpha J bleiben **Tierart und Geschlecht optional bis zu 14 Tage lokal erhalten**,
mit höchstens 1.024 UUIDs pro Server. Neue Tierinfos aktualisieren die Farbe.
Erkannte aktuelle Beobachtungen und zugängliche Registerdaten haben Vorrang
vor der gespeicherten Identität. [Details zum lokalen Cache](animal-identity-cache.md).

## Implementierung und Kompatibilität

`AnimalGenderGlow` verwendet die vorhandenen Client-Mixins `EntityGlowMixin`
und `EntityRendererMixin`. Minecraft 1.21.11/Yarn build 6 wurde anhand der
lokalen Sources geprüft: `Entity.isGlowing()` aktiviert den Umrisspfad;
`EntityRenderer.updateRenderState()` bereitet die ARGB-Farbe vor.
`HorseFinder.prepare()` wird anschließend ausgeführt und behält Vorrang.

Die Darstellung verändert weder Teams noch Status-Effekte, Finder-Auswahl
oder Tierregister-Einträge. Beim Abschalten liefert Minecraft wieder seine
normale Umrissfarbe; vorhandener Vanilla-Glow und Finder bleiben erhalten.
Die Tierübersicht entfernt Umrisse weiterhin ausdrücklich aus Modellvorschauen.
Die Funktionen und der Tastendruck-Handler liegen ausschließlich im Client.

Nametag und Glow verwenden denselben Geschlechtsparser. Der Renderpfad liest
nur bestehende UUID-Maps und erzeugt keine vollständigen Toast-Snapshots.
Shader oder fremde Outline-/Culling-Mods können die sichtbare Darstellung
beeinflussen; sie wurden nicht in sämtlichen Kombinationen geprüft.

## Regressionstests

`AnimalGlowConfigTest` prüft sichere Altkonfigurations-Defaults, Menüwerte,
Speichern/Laden und Snapshot-Wiederherstellung. Bestehende Nametagtests decken
gemeinsame Geschlechtsaliase, unbekannte Werte, Datenquellen und Cache-Leeren ab.

`UI-ANIMAL-GLOW` läuft im echten Client mit lokalem Paper-Server. Geprüft werden
weibliche Kuh, männlicher Esel, unbekanntes Tier, Entfernung, tote Tiere,
Servergrenze, frische Toast-Daten, Disconnect, Finder-Vorrang, erhaltenes
Vanilla-Glow, Menüschalter und die registrierte Tastenbelegung über Client-Ticks.
Der API-Ersatz bricht bei jedem unerwarteten Aufruf ab; die Serverprobe prüft,
dass kein Befehl versendet wurde. Screenshots dokumentieren Umrisse und Menü.

[Release-Validierung](releases/0.1.17-j.md).
