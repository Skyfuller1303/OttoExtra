# Kampfmodus-HUD

Unter **Tweaks → Kampfmodus → Kampfmodus-Icon anzeigen** (standardmäßig an).
Die Anzeige läuft nur auf Ottonien und nur mit einer Waffe in der Haupthand.
Leere Hand, Werkzeuge, offener Bildschirm, F1, Tod und Zuschauermodus blenden sie aus.

Das Icon liegt bei `Fensterbreite / 2 + 101`, `Fensterhöhe - 19`, also gespiegelt
zum Offhand-Item. Ist rechts bereits die Offhand oder Hotbar-Angriffsanzeige,
rückt es um 29 Pixel weiter nach rechts. Die GUI-Skalierung folgt Minecraft.

Die bestehenden Hooks in `ClientPlayNetworkHandlerMixin` liefern
`GameMessageS2CPacket` mit `overlay=true` sowie `OverlayMessageS2CPacket`.
Die Nachrichten werden nur gelesen. Die Mod sendet keine Kampfaktionen.

| Waffenklasse | Techniken |
| --- | --- |
| Klingen | Halbschwerttechnik, Flinker Schnitt, Aderlass |
| Stumpf | Wuchthau, Dreschen, Glockenschlag |
| Stangen | Entsatteln, Kühner Stich, Pfählen |
| Fernkampf | Sehnenschuss, Pfeilhagel, Ins Schwarze |

Waffen werden zuerst anhand ihres echten Itemtyps eingegrenzt. Bei Schwertern
unterscheidet die Mod die Ottonien-Klassen anhand von `minecraft:item_model`,
expliziten Kategorien in der Lore und den Namen aus `Resourcepack/cit/items/waffen`.
Ein Schwert ohne erkennbare Spezialklasse gilt als Klingenwaffe. Vollständig
umbenannte alte Waffen ohne Modell-/Kategorieinformation lassen sich damit
nicht sicher unterscheiden.

Bis eine eindeutige passende Actionbar-Nachricht eintrifft, zeigt das HUD die
Waffenklasse mit `?`. Der Modus wird pro Kampfklasse für die Sitzung behalten;
ein neuer Spieler-/Verbindungszustand oder Ausschalten setzt die Erkennung zurück.
Diese Zuordnung muss noch mit dem tatsächlichen Serververhalten beim Wechsel
zwischen zwei Waffen derselben Klasse abgeglichen werden. Unbeteiligte
Actionbars und Meldungen über nicht erlernte Techniken ändern den Modus nicht.

Die 16 PNG-Dateien in `assets/ottoextra/textures/gui/combat` sind unveränderte
Kopien aus `Resourcepack/ottonien-reformed-update/assets/ottonien/textures/item/gui`.
Techniken verwenden die Variante `_0` als festes Symbol; die Stufe wird nicht
ausgelesen. Die vier Klassen-Icons dienen der noch unbekannten Auswahl.

Für die Zuordnung von Stumpf- und Fernkampftechniken wurden außerdem die
offiziellen Wiki-Seiten [Stumpfwaffenkampf](https://wiki.ottonien.com/w/Stumpfwaffenkampf)
und [Fernkampf](https://wiki.ottonien.com/w/Fernkampf) abgeglichen.

## Prüfung

Automatisierte Tests decken alle zwölf Techniken, Formatierungen/Umlaute,
mehrdeutige/ablehnende Meldungen, Waffenklassen, Modusspeicherung, Config-Restore,
Icon-Ressourcen und die Position rechts neben der Hotbar ab.

Ingame noch prüfen: je Waffenklasse einen echten Moduswechsel durchführen,
zwischen Waffen wechseln, Waffe wegstecken, Tweaks-Schalter und F1 testen,
mit linker Haupthand/rechter Offhand sowie Hotbar-Angriffsanzeige testen.
Der genaue Actionbar-Wortlaut und die Speicherung des Modus auf dem Server
sind noch nicht anhand einer Live-Sitzung bestätigt.
