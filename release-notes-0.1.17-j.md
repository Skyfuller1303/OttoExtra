# OttoExtra 0.1.17-j – Alpha

8. September 2026. Tierart und Geschlecht bis zu **14 Tage lokal speichern**.

Anvisierte Tiere bleiben nach Disconnect und Minecraft-Neustart für Glow und
Geschlechts-Nametags bekannt. Die Zeit zählt ab der letzten erhaltenen Tierinfo;
neue Tierinfos aktualisieren die Daten. Gespeichert werden maximal 1.024 Tiere
pro Serveradresse. Die Daten bleiben auf diesem Rechner.

Die Funktion ist standardmäßig an und lässt sich unter **Tierinfo → Erweitert →
Lokaler Tiercache → Tiergeschlecht 14 Tage lokal merken** abschalten.
Der Glow selbst bleibt optional und wird wie bisher mit **F8** umgeschaltet.

Paarungsbereitschaft, Mast und andere veränderliche Tierwerte werden weiterhin
frisch abgefragt. Der Cache speichert dafür keine alten Werte dauerhaft und
erzeugt keine Einträge in der gemeinsamen Tier-/Zuchtdatenbank.

Die bisherige OttoExtra-JAR durch **`ottoextra-0.1.17-j.jar`** ersetzen und
Minecraft neu starten. Minecraft 1.21.11, Java 21 und Fabric-Abhängigkeiten
bleiben unverändert. Alle Funktionen aus [Alpha I](release-notes-0.1.17-i.md)
sind enthalten.

**695 Unit-Tests** sowie die Clienttests **UI-ANIMAL-GLOW**,
**UI-ANIMAL-NAMETAGS** und **UI-ANIMAL-OVERVIEW** bestehen.

[Bedienung und Speicherregeln](docs/animal-identity-cache.md),
[Release-Validierung](docs/releases/0.1.17-j.md).
