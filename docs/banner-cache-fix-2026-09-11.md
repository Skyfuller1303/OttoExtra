# Fehlende Lehensbanner: 0.1.17-p

## Ursache und Änderung

Die laufende Mod hielt exakt **32 Bannertexturen**, ohne offene Downloads oder
Fehler-Wartezeiten. Der bisherige Größencheck verhinderte anschließend jedes
weitere Banner, einschließlich bereits lokal gespeicherter PNGs. Aktivitätenanzeige,
Regionsanzeige und Karte teilen diesen Dienst und waren gemeinsam betroffen.

Im betroffenen Profil lagen 112 gültige Bannerdateien: 111 mit 16 × 16 Pixeln,
eine mit 220 × 220 Pixeln. Zusammen benötigen deren dekodierte RGBA-Pixel nur
307.264 Bytes. Eine feste Sperre nach 32 Einträgen war hierfür ungeeignet.

- `BannerTextureBudget`: höchstens 256 Einträge und 32 MiB dekodierte Pixel pro
  Dienst. Bei Überschreitung werden zuletzt selten benutzte Banner verdrängt.
  Dies ist ein Bilddatenbudget, keine Garantie für den gesamten RAM-/VRAM-Verbrauch.
- `BannerTextureService`: neue Banner werden auch bei gefülltem Cache geladen.
  Verdrängte dynamische Texturen werden auf dem Renderthread geschlossen; ihre
  Dateien bleiben erhalten und können erneut lokal geladen werden. Mitgelieferte
  Ressourcen bleiben im Eigentum von Minecraft und werden nicht zerstört.
- Auch Auffrischungen beachten vier gleichzeitige Ladevorgänge. Fehlgeschlagene
  Auffrischungen können nach der bestehenden Wartezeit von 45 Sekunden erneut
  versucht werden, statt am 20-Minuten-Zeitstempel hängen zu bleiben.
- `RegionActivityHud`: verwendet dieselbe Regionsbanner-Auswahl wie die Karte,
  einschließlich Überschreibungen, Vorschlagsbildern und kanonischen Cache-Schlüsseln.
  Die getrennten `activity-*`-Einträge werden nicht mehr neu erzeugt.
- Bildgrößenprüfung, Cache-Dateibudget und Download-Zustimmung bleiben bestehen.

## Tests

- Neuer echter Clienttest: vor der Korrektur am **33. Banner fehlgeschlagen**.
- Nach der Korrektur: **80 lokale Banner gleichzeitig geladen und sichtbar**,
  ohne Bilddownload-Zustimmung und ohne Netzwerkabrufe. Screenshot kontrolliert.
- Kleiner Testcache: Verdrängung schließt das native Bild; dasselbe Banner lässt
  sich anschließend aus seiner Datei wieder laden. Keine dauerhaft blockierte Lücke.
- Sechs Unit-Tests: Anzahl, Pixelbudget, Aktualisierung vorhandener Einträge,
  zuletzt verwendete Banner, mitgelieferte Bilder und zu große Einzelbilder.
- Vollständiger Build: **803 Tests bestanden**. Clienttests und Dedicated-Serverstart
  bestanden. JAR-Metadaten und Ausschluss der Testtreiber geprüft.

## Testversion

`build/libs/ottoextra-0.1.17-p.jar` — **31.088.125 Bytes**.

SHA-256: `4d17cafcadd56124a2d733a620d745ee7ca43576491e232f7678917839fa057e`.

Die Version enthält ebenfalls den Bootstrap-/Lehen-Sortierungsfix aus 0.1.17-o.
Im Profil **Ottonien 1.0.0** wurde 0.1.17-p um **22:08:30 Uhr MESZ** nach dem
Spielende installiert. Die aktive 0.1.17-n und das ältere vorgemerkte Update
0.1.17-o wurden gesichert und ersetzt. Genau eine aktive Core-JAR liegt im
Mods-Verzeichnis; ihre Prüfsumme stimmt. Einstellungen blieben unverändert.

Der neue Banner-Cache wird beim nächsten Spielstart geladen. Belege und Release-Kopie:
`modpack-backups/20260911-220216-banner-cache/`.
