# P2 – mittlere Priorität

[Übersicht](README.md) · [Ursprüngliche Prüfnachweise](VALIDIERUNG.md) · [Umsetzung und aktuelle Tests](UMSETZUNG.md)

Die Fehlerbeschreibungen und Quellzeilen unten dokumentieren den Stand vor den Fixes.

<a id="a26-02"></a>
## A26-02: Verwerfen und Restore übernehmen Chatprotokollierung nicht

**Status: im Code behoben. Beleg: Originalklassen ausgeführt.**

**Auslöser und Folge:** Chatprotokollierung ist aus; in den Einstellungen einschalten
und anschließend „Verwerfen“ wählen. Die laufende Konfiguration behält `chatLog=true`.
Die lokale Chataufzeichnung bleibt dadurch aktiv. Umgekehrt bleibt ein verworfenes
Ausschalten ebenfalls erhalten. Auch ein Backup mit anderem `chatLog`-Wert wird
im laufenden Configobjekt nicht vollständig übernommen und kann beim nächsten
Speichern wieder mit dem falschen Wert auf die Platte gelangen.

**Fundstellen:** [OttoExtraConfig.java, Zeile 1200](../../src/client/java/de/ottoextra/config/OttoExtraConfig.java#L1200)
kopiert die übrigen Tweaks-Felder, lässt `tweaks.chatLog` jedoch aus.
[OttoExtraSettingsScreen.java, Zeile 184](../../src/client/java/de/ottoextra/config/settings/OttoExtraSettingsScreen.java#L184)
ruft beim Verwerfen `restoreFrom(snapshot)` auf.
[ChatMessageLog.java, Zeile 45](../../src/client/java/de/ottoextra/logging/ChatMessageLog.java#L45)
liest den aktiven Wert.

**Nachweis:** `snapshotJson()` bei `false`, Änderung auf `true`, `restoreFrom(snapshot)`:
`RESTORE_CHATLOG expected=false actual=true`, `SNAPSHOT_EQUAL=false`.
Hier wurde der Configpfad ausgeführt; es wurde kein Benutzerchat aufgezeichnet.
Der frühere CONFIG-02-Fix zu Einwilligungen wird damit nicht als unbehoben erklärt:
Dies betrifft ein zusätzliches ausgelassenes Feld.

**Korrektur und Abnahme:** `chatLog` ausdrücklich übernehmen. Beide Wertrichtungen
mit Snapshot/Restore prüfen; anschließend Backup mit abweichendem Wert laden,
speichern und erneut einlesen. Der aktive und der gespeicherte Wert müssen dem
gewählten Snapshot/Backup entsprechen.

<a id="a26-03"></a>
## A26-03: RP-Datenreparatur kann neue Daten verlieren

**Status: im Code behoben. Beleg: beide Fehlerpfade mit der Originalklasse ausgeführt.**

**Auslöser A:** Bei einem frischen Profil vor dem ersten Serverbeitritt „RP-Daten
reparieren“ ausführen. Es gibt noch keine `known-players.json` und keine Sicherung.
Die Reparatur meldet `NO_VALID_BACKUP` und sperrt das Speichern. Danach angelegte
Personen sind im laufenden Client vorhanden, gehen beim Beenden aber verloren.

**Fundstellen:** [LocalRpIdentityStore.java, Zeile 118](../../src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java#L118):
Der fehlende Dateipfad gelangt über Zeile 140 zur Recovery und setzt in Zeile 208
`persistenceBlocked=true`. Die Speicherprüfung in Zeile 288 verhindert spätere
Writes. Der Store ist schon beim Clientstart initialisiert
([RpNamesModule.java, Zeile 48](../../src/client/java/de/ottoextra/rpnames/RpNamesModule.java#L48));
der Einstellungsdialog macht die Aktion vor dem ersten Join erreichbar.

**Nachweis A:** Nicht existierender Testpfad → `INITIAL=NO_DATA`,
`REPAIR=NO_VALID_BACKUP`, `PERSISTENCE_BLOCKED=true`. Nach `ensureSeen`:
`SIZE=1`, `PENDING_SAVE=null`, auch beim Beenden keine Datendatei.

**Auslöser B:** Bei bereits gesunder Datei unmittelbar nach einer neuen Änderung
reparieren, bevor der verzögerte Speichervorgang ausgeführt wurde. Zeilen 124–127
laden den alten Plattenstand in den laufenden Store und setzen `dirty=false`.
Die noch nicht gespeicherte Änderung wird verworfen. Die ergänzende Laufzeitprobe
legt ein zweites Profil an und repariert nach gemessener 1 ms: Der Speicherbestand
fällt von zwei auf eine Person. Auch nach explizitem Speichern und Shutdown enthält
die Datei nur das erste Profil. Die [Nachweisskripte](../../tools/audit-2026-09-08/README.md)
prüfen zusätzlich eine frische Installation ohne Reparatur als funktionierende Kontrolle.

**Korrektur:** Leere Erstinstallation ohne Korruptionsereignis als schreibbaren
`NO_DATA`-Zustand behandeln. Bei gesunder Datei ungespeicherte laufende Daten
erhalten; vor einem ersetzenden Reload kontrolliert sichern oder auf den Reload
verzichten. Die Schutzsperre bei tatsächlich beschädigten Daten ohne brauchbare
Sicherung muss erhalten bleiben.

**Abnahme:** A mit leerem Profil, anschließendem Anlegen und erneutem Laden;
B mit gesunder Datei und noch wartender Änderung. Zusätzlich beschädigte Datei
mit und ohne gültiges Backup prüfen. Testdaten ausschließlich in isolierten Pfaden.

<a id="a26-04"></a>
## A26-04: Kartenrenderer verliert offene GPU-Texturansichten

**Status: im Code behoben. Beleg: Produktcode und Minecraft-1.21.11-Implementierung; keine GPU-Messung.**

**Auslöser und Folge:** Die gemalte Karte wird gerendert; eine Größenänderung oder
GPU-Invalidierung ersetzt ihre Kopiertextur. Die bisherige Texturansicht bleibt
offen. Wiederholungen können Grafikressourcen dauerhaft auflaufen lassen.

**Fundstellen:** [PaintedMapRenderer.java, Zeile 146](../../src/client/java/de/ottoextra/map/PaintedMapRenderer.java#L146)
schließt nur `copyTexture` und setzt `copyView` anschließend auf `null`.
[Zeile 304](../../src/client/java/de/ottoextra/map/PaintedMapRenderer.java#L304)
schließt bei einem Größenwechsel ebenfalls nur die Textur und überschreibt die
View-Referenz. `GlTextureView` hält in Minecraft 1.21.11 eine eigene Referenz auf
die Textur; erst `close()` der View gibt diese frei. Eine geschlossene Textur
wird bei verbleibender View-Referenz nicht endgültig gelöscht.

Eine RGBA8-Textur mit 1920 × 1080 Pixeln benötigt rechnerisch etwa 7,9 MiB für
die Pixeldaten. Das ist eine Größenordnung pro entsprechender Allokation, keine
gemessene Leckrate und keine Aussage, dass jede Kartenansicht diese Größe verwendet.

**Korrektur und Abnahme:** Gemeinsame Freigabe für beide Pfade: erst die View,
dann die Textur schließen und Referenzen zurücksetzen. Mehrfachaufrufe müssen
sicher sein. Lifecycle-Test auf einmalige Freigabe jeder erzeugten View/Textur;
danach Größenwechsel und Ressourcenreload mit sichtbarer Karte und beobachteter
GPU-Ressourcenzahl prüfen. Der Xaero-Framebuffer-Umschaltpfad wurde geprüft und
liefert keinen zusätzlichen belastbaren Befund eines falschen Kopierziels.

<a id="a26-05"></a>
## A26-05: Langchat ignoriert RP-Formatierungsoption und Kanal

**Status: im Code behoben. Beleg: Aufrufkette und ausgeführter Splittervergleich.**

**Auslöser:** Eine Nachricht mit mehr als der konfigurierten Chunklänge und einer
Klammer über der Trennstelle senden, während RP-Formatierung aus ist oder ein
Nicht-RP-Kanal ausgewählt ist. Trotzdem werden zum Schließen/Öffnen der RP-Syntax
Klammern in die Teilnachrichten eingefügt.

**Fundstellen:** [ChatScreenMixin.java, Zeile 55](../../src/client/java/de/ottoextra/mixin/ChatScreenMixin.java#L55)
verwendet die Drei-Argument-Variante von `split`, die
[LongChatSplitter.java](../../src/client/java/de/ottoextra/chat/LongChatSplitter.java#L14)
mit `preserveRpSyntax=true` ausführt. Die korrekte Bedingung
`cfg.rpFormattingEnabled && ChatChannelState.current().rp` steht nur im späteren
[ChatMessageSplitMixin.java, Zeile 25](../../src/client/java/de/ottoextra/mixin/ChatMessageSplitMixin.java#L25).
Der ursprüngliche Screen-Aufruf wird vorher abgebrochen; die bereits gekürzten
Chunks werden dort nicht erneut aufgeteilt.

**Nachweis:** Für `"Start (" + "word ".repeat(80) + "end)"`, Limit 256 und Marker
`" >"` unterscheiden sich Drei-Argument-Ausgabe und Ausgabe mit explizitem
`preserveRpSyntax=false`: Nur die erste ergänzt Klammern an der Trennstelle.
Ausgeführt wurde der Splitter, nicht ein Ingame-Kanalwechsel.

**Korrektur und Abnahme:** Beide Einstiegspunkte müssen dieselbe vorhandene
Bedingung und die Vier-Argument-Variante nutzen. Lange Nachricht bei aktivem RP,
deaktivierter Formatierung und Nicht-RP-Kanal prüfen; nur der aktive RP-Fall darf
synthetische RP-Klammern erhalten. Unicode- und Chunklängengarantien weiter prüfen.

<a id="a26-06"></a>
## A26-06: Neue Langchat-Nachricht setzt laufende Sendepause zurück

**Status: im Code behoben. Beleg: Original-Sender ausgeführt.**

**Auslöser und Folge:** Während ältere Chunks auf ihre Sendepause warten, wird eine
weitere lange Nachricht eingereiht. Der nächste alte Chunk wird sofort beim
nächsten Tick versendet. Der eingestellte Abstand wird unterschritten; serverseitige
Chatbegrenzungen können dadurch greifen. Ein tatsächlicher Serverkick wurde nicht
ausgelöst oder behauptet.

**Fundstelle:** [LongChatSender.java, Zeile 22](../../src/client/java/de/ottoextra/chat/LongChatSender.java#L22)
setzt in jedem `enqueue` den gemeinsamen `cooldown` auf null, auch bei laufendem
Versand. Dieser Fehler ist unabhängig von der fehlenden Verbindungsbindung in A26-01.

**Nachweis:** Mit 1500 ms Pause werden `first` und `second` auf direkt
aufeinanderfolgenden Ticks gesendet, wenn zwischen diesen Ticks `third`
eingereiht wird. Das [gemeinsame Nachweisskript](../../tools/audit-2026-09-08/test-long-chat-sender.ps1)
erfasst die Tickfolge.

**Korrektur und Abnahme:** Eine laufende Pause durch weiteres Einreihen nicht
zurücksetzen. Definieren, ob die Pause auch nach dem letzten Chunk für eine neue
Nachricht gilt, und dieses Verhalten konsistent umsetzen. Während laufender Queue
weitere Nachrichten einreihen: Reihenfolge und konfigurierter Mindestabstand
müssen erhalten bleiben.

<a id="a26-07"></a>
## A26-07: Kontoübersicht zeigt alte Daten mit frischem Status

**Status: im Code behoben. Beleg: asynchrone Aufruf- und Renderpfade; keine Live-API-Abfrage.**

**Auslöser A:** Zweimal aktualisieren, während die erste Anfrage noch läuft.
Die zweite Antwort kommt zuerst, anschließend die erste. Der ältere Inhalt
überschreibt den neueren; auch Ladezustand und Zeitstempel folgen der alten Antwort.
Eine erneute Screen-Initialisierung startet ebenfalls einen Refresh.

**Fundstellen:** [AccountOverviewScreen.java, Zeile 104](../../src/client/java/de/ottoextra/verify/AccountOverviewScreen.java#L104)
startet Anfragen ohne Generationskennung. Die Callbacks in Zeilen 111–119 und
126–141 schreiben direkt in den Screenzustand. Der Refreshbutton bleibt nutzbar.
[AccountLinkApi.java](../../src/client/java/de/ottoextra/verify/AccountLinkApi.java#L51)
stellt separate asynchrone HTTP-Anfragen; deren Antwortreihenfolge ist nicht
gesichert. Die vorhandenen Schutzkennungen im AccountLinkModule schützen diese
Screen-Callbacks nicht.

**Auslöser B:** Nach einer erfolgreichen Abfrage schlägt der nächste Refresh fehl.
Das alte Profil bleibt gesetzt. `renderProfile` zeigt den Fehler nur bei
`profile == null` (Zeile 226); gleichzeitig setzt `markRefreshed()` auch bei
Fehlern einen neuen Zeitstempel (Zeilen 119/141/145). Alte Daten wirken damit frisch.

**Korrektur und Abnahme:** Refreshgeneration erfassen und überholte Antworten
verwerfen. Fehler zusätzlich zu noch angezeigten Altdaten sichtbar machen;
Erfolgszeitpunkt nur bei erfolgreichem Datenempfang ändern. Zwei steuerbare Futures
in Reihenfolge B, A abschließen: B muss sichtbar bleiben. Danach einen fehlgeschlagenen
Refresh prüfen: verständlicher Fehler und unveränderter letzter Erfolgszeitpunkt.

<a id="a26-08"></a>
## A26-08: Geänderte direkte Ressourcenpaketquelle wird ignoriert

**Status: im Code behoben. Beleg: Konfigurations- und Installationspfad; kein Downloadtest.**

**Auslöser:** Im Direktmodus ist die Manifest-URL leer. Eine gültige ZIP aus URL A
ist installiert. `directZipUrl` auf B ändern und den Client neu starten. Trotzdem
bleibt A aktiv; B wird nicht angefordert. Der Standardpfad mit Manifest ist von
diesem konkreten Fehler nicht betroffen.

**Fundstellen:** [OttoExtraConfig.java, Zeile 911](../../src/client/java/de/ottoextra/config/OttoExtraConfig.java#L911)
wählt die direkte Quelle bei leerem Manifest.
[ResourcePackUpdater.java, Zeile 127](../../src/client/java/de/ottoextra/resourcepack/ResourcePackUpdater.java#L127)
beendet den Direktpfad bei jeder brauchbaren lokalen ZIP ohne Abgleich mit `zipUri`.
[PackState.java](../../src/client/java/de/ottoextra/resourcepack/PackState.java#L18)
speichert keine Quellenidentität.

Dies ist ein anderer Auslöser als der behobene
[PACK-01](../bugs/p2-mittel/PACK-01-fehlende-zip-gilt-als-aktuell.md): Dort fehlte
die ZIP oder war ungültig; hier ist sie gültig, stammt aber aus der falschen Quelle.
Updates des Inhalts unter einer unveränderten URL wären ein zusätzlicher
Revalidierungsbedarf und sind nicht Voraussetzung dieses Befunds.

**Korrektur und Abnahme:** Quellenidentität im Installationszustand speichern und
bei geändertem Direktziel erneut herunterladen/installieren. Für alte Zustände
ohne Quellenangabe einen definierten Migrationspfad vorsehen. Mit lokalem HTTP-Testserver
zwei unterscheidbare gültige Pakete A/B anbieten: Nach Quellenwechsel muss B
angefordert, installiert und bei vorher aktiver Auswahl neu geladen werden.
Einwilligung und Benutzerdeaktivierung müssen weiter respektiert werden.
