# OttoExtra 0.1.15.4

## Tierinfo ohne Shift-Rechtsklick

Nach kurzem Anvisieren eines Zuchttiers zeigt OttoExtra oben eine schlanke Karte
mit kleinem 3D-Tiermodell, Name und Geschlecht, farblichem Paarungsstatus, Mast
und der verbleibenden Zuchtzeit an. Schafe zeigen zusätzlich „Scherbar“, Kühe „Melkbar“.
Im Tierinfo-Layouteditor lässt sich für jede Tierart ein eigenes Layout speichern.
Alle Bestandteile einschließlich Geschlechtssymbol und „Männlich“/„Weiblich“ sind
verschiebbar, skalierbar und einzeln ausblendbar; die Toast-Größe passt sich automatisch an.
Sprunghöhe und Geschwindigkeit besitzen eigene Layout-Elemente. Nach einer
Rechtsklick-Interaktion mit einem Tier wird der Toast neu geladen. Blockinteraktionen
pausieren automatische Tierabfragen kurz, damit Türen zuverlässig geöffnet bleiben.
Die große,
automatisch angeforderte Tierinfo wird dabei weder im Chat noch im Client-Log
ausgegeben. Eine manuelle Tierinfo per Shift-Rechtsklick bleibt weiterhin sichtbar.

## Minecraft-Konto verbinden

Diese Spielerversion ergänzt die persönliche Konto-Verknüpfung direkt in OttoExtra.

1. Nur `ottoextra-0.1.15.4.jar` installieren.
2. Minecraft starten und eine Welt oder einen Server betreten.
3. `/ottoextra verify` ausführen.
4. Den anklickbaren Link öffnen und mit Discord anmelden.
5. Mit `/ottoextra verify status` den Abschluss prüfen.

Die Verknüpfung erscheint danach unter `https://ottoextra.dev/account`.
`/ottoextra verify unlink` entfernt sie wieder und widerruft den lokalen Device-Token.

`/ottoextra account` öffnet die Kontoübersicht im Spiel. Sie zeigt den
Verbindungsstatus, Discord-Namen, RP-Profil, Stand, Rang, Gefolge und Hellerbestand.

Discord-Zugriffstokens werden nicht im Minecraft-Client gespeichert.
