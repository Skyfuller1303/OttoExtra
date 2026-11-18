# Tierart und Geschlecht 14 Tage lokal merken

Ab **0.1.17-j** speichert OttoExtra beobachtete Tierarten und Geschlechter auf
diesem Rechner. Dadurch funktionieren F8-Glow und die optionalen Nametags auch
nach einem Disconnect oder Minecraft-Neustart wieder, sobald das Tier geladen ist.

**Einstellungen → Tierinfo → Erweitert → Lokaler Tiercache → Tiergeschlecht
14 Tage lokal merken** ist standardmäßig an. Ausschalten und die Einstellungen
speichern entfernt den Cache des aktuellen Servers beim Weiterlaufen oder
Trennen der Verbindung. Die Daten der aktuellen Sitzung bleiben bis zum
Disconnect für die normale Tierinfo verfügbar. Bei deaktivierter Speicherung
bleibt es beim bisherigen Sitzungsspeicher.

## Welche Daten gespeichert werden

| Wert | Dauerhafte lokale Speicherung |
| --- | --- |
| Entity-UUID, Tierbezeichnung aus der Tierinfo, erkanntes Geschlecht | Ja |
| Zeitpunkt der erhaltenen Tierinfo | Ja, für die Altersprüfung |
| Letzter Mast-Stand, Zuchtbereitschaft, Wartezeiten (Paarung, Wachstum, Scheren), beobachtete Herzchen, Paarungen, Geburten und eigenes Scheren | Ja, als lokale Zeitgeber (ab Dateiversion 2, Feld `live`) |
| Melkstatus, Lebens-/Pferdewerte | Nein |
| Hof, Besitzer, Abstammung, Zugriffsrechte | Nein |
| Register-Name | Kommt weiterhin aus dem bestehenden Tierregister |

Die **14 Tage zählen ab der letzten tatsächlich erhaltenen Tierinfo**.
Ein Wiedersehen oder ein Lesezugriff verlängert sie nicht. Frische Tierinfos
aktualisieren den Cache. Unbekannte oder widersprüchliche Geschlechtsdaten
werden nicht als bekannte Geschlechter gespeichert.

Es bleiben höchstens **1.024 Tiere je Server** erhalten. Bei voller Kapazität
wird die älteste Beobachtung verdrängt. Abgelaufene Werte werden beim Lesen
nicht angezeigt und beim Laden bzw. nächsten Schreiben entfernt. Solange
Minecraft geschlossen ist, läuft keine Bereinigung im Hintergrund.

Die Dateien liegen unter `config/ottoextra/cache/animals/`. Der Dateiname ist
der SHA-256-Wert der Serveradresse; der Dateiinhalt enthält zusätzlich die
Serveradresse zur Prüfung. Verschiedene Serveradressen haben getrennte Dateien.
Die Funktion wird nur für aktive Ottonien-Verbindungen angebunden.

## Warum lokal und nicht automatisch in der Datenbank

Der Cache ist eine Anzeigehilfe für bereits beobachtete Tiere. Er erstellt
keine Registereinträge, lädt keine Zuchtwerte hoch und verändert keine
Berechtigungen. Für dauerhaft gemeinsam gepflegte Zuchtakten bleibt das
bisherige ausdrückliche Speichern im Register bzw. Züchter-Addon zuständig.

`HorseRegistry.observation()` enthält weiterhin ausschließlich Beobachtungen
der aktuellen Sitzung. Die Datei wird nicht in diese Map geladen.
`AnimalInfoService`, Tierakte und `saveName()` greifen für Tierwerte weiterhin
auf die bisherigen Datenquellen zurück. Ein nur aus der Datei bekanntes Tier
gilt daher nicht als frisch abgefragt oder speicherberechtigt.

Für den Glow gilt: erkannte aktuelle Beobachtung, dann bekannte zugängliche
Registerdaten, dann die noch gültige lokale Identität. Ungespeicherte Nametags
nutzen aktuelle Beobachtungen oder die lokale Identität; eigene Entity-Namen
bleiben dabei erhalten.

## Dateizugriff und Fehlerfälle

Ein einzelner Hintergrund-Worker lädt und schreibt die Dateien. Änderungen
werden gebündelt, beim Disconnect nachgeschrieben und beim normalen Clientende
abgewartet. Renderfunktionen lesen nur Speicher; sie führen keine Datei- oder
Netzwerkzugriffe aus. Geschrieben wird über eine temporäre Datei und Umbenennen.

Dateien über 2 MiB, falsche Serverzuordnungen, unbekannte Dateiversionen,
unlesbares JSON, ungültige Einträge und Zeitstempel aus der Zukunft werden
verworfen bzw. ignoriert. Neue Beobachtungen überschreiben beim parallelen
Einlesen keine neueren Daten. Ein defekter Cache verhindert den Spielstart
nicht und kann durch neue Beobachtungen wieder aufgebaut werden.

## Validierung

Unit-Tests prüfen 14-Tage-Grenze, Wiederladen, frische und veraltete Antworten,
Kapazitätsgrenze, ungültige Dateien, Servertrennung, Ausschalten/Löschen und
die Trennung von aktueller Beobachtung und dauerhafter Identität. Ein API-Ersatz
verweigert im Cache-Test jeden unerwarteten Netzwerkaufruf; Speichern einer
Zuchtakte mit ausschließlich alten Cachewerten wird abgelehnt.

Der reale Clienttest `UI-ANIMAL-GLOW` schreibt eine Tieridentität, leert die
Verbindung, lädt die Datei in einen neuen Cache und prüft die wiederhergestellte
Umrissfarbe. Er prüft außerdem den Menüschalter und das Löschen beim Ausschalten.
Nametag- und Tierübersicht-Clienttests sichern die bisherigen Funktionen ab.

[Release-Validierung](releases/0.1.17-j.md).

## Zeitgeber ab Dateiversion 2

Die Datei enthält seit der Statuszeile zusätzlich die Liste `live` mit den Zeiten aus der Tierinfo und eigenen Beobachtungen (`AnimalLiveState`). Alle Zeiten sind absolute Uhrzeiten, laufen also auch bei geschlossenem Spiel weiter. Jeder Eintrag verfällt 14 Tage nach seiner letzten Änderung; die Obergrenze von 1.024 Einträgen gilt getrennt für Identitäten und Zeitgeber. Dateien der Version 1 werden unverändert gelesen (ohne Zeitgeber) und beim nächsten Speichern als Version 2 geschrieben; eine ältere OttoExtra-Version verwirft eine Version-2-Datei und baut den Cache neu auf. Das Ausschalten der lokalen Speicherung löscht auch die Zeitgeber; sie bleiben dann nur bis zum Disconnect im Speicher. In den Einstellungen löscht „Alle Tiere dieses Servers vergessen“ Identitäten und Zeitgeber des verbundenen Servers.
