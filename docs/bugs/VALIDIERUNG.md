# Validierung des Bug-Audits

Datum: 6. September 2026. Dieses Dokument hält die ursprüngliche Audit-Validierung fest. Nachfolgende Codeänderungen und aktuelle Testergebnisse: [P1-UMSETZUNG.md](P1-UMSETZUNG.md). Projektstand und Befundindex: [README.md](README.md).

## Bestandstests

Im Verzeichnis `OttoExtra` ausgeführt:

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
```

Ergebnis: `BUILD SUCCESSFUL in 16s`; alle sechs ausgeführten Gradle-Tasks erfolgreich. Die XML-Ergebnisse unter `build/test-results/test` enthalten 72 Testsuites mit insgesamt 330 Tests, null Fehlern, null Failures und null übersprungenen Tests. Vorheriger Lauf ohne `--rerun-tasks` war nur `UP-TO-DATE`; deshalb wurde frisch ausgeführt.

Kompilierung meldete veraltete APIs; Gradle meldete veraltete Features im Hinblick auf Gradle 10. Diese Warnungen wurden nicht als zusätzlicher Laufzeitbug klassifiziert. Kein vollständiger Release-Build und kein `dist`-Task wurden ausgeführt.

## Lokale Gegenbeispiele ohne Spielstart

[repro-core.jsh](repro-core.jsh) arbeitet für CHAT-01, CHAT-03 und LETTER-03 direkt gegen die frisch kompilierten Projektklassen. PLAYERLIST-01 verwendet ausdrücklich ein kleines Modell der vorhandenen Vergleichsverzweigung und der lokal verifizierten Vanilla-Sortierung; es erzeugt keine Minecraft-Spielerobjekte.

Mit Java 21, aus `OttoExtra`:

```powershell
jshell --class-path build/classes/java/client docs/bugs/repro-core.jsh
```

Beobachtete Ausgabe:

```text
unicode partCount=2, highBeforeMarker=true, lowAtNextStart=true
nested parts=1001, firstLength=4, lastLength=300, lastEqualsInput=true
leadingBlank predicted=[Hallo], commands=[letter Hallo]
leadingTwoBlanks predicted=[Hallo], commands=[letter Hallo]
comparator A<B=true, B<S=true, S<A=true
comparator: java.lang.IllegalArgumentException: Comparison method violates its general contract!
```

Das Skript dokumentiert den fehlerhaften Auditstand. Nach einem Fix soll sich die jeweilige Ausgabe ändern. Es ist kein Ersatz für dauerhafte automatisierte Regressionstests.

## Separater Nachweis der Chat-Reparatur

Für CHAT-02 wurde die private Originalmethode `RpChatCorruptionRepair.duplicateSuffixStart(String)` per Reflection aufgerufen. Dafür waren zusätzlich die lokale Minecraft-common-JAR und Brigadier im JShell-Classpath erforderlich. Dieser zusätzliche Aufruf ist nicht Teil von `repro-core.jsh`.

```java
var cls = Class.forName("de.ottoextra.chat.RpChatCorruptionRepair");
var find = cls.getDeclaredMethod("duplicateSuffixStart", String.class);
find.setAccessible(true);
for (String body : java.util.List.of("*nickt*??", "*lacht* hahaha", "(1+1)=22")) {
    int cut = (int) find.invoke(null, body);
    System.out.println(body + " -> " + body.substring(0, cut));
}
```

Beobachtet:

```text
*nickt*?? -> *nickt*?
*lacht* hahaha -> *lacht* haha
(1+1)=22 -> (1+1)=2
```

Dies belegt die mehrdeutige Kürzungsheuristik. Ihr Einbau in die eingehende Chatpipeline wurde statisch nachverfolgt; kein tatsächlicher Serverchat wurde verändert.

## Versionsbezug der Spielerlistensortierung

Die Vanilla-Reihenfolge wurde im lokalen Minecraft-1.21.11-Quellarchiv unter

```text
.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-clientOnly-6dd721cd7d/
1.21.11-net.fabricmc.yarn.1_21_11.1.21.11+build.6-v2/
```

im Eintrag `net/minecraft/client/gui/hud/PlayerListHud.java`, Zeile 66, geprüft: absteigend nach `listOrder`, anschließend Spectatorstatus, Team und Name. Der nachgebildete Vergleichszyklus benötigt unterschiedliche `listOrder`-Werte. Eine gewöhnliche Liste ohne diese Voraussetzung ist kein Beleg für den Fehler.

## Nicht ausgeführte Prüfungen

Kein Minecraft-Start, Reconnect-Experiment, echter Briefversand, Upload, Live-API-Aufruf, Ressourcenpaket-Download oder Restore von Benutzerdaten. Die entsprechenden Abläufe sind nachvollzogene Codepfade und vorgeschlagene Reproduktionen. Die grünen Bestandstests decken diese neu beschriebenen Grenzfälle nicht ausreichend ab.

Beim ursprünglichen Audit wurden ausschließlich neue Dateien in `docs/bugs` angelegt. Gradle hat daneben reguläre Build-/Cacheausgaben erzeugt. Die anschließende P1-Umsetzung ändert auch Produktcode und Tests; ihr Nachweis steht im oben verlinkten Umsetzungsbericht. Vorhandene Änderungen im Worktree wurden nicht bereinigt.
