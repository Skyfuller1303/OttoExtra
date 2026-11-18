# Neue Beschriftungen und Hilfetexte

Dieser Entwurf gibt jeder Einstellung eine neue Beschriftung und einen Hilfetext. Er folgt der Gliederung aus `vorschlag-kategorien.md`. Die Texte liegen zusätzlich maschinenlesbar in `beschriftungen.json` (Config-Schlüssel → Beschriftung, Hilfetext, Stufe, Kategorie, Abschnitt), damit sie später ohne Abtippen in die Sprachdatei übernommen werden können.

279 Einstellungen, davon 270 mit neuer Beschriftung. 185 hatten bisher keinen Hilfetext; jetzt hat jede einen.

Die 36 Hilfetexte, die zunächst nur aus dem Namen der Einstellung abgeleitet waren, sind am 18.11.2026 gegen den Quelltext geprüft und berichtigt worden. Die übrigen beruhen auf den bisherigen Hilfetexten, den Kartenbeschreibungen und der README.

## Auffälligkeiten aus der Prüfung

Die Texte beschreiben das Verhalten, wie es im Quelltext steht. Dabei sind Stellen aufgefallen, an denen eine Einstellung anders wirkt, als ihr Name erwarten lässt:

1. **`regions.enabled` schaltet die Einblendung beim Betreten nicht ab.** Der Schalter wird nur von der Aktivitäts-Anzeige und der Sortierung der Tabliste gelesen. Einblendung und Ausblenden der Servermeldung laufen unabhängig davon.
2. **`discord.feedback` (Addon Discord) hat keine Wirkung.** Das Feld wird außerhalb der Einstellungen nirgends gelesen; die Meldungen erscheinen immer.
3. **Mehrere Hauptschalter wirken erst nach einem Neustart:** `map.enabled` und `letter.enabled` in beide Richtungen; `chat.enabled`, `rpnames.enabled` und `resourcepack.enabled` beim Einschalten. Der Bildschirm wies bisher nicht darauf hin; die Hilfetexte tun es jetzt.
4. **`rpnames.unknownShowAccount` ersetzt den Platzhalter** in Chat und Tabliste, statt den Accountnamen zusätzlich zu zeigen, und ist dort wirkungslos, sobald „Unbekannte kennenlernen“ an ist.
5. **`regions.showCoordinates` zeigt die Position des Spielers,** nicht die des Lehens.
6. **Maße und Schriftgrößen der Einblendung gelten nur im Stil „Klassisch“.** `regions.maxTextWidth` umbricht nur den Namen des Lehens.
7. **`farmer.feedAssistant` füllt die Schnellleiste nach** und steuert nicht den Blick. Die Pflanzhilfe dreht den Blick und pflanzt selbst.
8. **`translate.refreshBundle` tut ohne Rückmeldung nichts,** wenn die Übersetzung der Servertexte aus ist, die Zustimmung fehlt oder nicht von Deutsch nach Englisch übersetzt wird.
9. **Vote-Erinnerung:** Ist das Addon Vote installiert, bleibt die eingebaute Erinnerung inaktiv (`VoteReminderFeature`). Der neue Bildschirm zeigt dann nur die Einstellungen des Addons.
10. **Die Formatierungsleiste des Brief-Editors steht links;** der bisherige Text nannte sie „rechts“.

## Regeln für die Texte

Beschriftungen:

