# Briefmod-Audit gegen Ottonien Courier 2.1.0

Prüfdatum: 08.09.2026. Referenz sind der Quellbaum und die angepasste JAR unter
`input/` im übergeordneten Workspace. Der bestehende, bereits umfangreich geänderte
OttoExtra-Arbeitsstand war Ausgangspunkt; ein Vergleich nur mit Git-HEAD würde
zusätzlich fremde Vorarbeiten enthalten.

- [Serververtrag und Quellen](courier-2.1.0-contract.md)
- [Abweichungen, Korrekturen und verbleibende Grenzen](abweichungen.md)
- [Ausgeführte Tests und manuelle Serverprüfung](testlaeufe.md)
- [Automatisierter V2-Lauf mit lokalem Server und Client](e2e-v2-2026-09-08.md)
- [Nachfolgende Auditfixes: Seitenbeschriftung, Buchseitenzähler und erneute Clienttests](../audit-2026-09-08/UMSETZUNG.md)

Der zentrale Befund: Die bisher nachgebildete CourierNew-1.3.6-Logik passt nicht zur
gelieferten Courier-2.1.0. Das aktuelle Plugin fügt beim Anhängen Zeilenumbrüche ein
und berechnet Seiten aus Zeichenlänge und geschätzten Zeilen. OttoExtra simuliert
diesen Algorithmus jetzt direkt. Reset-Füllbefehle wurden entfernt.

Die Arbeitsseiten im Editor dienen weiterhin der Textbearbeitung. Ihre historischen
10-Zeilen-/816-Pixel-Grenzen erzwingen keine Buchseiten auf dem Server. Die separate
Buchvorschau zeigt die simulierten Courier-Seiten, einschließlich importiertem
Anfang und tatsächlichen Formatübergängen. Leere Zeilen werden mit Figure Spaces
übertragen; in der Vorschau sind diese optisch leer.

Der wiederholbare [JAR-Vertragstest](../../tools/test-courier-contract.ps1) führt die
echte Schreibmethode und den echten MessageManager der gelieferten JAR aus. Damit
ist der Abgleich unabhängig von den OttoExtra-Modelltests. Er ersetzt keinen
End-to-End-Test mit Paper, MythicItems, Permissions, Leseskills und Zustellung.
