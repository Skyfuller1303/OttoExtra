# OttoExtra 0.1.17-k – Alpha

8. September 2026. **Briefe lokal als TXT archivieren und wieder importieren.**

- Erfolgreich versendete Briefe werden automatisch unter
  `config/ottoextra/letters/letter_<Datum>_<Uhrzeit>.txt` gespeichert.
  Das funktioniert auch bei manuellem `/post Spielername`.
- Empfangene Briefe lassen sich im geöffneten Buch über **TXT exportieren**
  sichern: `<Autor>_<Datum>_<Uhrzeit>.txt`.
- Im Briefeditor unter **Gespeicherte Entwürfe… → Briefe als TXT importieren**
  eine Datei auswählen und als neuen Brief weiterbearbeiten.
- Farben, Schriftstile und Quellseiten werden übernommen. Ein bestehender
  Entwurf wird vor dem Import gesichert. Gleichnamige Dateien bleiben erhalten.

Die Speicherung erfolgt erst nach Couriers passender Versandbestätigung.
Fehlende Berechtigung oder fehlendes Geld erzeugen kein Versandarchiv.
TXT-Dateien bleiben lokal und werden nicht automatisch gelöscht.

Die bisherige OttoExtra-JAR durch **`ottoextra-0.1.17-k.jar`** ersetzen und
Minecraft neu starten. Minecraft 1.21.11, Java 21 und Fabric-Abhängigkeiten
bleiben unverändert. Alle Funktionen aus [Alpha J](release-notes-0.1.17-j.md)
einschließlich des 14-Tage-Tiercaches sind enthalten.

**703 Unit-Tests und alle 100 lokalen Server-/Client-Testfälle bestanden.**

[Bedienung und Dateiformat](docs/briefmod/txt-archiv.md),
[Release-Validierung](docs/releases/0.1.17-k.md).
