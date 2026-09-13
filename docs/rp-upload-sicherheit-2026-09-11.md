# RP-Upload, Löschstatus und Backups: Umsetzung

**Aktueller Stand: [0.1.17-n mit individueller Freigabe und Gesamtauswahl](rp-upload-freigabe-2026-09-11.md).** Der Betreiber hat die technische Wiederfreigabe beauftragt; der API-Zugang ist für authentifizierte Meldungen geöffnet. Auf jedem Client bleiben Online- und Upload-Zustimmung erforderlich. Der folgende Bericht dokumentiert die ursprüngliche Sicherheitsumsetzung vor der Wiederfreigabe. Betreiber bestätigt: netcup erstellt keine Backups. Weitere Tatsachen und Bewertung stehen im [Entscheidungsvermerk](rp-interessenabwaegung.md).

**Website-Nachtrag, 11.09.2026, 17:43:24 UTC:** Abschnitt 6 der Datenschutzerklärung beschreibt nun ausdrücklich die individuellen Freigaben für Online-Abfragen und RP-Uploads sowie Abwahl, Übermittlungsdaten und private Vorschläge. Hoster-Backupangabe aktualisiert. Nur `OttoExtra_Web/datenschutz.php` veröffentlicht; PHP-/HTML-Prüfung, SHA-256 und identische gerenderte HTTPS-Ausgabe mit Status 200 bestätigt. Neuer Quellhash: `46143695876b5cd99b4edaaf327a5ac60ed4f93be0768ee0d1b9eb9c50c703e2`. Dieser Nachtrag ersetzt den älteren Website-Hash im ursprünglichen Deployment-Nachweis; [Folgepatch](../tools/consent-smoke/privacy-notice-client-consent.patch) nach `rp-security.patch` anwenden. Client und Backend unverändert.

## Technischer Stand

| Bereich | Ergebnis |
|---|---|
| Alter Upload | `community-participant-rp-name` antwortet mit **410**, bevor ein Body als RP-Anfrage gespeichert wird. Alte Clients können diesen Zugang nicht weiter benutzen. |
| Neuer Upload | `POST /v2/rp-names/observations`, vorhandene Mojang-Challenge/JWT-Authentifizierung, signierte Antworten. Absender ausschließlich aus dem Token. Fehlende Anmeldung: **401**. Serverfreigabe derzeit aus. |
| Berechtigungen | Eigene Namen können nach Freigabe bestätigt werden. Fremde Angaben sind ausschließlich private Vorschläge; sie überschreiben oder löschen keinen fremden bestätigten Namen. Admin kann Vorschläge über die CLI prüfen und übernehmen. |
| Eingaben | Maximal 2.048 Body-Bytes; 120 Unicode-Zeichen im RP-Namen; keine Steuerzeichen/HTML-Klammern; begrenzte UUID-/Accountnamenformate, widersprüchliche Identitäten und zusätzliche Absenderfelder abgelehnt. |
| Missbrauchsschutz | 30 authentifizierte Versuche pro Absender und Minute, zusätzlich 120 pro IP und Minute. IP-Schlüssel für diese RP-Limits gehasht. Je Absender/Ziel höchstens ein aktueller Vorschlag; Frist sechs Kalendermonate. |
| Protokollierung | Der neue Handler schreibt keine vollständigen Request-Bodies/IP-Adressen in die alten RP-Anfrageprotokolle. Vorhandene allgemeine V2-/Webserver-Protokolle sind dadurch nicht abgeschafft; deren Regeln bleiben gesondert relevant. |
| Auth-Replay | Der bereits lokal geprüfte atomare Verbrauch einer Challenge ist jetzt auch live installiert. Der Live-Abgleich zeigte, dass genau dieser Fix zuvor noch fehlte. |
| Import | Selbst/Admin-bestätigte Namen und deren Fristen bleiben bei normalen Imports unverändert. Gesperrte UUIDs erhalten auch durch Imports keine neue RP-Zuordnung. |
| Client | Vorhandener authentifizierter HTTP-Transport statt separatem anonymem POST. Übertragung enthält keine behauptete Absender-UUID mehr. Upload- und Online-Freigabe werden geprüft; Widerruf verhindert weitere Jobs/erneute Versuche. Eigener neuer Zustimmungsscope verhindert spätere stille Übernahme alter Freigaben. |
| Datenbankstart | Bootstrap-Prüfungen hängen nicht mehr von Funktionen des Legacy-Controllers ab. Die RP-Spalte kann auch vor dem ersten Import der dynamischen Namensspalte angelegt werden. |

## Löschstatus und Wiederherstellung

