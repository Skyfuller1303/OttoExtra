# OttoExtra: erneute Fehlerprüfung vom 08.09.2026

**Alle zwölf Einträge umgesetzt: 1 × P1, 7 × P2, 4 × P3.** Darunter sind elf
Fehlerkorrekturen und eine Verbesserung zur Briefseitenanzeige. Es bleibt kein
Eintrag dieser Liste ohne Codekorrektur. Änderungen, aktuelle Tests und verbleibende
Abnahmegrenzen: [UMSETZUNG.md](UMSETZUNG.md).

Abschlussprüfung: **647 Unit-Tests und 73/73 Server-/Clientfälle bestanden**;
vollständiger Build erfolgreich. Die geprüfte JAR ist im Umsetzungsbericht verlinkt.

Geprüft wurde der aktuelle, bereits umfangreich veränderte Arbeitsstand von
OttoExtra `1.16.3-w`, einschließlich unversionierter Quellen. Die früheren
[23 Auditbefunde](../bugs/README.md) besitzen bereits Codekorrekturen und werden
hier nicht erneut als offen gezählt. Die neuen Kennungen `A26-…` sind unabhängig
von den alten Bug-IDs.

## Prioritäten und Befunde

P1 bedeutet hohe Dringlichkeit wegen ungewollter Aktionen über Verbindungsgrenzen.
P2 umfasst bedingten Datenverlust, falsche Zustände, Ressourcenlecks und missachtete
Einstellungen. P3 umfasst begrenzte Bedienungs-/Anzeigefehler und kleine
Verbesserungen. Die Einstufung bewertet Auswirkung und Auslöser; die Belegart steht
gesondert dabei. Innerhalb einer Stufe besteht keine zusätzliche Rangfolge.

| ID | Stufe | Befund | Beleg |
| --- | --- | --- | --- |
| A26-01 | P1 | [Langchat sendet Resttext über eine neue Verbindung](p1-hoch.md#a26-01) | Original-Sender mit kontrollierten Handlern ausgeführt |
| A26-02 | P2 | [Verwerfen/Restore übernimmt Chatprotokollierung nicht](p2-mittel.md#a26-02) | Originalklassen ausgeführt |
| A26-03 | P2 | [RP-Datenreparatur kann neue Daten verlieren](p2-mittel.md#a26-03) | Beide Fehlerpfade mit Originalklasse ausgeführt |
| A26-04 | P2 | [Kartenrenderer gibt Texturansicht nicht frei](p2-mittel.md#a26-04) | Produktcode und Minecraft-Implementierung |
| A26-05 | P2 | [Langchat ignoriert RP-Formatierungsoption und Kanal](p2-mittel.md#a26-05) | Aufrufkette und ausgeführter Splittervergleich |
| A26-06 | P2 | [Neue Langchat-Nachricht setzt laufende Sendepause zurück](p2-mittel.md#a26-06) | Original-Sender ausgeführt |
| A26-07 | P2 | [Kontoübersicht zeigt alte Daten mit frischem Status](p2-mittel.md#a26-07) | Asynchrone Aufruf- und Renderpfade |
| A26-08 | P2 | [Geänderte direkte Ressourcenpaketquelle wird ignoriert](p2-mittel.md#a26-08) | Konfigurations- und Installationspfad |
| A26-09 | P3 | [Backup-/Reparaturrückmeldung verschwindet sofort](p3-niedrig.md#a26-09) | Klick-Callback und UI-Neuaufbau |
| A26-10 | P3 | [Lange Chatnachricht fehlt im Eingabeverlauf](p3-niedrig.md#a26-10) | Mixin und Minecraft-ChatScreen |
| A26-11 | P3 | [Arbeitsseiten und Courier-Buchseiten klar benennen](p3-niedrig.md#a26-11) | Verbesserung aus Editor-/Vorschauvergleich |
| A26-12 | P3 | [Import-Seitenangabe überlappt Sperrhinweis](p3-niedrig.md#a26-12) | Code und vorhandene Clientaufnahme |

Jeder Eintrag enthält Auslöser, Auswirkung, Fundstellen, einen begrenzten
Korrekturvorschlag und Abnahmekriterien. Quellenlinks und Zeilenangaben beziehen
sich auf den Prüfstand dieses Datums.

## Ausgeführte Prüfung

Die folgenden Zahlen beschreiben den Auditstand vor den Korrekturen.
Der aktuelle Fixstand steht im [Umsetzungsbericht](UMSETZUNG.md).

Der frische Lauf `gradlew.bat test --rerun-tasks -x dist --console=plain` besteht
**637 Tests in 114 Suites, ohne Fehler oder übersprungene Tests**. Die gezielten
Fehlernachweise zeigen Lücken in dieser vorhandenen Testabdeckung. Ein grüner
Gesamtlauf widerlegt diese Gegenbeispiele nicht.

Aufbau, Ergebnisse, Wiederholung und Grenzen stehen in
[VALIDIERUNG.md](VALIDIERUNG.md). Die Nachweisskripte liegen unter
[tools/audit-2026-09-08](../../tools/audit-2026-09-08/README.md).

Der zuvor abgeschlossene [V2-Lauf mit lokalem Paper-Server und Fabric-Client](../briefmod/e2e-v2-2026-09-08.md)
bestand 66/66 Fälle. Er prüft die Briefmod. Er ist keine Laufzeitabnahme der hier
neu gefundenen Chat-, Konto-, Speicher- oder Kartenfehler.

## Bereits bekannte offene Abnahmen

Diese Punkte bleiben außerhalb der zwölf neuen Einträge:

- [Veraltete Tablisten-Einträge](../bugs/TABLIST-VERALTETE-EINTRAEGE.md): vorhandene
  Nutzermeldung und Diagnose, Ursache weiterhin offen.
- [Release-Nachprüfung](../release-1.17-review.md): dort dokumentierte
  Modkombinations- und Ingame-Abnahmen.
- [Briefeditor: zeitweise fehlende Aktionszeile in Screenshots](../briefmod/e2e-v2-2026-09-08.md#offene-visuelle-beobachtungen):
  Bedienbarkeit und Widgetgrenzen bestanden; weder Produktionsfehler noch
  harmloser Aufnahmeeffekt nachgewiesen. Weitere visuelle Untersuchung erforderlich.

Die Korrekturen wurden mit Verbindungsbindung und Datenerhalt begonnen und um die
übrigen P2- sowie P3-Punkte ergänzt. Die oben gesondert aufgeführten älteren
Beobachtungen bleiben davon getrennt.