1. **Sagen, was passiert, wenn der Schalter an ist.** „Ton beim Betreten“ statt „Betreten-Sound“. Bei einem Schalter soll „An“ hinter der Beschriftung einen sinnvollen Satz ergeben.
2. **Kein Wort „Modul“.** Der Hauptschalter nennt die Funktion: „Einblendung beim Betreten eines Lehens“ statt „Regionen-Modul“.
3. **Einheit in Klammern am Ende:** (ms), (Sekunden), (Pixel), (Blöcke), (Grad), (Prozent).
4. **Keine Angabe des Formats in der Beschriftung.** „(#RRGGBB)“ entfällt; das Farbfeld zeigt das Format selbst.
5. **Innerhalb eines Abschnitts nicht wiederholen, was die Überschrift sagt.** Unter „Inhalt der Einblendung“ heißt der Eintrag „Wappen“, nicht „Wappen im Toast“.
6. **Schaltflächen** nennen links die Sache und auf der Schaltfläche die Handlung: „Personenbuch“ – „Öffnen…“.
7. **Höchstens etwa 50 Zeichen.** Längere Erklärungen gehören in den Hilfetext.

Hilfetexte:

1. **Jede Einstellung hat einen.** Ein bis drei kurze Sätze, Anrede „du“.
2. **Erst die Wirkung, dann Besonderheiten:** Voraussetzungen, Ausnahmen, wann man den Wert ändern sollte.
3. **Bei Zahlen ein Anhaltspunkt:** „1,0 ist die übliche Größe“, „1000 ms = 1 Sekunde“.
4. **Kein „Standard: an“ im Text.** Den Standardwert zeigt die Schaltfläche „Zurücksetzen“; der Bildschirm kann ihn zusätzlich neben dem Feld anzeigen.
5. **Bei „Aus“ sagen, was stattdessen gilt,** wenn das nicht offensichtlich ist.

## Ersetzte Begriffe

| Bisher | Neu | Anmerkung |
|---|---|---|
| Toast, Badge, Banner-Einblendung | Einblendung | einheitlich für die Meldung beim Betreten eines Lehens und für die Tierinfo |
| HUD | Anzeige | „Aktivitäts-Anzeige“, „Lehen-Anzeige“, „Skript-Anzeige“ |
| Wappen-HUD | Lehen-Anzeige an der Minimap | sie zeigt Wappen, Name, Stand und Gefolge |
| Overlay | Ergänzung, oder ganz weggelassen | „Grenzen der Lehen“ statt „Minimap-Overlays“ |
| Actionbar | Meldung des Servers; Textzeile über der Schnellleiste | |
| Hotbar | Schnellleiste | |
| Nametag | Namensschild | |
| Glow, umranden | hervorheben | „Tiere nach Geschlecht hervorheben“ |
| Blur, Edge-Blur | Unschärfe, unscharfer Bildrand | |
| FOV | Sichtfeld | |
| Health | Leben, Lebenspunkte | |
| Theme | Farbschema | |
| Cache | gemerkte Daten, Zwischenspeicher | |
| Timeout | Wartezeit | |
| Delay | Pause | |
| Join | Betreten des Servers | |
| Shift | Umschalt | |
| Manifest-/Release-URL, API-Endpunkt | Download-Adresse, Adresse | |
| Asset-Name | Dateiname des Packs | |
| Drag&Drop | mit der Maus verschieben | |
| Auslöse-Item | Schreibzeug | |
| Friendly Fire | Schläge auf Verbündete | |
| Tactical Operation Mode | Kampfmodus für Gefechte | |
| Modul | entfällt | |

Beibehalten und im Hilfetext erklärt: **OOC** (Begriff des Servers, „außerhalb der Rolle gesprochen“), **Emote** (Handlung zwischen Sternen), **RP-Name**, **Accountname**, **Lehen**, **Gefolge**, **Tabliste**, **NPC**, **Resourcepack**, **Litematica**.

Zwei Wörter für dasselbe gibt es bisher bei Spielername / Accountname und bei Region / Lehen. Neu einheitlich: **Accountname** und **Lehen**.

## Auswahlwerte

Auswahlfelder zeigen künftig Text statt des Werts aus dem Code.

| Einstellung | Wert im Code | Anzeige bisher | Anzeige neu |
|---|---|---|---|
| Sichtbarkeit der Namensschilder | `NORMAL` | NORMAL | Immer |
| Sichtbarkeit der Namensschilder | `REALISTIC` | REALISTIC | Nur bei freier Sicht |
| Sichtbarkeit der Namensschilder | `HIDE_ALL` | HIDE_ALL | Nie |
| Platzhalter für Unbekannte | `Unbekannt` | Unbekannt | Unbekannt |
| Platzhalter für Unbekannte | `???` | ??? | ??? |
| Platz auf dem Bildschirm | `TOP_CENTER` | TOP_CENTER | Oben Mitte |
| Platz auf dem Bildschirm | `TOP_RIGHT` | TOP_RIGHT | Oben rechts |
| Platz auf dem Bildschirm | `TOP_LEFT` | TOP_LEFT | Oben links |
| Platz auf dem Bildschirm | `CENTER` | CENTER | Bildschirmmitte |
| Stil der Einblendung | `classic` | classic | Klassisch (Kasten) |
| Stil der Einblendung | `zonentitel` | zonentitel | Zonentitel |
| Stil der Einblendung | `banderole` | banderole | Banderole |
| Stil der Einblendung | `plakette` | plakette | Plakette |
| Stil der Einblendung | `minimal` | minimal | Schlicht |
| Platz auf dem Bildschirm | `MIDDLE_LEFT` | Mitte links | Mitte links |
| Platz auf dem Bildschirm | `TOP_LEFT` | Oben links | Oben links |
| Platz auf dem Bildschirm | `BOTTOM_LEFT` | Unten links | Unten links |
| Platz auf dem Bildschirm | `MIDDLE_RIGHT` | Mitte rechts | Mitte rechts |
| Platz auf dem Bildschirm | `TOP_RIGHT` | Oben rechts | Oben rechts |
| Platz auf dem Bildschirm | `BOTTOM_RIGHT` | Unten rechts | Unten rechts |
| Platz auf dem Bildschirm | `CUSTOM` | Frei | Frei |
| Art des Versands | `PAGE` | PAGE | Seite für Seite |
| Art des Versands | `LEGACY` | LEGACY | Zeile für Zeile |
| Art des Versands | `V2` | V2 | Genaue Nachbildung |
| Darstellung | `compact` | Kompakt | Kompakt |
| Darstellung | `detailed` | Ausführlich | Ausführlich |
| Darstellung | `custom` | Eigenes Layout | Eigenes Layout |
| Mitschreiben auf | `OTTONIEN` | OTTONIEN | Nur Ottonien |
| Mitschreiben auf | `ALL` | ALL | Allen Servern und im Einzelspieler |
| Mein Original für andere Nutzer freigeben | `OFF` | OFF | Niemand |
| Mein Original für andere Nutzer freigeben | `ALL` | ALL | Alle bestätigten Nutzer |
| Mein Original für andere Nutzer freigeben | `ALLOWLIST` | ALLOWLIST | Nur ausgewählte Spieler |
| Sprachen der Übersetzung (vier Einstellungen) | `de` | de | Deutsch |
| Sprachen der Übersetzung (vier Einstellungen) | `en` | en | Englisch |
| Sprachen der Übersetzung (vier Einstellungen) | `da` | da | Dänisch |
| Sprachen der Übersetzung (vier Einstellungen) | `sv` | sv | Schwedisch |
| Sprachen der Übersetzung (vier Einstellungen) | `pl` | pl | Polnisch |
| Sprachen der Übersetzung (vier Einstellungen) | `fr` | fr | Französisch |
| Sprachen der Übersetzung (vier Einstellungen) | `es` | es | Spanisch |

Bei „Art des Versands“ ändert sich auch die Reihenfolge: zuerst der Standard „Seite für Seite“.

## Noch nicht enthalten

- Texte der Unterbildschirme (Chatkanal-Farben, Gefolge, Farbschema-Editor, Editoren zum Verschieben). Dort sind unter anderem die acht Farbfelder des Farbschema-Editors noch mit englischen Feldnamen beschriftet.
- Englische Fassung (`en_us.json`).
- Namen der Tasten in Minecrafts Steuerung.
- Texte der Abschnitte „Backups“ (bleiben unverändert).

## Alle Einstellungen

Spalte „Neu“: neue Beschriftung, bei Schaltflächen und Auswahlfeldern mit dem Text auf dem Bedienelement.

### 1. Allgemein

Beschreibung der Kategorie: Dinge, die die ganze Mod betreffen.

**Abschnitt „Anzeigen auf dem Bildschirm anordnen“** – Ein Einstieg für alle verschiebbaren Anzeigen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| HUD anordnen | Anzeigen auf dem Bildschirm anordnen<br>Schaltfläche: „Anordnen…“ | Öffnet einen Bildschirm, in dem du Minimap, Lehen-Anzeige, Aktivitäts-Anzeige und Anzeigen von Addons mit der Maus verschiebst und in der Größe änderst. Geht auch mit dem Befehl /ottoextra hud. | Einfach |

**Abschnitt „Server-Resourcepack“** – Lädt das Ottonien-Resourcepack und hält es aktuell.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Resourcepack-Download | Ottonien-Resourcepack automatisch laden | Lädt das Resourcepack des Servers herunter und hält es aktuell. Braucht zusätzlich die Erlaubnis unter „Datenschutz & Sicherung“. Einschalten wirkt erst nach einem Neustart des Spiels. | Einfach |
| Pack auto-aktivieren | Resourcepack nach dem ersten Download einschalten | Schaltet das Pack nach der ersten Installation in Minecraft ein. Ein Pack, das du selbst ausgeschaltet hast, bleibt aus. | Einfach |
| Beim Start prüfen | Bei jedem Spielstart nach Updates suchen | Prüft beim Start, ob es eine neuere Fassung des Packs gibt. Ein in Minecraft ausgeschaltetes Pack bleibt auch nach einem Update aus. Wieder einschalten: Minecraft → Ressourcenpakete. | Einfach |
| Manifest-/Release-URL | Download-Adresse | Internetadresse, unter der die Mod die aktuelle Fassung des Packs findet. Nur ändern, wenn die Serverleitung eine neue Adresse nennt. | Experte |
| Asset-Name | Dateiname des Packs | Name der Datei, die von der Download-Adresse geladen wird. | Experte |
| Connect-Timeout (ms) | Wartezeit für den Verbindungsaufbau (ms) | Wie lange die Mod auf eine Verbindung wartet, bevor sie abbricht. 1000 ms = 1 Sekunde. | Experte |
| Request-Timeout (ms) | Wartezeit für den Download (ms) | Wie lange ein Download höchstens dauern darf. Bei langsamer Leitung erhöhen. 1000 ms = 1 Sekunde. | Experte |

**Abschnitt „Vote-Erinnerung“** – Erinnert einmal am Tag ans Voten.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Vote-Reminder aktivieren | Ans Voten erinnern | Zeigt einmal am Tag eine Erinnerung. Beim ersten Mal fragt ein Dialog, ob die Vote-Seiten geöffnet und dein Accountname in die Zwischenablage kopiert werden dürfen. | Einfach |
| Wiederholung (Minuten) | Erneut erinnern nach (Minuten) | Solange du an diesem Tag noch nicht gevotet hast, kommt die Erinnerung in diesem Abstand wieder. | Mehr |
| Anzeigedauer (Sekunden) | Erinnerung sichtbar für (Sekunden) | So lange bleibt die Erinnerung auf dem Bildschirm. | Mehr |
| Resetstunde (Europe/Berlin) | Neuer Vote-Tag beginnt um (Uhr) | Ab dieser Stunde deutscher Zeit gilt ein neuer Tag und die Erinnerung beginnt von vorn. | Experte |
| Verzögerung nach Join (Sekunden) | Wartezeit nach dem Betreten (Sekunden) | So lange wartet die erste Erinnerung, nachdem du den Server betreten hast. | Experte |
| Vote-Reminder aktivieren | Ans Voten erinnern | Zeigt einmal am Tag eine Erinnerung. Beim ersten Mal fragt ein Dialog, ob die Vote-Seiten geöffnet und dein Accountname in die Zwischenablage kopiert werden dürfen. | Einfach |
| Wiederholung (Minuten) | Erneut erinnern nach (Minuten) | Solange du an diesem Tag noch nicht gevotet hast, kommt die Erinnerung in diesem Abstand wieder. | Mehr |
| Anzeigedauer (Sekunden) | Erinnerung sichtbar für (Sekunden) | So lange bleibt die Erinnerung auf dem Bildschirm. | Mehr |
| Resetstunde (Europe/Berlin) | Neuer Vote-Tag beginnt um (Uhr) | Ab dieser Stunde deutscher Zeit gilt ein neuer Tag und die Erinnerung beginnt von vorn. | Experte |
| Verzögerung nach Join (Sekunden) | Wartezeit nach dem Betreten (Sekunden) | So lange wartet die erste Erinnerung, nachdem du den Server betreten hast. | Experte |

### 2. Chat

Beschreibung der Kategorie: Alles rund ums Schreiben und Lesen im Chat.

**Abschnitt „Kanäle“** – Kanalanzeige neben der Eingabe und Kanalwechsel.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Chat-Modul | Kanal-Anzeige und Chat-Hilfen | Zeigt links neben der Chat-Eingabe den aktiven Kanal. Ein Klick wechselt den Kanal, Umschalt+Klick wechselt zu Offtopic und Hilfe. Hauptschalter für alle Chat-Funktionen dieser Seite. Einschalten wirkt erst nach einem Neustart des Spiels vollständig. | Einfach |
| Beim Join automatisch /s (Sprechen) | Beim Betreten in den Kanal „Sprechen“ wechseln | Sendet beim Betreten automatisch /s, damit du nicht versehentlich in einem anderen Kanal schreibst. | Einfach |
| Shift+Tab wechselt Kanal | Kanal mit Umschalt+Tab wechseln | Im geöffneten Chat wechselt Umschalt+Tab den Kanal. Aus: Die Tastenkombination vervollständigt wie in Minecraft üblich Spielernamen. | Einfach |

**Abschnitt „Offtopic-Warnung“** – Warnzeichen vor dem öffentlichen Kanal.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Offtopic-Warnzeichen (!) | Warnzeichen vor Offtopic anzeigen | Blinkende Ausrufezeichen vor [Offtopic] erinnern daran, dass dieser Kanal öffentlich und außerhalb des Rollenspiels ist. | Einfach |
| Anzahl ! | Anzahl der Ausrufezeichen | Ein bis drei Ausrufezeichen. | Mehr |

**Abschnitt „Lange Nachrichten“** – Lange Texte werden automatisch in mehrere Nachrichten geteilt.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Lange Nachrichten splitten | Lange Nachrichten automatisch aufteilen | Du kannst beliebig lang tippen. Beim Senden wird der Text an Wortgrenzen geteilt und in mehreren Nachrichten kurz nacheinander verschickt. | Einfach |
| Fortsetzungs-Marker | Zeichen am Ende eines Teils | Wird an jeden Teil angehängt, auf den noch ein weiterer folgt. So erkennen Leser, dass die Nachricht weitergeht. | Mehr |
| Max. Zeichen je Teil | Zeichen je Teilnachricht (höchstens) | Länge eines Teils. Mehr als 256 Zeichen je Nachricht sind nicht möglich. | Experte |
| Max. Tipplänge | Zeichen in der Eingabe (höchstens) | Wie viel Text du insgesamt in die Chatzeile tippen kannst. | Experte |
| Absende-Intervall (ms) | Pause zwischen den Teilen (ms) | Wartezeit zwischen zwei Teilnachrichten. 1000 ms = 1 Sekunde. | Experte |

**Abschnitt „Farben & Hervorhebung“** – Farben der Kanäle, von Emotes und von OOC-Text.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Emotes und OOC farblich darstellen | Emotes und OOC-Text hervorheben | Text zwischen \*Sternen\* (Handlungen deiner Figur) und Text in (runden Klammern) (außerhalb der Rolle gesprochen, kurz OOC) bekommt eine eigene Farbe. Diese Hervorhebung hat Vorrang vor der Nachrichtenfarbe des Kanals. | Einfach |
| Chatkanal-Farben verwalten… | Farben der Chatkanäle<br>Schaltfläche: „Farben wählen…“ | Öffnet eine Übersicht, in der du für jeden Kanal die Farbe des Kanalnamens und der Nachrichten festlegst, außerdem die Farben für Texte von NPCs. | Einfach |
| Farbe: \*Emotes\* | Farbe für \*Emotes\* | Farbe für Text zwischen \*Sternen\*. | Mehr |
| \*Emotes\* kursiv | \*Emotes\* schräg stellen | Stellt Text zwischen \*Sternen\* zusätzlich zur Farbe kursiv dar. | Mehr |
| Farbe: (OOC-Kommentare) | Farbe für (OOC-Text) | Farbe für Text in (runden Klammern). | Mehr |
| (OOC) kursiv | (OOC-Text) schräg stellen | Stellt Text in (runden Klammern) zusätzlich zur Farbe kursiv dar. | Mehr |

**Abschnitt „Übersetzung“** – Addon TranslateUtils.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Eigene Nachrichten übersetzen | Meine Nachrichten übersetzen | Übersetzt, was du schreibst, vor dem Senden. Die Taste für die Übersetzung schaltet diese Einstellung um. | Einfach |
| Serveranzeigen übersetzen | Texte des Servers übersetzen | Übersetzt auf Ottonien, was du liest: Chat, Meldungen, Titel, Bossleisten, Tabliste, Inventartitel sowie Namen und Beschreibungen von Gegenständen. | Einfach |
| Datenschutzhinweis | Datenschutzhinweis zur Übersetzung<br>Schaltfläche: „Anzeigen…“ | Zeigt, welche Texte zum Übersetzen an den Übersetzungsdienst gesendet werden. Hast du noch nicht zugestimmt, kannst du dort zustimmen oder ablehnen. | Einfach |
| Ausgehende Eingabesprache | Ich schreibe auf<br>Auswahl: „Deutsch“, „Englisch“, „Dänisch“, „Schwedisch“, „Polnisch“, „Französisch“, „Spanisch“ | Die Sprache, in der du tippst. | Einfach |
| Ausgehende Zielsprache | Andere lesen auf<br>Auswahl: „Deutsch“, „Englisch“, „Dänisch“, „Schwedisch“, „Polnisch“, „Französisch“, „Spanisch“ | Die Sprache, in die deine Nachrichten übersetzt werden. | Einfach |
| Server-Eingabesprache | Der Server schreibt auf<br>Auswahl: „Deutsch“, „Englisch“, „Dänisch“, „Schwedisch“, „Polnisch“, „Französisch“, „Spanisch“ | Die Sprache der Texte, die vom Server kommen. | Einfach |
| Anzeige-Zielsprache | Ich lese auf<br>Auswahl: „Deutsch“, „Englisch“, „Dänisch“, „Schwedisch“, „Polnisch“, „Französisch“, „Spanisch“ | Die Sprache, in die Texte des Servers für dich übersetzt werden. | Einfach |
| Sprachkürzel an der Eingabe anzeigen | Sprachanzeige an der Chat-Eingabe | Zeigt rechts an der Eingabe zum Beispiel [DE>SV], grün bei eingeschalteter und rot bei ausgeschalteter Übersetzung. Ein Klick schaltet um. Wird nicht mitgesendet. | Mehr |
| Original als OOC-Nachricht senden | Original zusätzlich als OOC-Nachricht senden | Sendet deinen ursprünglichen Text direkt nach der Übersetzung als eigene Nachricht in (Klammern). Alle können sie lesen. | Mehr |
| Statusmeldung beim Umschalten | Meldung beim Ein- und Ausschalten | Zeigt eine kurze Meldung, wenn du die Übersetzung umschaltest. | Mehr |
| Emotes \*...\* schützen | \*Emotes\* nicht übersetzen | Text zwischen \*Sternen\* bleibt in der Sprache, in der du ihn geschrieben hast. | Mehr |
| OOC-Passagen (...) schützen | (OOC-Text) nicht übersetzen | Text in (runden Klammern) bleibt unverändert. Aus: Auch OOC-Text wird übersetzt. | Mehr |
| URLs schützen | Internetadressen nicht übersetzen | Links bleiben unverändert. | Mehr |
| Eckige Tags schützen | [Kennzeichen] nicht übersetzen | Kurze Kennzeichen in eckigen Klammern wie [OOC] oder [RP] bleiben unverändert. | Mehr |
| Original geschützt per Hover | Mein Original für andere Nutzer freigeben<br>Auswahl: „Niemand“, „Alle bestätigten Nutzer“, „Nur ausgewählte Spieler“ | Andere Nutzer der Mod sehen dein Original, wenn sie mit der Maus über die Übersetzung fahren. | Experte |
| Erlaubte Usernamen | Ausgewählte Spieler<br>Schaltfläche: „Bearbeiten…“ | Liste der Minecraft-Namen, die dein Original sehen dürfen. Gilt bei der Auswahl „Nur ausgewählte Spieler“. | Experte |
| Freigegebenes Original als Chatzeile | Freigegebene Originale als eigene Zeile zeigen | Zeigt Originaltexte, die andere für dich freigegeben haben, als eigene Zeile unter der Übersetzung. Aus: Sie erscheinen nur, wenn du mit der Maus darüberfährst. Das siehst nur du. | Experte |
| Lokaler Übersetzungscache | Gespeicherte Übersetzungen löschen<br>Schaltfläche: „Löschen“ | Leert den Zwischenspeicher auf deinem Rechner. Danach werden Texte neu übersetzt. Das Übersetzungspaket bleibt erhalten. | Experte |
| Freigegebenes Übersetzungsbundle | Übersetzungspaket neu laden<br>Schaltfläche: „Neu laden“ | Lädt die fertigen, freigegebenen Übersetzungen von Deutsch nach Englisch neu herunter. Wirkt nur, wenn „Texte des Servers übersetzen“ an ist, du dem Datenschutzhinweis zugestimmt hast und von Deutsch nach Englisch übersetzt wird. | Experte |
| API-URL | Adresse des Übersetzungsdienstes | Der Dienst muss mit LibreTranslate verträglich sein. Bei einem Fehler wird immer dein Original gesendet. | Experte |
| API-Key | Zugangsschlüssel | Nur nötig, wenn der Übersetzungsdienst einen verlangt. | Experte |
| Maximale Zeichen | Längste übersetzte Nachricht (Zeichen) | Längere Nachrichten werden nicht übersetzt und unverändert gesendet oder angezeigt. | Experte |
| Timeout (ms) | Wartezeit auf die Übersetzung (ms) | Antwortet der Dienst nicht rechtzeitig, wird dein Original gesendet. 1000 ms = 1 Sekunde. | Experte |

**Abschnitt „Discord-Brücke“** – Addon Discord.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Discord-Brücke aktiv | Chat mit Discord verbinden | Hauptschalter der Discord-Brücke. | Einfach |
| Eigene Chatnachrichten übertragen | Meine Chatnachrichten an Discord senden | Überträgt, was du im Spiel schreibst, an Discord. | Einfach |
| Verbindungsstatus übertragen | Online-Status an Discord melden | Meldet der Brücke, wenn du einen Server betrittst oder verlässt, und alle 30 Sekunden, dass du online bist. Gilt auf jedem Server, nicht nur auf Ottonien. | Einfach |
| Statusmeldungen anzeigen | Meldungen der Brücke anzeigen | Hat derzeit keine Wirkung: Die Meldungen der Brücke erscheinen immer im Chat. | Mehr |
| API-Endpunkt | Adresse der Discord-Brücke | Internetadresse ohne /api/v1 am Ende. Nur ändern, wenn du eine eigene Brücke betreibst. | Experte |
| Pollintervall (Sekunden) | Neue Nachrichten abholen alle (Sekunden) | In diesem Abstand holt die Mod neue Nachrichten aus Discord ab und zeigt sie im Chat. | Experte |

**Abschnitt „Skripte“** – Addon Skript. Zuordnung zu „Chat“ ist eine Annahme und zu prüfen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| OttoSkript aktiviert | Skripte verwenden | Aus: Anzeige, Knopf im Chat, Tasten und automatisches Senden sind abgeschaltet. Deine Skripte und ihr Fortschritt bleiben gespeichert. | Einfach |
| Bibliothek öffnen | Skript-Bibliothek<br>Schaltfläche: „Öffnen…“ | Vorbereitete Texte für Messen, Reden und Ankündigungen ansehen und bearbeiten. | Einfach |
| Skriptbutton im Chat anzeigen | Skript-Knopf an der Chat-Eingabe | Zeigt rechts an der Chat-Eingabe einen Knopf „S“, mit dem du ein Skript auswählst. | Einfach |
| HUD positionieren | Skript-Anzeige verschieben<br>Schaltfläche: „Verschieben…“ | Legt fest, wo die Skript-Anzeige auf dem Bildschirm steht. | Mehr |

### 3. Namen & Personen

Beschreibung der Kategorie: Wie andere Spieler heißen und wo ihr Name erscheint.

**Abschnitt „Personenbuch“** – Bekannte Personen ansehen und verwalten.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Personen verwalten… | Personenbuch<br>Schaltfläche: „Öffnen…“ | Dein Verzeichnis aller Personen, die deine Figur kennt. Hier siehst und änderst du RP-Namen, Titel und Farben. | Einfach |

**Abschnitt „Namen im Chat“** – RP-Namen statt Accountnamen im Chat.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| RP-Namen-Modul | RP-Namen statt Accountnamen anzeigen | Hauptschalter für diese Seite: RP-Namen im Chat, an Namensschildern und in der Tabliste, Kennenlernen und Untersuchen. Einschalten wirkt erst nach einem Neustart des Spiels. | Einfach |
| RP-Namen in OOC-Chats (Offtopic/Hilfe) | RP-Namen auch in Offtopic und Hilfe | Aus: In den Kanälen außerhalb des Rollenspiels stehen Accountnamen. | Einfach |
| Shift-Klick öffnet Personenbuch | Personenbuch per Umschalt+Klick öffnen | Umschalt+Linksklick auf einen Namen im Chat oder Umschalt+Rechtsklick auf einen Spieler öffnet dessen Eintrag. Im Kampf ist der Rechtsklick gesperrt. | Mehr |

**Abschnitt „Unbekannte Personen“** – Was bei Personen steht, die die eigene Figur noch nicht kennt.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Unbekannte als „Unbekannt“ zeigen | Unbekannte Personen verbergen | Wen deine Figur noch nicht kennt, erscheint mit einem Platzhalter statt mit seinem Namen. | Einfach |
| Platzhalter | Platzhalter für Unbekannte<br>Auswahl: „Unbekannt“, „???“ | Der Text, der anstelle des Namens steht. | Mehr |
| Unbekannte: Accountname zeigen | Accountnamen statt Platzhalter zeigen | Unbekannte Personen erscheinen im Chat und in der Tabliste mit ihrem Accountnamen statt mit dem Platzhalter. Am Namensschild bleibt der Platzhalter; der Accountname steht dort als zusätzliche Zeile, wenn „Accountnamen anzeigen“ an ist. Ist „Unbekannte kennenlernen“ an, bleibt auch in Chat und Tabliste der Platzhalter. | Mehr |

**Abschnitt „Namen über dem Kopf“** – Namensschilder über anderen Spielern.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Namensschild-Modul | Namensschilder mit Titel und RP-Namen | Zeigt über Spielern Titel und RP-Namen. Braucht eingeschaltete RP-Namen. Aus: Minecrafts übliche Namensschilder; die Sichtbarkeit darunter gilt dann nicht. | Einfach |
| Namensschilder | Sichtbarkeit der Namensschilder<br>Auswahl: „Immer“, „Nur bei freier Sicht“, „Nie“ | „Nur bei freier Sicht“ zeigt ein Schild nur, wenn du die Person wirklich sehen kannst, also nicht durch Wände. Umschalten geht auch mit der Taste „Namensschild-Modus wechseln“ (N); die Taste gibt es nur, wenn die Namensschilder beim Spielstart eingeschaltet waren. | Einfach |
| Titel anzeigen | Titel anzeigen | Zeigt den Titel der Figur am Namensschild. | Einfach |
| RP-Name am Schild | RP-Namen anzeigen | Zeigt den RP-Namen der Figur am Namensschild. | Einfach |
| Accountname am Schild | Accountnamen anzeigen | Zeigt den Minecraft-Accountnamen als zusätzliche Zeile. | Einfach |
| Titel-Schriftgröße | Schriftgröße des Titels | 1,0 ist die übliche Größe. | Mehr |
| Name-Schriftgröße | Schriftgröße des RP-Namens | 1,0 ist die übliche Größe. | Mehr |
| Account-Schriftgröße | Schriftgröße des Accountnamens | 1,0 ist die übliche Größe. | Mehr |
| Account-Farbe (#RRGGBB) | Farbe des Accountnamens | Farbe der Zeile mit dem Accountnamen. | Mehr |
| Zeilenabstand | Abstand zwischen den Zeilen | Größerer Wert = mehr Abstand zwischen Titel, RP-Name und Accountname. | Experte |

**Abschnitt „Namen in der Tabliste“** – Die Spielerliste auf der Tab-Taste.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| RP-Namen in Tabliste | RP-Namen in der Tabliste | Zeigt in der Spielerliste (Tab-Taste) RP-Namen statt Accountnamen. | Einfach |
| Titel in Tabliste anzeigen | Titel in der Tabliste | Zeigt vorhandene Titel bei allen Spielern, auch bei unbekannten. Chat und Namensschilder bleiben davon unberührt. | Einfach |
| Titel automatisch aktuell halten | Titel automatisch aktuell halten | Übernimmt regelmäßig den aktuellen Titel vom Server in dein Personenbuch. | Mehr |
| Tabliste nach Lehen sortieren | Tabliste nach Lehen ordnen | Gruppiert Spieler nach ihrem Lehen. Innerhalb eines Lehens steht zuerst die Leitung, dann folgen Rang und Name. | Mehr |
| Gegner zuerst | Gegner in der Tabliste zuerst | An: Gegner, Verbündete, Neutrale. Aus: Verbündete, Gegner, Neutrale. Gilt im Kampfmodus des Addons. | Mehr |

**Abschnitt „Namensfarben“** – Eigene Farben für RP-Namen und Accountnamen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| RP-Name-Farbe | Farbe aller RP-Namen | Leer = übliche Farbe. Im Personenbuch kannst du für einzelne Titel und Personen eine andere Farbe festlegen. | Mehr |
| Spielername-Farbe | Farbe aller Accountnamen | Leer = übliche Farbe. | Mehr |
| Adelsfarbe auch für andere Titel | Adelige immer in Adelsfarbe | Aus: Die Farbe richtet sich nach dem angezeigten Titel. An: Bei adeligen Personen färbt der Adelsstand auch ihre anderen Titel. Feste Titelfarben und eigene Farben haben Vorrang. | Mehr |

**Abschnitt „Kennenlernen“** – Dialog und Markierung, wenn man jemanden zum ersten Mal trifft.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Namen kennenlernen (proaktiv) | Unbekannte kennenlernen | Unbekannte Sprecher bekommen ein „!“ über dem Kopf. Linksklick auf „Unbekannt“ im Chat oder Umschalt+Rechtsklick auf den Spieler öffnet das Kennenlernfenster. Im Kampf bleibt es geschlossen. | Einfach |
| Leuchten (Fullbright) | Ausrufezeichen leuchtet im Dunkeln | Das Ausrufezeichen ist unabhängig vom Licht immer gut sichtbar. | Mehr |
| Höhe über Kopf | Höhe über dem Kopf (Blöcke) | Abstand des Ausrufezeichens zum Kopf. | Experte |
| Größe | Größe des Ausrufezeichens | 1,0 ist die übliche Größe. | Experte |
| Drehtempo (Y-Achse) | Drehgeschwindigkeit | 0 = steht still. 1,0 ist die übliche Geschwindigkeit. | Experte |

**Abschnitt „Personen untersuchen“** – Genaues Ansehen einer Person mit der Untersuchen-Taste.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| RP-Untersuchungsmodus aktivieren | Personen untersuchen | Mit der Taste „Untersuchen“ (Alt links) musterst du eine Person genauer. Aus: Die Taste hat keine Wirkung. | Einfach |
| Sichtbare Gegenstände in den Händen | Gegenstände in den Händen zeigen | Zeigt nach dem Untersuchen, was die Person sichtbar in den Händen hält. Verborgene Werte werden nicht gelesen. | Mehr |
| Sichtbare Rüstung | Rüstung zeigen | Zeigt nach dem Untersuchen die sichtbaren Rüstungsteile der Person. | Mehr |
| Sanfter Zoom | Beim Untersuchen heranzoomen | Das Bild rückt sanft näher an die Person. | Mehr |
| Rand leicht weichzeichnen | Bildrand weichzeichnen | Der Rand des Bildes wird beim Untersuchen leicht unscharf. | Mehr |
| Wechselnde RP-Gedanken | Gedanken der Figur einblenden | Zeigt beim Untersuchen einen zufälligen Satz aus Sicht deiner Figur. | Mehr |
| Zoomstärke | Stärke des Zooms (Grad) | Größerer Wert = näher heran. Zoom-Tasten anderer Mods funktionieren weiterhin. | Experte |
| Dauer des RP-Gedankens | Gedanke sichtbar für (Sekunden) | Höchstens zwei Sekunden und nie länger als das Untersuchen selbst. | Experte |
| Untersuchungsdauer | Dauer des Untersuchens (Sekunden) | So lange musst du die Person ansehen, bis die Angaben erscheinen. Höchstens zwei Sekunden. | Experte |

### 4. Karte & Minimap

Beschreibung der Kategorie: Weltkarte und Minimap (Xaero) mit den Ottonien-Ergänzungen.

#### Tab „Weltkarte“

**Abschnitt „Allgemein“** – Hauptschalter der Kartenergänzungen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Karten-Modul | Ottonien-Ergänzungen für Karte und Minimap | Hauptschalter für alles, was OttoExtra auf Xaeros Weltkarte und Minimap zeichnet. Eine Änderung wirkt erst nach einem Neustart des Spiels. | Einfach |
| Nur auf Ottonien | Nur auf dem Ottonien-Server | Aus: Die Ergänzungen erscheinen auch auf anderen Servern und im Einzelspieler. Nur zum Ausprobieren sinnvoll. | Mehr |

**Abschnitt „Was die Karte zeigt“** – Welche Informationen auf der Weltkarte erscheinen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Lehengrenzen | Grenzen der Lehen | Zeichnet die Grenzlinien der Lehen. | Einfach |
| Regionsnamen | Namen der Lehen | Schreibt die Namen der Lehen auf die Karte. | Einfach |
| Wappen | Wappen | Zeigt die Wappen der Lehen. | Einfach |
| Spieler-Aktivität | Orte, an denen gerade viel los ist | Zeigt Kreise dort, wo sich mehrere Spieler versammeln. | Einfach |
| NPC-Dörfer anzeigen | NPC-Dörfer | Zeigt die Dörfer der Nichtspieler-Figuren. | Einfach |
| Lehen-Infokarte per Klick | Infokarte beim Klick auf ein Lehen | Ein Klick auf ein Lehen zeigt Herrschaft, Lehnsherr, Stand, Gefolge, Entfernung und geschätzte Reisezeit. | Einfach |

**Abschnitt „Kartenstil“** – Gemalte Karte, Pergament und politische Einfärbung.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Gemalte Karte | Gemalte Ottonien-Karte | Zeigt eine gezeichnete Karte dort, wo du das Gelände noch nicht erkundet hast. | Einfach |
| Pergament-Kartenmodus | Pergament-Rahmen | Ergänzt die Karte um Rahmen, Kompass und Maßstabsleiste. | Einfach |
| Politische Karte | Politische Karte | Färbt die Lehen in der Farbe ihres Gefolges. Lässt sich auch direkt auf der Karte umschalten. | Einfach |
| Öffnungsanimation der Weltkarte | Bewegung beim Öffnen der Karte | Die Karte wird beim Öffnen aufgedeckt und zoomt heraus. | Einfach |
| Politik-Deckkraft | Deckkraft der politischen Farben am Tag | 0 % = unsichtbar, 100 % = deckend. | Mehr |
| Politik-Deckkraft (Nacht) | Deckkraft der politischen Farben in der Nacht | Nachts ist die Karte dunkler. Ein niedrigerer Wert hält sie lesbar. | Mehr |
| Vollflächige Karte beim Rauszoomen | Weit herausgezoomt nur die gemalte Karte zeigen | Sobald nur noch die Wappen der obersten Lehnsherren zu sehen sind, wird das erkundete Gelände ausgeblendet und überall die gemalte Karte gezeigt. Nur bei eingeschalteter politischer Karte. | Mehr |
| Gemalte Karte: einfacher Modus | Gemalte Karte vereinfacht darstellen | Einschalten, wenn die gemalte Karte schwarz bleibt. Das kommt bei manchen Modpacks vor. Der Übergang zwischen Tag und Nacht entfällt dann. | Mehr |
| Pergament-Rand (px) | Breite des Pergament-Rands (Pixel) | Abstand zwischen Kartenbild und Rahmen. | Experte |

**Abschnitt „Farben & Grenzen“** – Farben der Gefolge und Aussehen der Grenzlinien.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Gefolge-Liste… | Gefolge: Namen und Farben<br>Schaltfläche: „Bearbeiten…“ | Liste aller Gefolge und Lehen. Hier gibst du einem Gefolge einen eigenen Anzeigenamen und eine eigene Farbe. | Einfach |
| Gefolge-Farben… | Farben der politischen Karte<br>Schaltfläche: „Farben wählen…“ | Legt für jede Gruppe die Flächenfarbe auf der politischen Karte fest. | Einfach |
| Grenzfarbe (#AARRGGBB) | Farbe der Grenzlinien | Farbe, in der die Grenzen der Lehen gezeichnet werden. | Mehr |
| Strichgrafik | Grenzen gestrichelt zeichnen | Gilt für Weltkarte und Minimap. Aus: durchgezogene Linien. | Mehr |

**Abschnitt „Reisezeit“** – Geschwindigkeiten für die Reisezeit-Schätzung.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Reisehilfe: Fußtempo (Blöcke/s) | Tempo zu Fuß (Blöcke je Sekunde) | Grundlage für die geschätzte Reisezeit zu Fuß in der Infokarte. | Mehr |
| Reisehilfe: Pferdetempo (Blöcke/s) | Tempo zu Pferd (Blöcke je Sekunde) | Grundlage für die geschätzte Reisezeit zu Pferd in der Infokarte. | Mehr |

**Abschnitt „Zoom & Ausrichtung“** – Ab welchem Zoom etwas erscheint; Ausrichtung der gemalten Karte.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Namen ab Zoom | Namen sichtbar ab Zoomstufe | Kleinerer Wert = die Namen erscheinen schon, wenn du weiter herausgezoomt hast. 0 = immer sichtbar. | Experte |
| Wappen ab Zoom | Wappen sichtbar ab Zoomstufe | Kleinerer Wert = die Wappen erscheinen schon, wenn du weiter herausgezoomt hast. Wappen erscheinen nie früher als die Namen. | Experte |
| Politische Karte bis Zoom | Politische Farben sichtbar bis Zoomstufe | Zoomst du weiter hinein, verblassen die Farbflächen und verschwinden. | Experte |
| Kalibrier-Pfeile (Karte verschieben) | Pfeile zum Verschieben der gemalten Karte | Zeigt auf der Weltkarte Pfeile, mit denen du die gemalte Karte verschiebst, falls sie nicht zum Gelände passt. | Experte |

#### Tab „Minimap“

**Abschnitt „Minimap-Darstellung“** – Was die Minimap zusätzlich zeigt.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Grenzen auf Minimap | Grenzen der Lehen | Zeichnet die Grenzlinien auch in der Minimap. | Einfach |
| Gemalte Karte auf Minimap | Gemalte Karte | Zeigt die gemalte Karte auch in der Minimap. | Einfach |
| Politische Flächen auf Minimap | Politische Farben | Färbt die Lehen auch in der Minimap in der Farbe ihres Gefolges. | Einfach |
| Statuspunkt auf der Minimap anzeigen | Statuspunkt für Ressourcen | Ein Punkt an der Minimap: Grün = bereit, Gelb = abbaubar, Rot = erholt sich. | Mehr |

**Abschnitt „Lehen-Anzeige an der Minimap“** – Wappen, Name, Stand und Fraktion des Lehens, in dem man steht.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Wappen an Minimap | Wappen des aktuellen Lehens | Zeigt an der Minimap das Wappen des Lehens, in dem du gerade stehst. | Einfach |
| Wappen: Lehensname | Name des Lehens | Zeigt den Namen des Lehens als Textzeile. | Einfach |
| Wappen: Stand/Hierarchie | Stand des Lehens | Zeigt den Stand des Lehens und wem es untersteht. | Einfach |
| Gefolgename anzeigen | Name des Gefolges | Zeigt als eigene Zeile das Gefolge, das das Lehen hält. | Einfach |
| Wappen-Position (Drag&Drop)… | Lehen-Anzeige verschieben<br>Schaltfläche: „Verschieben…“ | Wappen und Textzeilen mit der Maus verschieben. Ecke ziehen ändert die Größe, das Mausrad die Schriftgröße. Benötigt Xaeros Minimap. | Einfach |
| Obersten Lehnsherrn zeigen | Obersten Lehnsherrn nennen | An: der oberste Lehnsherr der Kette, zum Beispiel Holdstewik. Aus: der direkte Lehnsherr, zum Beispiel Mayenburg. | Mehr |

**Abschnitt „Feinabstimmung der Lehen-Anzeige“** – Farben und genaue Maße der drei Textzeilen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| HUD Name: Farbe (#RRGGBB) | Farbe: Name des Lehens | Schriftfarbe der Zeile mit dem Namen. | Mehr |
| HUD Stand: Farbe (#RRGGBB) | Farbe: Stand | Schriftfarbe der Zeile mit dem Stand. | Mehr |
| HUD Gefolge: Farbe (#RRGGBB) | Farbe: Gefolge | Schriftfarbe der Zeile mit dem Gefolge. | Mehr |
| HUD Name: Skalierung | Schriftgröße: Name des Lehens | 1,0 ist die übliche Größe. | Experte |
| HUD Stand: Skalierung | Schriftgröße: Stand | 1,0 ist die übliche Größe. | Experte |
| HUD Gefolge: Skalierung | Schriftgröße: Gefolge | 1,0 ist die übliche Größe. | Experte |
| HUD Name: Breite | Breite: Name des Lehens (Pixel) | Längerer Text wird auf diese Breite begrenzt. | Experte |
| HUD Stand: Breite | Breite: Stand (Pixel) | Längerer Text wird auf diese Breite begrenzt. | Experte |
| HUD Gefolge: Breite | Breite: Gefolge (Pixel) | Längerer Text wird auf diese Breite begrenzt. | Experte |
| HUD Name: Höhe | Höhe: Name des Lehens (Pixel) | 0 = eine Zeile. Mit mehr Höhe wird langer Text auf mehrere Zeilen umbrochen. | Experte |
| HUD Stand: Höhe | Höhe: Stand (Pixel) | 0 = eine Zeile. Mit mehr Höhe wird langer Text auf mehrere Zeilen umbrochen. | Experte |
| HUD Gefolge: Höhe | Höhe: Gefolge (Pixel) | 0 = eine Zeile. Mit mehr Höhe wird langer Text auf mehrere Zeilen umbrochen. | Experte |

### 5. Lehen & Regionen

Beschreibung der Kategorie: Einblendungen und Listen zu dem Gebiet, in dem man sich bewegt.

**Abschnitt „Einblendung beim Betreten“** – Die Meldung, wenn man ein Lehen betritt.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Regionen-Modul | Lehen-Funktionen (Aktivität, Tabliste) | Schaltet die Aktivitäts-Anzeige und das Ordnen der Tabliste nach Lehen frei. Die Einblendung beim Betreten erscheint derzeit unabhängig von diesem Schalter. | Einfach |
| Betreten-Sound | Ton beim Betreten | Spielt zur Einblendung einen Ton. | Einfach |
| Vorschau anzeigen | Vorschau<br>Schaltfläche: „Anzeigen“ | Zeigt die Einblendung mit einem Beispiel, so wie sie gerade eingestellt ist. | Einfach |
| Actionbar ausblenden | Meldung des Servers ausblenden | Blendet die Meldung „Du betrittst …“ des Servers aus, egal ob sie im Chat, über der Schnellleiste oder als Titel erscheint. So steht die Angabe nicht doppelt da. | Mehr |
| Toast-Dauer (ms) | Dauer der Einblendung (ms) | 5000 ms = 5 Sekunden. | Mehr |

**Abschnitt „Inhalt der Einblendung“** – Was in der Meldung steht.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Wappen im Toast | Wappen | Zeigt das Wappen des Lehens. | Einfach |
| Herrschaft im Toast | Herrschaft | Nennt die Herrschaft, zu der das Lehen gehört. | Einfach |
| Lehnsherr im Toast | Lehnsherr | Nennt den Lehnsherrn. | Einfach |
| RP-Name des Lehnsherrn | Lehnsherrn mit RP-Namen nennen | Nennt den Lehnsherrn mit seinem RP-Namen, wenn deine Figur ihn kennt. Sonst steht dort der Accountname. | Einfach |
| Koordinaten im Toast | Eigene Koordinaten | Zeigt in der Einblendung deine aktuelle Position (X / Z). Nur im Stil „Klassisch“. | Einfach |
| Hinweiszeile anzeigen | Tasten-Hinweis | Zeigt die Zeile „Drücke [Taste] um mehr zu erfahren.“ | Mehr |

**Abschnitt „Aussehen der Einblendung“** – Stil, Platz auf dem Bildschirm und Farbschema.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Banner-Stil | Stil der Einblendung<br>Auswahl: „Klassisch (Kasten)“, „Zonentitel“, „Banderole“, „Plakette“, „Schlicht“ | „Klassisch“ ist ein Kasten, dessen Farben du unter „Farbschema“ wählst. Die anderen Stile sind fertig gestaltet. | Einfach |
| Position | Platz auf dem Bildschirm<br>Auswahl: „Oben Mitte“, „Oben rechts“, „Oben links“, „Bildschirmmitte“ | Wo die Einblendung erscheint. | Einfach |
| Toast-Themes | Farbschema<br>Schaltfläche: „Auswählen…“ | Hell, Dunkel oder ein eigenes Farbschema für den klassischen Stil wählen und bearbeiten. | Einfach |

**Abschnitt „Maße & Schrift der Einblendung“** – Genaue Größen. Langfristig nur noch im Farbschema-Editor (siehe offene Punkte).

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Schrift: Basis | Schriftgröße: Grundwert | Vergrößert oder verkleinert alle Zeilen gemeinsam. 1,0 ist die übliche Größe. Bei einem eigenen Farbschema gelten dessen Werte. | Experte |
| Schrift: Titel | Schriftgröße: Zeile „Du betrittst“ | Größe der obersten Zeile. Nur im Stil „Klassisch“. | Experte |
| Schrift: Region | Schriftgröße: Name des Lehens | Größe der Zeile mit dem Namen. | Experte |
| Schrift: Hierarchie | Schriftgröße: Zugehörigkeit | Größe der Zeilen mit Zugehörigkeit, Herrschaft und Lehnsherr. | Experte |
| Schrift: Hinweis | Schriftgröße: Hinweis und Koordinaten | Größe der Zeilen mit Koordinaten und Tasten-Hinweis. | Experte |
| Max. Textbreite | Breite des Textes höchstens (Pixel) | Ein längerer Name des Lehens wird umbrochen. Andere Zeilen werden nicht gekürzt. Nur im Stil „Klassisch“. | Experte |
| Min. Toastbreite | Breite der Einblendung mindestens (Pixel) | Schmaler wird der Kasten nie. | Experte |
| Max. Toastbreite | Breite der Einblendung höchstens (Pixel) | Breiter wird der Kasten nie. | Experte |
| Abstand oben | Abstand zum oberen Bildschirmrand (Pixel) | Gilt bei einem Platz am oberen Rand. | Experte |
| Wappengröße | Größe des Wappens (Pixel) | Kantenlänge des Wappens. | Experte |
| Wappenabstand | Abstand zwischen Wappen und Text (Pixel) | Freier Raum rechts vom Wappen. | Experte |
| Innenabstand links | Innenabstand links (Pixel) | Freier Raum zwischen Rand und Inhalt. | Experte |
| Innenabstand rechts | Innenabstand rechts (Pixel) | Freier Raum zwischen Rand und Inhalt. | Experte |
| Innenabstand oben | Innenabstand oben (Pixel) | Freier Raum zwischen Rand und Inhalt. | Experte |
| Innenabstand unten | Innenabstand unten (Pixel) | Freier Raum zwischen Rand und Inhalt. | Experte |

**Abschnitt „Aktivitäts-Anzeige“** – Liste aktueller Aktivitäten am Bildschirmrand.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Aktivitäts-HUD anzeigen | Aktivitäts-Anzeige | Zeigt am Bildschirmrand, in welchen Gefolgen oder Lehen gerade etwas los ist, mit Wappen und Aktivitätsstufe. Ein- und Ausblenden geht auch per Taste. | Einfach |
| Aktuelle Uhrzeit anzeigen | Uhrzeit anzeigen | Zeigt in der Anzeige die aktuelle Uhrzeit. | Einfach |
| Letzte Aktualisierung anzeigen | Zeitpunkt der letzten Aktualisierung anzeigen | Zeigt, wie alt die angezeigten Daten sind. | Einfach |
| Maximal sichtbare Aktivitäten | Einträge höchstens | So viele Gefolge oder Lehen zeigt die Liste gleichzeitig. | Mehr |
| Position des Aktivitäts-HUDs | Platz auf dem Bildschirm<br>Auswahl: „Mitte links“, „Oben links“, „Unten links“, „Mitte rechts“, „Oben rechts“, „Unten rechts“, „Frei“ | Wo die Aktivitäts-Anzeige steht. „Frei“ gilt nach dem Verschieben mit der Maus. | Mehr |
| Größe des Aktivitäts-HUDs | Größe | 1,0 ist die übliche Größe. | Mehr |
| Aktivitäts-HUD frei platzieren | Aktivitäts-Anzeige frei verschieben<br>Schaltfläche: „Verschieben…“ | Die Anzeige mit der Maus an eine beliebige Stelle ziehen; der Griff unten rechts ändert die Größe. | Mehr |
| Aktivitäts-HUD: X-Versatz | Verschiebung waagerecht (Pixel) | Verschiebt die Anzeige vom gewählten Platz aus nach links oder rechts. | Experte |
| Aktivitäts-HUD: Y-Versatz | Verschiebung senkrecht (Pixel) | Verschiebt die Anzeige vom gewählten Platz aus nach oben oder unten. | Experte |

**Abschnitt „Kartendaten“** – Daten zu Lehen und Gefolgen neu vom Server holen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Kartendaten neu laden | Daten zu Lehen und Gefolgen neu laden<br>Schaltfläche: „Neu laden“ | Holt Grenzen, Namen und Zugehörigkeiten neu. Hilfreich, wenn Karte oder Einblendung veraltet wirken. | Einfach |

### 6. Briefe

Beschreibung der Kategorie: Briefe und Verkündungen schreiben.

**Abschnitt „Brief-Editor“** – Der Editor zum Schreiben.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Brief-Modul | Brief-Editor verwenden | Ein Rechtsklick mit dem Schreibzeug („Pergament und Feder“) öffnet den Editor von OttoExtra. Eine Änderung wirkt erst nach einem Neustart des Spiels. | Einfach |
| Brief-Editor öffnen… | Brief-Editor<br>Schaltfläche: „Öffnen…“ | Öffnet den Editor zum Schreiben von Briefen und Verkündungen. | Einfach |
| Briefvorschau aktivieren | Buch-Ansicht im Editor | Zeigt im Editor den Reiter „Buch“ mit einer Vorschau des fertigen Briefs. | Einfach |
| Versandmenü nach dem Schreiben öffnen | Nach dem Schreiben gleich den Empfänger wählen | Öffnet nach dem Schreiben die Auswahl des Empfängers. Aus: Der Brief bleibt in deiner Hand und lässt sich später über „Verschicken…“ im geöffneten Brief versenden. | Einfach |

**Abschnitt „Schreiben & Formatieren“** – Formatierung, Seitenleiste und Signatur.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Formatierung aktivieren | Farben und Schriftstile in Briefen | Zeigt eine Formatierungshilfe und stellt den Brief schon beim Schreiben farbig dar. | Einfach |
| Leiste rechts anzeigen | Formatierungsleiste anzeigen | Zeigt links neben dem Brief eine Leiste mit Farben und Schriftstilen zum Anklicken. Braucht „Farben und Schriftstile in Briefen“ und genug Platz im Fenster. | Einfach |
| Signatur | Signatur | Text, den die Feder-Schaltfläche im Editor einfügt. \n steht für eine neue Zeile. {{name}}, {{title}}, {{full}} und {{mc}} werden durch deinen RP-Namen, deinen Titel, Titel mit Namen und deinen Accountnamen ersetzt. | Einfach |
| &-Codes beim Einfügen umwandeln | Farbcodes beim Einfügen umwandeln | Eingefügter Text mit Codes wie &6 oder &l wird sofort farbig dargestellt. | Mehr |

**Abschnitt „Archiv“** – Ablage gespeicherter Briefe.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Briefexporte nach Spielernamen ablegen | Exportierte Briefe nach Verfasser ablegen | Legt Briefe, die du von Hand exportierst, in einem Unterordner je Verfasser ab. Bereits exportierte Briefe bleiben, wo sie sind. | Mehr |

**Abschnitt „Verkündungen“** – Seitenaufteilung für Verkündungen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Automatische Optimierung erlauben | Schaltfläche „Automatisch optimieren“ anbieten | Zeigt die Schaltfläche in der Prüfung vor dem Versand einer Verkündung. Achtung: Das Optimieren kann Umbrüche, Formatierung und Seitenaufteilung verändern. Den Entwurf danach vollständig prüfen. | Mehr |
| Sichere Zeilen/Seite | Zeilen je Seite (höchstens) | Seiten einer Verkündung mit mehr Zeilen werden vor dem Versand als Warnung gemeldet. Der Versand bleibt möglich. | Experte |
| Sichere Zeichen/Zeile | Zeichen je Zeile (höchstens) | Zeilen einer Verkündung mit mehr Zeichen werden vor dem Versand als Warnung gemeldet. Der Versand bleibt möglich. | Experte |

**Abschnitt „Versand“** – Technik des Versands. Nur ändern, wenn der Versand nicht funktioniert.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Versandmodus | Art des Versands<br>Auswahl: „Seite für Seite“, „Zeile für Zeile“, „Genaue Nachbildung“ | „Seite für Seite“ sendet eine Nachricht je Seite. „Zeile für Zeile“ sendet jede Zeile einzeln. „Genaue Nachbildung“ bildet das Brief-System des Servers genau nach und hält Vorschau und Buchseiten gleich. | Experte |
| Auslöse-Item (Name) | Name des Schreibzeugs | Der Editor öffnet sich bei einem Rechtsklick mit einem Gegenstand, dessen Name diesen Text enthält. | Experte |
| Zeilen-Befehl | Befehl zum Schreiben | Der Serverbefehl, mit dem der Text übertragen wird. | Experte |
| Versand-Befehl | Befehl zum Verschicken | Der Serverbefehl, mit dem der Brief versendet wird. | Experte |
| Verkündungs-Abschluss | Befehl zum Abschließen einer Verkündung | Leer: Die Mod sendet den Befehl nicht. Du bekommst einen Hinweis im Chat und gibst ihn selbst ein. | Experte |
| Zeilen-Delay min (ms) | Pause zwischen Zeilen, kürzeste (ms) | Die Mod wartet zwischen zwei Zeilen, damit der Server den Versand nicht als Spam wertet. 1000 ms = 1 Sekunde. | Experte |
| Zeilen-Delay max (ms) | Pause zwischen Zeilen, längste (ms) | Obere Grenze der Pause zwischen zwei Zeilen. | Experte |
| Seiten-Delay (PAGE, ms) | Pause zwischen Seiten (ms) | Gilt bei der Versandart „Seite für Seite“. | Experte |

### 7. Tiere & Hof

Beschreibung der Kategorie: Tierinfo, Zucht und Hilfen für die Landwirtschaft.

**Abschnitt „Tierinfo beim Anvisieren“** – Die Einblendung, wenn man ein Tier ansieht.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Automatische Tierinfo | Tierinfo beim Ansehen | Siehst du ein Tier kurz an, fragt die Mod seine Daten ab und zeigt die wichtigsten Werte oben am Bildschirm. | Einfach |
| Stil | Darstellung<br>Auswahl: „Kompakt“, „Ausführlich“, „Eigenes Layout“ | Kompakt: schmale Leiste mit Statusstreifen. Ausführlich: Karte mit Statusband und Fortschrittsbalken. Eigenes Layout: frei anpassbar. | Einfach |
| Toast-Layout bearbeiten… | Eigenes Layout bearbeiten<br>Schaltfläche: „Bearbeiten…“ | Bestandteile der Tierinfo je Tierart ein- und ausblenden, verschieben und in der Größe ändern. Gilt nur bei der Darstellung „Eigenes Layout“. | Mehr |
| Automatische Antwort ausblenden | Automatische Antworten im Chat ausblenden | Die Antwort des Servers auf die automatische Abfrage erscheint nicht im Chat. Abfragen, die du selbst eingibst, bleiben sichtbar. | Mehr |

**Abschnitt „Namen über Tieren“** – Geschlecht am Namensschild der Tiere.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Geschlecht im Nametag anzeigen | Geschlecht am Namensschild | Ergänzt ♀ oder ♂, sobald das Geschlecht des Tiers bekannt ist. | Einfach |
| Tiernamen nach Geschlecht färben | Tiernamen nach Geschlecht färben | Weiblich rosa, männlich blau. Bei unbekanntem Geschlecht bleibt die Farbe. | Einfach |
| Auch ungespeicherte Tiere | Auch bei nicht eingetragenen Tieren | Nutzt bekannte Daten auch für Tiere ohne Eintrag im Tierregister. Es wird nichts zusätzlich abgefragt oder eingetragen. | Mehr |

**Abschnitt „Tiere hervorheben“** – Leuchtende Umrisse nach Geschlecht, Zuchtpause und Schur.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Tiere nach Geschlecht umranden | Tiere nach Geschlecht hervorheben | Umrandet Tiere mit bekanntem Geschlecht: weiblich rosa, männlich blau. Zuerst die Tierinfo abfragen. Umschalten geht auch per Taste (F8). Reichweite 64 Blöcke. | Einfach |
| Gepaarte Tiere umranden | Tiere in der Zuchtpause hervorheben | Tiere, die sich gerade gepaart haben, leuchten violett, bis sie wieder können. Hat Vorrang vor der Geschlechtsfarbe. Die Wartezeit ist geschätzt, bis du das Tier wieder ansiehst. | Einfach |
| Geschorene Schafe umranden | Geschorene Schafe hervorheben | Von dir geschorene Schafe leuchten golden, bis sie wieder Wolle haben. Hat Vorrang vor den anderen Farben. | Einfach |

**Abschnitt „Zucht“** – Addon AnimalBreeding.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Ich züchte Tiere | Ich züchte Tiere | Schaltet Zuchtakten, den Abgleich mit dem Hof und das automatische Ausfüllen der Herkunft frei. Zusätzlich muss der Server dich als Züchter freigeben. | Einfach |

**Abschnitt „Tierübersicht“** – Welche Tierarten die Übersicht zeigt.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Rinder | Rinder | Bietet Rinder als eigenen Filter in der Tierübersicht an. | Mehr |
| Brakel | Hühner (Brakel) | Bietet Hühner als eigenen Filter in der Tierübersicht an. | Mehr |
| Hausschweine | Schweine | Bietet Schweine als eigenen Filter in der Tierübersicht an. | Mehr |
| Ziegen | Ziegen | Bietet Ziegen als eigenen Filter in der Tierübersicht an. | Mehr |
| Zaupel | Schafe (Zaupel) | Bietet Schafe als eigenen Filter in der Tierübersicht an. | Mehr |

**Abschnitt „Fütter- und Pflanzhilfe“** – Addon Farmer.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Fütterhilfe aktivieren | Fütterhilfe | Füllt beim Füttern den leer gewordenen Platz in der Schnellleiste mit demselben Futter aus dem Inventar auf. Ein- und Ausschalten per Taste (G). | Einfach |
| Pflanzhilfe aktivieren | Pflanzhilfe | Ein- und Ausschalten per Taste (V). Solange du Saatgut hältst, dreht die Hilfe deinen Blick zum nächsten freien Feld in Reichweite und pflanzt selbst. Du läufst weiterhin selbst. | Einfach |
| Schlangenlinien-Modus | In Schlangenlinien pflanzen | An: Zusammenhängende Reihen werden abwechselnd hin und zurück bepflanzt. Aus: Es wird immer das nächste Feld vor dir gewählt. | Mehr |
| Statusmeldungen anzeigen | Meldungen der Hilfen anzeigen | Zeigt über der Schnellleiste eine kurze Meldung, wenn du eine Hilfe per Taste ein- oder ausschaltest. | Mehr |

**Abschnitt „Gemerkte Tiere“** – Lokal gespeicherte Tierdaten.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Tiergeschlecht und Wartezeiten 14 Tage lokal merken | Tierdaten 14 Tage merken | Speichert Tierart, Geschlecht und Wartezeiten von bis zu 1.024 Tieren je Server auf deinem Rechner, damit sie nach einem Neustart noch da sind. Hof und Besitzer werden nicht gespeichert. Ausschalten und Speichern löscht die Daten des aktuellen Servers. | Mehr |
| Alle Tiere dieses Servers vergessen | Gemerkte Tiere dieses Servers löschen<br>Schaltfläche: „Löschen“ | Löscht die gespeicherten Tiere und Zeiten des Servers, mit dem du gerade verbunden bist. Sie werden beim nächsten Ansehen neu gesammelt. | Mehr |

**Abschnitt „Abfrage“** – Zeitwerte der Serverabfrage.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Anvisierverzögerung (ms) | Abfrage nach so langem Ansehen (ms) | So lange musst du ein Tier ansehen, bevor die Mod seine Daten abfragt. 1000 ms = 1 Sekunde. | Experte |
| Auffrischintervall (ms) | Dasselbe Tier erneut abfragen nach (ms) | Frühestens nach dieser Zeit fragt die Mod dasselbe Tier wieder ab. 60000 ms = 1 Minute. Nach dem Füttern wird sofort neu abgefragt. | Experte |
| Antwort-Zeitlimit (ms) | Wartezeit auf die Antwort (ms) | Antwortet der Server nicht in dieser Zeit, gilt die Abfrage als gescheitert. | Experte |

### 8. Kampf

Beschreibung der Kategorie: Anzeigen und Effekte im Kampf.

**Abschnitt „Kampfmodus-Anzeige“** – Zeigt an, ob der Kampfmodus aktiv ist.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Kampfmodus-Icon anzeigen | Kampftechnik neben der Schnellleiste anzeigen | Zeigt die zuletzt erkannte Kampftechnik als Symbol rechts neben der Schnellleiste, solange du eine Waffe in der Hand hältst. Vor der ersten Erkennung steht dort die Waffenart mit einem Fragezeichen. | Einfach |

**Abschnitt „Verwundung & Adrenalin“** – Bildeffekte bei wenig Leben.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Effekt aktivieren | Effekte bei wenig Leben | Je weniger Leben du hast, desto stärker verändert sich das Bild. Achtung: Im vollen Umfang pulsiert und verschwimmt das Bild und ein Herzschlag ist zu hören. | Einfach |
| Reduzierte Effekte (ohne Puls, Blur, FOV, Herzschlag) | Nur ruhige Effekte | Verzichtet auf Pulsieren, Unschärfe, Änderung des Sichtfelds und Herzschlag. Empfohlen, wenn dir bewegte Bilder unangenehm sind. | Einfach |
| Intensität | Stärke der Effekte | 1,0 ist die übliche Stärke. | Mehr |
| Roter Rand | Roter Bildrand | Färbt den Rand des Bildes rot. | Mehr |
| Herzschlag-Sound | Herzschlag-Geräusch | Spielt einen Herzschlag, der bei weniger Leben schneller wird. Nicht zu hören, solange „Nur ruhige Effekte“ an ist. | Mehr |
| Edge-Blur (Tunnelblick) | Unscharfer Bildrand (Tunnelblick) | Die Ränder verschwimmen. Die Bildmitte und alle Anzeigen bleiben scharf. | Mehr |
| FOV-Adrenalin | Sichtfeld weitet sich | Das Sichtfeld wird bei wenig Leben etwas größer. | Mehr |
| Beruhigen ohne Schaden | Ohne neuen Schaden beruhigen | Herzschlag und starke Unschärfe lassen nach einigen Sekunden ohne neuen Schaden nach. | Mehr |
| Start ab Health | Effekte beginnen unter (Lebenspunkte) | 2 Lebenspunkte = 1 Herz. 20 entspricht 10 Herzen, der Hälfte des Lebens auf Ottonien. | Experte |
| Blur ab Herzen | Unschärfe beginnt unter (Herzen) | Ab dieser Zahl an Herzen verschwimmt der Bildrand. | Experte |
| Blur-Stärke | Stärke der Unschärfe | 0 = aus, 3 = stark. | Experte |
| FOV-Zuschlag (Grad) | Sichtfeld-Zuwachs höchstens (Grad) | Um so viel weitet sich das Sichtfeld bei voller Stärke. | Experte |

**Abschnitt „Verbündete & Gegner“** – Addon TacticalFactions.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Tactical Operation Mode | Kampfmodus für Gefechte | Hauptschalter des Addons auf Ottonien: färbt die Namen von Verbündeten und Gegnern, markiert Angreifer, ordnet die Tabliste und schaltet den Schutz vor Schlägen auf Verbündete frei. | Einfach |
| Spielernamen im Kampf anzeigen | Accountnamen im Kampf anzeigen | Zeigt im Kampfmodus zusätzlich den Minecraft-Accountnamen am Namensschild. | Einfach |
| Politische Farben | Namen nach Beziehung färben | Verbündete und Gegner bekommen im Kampfmodus die unten gewählten Farben. | Einfach |
| Politik-Buch öffnen | Politik-Buch<br>Schaltfläche: „Öffnen…“ | Legt für Gefolge, eigene Gruppen und einzelne Spieler fest, ob sie Verbündete, Neutrale oder Gegner sind. Enthält auch die Trupps. | Einfach |
| Angreifer mit rotem ! markieren | Angreifer mit rotem „!“ markieren | Wer dich angreift, bekommt ein rotes Ausrufezeichen am Namen. | Mehr |
| Friendly Fire verhindern | Schläge auf Verbündete verhindern | Achtung: Diese Funktion kann zu einem Bann führen. Sie ist nur in bestimmten Fassungen des Addons vorhanden. | Mehr |
| Neutrale Spieler ebenfalls schützen | Auch Neutrale nicht treffen | Verhindert zusätzlich Schläge auf Spieler, die weder Verbündete noch Gegner sind. Wirkt nur zusammen mit „Schläge auf Verbündete verhindern“. | Mehr |
| Verbündetenfarbe | Farbe für Verbündete | Namensfarbe von Verbündeten im Kampfmodus. | Mehr |
| Gegnerfarbe | Farbe für Gegner | Namensfarbe von Gegnern im Kampfmodus. | Mehr |

### 9. Handwerk & Bauen

Beschreibung der Kategorie: Hilfen beim Herstellen, Abbauen und Bauen.

**Abschnitt „Herstellungsanzeige“** – Eigene Fortschrittsanzeige beim Herstellen.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Eigene Herstellungsanzeige | Eigene Fortschrittsanzeige beim Herstellen | Ersetzt die Kästchen-Anzeige des Servers an Werkbänken durch ein bewegtes Symbol mit Fortschrittsbalken. Alle anderen Meldungen bleiben, auch die zum Abschluss. | Einfach |
| Herstellungs-Geräusche | Geräusche beim Herstellen | Ein Schlag je Arbeitsschritt, dazu ein Ton bei Erfolg oder Fehlschlag. | Einfach |

**Abschnitt „Werkzeugschutz“** – Verhindert, dass ein Werkzeug versehentlich zerbricht.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Werkzeugschutz aktivieren | Werkzeuge vor dem Zerbrechen schützen | Verhindert Klicks mit einem Werkzeug, das fast kaputt ist, und warnt einmal bei niedriger Haltbarkeit. | Einfach |
| Warnung unter (%) | Warnen unter (Prozent Haltbarkeit) | Fällt die Haltbarkeit unter diesen Wert, erscheint einmal eine Warnung. | Mehr |
| Blockieren ab (Nutzungen) | Sperren bei verbleibenden Nutzungen | Bleiben dem Werkzeug höchstens so viele Nutzungen, werden Klicks damit abgebrochen. | Mehr |

**Abschnitt „Ressourcen-Hinweise“** – Addon Resources.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Ressourcenmodul aktivieren | Ressourcen-Hinweise verwenden | Hauptschalter des Addons Resources. | Einfach |
| Hinweise beim Abbau anzeigen | Hinweise im Ressourcengebiet | Zeigt Einblendungen, wenn du ein Ressourcengebiet betrittst, dort mit dem Abbau beginnst oder es mit offenem Abbau verlässt. | Einfach |
| Synchronisierungsradius | Umkreis für Ressourcen (Blöcke) | Die Mod lädt die Ressourcen in diesem Umkreis um dich. | Experte |
| Cache-Dauer in Sekunden | Daten neu laden nach (Sekunden) | So lange gelten geladene Ressourcen-Daten, bevor sie neu geholt werden. | Experte |

**Abschnitt „Bauen mit Litematica“** – Ergänzungen für die Mod Litematica.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Lokalisierte Blocknamen im HUD | Deutsche Blocknamen in Litematica | Zeigt in Litematicas Blockangaben den Namen aus dem Resourcepack statt der technischen Bezeichnung (minecraft:xyz). | Einfach |
| Fachwerk automatisch abstimmen | Fachwerk automatisch einstellen | Ein Rechtsklick mit dem Stemmeisen stellt einen Fachwerk-Block Schritt für Schritt so ein, wie die Litematica-Vorlage es vorgibt. Umschalten geht auch per Taste. | Mehr |

### 10. Datenschutz & Sicherung

Beschreibung der Kategorie: Was die Mod ins Internet schickt, lokal speichert und sichert.

**Abschnitt „Einwilligungen“** – Jede Verbindung nach außen ist einzeln erlaubt oder verboten.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Online-Dienste erlauben | Online-Dienste von OttoExtra | Erlaubt der Mod, dich bei ihrem Dienst anzumelden und Daten abzurufen. Ohne Erlaubnis bleiben alle lokalen Funktionen nutzbar. | Einfach |
| Automatische Resourcepack-Downloads | Resourcepack herunterladen | Erlaubt der Mod, das Resourcepack zu prüfen, herunterzuladen, zu aktualisieren und im Minecraft-Ordner zu speichern. | Einfach |
| Externe Skin- und Bannerbilder | Skins und Wappen herunterladen | Erlaubt der Mod, Spieler-Skins und Wappen der Lehen aus dem Internet zu laden und auf deinem Rechner zu speichern. | Einfach |
| GitHub-Versionsprüfung | Nach neuer Mod-Version suchen | Fragt beim Start bei GitHub nach einer neuen Version von OttoExtra. Die Mod aktualisiert sich nicht selbst. | Einfach |
| Beobachtete RP-Namen an API senden | Beobachtete RP-Namen teilen | Sendet RP-Namen, die du im Spiel siehst, an die Datenbank von OttoExtra. Übertragen werden dein Accountname sowie RP-Name und Accountname des beobachteten Spielers. Braucht zusätzlich die Online-Dienste. | Einfach |

**Abschnitt „Chatprotokoll“** – Speichert den Chat lokal in einer Datei.

| Bisher | Neu | Hilfetext | Stufe |
|---|---|---|---|
| Chatprotokoll speichern | Chat in einer Datei mitschreiben | Speichert den Chat auf deinem Rechner, auch ausgeblendete Antworten. Höchstens 8 MiB je Datei und 64 MiB insgesamt; nichts wird automatisch gelöscht. Beim Einschalten wirst du um Bestätigung gebeten. | Einfach |
| Chatlog-Umfang (Änderung braucht neue Freigabe) | Mitschreiben auf<br>Auswahl: „Nur Ottonien“, „Allen Servern und im Einzelspieler“ | Eine Änderung braucht eine neue Bestätigung. | Einfach |
