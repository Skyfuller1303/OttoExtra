> Historischer Stand vor dem Audit vom 08.09.2026. Die Annahmen über CourierNew 1.3.6, Reset-Füllbefehle und feste Server-Pixelbudgets gelten nicht für die gelieferte Courier-2.1.0. Aktueller Vertrag, Abweichungen und Tests: [Briefmod-Audit](briefmod/README.md).

# CourierNew-Protokoll der Briefmod V2

Grundlage ist das lokal analysierte Plugin `CourierNew` 1.3.6. OttoExtra bindet das
Plugin nicht als Dependency ein, sondern bildet nur sein beobachtetes Befehlsprotokoll ab.

## `/letter`

- Bukkit liefert die Argumente getrennt; CourierNew setzt sie mit genau einem Leerzeichen
  hinter jedem Argument wieder zusammen.
- Die wörtliche Folge `\\n` wird zu einem Zeilenumbruch.
- Gültige `&`-Farbcodes werden in Minecraft-Formatierungen übersetzt.
- Bei einer vorhandenen letzten Seite mit Länge 1 bis 255 wird der Payload angehängt.
- Bei leerer oder mindestens 256 Zeichen langer letzter Seite wird eine neue Seite erzeugt.
- CourierNew besitzt keine Pixel-, Zeilen- oder Wortbudget-Pagination.

V2 teilt den sichtbaren Inhalt clientseitig anhand der Live-Server-Grenzen auf: 108 Pixel
pro Zeile, höchstens zwölf Zeilen, höchstens 816 summierte gerenderte Pixel und ein sicherer
`/letter`-Payload pro Seite. Diese zusätzlichen Grenzen stammen nicht aus der sichtbaren
CourierNew-Klasse, sind aber im vollständigen Serverpfad reproduzierbar. Nach jeder
Seite außer der letzten füllen unsichtbare Formatresets die gespeicherte CourierNew-Seite bis
zur 256er-Grenze. Sie liegen hinter dem sichtbaren Seiteninhalt und funktionieren unabhängig
vom aktiven Schriftpaket. Vorschau und Empfängerbuch erhalten dadurch dieselben Seiten.

## Einschränkungen

- Wörtliches `\\n` ist durch das Serverprotokoll nicht verlustfrei darstellbar.
- ASCII-Leerraum wird so normalisiert, wie CourierNew die Bukkit-Argumente zusammensetzt.