Der maßgebliche Steuerzustand liegt in `OttoRegions Web/var/rp-private/state.php`, getrennt von der Datenbank und von den verwalteten RP-Sicherungen. Er enthält die feste ursprüngliche Migrationszeit, den Wartungs-/Freigabestatus und minimale UUID-Prüfwerte für Sperren. Diese Prüfwerte sind **pseudonym, nicht anonym**. Keine gelöschten RP-Namen werden als Sperrliste aufbewahrt. Sperren müssen so lange beachtet werden, wie die weitere Verarbeitung verhindert werden muss; Erforderlichkeit und spätere Aufhebung gehören zur Betreiberprüfung.

Der Hoster beschränkt PHP mit `open_basedir` auf bestimmte Verzeichnisse. Deshalb ist der Steuerzustand im Webverzeichnis untergebracht. **`.htaccess` allein hat im Live-Test statische Dateien nicht gesperrt und ist ausdrücklich nicht die tragende Schutzmaßnahme.** Zustand, Sperrdatei und atomare Zwischenstände sind PHP-Dateien mit festem `404`/`exit`/`__halt_compiler`-Vorspann. Der Inhalt wird nur als Datei gelesen, nie als Konfiguration ausgeführt. Direktaufrufe von `state.php` und `state.lock.php` über beide API-Domains wurden mit 404 geprüft.

Dateirechte: SSH-Benutzer `market` verwaltet den Zustand; der PHP-Benutzer `mattisdettmann` erhält eine gezielte Lese-/Verzeichnis-ACL. Die ACL bleibt auch bei atomarem Austausch der Zustandsdatei erhalten und wurde nach solchem Austausch live geprüft. Verschlüsselungsschlüssel und Backups bleiben außerhalb des Webverzeichnisses und sind nur für den Verwaltungsbenutzer zugänglich.

Bei Löschung/Widerspruch wird zuerst der Sperrstatus unter exklusiver Dateisperre gesichert. Dann werden RP-Zuordnung, zugehörige Vorschläge sowie zuordenbare RP-Protokolle entfernt. Auch alte nur mit Accountnamen protokollierte Anfragen werden berücksichtigt. Unbekannte frühere Aliase, zusätzliche Logsysteme und andere Datentypen müssen im konkreten Betroffenenfall zusätzlich geprüft werden.

Während Wiederherstellungen schließen Legacy- und V2-API die Ausgabe mit 503. Ein Fehler lässt diesen Zustand bestehen. Erst aktuelle Sperren anwenden, abgelaufene RP-Daten bereinigen und alle Chargen abschließen, dann die Ausgabe öffnen. Die Altdatenfrist bleibt an **11.09.2026, 14:10:01 UTC** gebunden; ein altes Backup startet keine neue sechsmonatige Frist.

Eine vollständige VM-/Dateisystem-Wiederherstellung kann auch die Steuerdatei zurücksetzen. Deshalb muss hierfür der **aktuelle Steuerzustand aus einer separat aktuellen Sicherung** vorliegen und vor Öffnung eingespielt werden. Die neue RP-Backupfunktion löst keine ungeprüfte Wiederherstellung eines kompletten netcup-Snapshots. Lokale Spielerexporte sind ebenfalls nicht automatisch fernlöschbar.

## Verwaltete RP-Backups

- Täglich **03:27 Uhr Serverzeit**, zusätzlich zur RP-Bereinigung um 03:17 Uhr. Bestehende Jobs bleiben erhalten. Ersten zeitgesteuerten Lauf noch beobachten.
- Verzeichnis neben `public_html`: `ottoextra-rp-private/backups/`. Enthalten sind ausschließlich RP-Zuordnungen, zwei RP-Protokolltabellen und Vorschläge. Kein vollständiger Datenbankdump; keine Kopie des aktuellen Sperrstatus oder des Schlüssels im Archiv.
- **30 Tage** für diese Backupkopien, Bereinigung bei nächstem erfolgreichen täglichen Lauf. Abgelaufene Kopien werden vor dem Erstellen der neuen Sicherung entfernt, sodass ein Fehler beim Sichern die Rotation nicht verhindert.
- Streaming-Verschlüsselung mit libsodium Secretstream, XChaCha20-Poly1305; authentifizierte Reihenfolge, Abschlussmarkierung und vollständige Leseprüfung nach Erstellung. Manipulation, abgeschnittene Archive und angehängte Daten werden abgelehnt.
- Verarbeitung jeweils eines Datensatzes, höchstens 16 MiB pro Datensatz; keine Begrenzung der gesamten Datenbank auf 32 MiB. Auswahl per unbuffered PDO, Zeitstempel in UTC, vorherige Session-Zeitzone und PDO-Einstellung werden wiederhergestellt.
- Erste Live-Sicherung: **47.470.400 Bytes, 65.694 Datensätze**, am 11.09.2026 um 15:33:44 UTC erstellt und vollständig auf Authentizität/Entschlüsselbarkeit geprüft. Keine Live-Datenbank aus dieser Sicherung zurückgespielt, keine Spieler wegen eines Tests gelöscht.
- **Betreiberbestätigung vom 11.09.2026: netcup erstellt keine Backups.** Diese Hosterfrage ist beantwortet; die eigenen RP-Backups bleiben aktiv. Selbst angelegte externe/manuelle Dumps oder vollständige Dateisystem-Kopien sind davon nicht erfasst. Die gefundene lokale Serverdatei `schema.sql` war ein kleines Schema, kein belegter Datenbankdump.

