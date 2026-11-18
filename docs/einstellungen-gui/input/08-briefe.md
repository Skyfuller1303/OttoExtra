# Briefe

- Position: Reiter 8 in der oberen Reiterleiste (Kern-Mod; danach folgen Addon-Reiter und zuletzt „Backups“)
- Modul-ID im Code: `letter`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Brief-Editor mit Pergament-Optik, Platzhaltern und Verkündungen.
- Umfang: 2 Tab(s), 4 Karte(n), 20 Einträge

## Tab „Basis“

### Karte „Editor“

Kartenbeschreibung im GUI: Rechtsklick mit dem Auslöse-Item öffnet den Editor.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Brief-Modul | Schalter (An/Aus) | An |  | – | `letter.enabled` |
| 2 | Versandmodus | Auswahl (Klick schaltet weiter) | `PAGE` | `LEGACY`<br>`PAGE`<br>`V2` | V2 bildet CourierNew exakt nach und hält Vorschau und Buchseiten synchron. PAGE: eine Nachricht pro Entwurfsseite. LEGACY: eine Nachricht pro Zeile. | `letter.sendMode` |
| 3 | Auslöse-Item (Name) | Textfeld | `Pergament und Feder` |  | Item-Anzeigename, der den Editor per Rechtsklick öffnet. Teiltreffer genügt. | `letter.triggerItemName` |
| 4 | Versandmenü nach dem Schreiben öffnen | Schalter (An/Aus) | An |  | Öffnet nach abgeschlossenem Schreiben automatisch die Empfängerauswahl. Ausgeschaltet bleibt der Brief in deiner Hand und lässt sich später über „Verschicken…“ im geöffneten Brief versenden. Standardmäßig an. | `letter.openRecipientAfterWrite` |
| 5 | Briefexporte nach Spielernamen ablegen | Schalter (An/Aus) | Aus |  | Betrifft nur manuelle Exporte. Legt sie nach dem Autor des Buchs in Unterordnern ab. Standardmäßig aus; vorhandene Exporte bleiben am bisherigen Ort. | `letter.exportSubfoldersByPlayer` |
| 6 | Brief-Editor öffnen… | Schaltfläche | – | Text auf der Schaltfläche: „Editor öffnen…“ | – | `letter.open` |

### Karte „Formatierung“

Kartenbeschreibung im GUI: Farben & Stilcodes für Briefe.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 7 | Formatierung aktivieren | Schalter (An/Aus) | An |  | Zeigt eine Formatierungshilfe und färbt den Brief live. Beim Senden werden §-Codes automatisch zu sicheren &-Codes. | `letter.formattingEnabled` |
| 8 | Leiste rechts anzeigen | Schalter (An/Aus) | An |  | – | `letter.formattingSidebarVisible` |
| 9 | &-Codes beim Einfügen umwandeln | Schalter (An/Aus) | An |  | Aus der Zwischenablage eingefügte &6/&l-Codes werden zu §6/§l, damit sie live formatiert erscheinen. | `letter.formattingConvertAmpersandOnPaste` |
| 10 | Briefvorschau aktivieren | Schalter (An/Aus) | An |  | – | `letter.previewEnabled` |
| 11 | Signatur | Textfeld | `§oHochachtungsvoll,\n§l{{full}}` |  | Text der Feder-Schaltfläche. <br> steht für einen Zeilenumbruch; {{name}}, {{title}}, {{full}} und {{mc}} werden mit deinem eigenen Account ersetzt. | `letter.signature` |

## Tab „Erweitert“

### Karte „Serverbefehle“

Kartenbeschreibung im GUI: Befehle, über die Briefe geschrieben und verschickt werden.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 12 | Zeilen-Befehl | Textfeld (Befehl mit `/`) | `letter` |  | – | `letter.letterCommand` |
| 13 | Versand-Befehl | Textfeld (Befehl mit `/`) | `post` |  | – | `letter.postCommand` |
| 14 | Verkündungs-Abschluss | Textfeld (Befehl mit `/`) | `verkünden` |  | Leer = die Mod sendet keinen Abschlussbefehl; du bekommst stattdessen einen Hinweis im Chat und führst ihn selbst aus. | `letter.announcementSubmitCommand` |

### Karte „Layout & Timing“

Kartenbeschreibung im GUI: Sichere Seitengrenzen für Verkündungen und Sende-Verzögerungen (Anti-Spam).

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 15 | Automatische Optimierung erlauben | Schalter (An/Aus) | Aus |  | Standardmäßig aus. Blendet im Verkündungs-Preflight „Automatisch optimieren“ ein. Achtung: Die Optimierung kann fehlerhafte Umbrüche, verschobene Formatierungen oder ein verändertes Seitenlayout verursachen. Entwurf vor dem Versand vollständig prüfen. | `letter.announcementAutoOptimizeEnabled` |
| 16 | Sichere Zeilen/Seite | Zahlenfeld (ganze Zahl) | `11` | 4 – 12 | – | `letter.announcementSafeLinesPerPage` |
| 17 | Sichere Zeichen/Zeile | Zahlenfeld (ganze Zahl) | `17` | 8 – 18 | – | `letter.announcementSafeCharsPerLine` |
| 18 | Zeilen-Delay min (ms) | Zahlenfeld (ganze Zahl) | `500` | 500 – 10000 | – | `letter.letterSendDelayMinMs` |
| 19 | Zeilen-Delay max (ms) | Zahlenfeld (ganze Zahl) | `1200` | 500 – 10000 | – | `letter.letterSendDelayMaxMs` |
| 20 | Seiten-Delay (PAGE, ms) | Zahlenfeld (ganze Zahl) | `1200` | 200 – 5000 | Pause zwischen Seiten im PAGE-Modus (ms). | `letter.pageModeSendDelayMs` |
