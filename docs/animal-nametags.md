# Geschlecht in Tier-Nametags

Ab Alpha 0.1.17-e unter **Einstellungen → Tierinfo → Grundlagen → Tier-Nametag**:

- **Geschlecht im Nametag anzeigen** ergänzt bei bekanntem Geschlecht ein rosa
  `♀` oder blaues `♂` hinter dem registrierten Tiernamen.
- **Tiernamen nach Geschlecht färben** färbt den Namen selbst rosa bzw. blau.
  Diese Option funktioniert unabhängig vom Symbol.

| Geschlecht anzeigen | Namen färben | Beispiel für ein weibliches Tier |
| --- | --- | --- |
| Aus | Aus | Goldener Name `Lotte` |
| An | Aus | Goldener Name `Lotte` mit rosa `♀` |
| Aus | An | Rosa Name `Lotte` |
| An | An | Rosa Name `Lotte ♀` |

Beide Schalter sind standardmäßig aus. Gespeichert werden
`animalInfo.nametagShowGender` und `animalInfo.nametagGenderColor`.
Speichern, erneutes Laden, Verwerfen und Wiederherstellen von Einstellungen
behandeln beide Werte unabhängig. Vorhandene Konfigurationen behalten die
bisherige Darstellung.

## Ungespeicherte Tiere und lokaler Cache (Alpha F)

**Auch ungespeicherte Tiere** (`animalInfo.nametagIncludeUnregistered`) erweitert
die beiden Geschlechtsoptionen auf Tiere ohne Register-Eintrag. Der Schalter ist
zunächst aus; zusätzlich muss das Geschlechtssymbol oder die Namensfarbe aktiv sein.
Beim ersten Anvisieren muss OttoExtra zunächst die normale Tierinfo erhalten.
Danach gilt auch für dieses Tier die gewählte Darstellung.

Ein vorhandener Nametag beziehungsweise eigener Tiername bleibt erhalten.
Fehlt ein solcher Name, wird die Tierbezeichnung aus dem Toast verwendet,
beispielsweise `Rotvieh Rind ♀`. Ein eingetragener Register-Name hat Vorrang.
Auch Tiere wie Wölfe können ihre bekannten Toast-Daten verwenden. Ohne bekannte
Geschlechtsdaten erzeugt OttoExtra keinen zusätzlichen Nametag.

Der lokale Zwischenspeicher verwendet dieselben Beobachtungen, die bereits über
das Toast-Ereignis ankommen. Er enthält **bis zu 1.024 Tiere pro Verbindung**, nach
Entity-UUID getrennt. Bei voller Kapazität wird die älteste Beobachtung verdrängt;
neue Antworten aktualisieren vorhandene Einträge. Verspätete ältere Antworten
überschreiben keine neueren Beobachtungen.

Die Daten liegen ausschließlich im **Arbeitsspeicher** und werden beim Disconnect
geleert. Es entstehen keine Cache-Datei, keine automatischen Register-Einträge
und keine zusätzlichen Serverabfragen für die Nametags. Bei erneutem Anvisieren
in derselben Verbindung kann der Nametag sofort auf bekannte Werte zurückgreifen.
Nach erneutem Verbinden kommen die Werte wieder mit der normalen Tierinfo.

Der kurze Toast-Cache (standardmäßig fünf Sekunden, höchstens 128 Einträge) bleibt
separat: Er steuert das bestehende Abfrageverhalten für veränderliche Werte wie
Mast und Paarungsbereitschaft. Der Nametag-Speicher verhindert diese normalen
Aktualisierungen nicht. Eine dauerhafte lokale Datei ist für diese Anzeige nicht
nötig und würde zusätzliche Regeln für veraltete Daten erfordern.

## Daten und Darstellung

Die letzte bekannte Tierinfo zur UUID hat Vorrang. Fehlt dort ein erkanntes
Geschlecht, dienen bereits geladene, zugängliche Tierregister-Daten als Ersatz.
Bei unbekanntem oder widersprüchlichem Geschlecht bleibt der Name unverändert;
ein Symbol wird dann nicht ergänzt. Der Renderpfad führt keine Abfragen aus.

Die Farben entsprechen dem Tierinfo-Toast: weiblich `#FF79B8`, männlich `#68B8FF`.
Deutsch, Englisch, Stute, Hengst, Wallach und die Geschlechtssymbole werden erkannt.
Sonstige Textformatierungen bleiben erhalten. Die Entity selbst wird nicht umbenannt.
Entspricht der Vanilla-Nametag bereits dem registrierten Namen, wird nur diese
Zeile dekoriert. Ein anderslautender Vanilla-Name bleibt neben dem Register-Namen
wie bisher bestehen.

Die bestehende Sichtbarkeit gilt weiter: anvisiertes unterstütztes Tier,
kein geöffnetes Menü, eingeblendetes HUD und weniger als 64 Blöcke Entfernung.
Die Einstellungen betreffen die **von OttoExtra dargestellten Tier-Nametags**,
nicht Spieler-Nametags oder den Toast. Das separate ältere AnimalBreeding-Addon
übernimmt bei Installation weiterhin seinen eigenen Pferde-Nametag; dessen
Darstellung wird durch diese Core-Einstellungen nicht verändert.

## Prüfung

- Alle vier Kombinationen, bekannte Aliase, unbekanntes/widersprüchliches
  Geschlecht, Formatierung und Originaltext-Erhaltung.
- Passende UUID, Beobachtungs-Vorrang, Datenbank-Ersatz und Leeren nach Disconnect.
- Speichern/Laden, Snapshot-Wiederherstellung und unabhängige Einstellungswerte.
- `UI-ANIMAL-NAMETAGS`: echter Minecraft-Client mit Kuh-Entity und produktiver
  Render-State-Vorbereitung einschließlich Mixin. Erfasst die ausgegebenen
  Label-Befehle und prüft alle Kombinationen, Vermeidung doppelter Nametags,
  Zielwechsel, ausgeblendetes HUD und Entfernung.
- Der Clienttest betätigt beide neuen Menüschalter und lässt anschließend nur
  die Namensfarbe aktiviert. Die Menüansicht wird als Screenshot geprüft.

Die Tierdaten des Clienttests stammen aus einem isolierten Test-API-Ersatz;
der lokale Paper-Server stellt die Welt bereit. Es werden keine produktiven
Tierregister-Daten geändert. Der Fall prüft ab Alpha F außerdem ungespeicherte
Kühe und Wölfe, das Toast-Beobachtungsereignis, eigene Tiernamen, erneutes
Anvisieren, frische Geschlechtswerte, Register-Vorrang und Cache-Leeren.
Der Menütest betätigt auch den dritten Schalter. Unit-Tests prüfen die
Speichergrenze, Verdrängung, veraltete Antworten und alle acht Schalterkombinationen.
[Aktuelle Release-Validierung](releases/0.1.17-f.md).
