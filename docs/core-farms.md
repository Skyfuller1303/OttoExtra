# Höfe und Namenswürfel in OttoExtra

Die normale Tierübersicht enthält **Höfe…** neben der Sortierung. Das Menü lädt
die eigenen und freigegebenen Höfe aus der vorhandenen API, einschließlich leerer
Höfe. Namen eingeben und **Hof anlegen** wählen. Es gibt keine neue Einstellung:
Weder AnimalBreeding noch der Züchtermodus wird für diese Core-Funktionen benötigt.

Bereits vor der ersten Registrierung öffnet **Hof wählen…** die Hofauswahl.
Hof markieren und **Auswählen** drücken; Name und Hof bleiben als Entwurf im
Tierformular. Erst **Speichern** registriert das Tier und weist anschließend
den gewählten Hof zu. Schreibgeschützte Höfe können nicht ausgewählt werden.
Ein erneuter Klick auf den markierten Hof und **Keine Änderung** entfernt den
Hofentwurf. Abbrechen speichert keine Tierdaten. Scheitert nur die Hofzuordnung,
bleibt das Tier gespeichert und das Formular zeigt den Teilerfolg an.

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
Würfel-Button mit Tooltip. Der Katalog enthält 166 weibliche und 167 männliche
Grundnamen sowie die vorhandenen Vorschläge für Fellfarbe und Zeichnung.
Der bisherige Entwurf wird beim nächsten Würfeln nach Möglichkeit nicht wiederholt;
bereits bekannte Tiernamen werden vermieden, solange freie Vorschläge vorhanden sind.
Erst **Speichern** ändert den registrierten Namen.

Der Core-Katalog liegt unter `assets/ottoextra/names/animals.json`. Der Würfel
wird direkt als kleines Pixelmotiv gezeichnet. Vorhandene AnimalBreeding-Oberflächen
bleiben beim installierten Zusatzmod dessen Zuständigkeit.

Clienttests für Erstellung, Namenskonflikt, schreibgeschützte Höfe, Zuordnung,
Entwurfserhalt, Würfeln ohne Speichern und kombinierte Sortierung liegen unter
`../test-results/core-farms-20260908/`.
