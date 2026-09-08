# OttoExtra 0.1.16

Veröffentlicht am 10. August 2026 für Minecraft 1.21.11. Diese Release Notes
fassen alle Neuerungen seit OttoExtra 0.1.15.3 zusammen.

## Highlights

- Tierinfos erscheinen nach kurzem Anvisieren kompakt am oberen Bildschirmrand
  und lassen sich je Tierart vollständig anpassen oder deaktivieren.
- Ein neues Aktivitäts-HUD und animierte Weltkarten-Pings zeigen, in welchen
  Lehen und Gefolgen gerade besonders viel los ist.
- `/ottoextra verify` verbindet das Minecraft-Konto sicher mit OttoExtra und
  Discord; `/ottoextra account` zeigt das eigene Profil direkt im Spiel.
- Ein verpflichtender Erststart-Assistent gibt vollständige Kontrolle über
  Downloads und optionale Datenübertragungen.
- Neue Addon-Schnittstellen ermöglichen Tactical Factions und TranslateUtils
  tiefere Integrationen, ohne eine feste Mindestversion in deren Mod-Metadaten
  vorauszusetzen.

## Tierinfos

Nach kurzem Anvisieren eines Zuchttiers fragt OttoExtra die vorhandene
Server-Tierinfo automatisch ab. Angezeigt werden je nach Tierart:

- kleines 3D-Tiermodell und Tiername
- Geschlecht mit Symbol und Text
- Paarungsbereitschaft und verbleibende Zuchtzeit
- Maststatus
- Scherbarkeit bei Schafen und Melkbarkeit bei Kühen
- Sprunghöhe und Geschwindigkeit bei Pferden und Packeseln

Schaf, Kuh, Schwein, Streitross, Reitpferd, Ackergaul, normales Pferd,
Packesel, Ziege, Huhn, Wolf und sonstige Tiere besitzen getrennte Layouts.
Jede Tierart kann einzeln aktiviert oder deaktiviert werden. Modell und
Textbestandteile lassen sich einzeln anzeigen, verschieben und skalieren.

Automatische Antworten werden vor Chat und normalem Client-Log abgefangen.
Manuelle Tierinfo-Abfragen bleiben sichtbar. Nach Tierinteraktionen werden
veraltete Werte neu abgefragt; kurz zuvor verwendete Türen und andere Blöcke
werden nicht von der automatischen Interaktion gestört.

## Aktivitäten und Weltkarte

Das neue Aktivitäts-HUD listet aktive Gefolge beziehungsweise Lehen kompakt mit
Wappen und Aktivitätsstufe. Es bietet feste Bildschirmpositionen, Pixelversatz,
Skalierung, eine maximale Eintragszahl und einen frei verschiebbaren Modus.

Auf Xaeros Weltkarte erscheinen Aktivitätsringe ohne weiche Kanten. Hohe
Aktivität erhält zwei Ringe: Der zweite beginnt 70 Prozent zeitversetzt und die
Animation läuft 30 Prozent langsamer. Beim Herauszoomen werden Aktivitäten
nicht addiert; für eine zusammengefasste Lehensgruppe wird ausschließlich die
höchste vorhandene Stufe dargestellt.

## Konto und Datenschutz

Mit `/ottoextra verify` wird ein zeitlich begrenzter Browser-Link erzeugt. Die
Discord-Anmeldung findet auf `ottoextra.dev` statt; Discord-Zugriffstokens
werden nicht in Minecraft gespeichert. Status und Widerruf sind über
`/ottoextra verify status` und `/ottoextra verify unlink` erreichbar.

`/ottoextra account` öffnet eine Kontoansicht mit Minecraft- und
Discord-Verbindungsstatus, RP-Name, Titel, Stand, Rang, Gefolge, Testrechten und
Hellerbestand.

Vor dem ersten Hauptmenü fragt OttoExtra folgende optionale Zugriffe getrennt ab:

- automatischer Download des Ottonien-Resourcepacks
- externe Spieler-Skins und Regionswappen
- Versionsprüfung über GitHub
- Teilen beobachteter RP-Namen mit der OttoExtra-API

Keine Erlaubnis ist vorausgewählt. Alle Entscheidungen können später in den
Datenschutz-Einstellungen geändert werden. Der Update-Hinweis öffnet lediglich
die Release-Seite und ersetzt die installierte Mod niemals automatisch.

## RP-Namen, Titel und Addons

- Titel, RP-Name und Minecraft-Accountname sind im Nametag getrennte Zeilen;
  Titel werden nicht mehr in der RP-Namenszeile wiederholt.
- Titelfarben können aus dem API-Stand stammen. Persönliche Einstellungen haben
  weiterhin Vorrang, danach folgen Stand, Titelkatalog, Gruppe und Standardfarbe.
- Gesperrte Personenbuchprofile übernehmen weiterhin öffentliche API-Metadaten,
  ohne manuelle Identität oder Farben zu überschreiben.
- Addons können Nametags sichtbar machen, die drei Nametag-Bestandteile
  verändern und anklickbare Felder im ausgewählten Personenprofil ergänzen.
- Der Nametag-Hook funktioniert auch bei deaktiviertem normalen Nametag-Modul;
  Addon-Ausnahmen werden isoliert.
- Kontextreiche synchrone und asynchrone Chat-Hooks erlauben Übersetzungsaddons,
  eingehende Zeilen kontrolliert zu verzögern und danach verlustfrei einzufügen.

## Briefe

Der neue optionale Versandmodus `V2` kombiniert mehrzeilige Fließtextblöcke mit
sicheren Legacy-Zeilen für Leerraum und Sonderfälle. Vorschau und Versand
verwenden dieselbe Planung und berücksichtigen die gemessenen Grenzen des
Serverplugins. Die bestehenden Modi `PAGE` und `LEGACY` bleiben auswählbar.

Eine interne Kalibrierung prüft Seitenanzahl, Pixelbreite, Formatierung,
Leerraum und Escape-Verhalten mit sicheren `/letter`-Testfällen. Verkündungen
verwenden bei ausgewähltem V2 weiterhin den kompatiblen PAGE-Pfad.

## Kompatibilität und Diagnose

- Minecraft 1.21.11, Java 21, Fabric Loader und Fabric API bleiben die Basis.
- ModMenu sowie Xaero's Minimap und World Map bleiben optionale Integrationen.
- Ottonien-Funktionen akzeptieren nur die offizielle Domain und echte
  Subdomains, nicht ähnlich benannte Hosts.
- Ausführliche OttoExtra- und Tactical-Factions-Diagnosen erscheinen nur bei
  aktivem `/ottoextra debug on`; wichtige gedrosselte Warnungen bleiben sichtbar.

## Installation

Die bisherige OttoExtra-JAR durch `ottoextra-0.1.16.jar` ersetzen und Minecraft
vollständig neu starten. Bestehende Konfigurationen und Tierlayouts werden
automatisch ergänzt; gültige eigene Werte bleiben erhalten.
