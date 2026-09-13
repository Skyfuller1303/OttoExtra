# Chatprotokoll-Regression

Aus dem OttoExtra-Projektverzeichnis:

```powershell
.\gradlew.bat --offline --console=plain -I tools/chat-log-regression/client.init.gradle runClientGameTest
```

Der isolierte Minecraft-1.21.11-Client verwendet `build/chat-log-regression/run`;
DevAuth und die optionalen Kartenmods sind für diesen Lauf ausgeschaltet.
Es werden synthetische Nachrichten in das tatsächliche, durch Mixins erweiterte
ChatHud eingespeist. Ein externer Server oder Account wird nicht benötigt.

Geprüft werden ursprünglicher RP-Text, signierter und profilfreier Chat, Unicode,
Zeilenumbrüche, echte Wiederholungen, sofortiges Logging bei wartendem Addon,
einmalige Speicherung trotz Replay/History-Refresh, ausgeblendete Tierinfo-Antworten
und das sofortige Aus- und Einschalten des Chatprotokolls. Alle Einträge werden
aus der tatsächlich angelegten Datei
`logs/chat/ottoextra-chat-JJJJ-MM-TT-1.log` gelesen. Eine erneute Anzeige oder ein
Toggle darf weder eine zweite Datei noch doppelte Einträge erzeugen.

Zum Abschluss hängt der Test Minecrafts `LatestFile`-Appender vorübergehend ab.
Eine weitere Nachricht muss trotzdem sofort in der eigenen Chatdatei stehen,
während sie in `latest.log` fehlt. Der Appender wird im `finally` wieder angehängt.
Damit wird ausdrücklich geprüft, dass die Ablage keine Minecraft-Logrotation,
Komprimierung oder funktionierende Minecraft-Dateiausgabe voraussetzt.

Ein erfolgreicher Lauf meldet `CHAT_SESSION_LOG_PASS`. `ChatMessageLogTest`
ergänzt Prüfungen für neue Sitzungen, fortlaufende Nummern mit Lücken, lokalen
Datumswechsel, zurückgestellte Uhren, parallele Clients, sofortigen Flush,
unveränderte Altdateien und Schreibfehler.

Die Testmod wird nur durch das Init-Skript aktiviert und ist nicht Teil des
regulären Mod-JARs. Die Änderungen liegen im Client-Mixin und in `ChatMessageLog`.

Der Fix ruft weiterhin `ChatHud.logChatMessage(ChatHudLine)` auf und unterdrückt
mit `WrapWithCondition` nur dessen späteren Aufruf innerhalb von `addMessage`.
Andere Mixins auf dem Logger selbst bleiben wirksam. Mods, die den Ablauf von
`ChatHud.addMessage` ersetzen oder dessen Logging-Aufruf umleiten, benötigen
gegebenenfalls eine gesonderte Kompatibilitätsprüfung.
