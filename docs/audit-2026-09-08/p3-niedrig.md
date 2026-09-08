# P3 – niedrige Priorität und kleine Verbesserungen

[Übersicht](README.md) · [Ursprüngliche Prüfnachweise](VALIDIERUNG.md) · [Umsetzung und aktuelle Tests](UMSETZUNG.md)

Die Fehlerbeschreibungen und Quellzeilen unten dokumentieren den Stand vor den Fixes.

<a id="a26-09"></a>
## A26-09: Backup- und Reparaturrückmeldung verschwindet sofort

**Status: im Code behoben. Beleg: Klick-Callback und UI-Neuaufbau.**

**Auslöser und Folge:** Backup erstellen/wiederherstellen oder RP-Daten reparieren.
Die Aktion setzt eine Erfolgs- oder Fehlermeldung und löscht sie noch im selben
Klick-Callback. Dadurch ist insbesondere ein fehlgeschlagener Kopiervorgang für
den Benutzer nicht sichtbar erklärt.

**Fundstellen:** [OttoExtraSettingsScreen.java, Zeile 218](../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java#L218)
leert in `rebuildContent()` die `statusMessage`. Die Aktionspfade setzen den
Status zuerst und rufen danach den Neuaufbau auf: Reparatur 480–496,
Backup-Erstellung 508–513, Wiederherstellung 535–546.

**Korrektur und Abnahme:** Meldung nach dem Neuaufbau setzen oder beim Aktualisieren
der Backupliste erhalten. Erfolgreiche und kontrolliert fehlgeschlagene Aktionen
ausführen: Meldung muss im nächsten gerenderten Frame und bis zum vorgesehenen
Benutzerwechsel sichtbar bleiben. Für diese kleine Anzeigeänderung genügt eine
gezielte UI-Abnahme; keine umfangreiche neue Testinfrastruktur erforderlich.

<a id="a26-10"></a>
## A26-10: Lange Chatnachricht fehlt im Eingabeverlauf

**Status: im Code behoben. Beleg: Mixin und Minecraft-1.21.11-ChatScreen.**

**Auslöser:** Eine aufgeteilte lange Chatnachricht senden, Chat erneut öffnen und
Pfeil nach oben drücken. Der vollständige gesendete Text ist nicht im Verlauf;
stattdessen erscheint gegebenenfalls eine ältere Nachricht.

**Fundstelle:** [ChatScreenMixin.java, Zeile 45](../../src/client/java/de/ottoextra/mixin/ChatScreenMixin.java#L45)
empfängt `addToHistory`, verwendet es jedoch nicht. Der Abbruch in Zeile 58 liegt
vor dem regulären `ChatScreen.sendMessage`-Pfad, der in Minecraft den Text dem
Verlauf hinzufügt. Die Queue sendet direkt über den Netzwerkhandler und holt das
nicht nach.

**Korrektur und Abnahme:** Bei `addToHistory=true` den vollständigen ursprünglichen
Text genau einmal dem Verlauf hinzufügen, bevor der normale Screenpfad abgebrochen
wird. Bei `false` nichts ergänzen. Keine einzelnen Chunks zusätzlich eintragen.
Der frühe Split bleibt notwendig, weil Vanilla den Text vor dem normalen Versand
auf die Chatlänge kürzt. Langtext senden und per Pfeil-hoch vollständig zurückholen;
kurze Nachrichten und Befehle behalten ihren bisherigen Verlauf.

<a id="a26-11"></a>
## A26-11: Arbeitsseiten und Courier-Buchseiten klar benennen

**Status: Verbesserung umgesetzt; kein neu nachgewiesener Transportfehler.**

**Beobachtung:** Der Editor nennt seine lokalen Arbeitsseiten schlicht „Seite“.
Die Buchvorschau zeigt dagegen die durch Courier simulierten echten Ausgabeseiten.
Beide Zähler können sich bei langem oder formatiertem Inhalt unterscheiden. Das
erschwert die Einschätzung, wie lang der fertige Brief wird.

**Fundstellen:** [LetterEditorScreen.java, Zeile 1387](../../src/client/java/de/ottoextra/letter/ui/LetterEditorScreen.java#L1387)
zeigt `draft.pages.size()`.
[LetterPreviewScreen.java, Zeile 39](../../src/client/java/de/ottoextra/letter/ui/LetterPreviewScreen.java#L39)
verwendet `LetterServices.previewPages`.
[LetterV2Planner.java, Zeile 23](../../src/client/java/de/ottoextra/letter/send/LetterV2Planner.java#L23)
kennzeichnet die alten 10-Zeilen-/248-Zeichen-/816-Pixel-Budgets ausdrücklich als
lokale Editorbudgets.

**Kleine Verbesserung:** Im Editor „Arbeitsseite X/Y“, in der Vorschau „Buchseite
X/Y“ verwenden. Im Editor zusätzlich „Voraussichtlich N Buchseiten“ aus derselben
Planung wie die Vorschau anzeigen; bei nicht planbarem Inhalt einen verständlichen
Status anstelle eines scheinbar gültigen Zählers. Berechnung nach Textänderungen
aktualisieren bzw. zwischenspeichern, statt sie in jedem Renderframe neu auszuführen.

Die Zeilenbreite bleibt in Pixeln zu beurteilen: Die Planung verwendet 108 Pixel,
und Minecraft-Zeichen haben unterschiedliche Breiten. Eine feste Spaltenzahl oder
das pauschale Umstellen auf 14 Editorzeilen bildet Courier-Seiten nicht zuverlässig
ab. Serverpagination und geprüfte Schreibbefehle müssen für diese Beschriftungsänderung
nicht verändert werden. Live-Markierungen der tatsächlichen Buchgrenzen wären ein
weiterer, größerer UI-Schritt und sind für den kleinen Verbesserungsvorschlag optional.

**Abnahme:** Mehrseitiger, formatierter Brief und importiertes Buch: Begriffe sind
eindeutig, der angezeigte Buchseitenzähler stimmt mit der Vorschau überein.
Deutsch/Englisch und 1280 × 720 bei GUI-Skalierung 3 prüfen; Texte und Aktionen
müssen in das vorhandene adaptive Layout passen.

<a id="a26-12"></a>
## A26-12: Import-Seitenangabe überlappt den Sperrhinweis

**Status: im Code behoben. Beleg: vorhandene Clientaufnahme und feste Textpositionen.**

**Auslöser und Folge:** Im englischen Editor wird ein bereits beschriebenes Buch
importiert. Die Beschriftung `Page 2/2` überlappt teilweise `Already written`.
Der Schutz des alten Textes funktioniert; die Lesbarkeit der Kopfzeile leidet.

**Fundstelle:** [LetterEditorScreen.java, Zeile 1387](../../src/client/java/de/ottoextra/letter/ui/LetterEditorScreen.java#L1387)
zeichnet die Seitenangabe und in Zeile 1397 den Sperrhinweis mit nur 44 Pixeln
festem horizontalem Abstand auf derselben Höhe. Der Platz richtet sich nicht nach
der tatsächlich übersetzten Textbreite. Die Beobachtung ist im
[V2-Laufbericht](../briefmod/e2e-v2-2026-09-08.md#offene-visuelle-beobachtungen) festgehalten.

**Korrektur und Abnahme:** Texte anhand gemessener Breiten mit Mindestabstand
anordnen oder den Sperrhinweis in eine eigene Zeile setzen. Mit A26-11 gemeinsam
gestalten. Import mit ein- und zweistelligen Seitenzahlen in Deutsch/Englisch und
kleinem Fenster prüfen. Eine visuelle Abnahme reicht für diese Layoutkorrektur.
