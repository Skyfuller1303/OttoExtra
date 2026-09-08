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
Die Einstellungen betreffen die **von OttoExtra dargestellten Register-Nametags**,
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
Tierregister-Daten geändert. [Release-Validierung](releases/0.1.17-e.md).
