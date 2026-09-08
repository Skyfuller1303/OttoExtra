# P2-Umsetzung vom 6. September 2026

Alle 16 zu Beginn dieser Arbeit noch offenen P2-Befunde sind korrigiert. CONFIG-02 war bereits mit P1 behoben; MAP-01 bleibt mit seinem vorhandenen Patch und offener visueller Ingame-Abnahme erhalten. Bestehende Worktree-Änderungen wurden beibehalten.

## Änderungen und Regressionen

| Befunde | Ergebnis | Regression |
| --- | --- | --- |
| CHAT-01 / CHAT-03 | Strikter Fortschritt, begrenzte Teile und Unicode-sichere Schnitt-/Markergrenzen. | [ChatP2RegressionTest.java](../../src/test/java/de/ottoextra/chat/ChatP2RegressionTest.java) |
| CHAT-02 | Keine mehrdeutige Kürzung empfangenen Textes; vorhandene Suffix-Tests auf Texttreue angepasst. | [ChatP2RegressionTest.java](../../src/test/java/de/ottoextra/chat/ChatP2RegressionTest.java) |
| CONFIG-01 | Bytegetreue, eindeutige Fehlersicherung vor Defaults; fehlgeschlagene Sicherung sperrt Saves. | [ConfigLoadRecoveryTest.java](../../src/test/java/de/ottoextra/config/ConfigLoadRecoveryTest.java) |
| HORSES-01 / HORSES-02 | Retry nach fünf Sekunden; Generation und Anfragefolge schützen Cache und laufende Requests. | [HorseRegistryLifecycleTest.java](../../src/test/java/de/ottoextra/horses/HorseRegistryLifecycleTest.java) |
| LETTER-02 | Preflight vor Schreibstart; finale optimierte Seiten bestimmen PAGE-/V2-Befehle und Submit-Queue. | [AnnouncementPreflightFlowTest.java](../../src/test/java/de/ottoextra/letter/ui/AnnouncementPreflightFlowTest.java) |
| NAMETAGS-01 | Erfasste PlayerEntity-UUID statt Namensfallback; fremde und wiederverwendete States bleiben korrekt. | [NametagIdentityTest.java](../../src/test/java/de/ottoextra/nametags/NametagIdentityTest.java) |
| PACK-01 / PACK-02 / PACK-03 | Dateiprüfung beim Aktualitätsvergleich, Reload nach Austausch, gespeicherte Vanilla-Deaktivierung respektiert. | [PackRecoveryTest.java](../../src/test/java/de/ottoextra/resourcepack/PackRecoveryTest.java) |
| PLAYERLIST-01 | Eine transitive Sortierhierarchie über Status, RP-Schlüssel und Vanilla-Gleichstand. | [PlayerListComparatorTest.java](../../src/test/java/de/ottoextra/playerlist/PlayerListComparatorTest.java) |
| REGIONS-01 | Alte Sync-Antworten ändern weder neuen Bootstrap/Snapshot noch neue Abfragesperren. | [RegionSyncLifecycleTest.java](../../src/test/java/de/ottoextra/regions/RegionSyncLifecycleTest.java) |
| RPNAMES-01 | Widerruf invalidiert wartende Jobs auch bei erneutem Opt-in; Deduplikation erlaubt spätere neue Beobachtungen. | [RpNameConsentQueueTest.java](../../src/test/java/de/ottoextra/rpnames/upload/RpNameConsentQueueTest.java) |
| RPNAMES-02 | UUID-Widersprüche verhindern Profilübernahme; beide Identitäten überleben Speichern/Laden und Umbenennen. | [IdentityReuseTest.java](../../src/test/java/de/ottoextra/rpnames/store/IdentityReuseTest.java) |
| TWEAKS-01 | Hauptschalter stoppt Blur sofort; explizite Vorschau bleibt separat steuerbar. | [LowHealthBlurTest.java](../../src/test/java/de/ottoextra/tweaks/LowHealthBlurTest.java) |

## Ausgeführte Prüfung

