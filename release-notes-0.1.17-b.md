# OttoExtra 0.1.17-b – Alpha

8. September 2026. Korrektur der Schreibmaterial-Prüfung aus `0.1.17-a`.

## Installation

Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
Fabric API **ab 0.141.4+1.21.11**. Die bisherige OttoExtra-JAR im Clientprofil durch
**`ottoextra-0.1.17-b.jar`** ersetzen; nur eine OttoExtra-Version gleichzeitig laden.

## Briefschreiben

Ein einzelnes Pergament in der Hand wird jetzt auch bei zusätzlichem Vorrat
rechts davon, im Hauptinventar oder in der Nebenhand beschrieben. Die bisherige
inventarweite Sperre und ihre irreführende Meldung sind korrigiert.

Courier verbraucht Schreibmaterial von links nach rechts durch die Hotbar.
Liegt ähnliches Material vor dem aktiven Slot, nennt der Editor nun diesen Grund.
**Ein einzelnes Pergament im ersten Hotbar-Slot ist eine sichere Anordnung**;
der Vorrat darf im restlichen Inventar bleiben. Ein echter Stapel in der Haupthand
muss weiterhin geteilt werden. Bei einer Ablehnung bleibt der Entwurf erhalten.

Bitte mit mehreren getrennten Vorräten, mehrseitigen Briefen und dem erhaltenen
Buch testen. [Ursache und gezielte Nachweise](docs/briefmod/pergament-slot-fix-0.1.17-b.md),
[abschließende Release-Validierung](docs/releases/0.1.17-b.md).

Automatisch geprüft: **80/80 echte lokale Client-/Serverfälle** mit der
bereitgestellten Courier-Version und **648 Unit-Tests**, jeweils ohne Fehler.

Die übrigen Funktionen, standardmäßiges lokales Chatprotokoll und offenen
Beobachtungen aus [0.1.17-a](release-notes-0.1.17-a.md) bleiben bestehen.
