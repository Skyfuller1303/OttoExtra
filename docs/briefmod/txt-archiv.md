# Lokales TXT-Archiv für Briefe

Automatische Abschriften liegen im Minecraft-Profil unter
**`config/ottoextra/letters/sent/`**, manuelle Exporte unter
**`config/ottoextra/letters/`**.
Das bestehende `draft.json` bleibt dort erhalten. TXT-Archive werden weder
hochgeladen noch nach 14 Tagen gelöscht; die Frist des Tiercaches gilt hier nicht.

## Geschriebene Briefe

Nach dem abgeschlossenen **Schreiben** über OttoExtra wird automatisch eine
Abschrift als `sent/written_<Datum>_<Uhrzeit>.txt` gespeichert. Sie enthält den
Autor und sämtliche tatsächlichen Buchseiten, einschließlich bereits früher
geschriebener Seiten beim Fortsetzen eines Briefs. Auch ein fortgesetzter
Schreibvorgang aus der Wiederherstellung sichert nach Abschluss eine Abschrift.

Das geschieht bereits vor der Empfängerauswahl: Die Kopie bleibt erhalten, wenn
der Brief noch nicht verschickt, später von Hand übergeben oder der Versand
abgelehnt wird. Die Schreibkopie bestätigt keine Zustellung. Eine spätere
Erweiterung des Briefs erhält eine neue Datei; vorhandene Stände werden nicht
überschrieben. Bloßes Tippen im Editor bleibt in der bestehenden Entwurfsablage.

Unter **Briefe → Basis → Editor → Versandmenü nach dem Schreiben öffnen** lässt
sich das automatische Öffnen der Empfängerauswahl abschalten. Die Option ist
standardmäßig an. Ausgeschaltet bleibt nach dem Schreiben das Spiel geöffnet;
über **Verschicken…** im geöffneten Brief ist die Empfängerauswahl weiterhin
erreichbar. Die Abschrift wird bei beiden Einstellungen gespeichert.

Die Abschlussprüfung muss das geschriebene Buch zuerst bestätigen. Abgelehnte
oder unterbrochene Schreibvorgänge erzeugen daher keine unvollständige Abschrift.
Schlägt das Speichern fehl, erscheint eine Fehlermeldung und der Entwurf bleibt
im Cache erhalten.

## Versendete Briefe

Beim Versand über OttoExtra oder ein manuell eingegebenes `/post Spielername`
wird der Inhalt des tatsächlich gehaltenen Briefs vor dem Befehl aufgenommen.
Erst die passende Courier-Erfolgsmeldung schreibt die TXT-Datei:

`sent/letter_2026-09-08_18-30-45.txt`

Diese Versandkopie ergänzt die beim Schreiben angelegte Abschrift und enthält
zusätzlich den Empfänger. Auch ein bereits vorhandener Brief, der ohne erneutes
Schreiben über `/post Spielername` gesendet wird, erhält seine Versandkopie in
`sent`.

Abgelehnte Sendungen wegen fehlender Berechtigung oder fehlenden Geldes erzeugen
keine zusätzliche Versandkopie; die Schreibkopie bleibt erhalten. Die Bestätigung
gehört zur aktuellen Verbindung und zum angegebenen Empfänger; unbeantwortete
Vorgänge verfallen nach 30 Sekunden.
Mehrere Vorgänge werden in Befehlsreihenfolge zugeordnet, maximal 16 gleichzeitig.
Ein Disconnect verwirft ausstehende Bestätigungen.

Referenz ist `Success_Sent` aus der bereitgestellten Courier-2.1.0-Konfiguration:
`[Courier] Der Brief wurde Spielername gesendet.` Auch der auf Ottonien verwendete
Präfix `[Brief]` wird erkannt. Dies bestätigt die Annahme durch Courier,
**nicht das Abholen durch den Empfänger**. Eine anders konfigurierte
Erfolgsmeldung wird nicht erraten. Rundsendungen und Verkündungen gehören nicht
zu diesem Versandarchiv; nach einem separat abgeschlossenen Schreiben über
OttoExtra bleibt ihre Schreibkopie erhalten.

## Empfangene oder noch unversendete Briefe exportieren

Den Brief in der Haupthand öffnen und **Brief exportieren** anklicken.
Die Datei verwendet den im Buch gespeicherten Autor, zum Beispiel:

`Skyfuller1303_2026-09-08_18-31-12.txt`