- Chat-Gegenbeispiele vor der Korrektur ausgeführt: alle drei neuen Tests schlugen am ursprünglichen Verhalten fehl.
- 20 neue P2-Regressionstests in elf Testklassen. Sie prüfen unter anderem steuerbare Futures bei Reconnect, eine blockierte Uploadqueue mit Widerruf, temporäre Config-/ZIP-/Profildateien und die Optimierung vor Planbildung. Keine echten Chatnachrichten oder RP-Uploads versandt.
- Gezielte Regressionen und angrenzende Tests ausgeführt; anschließend `.\gradlew.bat test build -x dist --console=plain`: **BUILD SUCCESSFUL**, 22 Sekunden. **414 Tests, 93 Suites, 0 Fehler, 0 übersprungen** laut frisch erzeugten JUnit-XML-Dateien. Die Gesamtsumme umfasst den vollständigen aktuellen Worktree einschließlich vorhandener XaeroPlus-Tests.
- Eigene Änderungen gegen den vor Arbeitsbeginn gesicherten Worktree geprüft. Bestehende öffentliche Methodensignaturen bleiben für Add-ons erhalten. Die Entity-Mixins nutzen ihre vorhandenen Injection-Ziele; neue Pflichtabhängigkeiten oder Mixin-Einträge sind nicht hinzugekommen.
- Remappte JAR geprüft: Version `1.16.3-k`, `environment=client`, 22 bestehende Client-Mixins. `dist` ausgeschlossen, da der vorhandene Build-Finalizer sonst dortige JARs löscht. Keine Installation in ein Minecraft-Profil und keine Versionsanhebung.
- Separat gesicherte JAR, damit weitere Builds das Prüfartefakt nicht überschreiben: [build/p2-fixes/ottoextra-1.16.3-k.jar](../../build/p2-fixes/ottoextra-1.16.3-k.jar), 30.810.284 Bytes.
- SHA-256: `f19fb9a35a5f519df03edc17d44def85e636b9727cc22a19d952144255ddeb42`.

## Grenzen und Ingame-Abnahme

Der Codefix-Status ist von der visuellen und Multiplayer-Abnahme zu unterscheiden. Minecraft wurde für diese Arbeit nicht gestartet; es gab keinen Live-API-Test.

1. Nametags: Spieler und gleichnamiges Tier/NPC prüfen, einschließlich EntityCulling, F1 und Ausblendungsregeln. Unzugeordnete rohe Renderstates bleiben bei Vanilla.
2. Ressourcenpakete: Fehlende ZIP bei erhaltenem State erneut laden; sichtbare Texturänderung nach erfolgreichem Austausch prüfen. Deaktivierung im Vanilla-Menü muss über Neustarts erhalten bleiben; Erstinstallation darf weiterhin automatisch aktivieren. Die automatisierten Tests decken Dateiprüfung und Aktivierungs-/Reloadentscheidung ab, keinen echten Download oder GPU-Reload. Windows-Dateisperren beim ZIP-Austausch bleiben ein separat zu prüfender Fall.
3. Verkündung: Öffnen/Optimieren/Abbrechen ohne Schreibstart; nach Bestätigung PAGE und V2 einschließlich optionalem Submit und Disconnect-Recovery prüfen. Der Preflight prüft die final vorhergesagten Seiten.
4. Low Health: Hauptschalter aus bei niedrigen HP, anschließend an/aus sowie explizites Test/Stop prüfen.
5. Pferde/Regionen: Wiederholungen nach API-Ausfall und schneller Reconnect mit realer Verbindung; die Antwortreihenfolgen sind bereits isoliert abgesichert.
6. MAP-01: bestehende [XaeroPlus-Abnahme](p2-mittel/MAP-01-xaeroplus-zeichnungen-verdeckt.md) bleibt offen.

CHAT-02 nimmt bewusst keine spekulative Reparatur mehr vor: Ohne eindeutiges Protokollsignal können auch echte serverseitige Duplikate sichtbar bleiben. Die übrige Chatformatierung und die Prüfung auf unbeabsichtigte Textänderungen bleiben bestehen.
