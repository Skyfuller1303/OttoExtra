# Titelfarben

Unter **RP-Namen → Basis → Farben** gibt es den Schalter
**Adelsfarbe auch für andere Titel**. Er ist standardmäßig aus, auch beim
Laden einer vorhandenen Konfiguration ohne diese Option.

Aus: Der angezeigte Titel bestimmt die Farbe. Ein adliger Legat trägt deshalb
die Custom-Titelfarbe; Ritter, Dame, Graf, Gräfin und die entsprechenden
Adelstitel-Varianten behalten die Adelsfarbe.

An: Der über die API bekannte Adelsstand färbt auch andere Titel dieser Person.
Explizite Titelfarben im Katalog und persönliche Farbanpassungen haben Vorrang.

| Titel | Standardfarbe |
| --- | --- |
| Vorkoster / Vorkosterin | `#ea7749` |
| Legende / Legendin | `#6a9fae` |
| Chronist / Chronistin | `#86a0b3` |

Diese drei Farben bleiben auch bei aktivierter Adelsübernahme bestehen.
Die gemeinsame Auflösung wird von Chat, Tabliste, Namensschildern, Kennenlernen
und den Standardfarben im Personenbuch verwendet. Bei bekannten Titeln bestimmt
die Titelkategorie auch gegenüber einem abweichenden Team-/Klerusstand die Farbe.
Für unbekannte Titel bleibt Team/Klerus als Rückfall erhalten.

Der Katalog wurde am 07.09.2026 mit
[Stände und Titel im Ottonien-Wiki](https://wiki.ottonien.com/w/St%C3%A4nde_und_Titel)
abgeglichen: 134 Wiki-Zeilen sind abgedeckt. Ergänzt wurden Kreuzfahrer/in,
Federzunge, Komplementär, Gildenmitglied und Verwaltungsnonne. Letztere gehört
farblich zum Klerus; Gelehrter zu den gewöhnlichen Fertigkeitstiteln.
Adels-Reskins behalten die im Wiki gekennzeichnete Adelszuordnung.
Legende/Legendin und die drei exakten Hexwerte stammen aus der Projektvorgabe;
Legende ist auf der geprüften Wiki-Seite noch nicht aufgeführt.

Bestehende mitgelieferte Wiki-Kataloge werden einmalig aktualisiert. Persönliche
Titelfarben, bearbeitete Kategoriefarben, deaktivierte Einträge und eigene
Titelvarianten bleiben erhalten. Eigenständige benutzerdefinierte Kataloge
werden nicht mit Standardtiteln aufgefüllt. Nach dem Update entfernte neue
Einträge werden beim nächsten Start nicht erneut angelegt.

Technisch: `rpnames.nobilityColorForAllTitles` in der Hauptkonfiguration und
`bundledRevision: 1` im Titelkatalog. Keine zusätzliche Netzwerkabfrage im Spiel.
