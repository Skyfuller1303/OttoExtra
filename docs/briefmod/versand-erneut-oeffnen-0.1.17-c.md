# Versand eines bereits geschriebenen Briefs erneut öffnen

Ab Alpha `0.1.17-c`, 8. September 2026.

## Bedienung

Den noch nicht versendeten Courier-Brief in der Haupthand halten, per Rechtsklick
öffnen und neben **Bearbeiten** auf **Verschicken…** klicken. Die bekannte
Empfängerauswahl öffnet sich. **Zurück** führt wieder zur Buchansicht.

Alternativ öffnet `/ottoextra letter send` die Empfängerauswahl für den gehaltenen
Brief. Das funktioniert auch nach erneutem Einloggen oder nach Ablauf des alten
Chat-Hinweises. Erst die Auswahl eines Empfängers und **Brief senden** lösen
`/post` aus. Das erneute Öffnen schreibt keine Buchseite und benötigt kein Pergament.

## Umsetzung

Zuvor war der Einstieg an `pendingDraft` mit zehn Minuten Lebensdauer und an den
nur während der Sitzung gespeicherten Schreibkontext gebunden. Der neue Einstieg
liest den tatsächlichen unversendeten Brief aus der Haupthand und erstellt einen
frischen Versandkontext mit Server, Spieler, aktivem Slot und unveränderten Seiten.
Ein unabhängiger gespeicherter Entwurf bleibt erhalten.

Courier öffnet über `BookManager.openBook` eine temporäre Lesekopie ohne die
ursprüngliche Lore und andere Item-Metadaten. Deshalb wird das tatsächlich
benutzte Item bereits im vorhandenen `UseItemCallback` erfasst. Der vorhandene
`ScreenEvents.AFTER_INIT`-Hook merkt sich die zugehörige Buchansicht. Erst nach
Rückkehr des echten Items ergänzt ein Client-Tick **Bearbeiten** und
**Verschicken…** nebeneinander. Eine bloße Prüfung der kurzzeitig angezeigten
Lesekopie würde empfangene Briefe fälschlich als unversendet einstufen.

Der Zusammenhang zwischen Buchansicht und Original bleibt beim Zurückkehren aus
der Empfängerauswahl erhalten. Er wird beim Disconnect gelöscht; unbenutzte
Ansichten werden nur schwach referenziert. Es sind keine neuen Mixins nötig.
Andere Mods, die die Buchansicht oder Iteminteraktion vollständig ersetzen,
benötigen weiterhin eine eigene Kompatibilitätsabnahme.

Vor dem Buttonklick wird das gehaltene Item erneut mit dem Öffnungszustand
verglichen. Vor `/post` gelten weiterhin die vorhandenen Kontextprüfungen.
Empfangene Briefe und gewöhnliche Bücher bieten den Einstieg nicht an.
Ein laufender Schreibauftrag kann durch das erneute Öffnen nicht ersetzt werden.
Nach einer serverseitigen Versandablehnung kann der erhaltene Brief erneut
geöffnet und die Zustellung bewusst wiederholt werden.

## Automatisierte Abnahme

Die neuen Fälle laufen mit echtem Fabric-Client und der unveränderten angepassten
Courier-2.1.0-JAR auf dem isolierten Paper-Testserver. Die Buchansicht wird über
Minecraft-Iteminteraktion geöffnet; die Versandbuttons werden tatsächlich betätigt.

| Fall | Prüfung |
| --- | --- |
| RP01 | Schreiben, Dialog schließen, Buch öffnen, Verschicken, Zurück, erneut Verschicken; keine zusätzlichen Schreibbefehle |
| RP02 | Vorhandener mehrseitiger Brief nach erneutem Login; Formatierung und separater Entwurf bleiben erhalten |
| RP03 | Echter Clientbefehl verwendet den gehaltenen Brief statt eines veralteten Entwurfs |
| RP04 | Empfangener Brief, gewöhnliches Buch und Brief ausschließlich in der Nebenhand werden nicht übernommen |
| RP05 | Slotwechsel im wieder geöffneten Dialog verhindert `/post` |
| RP06 | Austausch des Buchs vor Buttonklick oder vor Versand wird abgefangen |
| RP07 | Aktiver Schreibauftrag bleibt erhalten und endet mit den erwarteten Buchseiten |
| RP08 | Server lehnt `/post` ab; erneutes Öffnen erlaubt später genau einen weiteren Zustellversuch |
| RP09 | Angezeigtes Nebenhand-Buch bietet keinen Versand eines anderen Haupthand-Briefs an; Bearbeiten des Haupthand-Briefs bleibt erreichbar |

Ergebnisse und Artefakt: [Release-Validierung](../releases/0.1.17-c.md).
