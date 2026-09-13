# RP-Uploads und Gesamtauswahl: Testversion 0.1.17-n

Stand: **11.09.2026**. Auf Betreiberwunsch sind RP-Uploads technisch wieder verfügbar.
Jeder Nutzer muss selbst eine aktuelle Freigabe erteilen. Die bisherige globale
Client-/Serversperre ist aufgehoben; die Datenverarbeitung wird damit nicht als
rechtlich abschließend geprüft bescheinigt. Betreiberangaben und verbleibende
Dokumentationsfragen stehen im [Entscheidungsvermerk](rp-interessenabwaegung.md).

## Bedienung

- Erste Assistentenseite: **Alles erlauben**, **Alles ablehnen** oder **Einzeln entscheiden**.
  Die Übersicht nennt alle fünf Zwecke mit denselben Diensten und Daten wie die Einzelseiten.
  Eine Gesamtauswahl beendet den Assistenten sofort und erhält vorhandene RP-/Anzeigeeinstellungen.
- Bei bereits abgeschlossenem Assistenten: **OttoExtra-Einstellungen → Datenschutz**.
  **OttoExtra-Onlinedienste** und **Beobachtete RP-Namen teilen** jeweils nach dem Hinweis erlauben.
- Standard bleibt aus. Die Gesamtauswahl erteilt genau die fünf aufgeführten Freigaben;
  sie schaltet keine anderen Mod-Einstellungen oder separat geregelten lokalen Chatlogs ein.
- Freigaben lassen sich weiterhin einzeln abschalten. Alte Upload-Jobs werden durch
  erneutes Erlauben nicht wieder gültig. Ablehnen verlangt keine neue Einrichtung.
- Bestehende Online-Freigaben werden erhalten. Alte RP-Upload-Freigaben mit früherem
  Scope werden verworfen; neuer Scope: `authenticated-rp-proposals-v2`.

## Schutz und Live-Stand

Der geschützte V2-Zugang bleibt unverändert: authentifizierter Absender, begrenzte
Eingaben und Anfrageraten, eigene Bestätigung und private Vorschläge für fremde Namen.
Uploads benötigen beide Client-Freigaben. Fremde Vorschläge überschreiben keine
bestätigten Namen; die Administration kann sie nach Prüfung übernehmen. Sperren,
sechs Kalendermonate und verschlüsselte RP-Backups mit 30 Tagen bleiben wirksam.

Am **11.09.2026, 17:58 UTC** wurde ausschließlich `uploads_enabled` im bestehenden
Steuerzustand unter exklusiver Sperre auf `true` gesetzt. Wartungsstatus, Löschsperren
und ursprüngliche Migrationszeit blieben erhalten. Alle **11 Backend-Dateien** des
vorherigen Deployments stimmen weiterhin mit den lokal getesteten Quellen überein.
Keine Testnamen oder Löschanträge wurden in die Produktionsdatenbank geschrieben.

Live geprüft: alter Upload **410**, neuer Upload ohne/mit ungültigem Token **401**,
falsche Methode **405**, private Steuerdateien über beide Domains **404**.
Die [Datenschutzerklärung](https://ottoextra.dev/datenschutz#client-freigaben) beschreibt
nun Verfügbarkeit ab 0.1.17-n und die Sammelauswahl. PHP-Syntax, zurückgelesener
Quellhash und identische gerenderte HTTPS-Ausgabe mit **200** geprüft.

## Nachweise

- **794 Java-Tests**, keine Fehler oder ausgelassenen Tests; Build erfolgreich.
- Signierte Upload-Antworten über die öffentliche Client-Methode, fehlende Einzelzustimmungen,
  Widerruf beider Zwecke, Wiederfreigabe alter Jobs, Token-Erneuerung und ungültige Signaturen geprüft.
- Echter Minecraft-Client mit Dialog-/Mausklicktests: Einzelentscheidung, RP-Freigabe/Widerruf,
  Gesamtfreigabe/-ablehnung, direkter Abschluss, unveränderte RP-Einstellungen und geänderte Quelle.
  Screenshots unter `build/consent-smoke/client/screenshots/`; Übersicht `0000_consent-overview-de.png`.
  Die Test-Gesamtfreigabe wird synchron vor dem nächsten Tick zurückgenommen; keine echte
  OttoExtra-Anmeldung oder Spielerübermittlung im UI-Test.
- **15 PHP-HTTP-/CLI-/Importprüfungen** mit synthetischen Konten und separater MariaDB bestanden.
  Testcontainer anschließend gestoppt.
- Dedizierter Loopback-Server mit der Client-JAR im Mods-Verzeichnis startet und stoppt sauber.
- JAR-Metadaten, DE-/EN-Texte und fehlende Test-/Debugmarker geprüft.

Artefakt: `build/libs/ottoextra-0.1.17-n.jar`, **31.085.957 Bytes**.
SHA-256: `303ddd688a20c0d0e0bbd478713569cad5e8408dfca0f6e0ff081b0bce92f504`.
Website-SHA-256 nach Ergänzung der Sammelauswahl:
`9a5f34cef8994b6a6e8d58e9b8cd5ffc4cf0062d0c1e5e244c0ab9700ea63e8e`.

Quellsicherungen, feste getestete JAR und Deployment-Nachweise liegen unter
`modpack-backups/20260911-195114-rp-upload-freigabe/`. Die neuen Website-Änderungen sind
im [Folgepatch](../tools/consent-smoke/rp-upload-release-website.patch) nach dem vorherigen
`privacy-notice-client-consent.patch` gesichert. Das Aktivieren des Serverzustands
ist eine gesonderte Betreiberaktion und wird nicht durch Anwenden dieses Website-Patches ausgeführt.

## Ottonien-Profil

Das laufende Minecraft sperrt noch `ottoextra-0.1.17-l.jar`. Daher ist der Austausch
beim Stand dieses Vermerks **vorbereitet, noch nicht als installiert bestätigt**.
Ein versteckter einmaliger Helfer wartet höchstens zwölf Stunden auf das Ende des
Profils `Ottonien 1.0.0` und installiert danach die fest gesicherte, geprüfte JAR.
Er beendet kein Spiel, ändert keine Zustimmung und erstellt keinen Autostart-Eintrag.
Wenn sich die vorhandene Core-JAR zwischenzeitlich ändert, bricht er den Austausch ab.

Status: `pending-install-status.json` im Sicherungsordner. Nach erfolgreichem Austausch:
`installation-receipt.json`; alte Core-JAR unter `installed-core-before/`.
Nach Spielende kurz auf den Austausch warten, dann Minecraft neu starten.
Bei bereits beendetem Rechner wird der Helfer nicht automatisch erneut gestartet.
