# OttoExtra 0.1.17

Release vom 13. September 2026. Zusammenfassung der Änderungen seit **0.1.16.1**,
einschließlich der Entwicklungsstände 1.16.3 bis 1.16.3-w und 0.1.17-a bis 0.1.17-q.

## Neue Funktionen

### Briefe

- Überarbeiteter Courier-V2-Editor mit Formatierung, Vorschau und gemeinsamer
  Planung von Arbeits- und Buchseiten.
- Geschriebene Briefe später über **Verschicken…** oder `/ottoextra letter send`
  versenden, auch nach erneutem Login. Das automatische Öffnen der
  Empfängerauswahl nach dem Schreiben ist abschaltbar.
- Automatische lokale TXT-Abschriften nach abgeschlossenem Schreiben und nach
  bestätigtem Versand unter `config/ottoextra/letters/sent`. Auch manuelles
  `/post Spielername` wird berücksichtigt.
- **Brief exportieren** für geöffnete, auch empfangene Briefe. TXT-Dateien lassen
  sich wieder als bearbeitbare Entwürfe importieren; Farben, Stile und Seiten
  bleiben erhalten. Vorhandene Entwürfe werden gesichert.

### Tiere und Höfe

- Tierübersicht mit eigenen und freigegebenen Akten, Modellvorschau und Suche
  nach Name, Hof, Herkunft oder UUID. Löschen verlangt Bestätigung und Rechte.
- Höfe direkt in OttoExtra anzeigen und anlegen, einschließlich leerer Höfe.
  AnimalBreeding und der Züchtermodus sind dafür nicht erforderlich.
- **Hof bereits beim Speichern eines neuen Tiers auswählen.** Die Auswahl bleibt
  ein Entwurf, bis das Tier gespeichert wird. Scheitert die anschließende
  Hofzuordnung, bleibt das Tier registriert und eine Meldung zeigt den Teilerfolg.
- Namenswürfel mit 166 weiblichen und 167 männlichen Grundnamen sowie Vorschlägen
  passend zu Fellfarbe und Zeichnung. Erst Speichern übernimmt den Namen.
- Esel und weitere Nutztiere in der Übersicht. **Alle** zeigt sämtliche
  gespeicherten Tierarten; die Einstellungen steuern zusätzliche Einzelfilter.
- Aufklappbare Hofgruppen und kombinierte Tempo-/Sprungsortierung. Beide Werte
  zählen gleich stark relativ zum Bestwert des jeweiligen Hofes.
- Geschlechtssymbol und Namensfarbe unabhängig einstellbar, auch für lokal
  bekannte, noch nicht registrierte Tiere. Abschaltbarer Cache für Tierart und
  Geschlecht: bis zu 14 Tage und 1.024 Tiere je Serveradresse.
- Ausgewählte Finder-Ziele erhalten eine goldene Umrandung bei aktiver
  Ottonien-Verbindung. Automatische Geschlechtsumrisse bleiben deaktiviert.

### RP, Karte und Bedienung

- Kennenlernen direkt per Klick auf **Unbekannt** im Chat. Abbrechen und
  Bestätigen kehren zum erhaltenen Chatentwurf zurück; gelernte Namen werden
  auch in bestehenden Nachrichten aktualisiert.
- RP-Einstiegsprofile **Komfort**, **Ausgewogen** und **Immersiv** mit anpassbarer
  Vorschau. Überspringbare Startseite für die Versionsreihe 0.1.17.
- Lehensdetails auf der Weltkarte mit Wappen, Führung, Bewohnern, Zugehörigkeit,
  Beschreibungen und Koordinaten. NPC-Dörfer und bespielbare NPC-Lehen zeigen
  ihre vorhandenen Angaben.
- Breite und Höhe der Minimap-Texte per Ziehen einstellen, Schriftgröße
  unabhängig ändern; lange Namen und Zugehörigkeiten können mehrzeilig sein.
- Optionale Kampfmodus-Anzeige neben der Hotbar.
- Eigenständiges Chatprotokoll unter
  `logs/chat/ottoextra-chat-JJJJ-MM-TT-X.log`, mit neuer Datei pro Sitzung und Tag.

## Datenschutz und Einstellungen

- Optionale Onlinedienste, Bilddownloads, Resourcepack-Downloads, Updatechecks
  und das Teilen beobachteter RP-Namen benötigen informierte Freigaben.
- Einrichtung mit **Alles erlauben**, **Alles ablehnen** oder **Einzeln
  entscheiden**. Überspringen erteilt keine Zustimmung; bestehende
  Anzeigeeinstellungen bleiben erhalten. Freigaben lassen sich widerrufen.
