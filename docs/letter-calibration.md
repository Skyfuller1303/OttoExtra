> Historischer Stand vor dem Audit vom 08.09.2026. Die Annahmen über CourierNew 1.3.6, Reset-Füllbefehle und feste Server-Pixelbudgets gelten nicht für die gelieferte Courier-2.1.0. Aktueller Vertrag, Abweichungen und Tests: [Briefmod-Audit](briefmod/README.md).

# Briefmod-V2-Konformitätsprüfung

Die Kalibriergrenzen von zwölf Zeilen, 248 Zeichen und 816 summierten Pixeln stammen
nicht aus der sichtbaren CourierNew-Klasse. Der Live-Test mit dem vollständigen Serverpfad
bestätigt sie jedoch weiterhin: Ein längerer Editorblock wird dort bereits bei „. At“ auf
eine Folgeseite verschoben. V2 kombiniert deshalb die Pluginlogik mit diesen beobachteten
Servergrenzen.

Die automatischen Tests prüfen deshalb direkt:

- Argumentnormalisierung, abschließendes Leerzeichen, `\\n` und `&`-Formatcodes,
- Anhängen bei Seitenlängen 1 bis 255 sowie neue Seiten bei 0 und ab 256,
- visuelle Aufteilung auf 108 Pixel und 14 Zeilen,
- Commandlänge bis 256 Zeichen,
- schriftpaketunabhängige Leerzeilenfüllung und exakte erneute Ausführung aller Commands,
- Fortsetzung aus einem importierten CourierNew-Buchzustand.

Für die manuelle Serverprüfung werden Vorschau und zugestelltes Buch seitenweise
verglichen. Kalibriercommands führen ausschließlich `/letter`, niemals `/post` oder den
Verkündungsbefehl, aus.
