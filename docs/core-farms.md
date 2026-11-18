# Höfe und Namenswürfel in OttoExtra

Die Tierübersicht ist ein Buch: links stehen die Höfe aus der vorhandenen API
(eigene und freigegebene, einschließlich leerer Höfe) mit Symbol, Tierzahl und
Zahl der zuchtbereiten Tiere, rechts die Tiere des gewählten Hofs. **+ Neuer Hof**
legt über die API einen Hof an; Symbol (12 Formen × 6 Farben) und Notiz des Hofs
bleiben lokal. Ein Klick auf den Hofnamen rechts oben öffnet Symbol und Notiz.
Es gibt keine neue Einstellung: Weder AnimalBreeding noch der Züchtermodus wird
für diese Core-Funktionen benötigt.

Im Tierregister öffnet **anderer Hof** die Hofauswahl auf der rechten Buchseite,
bereits vor der ersten Registrierung. Hof markieren und **Fertig** drücken; Name
und Hof bleiben als Entwurf im Formular. Erst **Speichern** registriert das Tier
und weist anschließend den gewählten Hof zu. Schreibgeschützte Höfe können nicht
ausgewählt werden. Abbrechen speichert keine Tierdaten. Scheitert nur die
Hofzuordnung, bleibt das Tier gespeichert und das Formular zeigt den Teilerfolg an.
Die Notiz eines Tiers wird nur lokal gespeichert, nicht in der API.

**Alle** in der Tierübersicht zeigt sämtliche gespeicherten Tierarten. Die
Artenschalter in den Einstellungen bestimmen die zusätzlichen Einzelfilter.
Der Finder zeichnet ausgewählte unterstützte Tiere wieder mit goldener Umrandung;
automatische Geschlechtsumrisse bleiben unverändert deaktiviert.

Höfe werden nach dem vorhandenen API-Modell dem Besitzer zugeordnet. Sie haben
keine eigene Lehens-ID oder Kartengrenze. Lehenszugehörigkeit, Sichtbarkeit und
Hof-/Tierrechte prüft die bestehende API. Erstregistrierungen von Tieren bleiben
auf das zuständige Lehen des Gefolges beschränkt; fremde Hofnamen werden nicht
übernommen. Die Mod führt keine produktiven Testschreibvorgänge aus.

Die Hofzuweisung übernimmt die gelesene Tierakte mit unveränderten übrigen Feldern
und erhöht nur deren gelesenen Versionszeitpunkt um eins. So wird eine zwischenzeitlich
neuere Serverversion als Konflikt erkannt. Bestätigte Antworten aktualisieren
die Anzeige; alte Antworten nach einem Verbindungswechsel werden verworfen.

Neben dem Tiernamen sitzt ein per Maus, Tab und Enter/Leertaste bedienbarer
Würfel-Button im Schieferstil des Buchs, ohne Tooltip (Vanilla-Tooltips
zeichnen Text mit Schatten). Der Katalog enthält 166 weibliche und 167 männliche
Grundnamen sowie die vorhandenen Vorschläge für Fellfarbe und Zeichnung.
Der bisherige Entwurf wird beim nächsten Würfeln nach Möglichkeit nicht wiederholt;
bereits bekannte Tiernamen werden vermieden, solange freie Vorschläge vorhanden sind.
Erst **Speichern** ändert den registrierten Namen.

Der Core-Katalog liegt unter `assets/ottoextra/names/animals.json`. Der Würfel
wird direkt als kleines Pixelmotiv gezeichnet. Vorhandene AnimalBreeding-Oberflächen
bleiben beim installierten Zusatzmod dessen Zuständigkeit.

## Höfe teilen

Höfe lassen sich wie die Ressourcenlisten von OttoExtra Resources über Codes
teilen (Protokoll: `OttoExtra-Discordbot/web/docs/farm-lists-protokoll.md`,
Dienst `https://ottoextra.dev/api/v1/farm-lists/{Kennung}`). Eine geteilte
Liste enthält einen Kopf (Name, Notiz, Symbol), eine Menge von Höfen (Name,
Notiz, Symbol) und für deren Tiere die Hofzuordnung, die Notiz und eine
Beschriftung (der Registername). Werte der Ottonien-API (Mast, Zucht, Tempo,
Sprung, Leben, Besitzer), Beobachtungen und der lokale API-Schlüssel eines Hofs
werden nie gesendet.

- **Teilen** rechts oben auf der Höfe-Seite legt für den gewählten eigenen Hof eine Liste an oder
  öffnet die vorhandene. Die Teilen-Seite zeigt den Code für die Gruppe (sehen
  und bearbeiten, wie ein Passwort) und den Lesecode für andere (beginnt mit
  `H`), je mit **Kopieren** und **Link** (`https://ottoextra.dev/h/<Code>`).
  Bei eigenen Listen werden weitere eigene Höfe per Chip hinzugefügt oder
  entfernt; ein Hof gehört höchstens zu einer eigenen Liste.
- **Codes erneuern** (zweimal klicken) erzeugt neue Codes, schreibt alles unter
  der neuen Kennung und hinterlässt unter der alten Löschmarken.
  **Nicht mehr teilen** bzw. **Verlassen** (zweimal klicken) entfernt die Liste
  nur auf diesem Gerät.
- Beitreten geht über das Codefeld links oder `/ottoextra tiere beitreten <Code>`.
  Mit dem Lesecode ist die Liste schreibgeschützt; mit dem Gruppencode lassen
  sich Name, Notiz und Symbol der geteilten Höfe ändern (Klick auf den Hofnamen).
- Eigene Listen folgen dem Register und den lokalen Notizen. Änderungen einer
  anderen Person mit dem Gruppencode an Notiz und Symbol eines eigenen Hofs oder
  an Tiernotizen werden ins lokale Tierbuch übernommen.
- Abgleich alle 30 Sekunden je Liste, Änderungen gesammelt in einem PATCH,
  `since` = letzte Serverzeit − 10 s, erste Abfrage einer Sitzung ohne `since`.
  Bei Fehlern wird nach 30 Sekunden erneut versucht. Ohne Zustimmung zu
  Online-Diensten findet kein Kontakt statt. Mit einem Lesecode wird nie gesendet.
- Die Basisadresse steht in `animalInfo.farmShareBaseUrl` (Vorgabe
  `https://ottoextra.dev`); erlaubt ist nur https, http nur für localhost.

## Eigenes Reittier

Für das Tier, auf dem man reitet, erscheint keine Tierinfo
(**Einstellungen → Tierinfo → Eigenes Reittier ausblenden**, Vorgabe an).

Clienttests für Erstellung, Namenskonflikt, schreibgeschützte Höfe, Zuordnung,
Entwurfserhalt, Würfeln ohne Speichern und kombinierte Sortierung liegen unter
`../test-results/core-farms-20260908/`.