- RP-Uploads sind standardmäßig aus und benötigen sowohl Online- als auch
  Upload-Freigabe. Alte Upload-Zustimmungen werden nicht übernommen.
- Chatprotokoll standardmäßig aus, mit eigenem Hinweis zu Inhalt und
  Serverumfang. Vote-Seiten und Kopieren des Accountnamens verlangen eine
  ausdrückliche Bestätigung.
- Begrenzte HTTP-Antworten und Bildcaches; veraltete Antworten nach Widerruf
  oder Verbindungswechsel werden verworfen. Lokale Export-/Löschwerkzeuge und
  verbesserte Sicherung und Wiederherstellung.

## Behobene Fehler

- Eigener RP-Name erscheint bei aktiven Proxy-Identitäten im Chat korrekt.
  Kennenlernen füllt vorhandene Namens- und Titeldaten aus. Personenbuch und
  Bearbeitung behalten die gewählte UUID; ältere Namensbesitzer verdrängen
  keine aktuelle Identität. Verbindungswechsel entfernen alte Vorschläge.
- Titel- und Profilsperren werden nur bewusst gesetzt. Ungesperrte Titel
  können aktualisiert werden, ohne persönliche Namen und Farben zu verlieren.
  **Development** erhält dieselbe Team-Titelfarbe wie **Gamemaster**.
- Gespeicherte Nutztiere verschwinden nicht mehr durch Standardfilter aus
  **Alle**. Die Aktualisierung der Übersicht endet auch beim Wechsel in
  Unterfenster; Such-Glow berücksichtigt ausgewählte unterstützte Tiere.
- Briefempfänger ohne doppelte Account-Einträge. Korrekte Pergamentprüfung bei
  zusätzlichem Vorrat, stabilere Cursorpositionen, Absätze und lange Wörter.
  Alte oder ausgetauschte Bücher werden nicht versehentlich weiterverarbeitet.
- Doppelte OOC-Emote-Anhänge werden anhand des signierten Originaltexts
  korrigiert. Absichtliche Wiederholungen bleiben erhalten. Originalnachrichten
  werden auch bei verzögerter Verarbeitung protokolliert, ohne Doppeleinträge.
- Langchat bleibt an die Verbindung gebunden und erhält den vollständigen
  Eingabeverlauf. Tierabfragen pausieren beim Reiten und verarbeiten Antworten
  ohne doppelte Auswertung.
- Lehenssortierung lädt auch größere signierte API-Grunddaten. Banner bleiben
  nach den ersten 32 Bildern verfügbar; der Cache ist auf 256 Banner und
  32 MiB dekodierte Bilddaten begrenzt. Karte und Aktivitätsanzeige teilen
  dieselben Banner und Cache-Schlüssel.
- Korrekturen an Tablisten-Wappen, Kartenklicks, Texturfreigabe, Kontoverknüpfung,
  Resourcepack-Wiederherstellung, Backups und der Anzeige bei wenig Leben.

## Installation und Kompatibilität

- **Datei:** `ottoextra-0.1.17.jar` – ohne Alpha-/Buchstabenzusatz.
- **Minecraft Java:** 1.21.11; **Java:** mindestens 21.
- **Fabric Loader:** mindestens 0.18.1; gebaut mit 0.19.3.
- **Fabric API:** mindestens 0.141.4+1.21.11.
- Ausschließlich im **Client** installieren. ModMenu, Xaero und Litematica
  bleiben optionale Integrationen. Das vollständige lokale Modpack verwendet
  Java 25; Anforderungen einzelner Zusatzmods gelten zusätzlich.
- Alte OttoExtra-JAR aus dem Mods-Ordner entfernen und die neue JAR einsetzen.
  Konfiguration und Daten vorher sichern. Sources-JAR nicht installieren.

## Prüfstand und bekannte Grenzen

Build- und Laufzeitnachweise für diesen Stand stehen in
[docs/releases/0.1.17.md](https://github.com/Skyfuller1303/OttoExtra/blob/0.1.17/docs/releases/0.1.17.md).
Die neuesten Chat-/Tier-/Hof-Fixes sind durch Regressionstests abgesichert;
eine vollständige Prüfung auf dem produktiven Ottonien-Server steht aus.
Der Finder-Wandtest und die Prüfung technischer Serverkontrolle sind weiterhin
offen. Frühere Alpha-Nachweise gelten nicht automatisch als neue Live-Abnahme.

Die frühere lokale Mouse-Wheelie-Korrektur und Reparaturen fremder
Modpack-JARs sind **nicht** Bestandteil dieser OttoExtra-JAR. Nicht jede
optionale Modkombination wurde geprüft; veraltete Tablisteneinträge nach
Logout sind weiterhin nicht als behoben belegt.
