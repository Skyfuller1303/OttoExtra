# Startseite für OttoExtra 0.1.17

Implementiert in **0.1.17-q** (Minecraft/Fabric 1.21.11).

## Verhalten

- Vor dem Minecraft-Hauptmenü erscheint einmal pro Versionsreihe eine eigene
  Startseite mit dem vorhandenen Schild-Logo, „OttoExtra 0.1.17“ und Funktionsübersicht.
- **Berechtigungen prüfen** öffnet den bestehenden Assistenten mit Zweckübersicht,
  „Alles erlauben“, „Alles ablehnen“ und Einzelentscheidungen.
- **Überspringen** und **Escape** öffnen das Hauptmenü. Sie erteilen keine Zustimmung.
  Gültige Freigaben einschließlich Quellen, Entscheidungszeiten und laufender
  Berechtigungsnachweise bleiben erhalten. Ohne Freigaben bleiben optionale
  Online-Funktionen aus. Einstellungen erlauben spätere Änderungen.
- Zurück auf der Assistentenübersicht und Escape im Assistenten kehren zur
  Startseite zurück. Noch nicht abgeschlossene Einzelentscheidungen werden verworfen.
- Kleine Fenster verwenden eine kompakte Ansicht mit untereinanderliegenden Buttons.
  Texte sind auf Deutsch und Englisch vorhanden.

## Keine pauschale erneute Zustimmung

Ein Logo oder Versionswechsel ändert keinen Datenverarbeitungszweck. Deshalb bleibt
`ConsentPolicy.VERSION` bei 3; bestehende gültige Entscheidungen werden durch diese
Startseite nicht zurückgesetzt. Ändern sich Zwecke oder Quellen, müssen weiterhin
die entsprechenden Consent-Versionen beziehungsweise Scopes angepasst werden.
Die vorhandene Prüfung entzieht veralteten Freigaben vor optionalen Übertragungen
ihre Wirkung. Ein übersprungener Hinweis ersetzt diese Prüfung nicht.

## Speicherung und betroffene Stellen

| Stelle | Aufgabe |
| --- | --- |
| `src/client/java/de/ottoextra/welcome/ReleaseWelcomeScreen.java` | Logo, Übersicht, Review-/Skip-Navigation |
| `WelcomeScreen.java` im selben Verzeichnis | Rückkehr ohne Zustimmung aus dem Assistenten |
| `WelcomeScreenManager.java` | Automatischer Start, Skip und Reihenfolge anderer Hinweise |
| `WelcomeState.java` | Lokaler Release-Marker, getrennt von Freigaben |
| `src/main/resources/assets/ottoextra/lang/{de_de,en_us}.json` | Sichtbare Texte |

`config/ottoextra/welcome.json` enthält Schema 2, gesehene Versionsreihe,
installierte Version und Anzeigeabschlusszeit. Schreiben erfolgt erst nach
Überspringen oder Abschluss des Assistenten. Der frühere Schema-1-Marker blendet
die neue Startseite nicht aus. `0.1.17-q` und spätere `0.1.17-*` teilen einen
Marker; `0.1.18` erscheint erneut. Bei ungültigem Marker erscheint der Hinweis wieder.
Bei Erststart markiert Überspringen das Onboarding als abgeschlossen, ohne
Consent-Flags, Scopes oder Entscheidungszeiten zu setzen.

## Prüfung

- `gradlew.bat test build -x dist --console=plain`: Release-Marker, Altformat,
  Hotfix-/Versionswechsel sowie bestehende Regressionstests.
- `gradlew.bat -I tools/consent-smoke/client.init.gradle runClientGameTest -x dist --console=plain`:
  echter Minecraft-Client im isolierten Testprofil; automatischer Start,
  Überspringen ohne Zustimmung, gültige Freigabe bleibt erhalten,
  Zurück/Escape ohne Speicherung, Deutsch/Englisch und 320×240 GUI-Pixel.
- `python tools/consent-smoke/server-smoke.py`: Dedicated-Server-Start mit Release-JAR.

Die Tests verbinden sich mit keinem Spielserver. Testklassen und Testprofile
werden nicht in die veröffentlichbare JAR aufgenommen.

Ergebnis am 11.09.2026: **807 JUnit-Tests bestanden**, Client-Prüfungen einschließlich
der 80-Banner-Regression bestanden, Server sauber gestartet und beendet.
Screenshots der deutschen und englischen Startseite sowie der kompakten Ansicht
wurden geprüft. Die JAR-Metadaten und enthaltenen Ressourcen wurden ebenfalls geprüft.

Test-JAR: `build/libs/ottoextra-0.1.17-q.jar` (31.093.465 Bytes).
SHA-256: `1907208b16ffeac1c2c5bd65a874b0a60e3af4c5cf7366ce04a0072bde5f02b7`.