## Bedienung auf dem API-Server

Alle folgenden Befehle im Backendverzeichnis als Verwaltungsbenutzer ausführen. Nur verifizierte Betroffenenanträge bearbeiten. Diese CLI ist über HTTP gesperrt. Bei verwalteten RP-Restores werden ausschließlich vorhandene Spielerzuordnungen wiederhergestellt; keine Accounts neu erzeugt.

```sh
php tools/rp-privacy.php status
php tools/rp-privacy.php proposals
php tools/rp-privacy.php approve-proposal ABSENDER-UUID ZIEL-UUID --apply
php tools/rp-privacy.php reject-proposal ABSENDER-UUID ZIEL-UUID --apply

# Nach Prüfung von Identität und Antrag:
php tools/rp-privacy.php block SPIELER-UUID --apply
# Nur nach geklärter Aufhebung der Sperre; keine alten Namen wiederherstellen:
php tools/rp-privacy.php unblock SPIELER-UUID --apply

php tools/rp-privacy.php rotate
php tools/rp-privacy.php backup --apply
# Explizites Ersetzen des RP-Bestands durch eine verwaltete Sicherung:
php tools/rp-privacy.php restore rp-backup-ZEITSTEMPEL-ZUFALL.box --apply

# Für einen separat durchgeführten vollständigen Datenbank-Restore:
php tools/rp-privacy.php restore-begin --apply
# Erst jetzt Datenbank wiederherstellen; aktuellen Steuerzustand erhalten.
php tools/rp-privacy.php restore-finish --apply
```

`restore-finish` nur nach Abschluss des Datenbank-Restores ausführen. Fehler oder verbleibende Chargen lassen die API geschlossen; Ursache korrigieren und erneut ausführen. Steuerdatei, ACL und Schlüssel bei allgemeinen Deployments nicht überschreiben/entfernen. Eine Wiederfreigabe von Uploads ist absichtlich kein beiläufiger CLI-Schalter.

## Tests und Live-Abnahme

- **790 Java-Tests**, keine Fehler/ausgelassenen Tests; `test build -x dist --console=plain` erfolgreich. Neue Tests nutzen echte signierte HTTP-Antworten für Upload, Token-Erneuerung, Widerruf, gefälschte Erfolgs-/401-Signaturen und die aktive Freigabesperre. Die freigegebene Transportstrecke wird isoliert unter einer Testfreigabe geprüft; der reale öffentliche Einstieg bleibt gesperrt.
- **47 Sicherheits-/Backupprüfungen je Datenbank** auf MariaDB 10.11.19 und MySQL 8.4.11: zwei Identitäten, fremde Angaben, Fristen, Sperren, Restore, SQL-Fehler, Ciphertext-Manipulation, Backupgrenze und über 32 MiB große Sicherung/Wiederherstellung unter 64 MiB PHP-Speicher.
- **26 Aufbewahrungsprüfungen je Datenbank** weiterhin bestanden; bestehender Auth-Selbsttest und 23 External-Change-Log-Prüfungen bestanden.
- **15 echte PHP-HTTP-/CLI-/Importprüfungen** mit vollständigen Front-Controllern und isolierter Datenbank bestanden, einschließlich Vorschlagsfreigabe und tatsächlichem Import gesperrter/bestätigter Namen.
- **12 installierte Dateien** nach letztem Patch per SHA-256 zurückgelesen. Beide Domains: private Steuerdateien 404. Alter Upload 410; neuer Upload ohne Token 401, falsche Methode 405; öffentliche Spielerliste 200 mit 7.064 Einträgen. Datenschutzhinweis online geprüft.

Die ersten Live-Versuche fanden die PHP-Pfadbeschränkung und einen Datenbestand über der ursprünglichen Backupgrenze. Während der Pfadkorrektur lieferte die API vorsorglich 503. Der abschließende Live-Test bestätigt wieder funktionierende Lesezugriffe und Zugriffsschutz; die Backupfunktion wurde danach auf Streaming umgestellt und erneut getestet.

Quellsicherung vor Beginn: `modpack-backups/20260911-165733-rp-security/`. Nachweise unter `deployment/` darin; Quellpatch unter [tools/consent-smoke/rp-security.patch](../tools/consent-smoke/rp-security.patch). Testwerkzeuge und Testdaten wurden nicht auf die API hochgeladen. Der Client wurde gebaut; die zuvor installierte Launcher-JAR wurde bei diesem Backend-Schritt nicht ersetzt. Vor Freigabe des neuen Upload-Protokolls ist ein eindeutig versionierter Client bereitzustellen.
