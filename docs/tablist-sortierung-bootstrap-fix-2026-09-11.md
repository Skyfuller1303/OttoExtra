# Lehen-Sortierung: Bootstrap-Limit korrigiert

Stand: 11.09.2026, OttoExtra **0.1.17-o**.

## Ursache

Im tatsächlich laufenden Ottonien-Profil waren Sortierung, RP-Modul und
Online-Dienste aktiviert. Nach dem Neustart funktionierte die Minecraft-/API-
Anmeldung. Der signierte `/v2/bootstrap` lieferte jedoch **5.546.504 Bytes**;
das allgemeine JSON-Limit betrug **4.194.304 Bytes** (4 MiB).

Der HTTP-Client verwarf die Antwort als `IOException`. Der Regionsdienst konnte
seinen Bootstrap deshalb nicht abschließen; die Sortierung wartete mit leerem
Spielerindex. Die früher abgelaufene Minecraft-Sitzung war ein zusätzliches
Problem und erklärte den Fehler nach der Neuanmeldung nicht.

## Änderung

- `BoundedBody.BOOTSTRAP_JSON_BYTES`: begrenzte 16 MiB für den Gesamtstand.
- `HttpOttoExtraApiClient.fetch`: nur die exakte Bootstrap-URI erhält dieses
  Limit. Andere JSON-Abrufe behalten 4 MiB, einschließlich Sync.
- Online-Zustimmung, HTTPS, Antwortsignaturen und die bestehenden
  Abbruch-/Zeitgrenzen bleiben wirksam. Kein unsignierter Ausweichabruf.
- BetterTab-Mixins und Sortierlogik mussten nicht geändert werden.

## Nachweis

- Neuer Regressionstest mit einer signierten Antwort über 5 MiB: vor dem Fix
  fehlgeschlagen, danach erfolgreich; Fraktions- und Regionsdaten werden gelesen.
- Antworten über 16 MiB bleiben für Bootstrap gesperrt; Sync bleibt auf 4 MiB
  begrenzt. Vorhandene Tests prüfen auch gestreamte Überschreitungen und Signaturen.
- Vollständiger Build: **797 Tests, keine Fehler**, Dedicated-Serverstart bestanden.
- Die getestete HTTP-Klasse wurde einmalig in der laufenden Spielsitzung ersetzt.
  Danach wurde der normale Regionsabruf erneut ausgeführt: Bootstrap vollständig,
  **339 zugeordnete Spieler** im Sortierindex.
- Beide tatsächlich transformierten BetterTab-Ausführungspfade wurden auf dem
  Renderthread mit den aktuellen Online-Spielern geprüft: **61 Einträge,
  60 zugeordnet, 34 Lehen-Gruppen, 0 Reihenfolgefehler**. Zuschauer werden bei
  dieser Gruppenprüfung getrennt behandelt; unbekannte Spieler dürfen nicht
  zwischen bekannten Lehen-Gruppen stehen.

Die fehlgeschlagenen Bootstrap-Wiederholungen und Diagnoseabrufe hatten zuvor
das Stundenlimit ausgelöst. Ausschließlich der Bootstrap-Zähler des betroffenen
Betreiberkontos für das aktuelle Zeitfenster wurde einmalig zurückgesetzt
(21 Versuche). Die Rate-Limits selbst und andere Konten blieben unverändert.
Der kurzzeitig geschützte Wiederherstellungshelfer auf dem Backend wurde entfernt.

## Artefakt und Installation

- JAR: `build/libs/ottoextra-0.1.17-o.jar`, **31.086.040 Bytes**.
- SHA-256: `616fc3de6815537227c3c7b8061110aa320c306b1c0b43a8ef1365edb5aa6806`.
- Dauerhafte Installation im Profil **Ottonien 1.0.0** vorbereitet: Die JAR liegt
  geprüft als `.jar.pending` bereit. Der einmalige Helfer wartet auf Spielende,
  sichert die bisherige `0.1.17-n` und ersetzt sie. Einstellungen werden nicht geändert.
- Die laufende Sitzung verwendet bereits die korrigierte HTTP-Methode, meldet
  bis zum Neustart weiterhin die beim Start geladene Version `0.1.17-n`.
- Belege und unveränderliches Release-Artefakt:
  `modpack-backups/20260911-212713-tablist-bootstrap/`.