Der Zeitpunkt ist der lokale Exportzeitpunkt, da Courier kein verlässlich
auswertbares Empfangsdatum liefert. Unzulässige Dateinamenszeichen werden ersetzt.
Bei Kollisionen folgen `_1`, `_2` usw.; vorhandene Dateien werden nicht überschrieben.

Die Chatbestätigung zeigt einen goldenen `[Brief]`-Präfix, den Erfolgstext grün,
den Dateinamen gold und den Speicherort grau. Exportfehler erscheinen rot.

Empfangene Briefe erhalten ausschließlich den Exportknopf. Bearbeiten und
erneutes Versenden bleiben für sie gesperrt. Die Aktionen sind an das wirklich
geöffnete Buch gebunden und übernehmen kein anderes Buch nach einem Slotwechsel.
Normale Bücher erhalten keine Briefaktionen.

## Wieder importieren

1. Briefeditor → **Gespeicherte Entwürfe…** → **Briefe als TXT importieren**.
2. Datei auswählen und **Importieren** anklicken, alternativ doppelklicken.
3. Der Inhalt öffnet sich als neuer, vollständig bearbeitbarer Briefentwurf.
   Zum Schreiben wird frisches Pergament benötigt.

Ein vorhandener nichtleerer Entwurf wird zuvor in den gespeicherten Entwürfen
gesichert. Kann diese Sicherung nicht überprüft werden, wird der Import abgebrochen.
Ein beschädigtes TXT verdrängt den aktuellen Entwurf nicht.

Über **Letters-Ordner öffnen** kann man Dateien kopieren oder eigene TXT-Dateien
ablegen. Die Importliste enthält auch die Abschriften aus **`sent/`** und zeigt
deren Unterordner im Dateinamen an. Anschließend **Aktualisieren** anklicken.
Normale UTF-8-Dateien, auch mit
BOM und Windows-Zeilenumbrüchen, werden ebenfalls eingelesen. Maximal 256 KiB,
100 Seiten und 32.767 Zeichen je Quellseite; diese Grenzen schützen den Editor
vor versehentlich importierten großen Dateien.

## Dateiformat

OttoExtra exportiert lesbaren UTF-8-Text mit einer kurzen Kopfzeile für Autor,
Empfänger und Datum. `§`-Codes erhalten Farbe und Schriftstil. Seiten werden durch
einen Formfeed mit umgebenden Zeilenumbrüchen getrennt (`\n\f\n`). Im Text werden
Backslashes verdoppelt und wörtliche Formfeeds als `\f` maskiert. Der Import erkennt
den Versionskopf und nimmt die Maskierung zurück. Normale TXT-Dateien erhalten
keine solche Escape-Auswertung; Formfeeds markieren dort direkte Seitenwechsel.

Die TXT-Datei bewahrt die Originalseiten. Im V2-Editor werden sie als explizite
Quellseiten angelegt; Couriers vorhandene Grenzen und Formatnormalisierung gelten
weiterhin beim erneuten Schreiben. Die Vorschau zeigt das resultierende Buch.

## Regressionstests

- `LetterTextArchiveTest`: Unicode, Farben, leere Seiten, Backslashes/Formfeeds,
  CRLF/BOM, sichere Namen, Kollisionen, defekte UTF-8-Daten und Größenlimits.
- `TXT01`: echter Versand, Zustellung durch Courier, Export aus dem empfangenen
  Buch, Import über das Menü, Sicherung des alten Entwurfs und erneutes V2-Schreiben.
- `TXT02`/`TXT03`: fehlende Berechtigung und fehlendes Geld bewahren die
  Schreibkopie, erzeugen jedoch keine Versandkopie; anderer Brief nach Ablehnung
  und normale Bücher.
- `TXT04`: manuelles `/post`, fremde beziehungsweise fehlende Bestätigung und
  verworfener Verbindungszustand.
- `TXT05`: beschädigte Datei verdrängt keinen Entwurf; ein alter Exportbutton
  übernimmt keinen ausgetauschten Brief.
- `TXT06`: Schreiben ohne Versand sichert sämtliche Buchseiten in `sent/`;
  die Abschrift lässt sich über das bestehende Importmenü wieder öffnen.
- `TXT07`: Ein vom Server abgelehnter Schreibvorgang erzeugt keine Abschrift.
- `I01`/`R01-runtime`: Fortsetzen eines vorhandenen Briefs und Wiederaufnahme nach
  Disconnect sichern die vollständigen Seiten genau einmal nach Abschluss.

Die Fälle laufen über `tools/courier-e2e/run.ps1` mit lokalem Paper-Server,
unveränderter `input/courier-2.1.0.jar` und Fabric-Testclient.
