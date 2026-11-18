# TranslateUtils

- Quelle: Addon `OttoExtra-TranslateUtils` (eigene Mod, eigenes Repository; der Reiter erscheint nur, wenn das Addon installiert ist)
- Modul-ID im Code: `translateutils`
- Modulbeschreibung (im Code hinterlegt, im GUI derzeit nicht angezeigt): Übersetzt ausgehenden Chat und dynamische deutsche Serveranzeigen direkt auf dem Client.
- Umfang: 2 Tab(s), 3 Karte(n), 23 Einträge

## Tab „Basis“

### Karte „Übersetzung“

Kartenbeschreibung im GUI: Getrennte Sprachrichtungen für eingehende Anzeigen und ausgehenden Chat. Der Hotkey schaltet die ausgehende Übersetzung.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 1 | Eigene Nachrichten übersetzen | Schalter (An/Aus) | Aus |  | – | `translate.enabled` |
| 2 | Serveranzeigen übersetzen | Schalter (An/Aus) | Aus |  | Übersetzt auf Ottonien Chat, Actionbar, Titel, Bossbars, Scoreboard, Tab-Kopf/-Fuß, Inventartitel, Custom-Itemnamen und Lore. | `translate.fullTranslationEnabled` |
| 3 | Datenschutzhinweis | Schaltfläche | – | Text auf der Schaltfläche: „Anzeigen…“ | – | `translate.privacyNotice` |
| 4 | Ausgehende Eingabesprache | Auswahl (Klick schaltet weiter) | `en` | `de`<br>`en`<br>`da`<br>`sv`<br>`pl`<br>`fr`<br>`es` | – | `translate.sourceLanguage` |
| 5 | Ausgehende Zielsprache | Auswahl (Klick schaltet weiter) | `de` | `de`<br>`en`<br>`da`<br>`sv`<br>`pl`<br>`fr`<br>`es` | – | `translate.targetLanguage` |
| 6 | Server-Eingabesprache | Auswahl (Klick schaltet weiter) | `de` | `de`<br>`en`<br>`da`<br>`sv`<br>`pl`<br>`fr`<br>`es` | – | `translate.incomingSourceLanguage` |
| 7 | Anzeige-Zielsprache | Auswahl (Klick schaltet weiter) | `en` | `de`<br>`en`<br>`da`<br>`sv`<br>`pl`<br>`fr`<br>`es` | – | `translate.incomingTargetLanguage` |
| 8 | Sprachkürzel an der Eingabe anzeigen | Schalter (An/Aus) | An |  | Zeigt z. B. [DE>SV] rechts an der Chat-Eingabezeile — grün wenn aktiv und rot wenn ausgeschaltet. Gilt auch in OOC-Kanälen. Ein Klick schaltet die Übersetzung ein oder aus. Wird nicht mitgesendet. | `translate.showLanguageTag` |
| 9 | Original als OOC-Nachricht senden | Schalter (An/Aus) | An |  | Sendet das Original direkt nach der Übersetzung als eigene öffentliche (...)-OOC-Nachricht. Kann gleichzeitig mit der geschützten Hover-Freigabe verwendet werden. | `translate.showOriginalMessage` |
| 10 | Original geschützt per Hover | Auswahl (Klick schaltet weiter) | `OFF` | `OFF`<br>`ALL`<br>`ALLOWLIST` | OFF: keine Hover-Freigabe. ALL: alle verifizierten OttoExtra-Nutzer. ALLOWLIST: nur ausgewählte Minecraft-Usernamen. | `translate.protectedOriginalMode` |
| 11 | Erlaubte Usernamen | Schaltfläche | – | Text auf der Schaltfläche: „Bearbeiten…“ | – | `translate.protectedOriginalUsers` |
| 12 | Freigegebenes Original als Chatzeile | Schalter (An/Aus) | Aus |  | Persönliche Anzeigeoption: Lädt freigegebene Originaltexte automatisch und zeigt sie lokal direkt unter der Übersetzung. Ausgeschaltet bleibt die Hover-Anzeige. | `translate.protectedOriginalInline` |
| 13 | Statusmeldung beim Umschalten | Schalter (An/Aus) | An |  | – | `translate.feedbackMessages` |
| 14 | Lokaler Übersetzungscache | Schaltfläche | – | Text auf der Schaltfläche: „Cache leeren“ | – | `translate.clearCache` |
| 15 | Freigegebenes Übersetzungsbundle | Schaltfläche | – | Text auf der Schaltfläche: „Bundle neu laden“ | – | `translate.refreshBundle` |

### Karte „Geschützte Inhalte“

Kartenbeschreibung im GUI: Diese Textteile werden vor der Übersetzung maskiert und danach unverändert wiederhergestellt.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 16 | Emotes *...* schützen | Schalter (An/Aus) | An |  | Text zwischen Sternchen bleibt in der Originalsprache. | `translate.protectEmotes` |
| 17 | OOC-Passagen (...) schützen | Schalter (An/Aus) | An |  | Text in runden Klammern bleibt in der Originalsprache. Ausschalten übersetzt auch OOC. | `translate.protectOoc` |
| 18 | URLs schützen | Schalter (An/Aus) | An |  | – | `translate.protectUrls` |
| 19 | Eckige Tags schützen | Schalter (An/Aus) | An |  | Kurze Tags wie [OOC] oder [RP] werden nicht übersetzt. | `translate.protectBracketTags` |

## Tab „Erweitert“

### Karte „Übersetzungs-API“

Kartenbeschreibung im GUI: LibreTranslate-kompatibler Endpunkt. Bei Fehlern oder Timeout wird immer die Originalnachricht gesendet.

| Nr. | Beschriftung | Bedienelement | Standard | Bereich / Werte | Hilfetext (Tooltip) | Config-Schlüssel |
|---|---|---|---|---|---|---|
| 20 | API-URL | Textfeld | `https://translate.ottoextra.dev/translate` |  | – | `translate.apiUrl` |
| 21 | API-Key | Textfeld | (leer) |  | Optional — nur für geschützte Übersetzungsserver. | `translate.apiKey` |
| 22 | Maximale Zeichen | Zahlenfeld (ganze Zahl) | `1000` | 50 – 5000 | – | `translate.maxCharacters` |
| 23 | Timeout (ms) | Zahlenfeld (ganze Zahl) | `5000` | 1000 – 30000 | – | `translate.timeoutMillis` |
