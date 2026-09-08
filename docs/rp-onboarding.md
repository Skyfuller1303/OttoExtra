# RP-Modus beim Erststart

Nach den Hinweisen und den vier grundlegenden Download-/Datenschutzentscheidungen
folgt die optionale RP-Einrichtung. Die drei Modi stehen als Karten nebeneinander,
mit den Auswahlbuttons in einer gemeinsamen Reihe unter ihren Erklärungen.
Ein Klick öffnet die Detailauswahl; erst **Übernehmen** ändert die Einstellungen.

Alle Erststart-Seiten verwenden denselben OttoExtra-Stil mit dunkelblauem
Hintergrund (`#10171f`), Karten und Buttons (`#131b24`) sowie goldenen Akzenten,
einschließlich der vorherigen Hinweise und Grundeinstellungen.
Zurück, Ablehnen und Erlauben stehen dort in einer Reihe. Beide
Einwilligungsentscheidungen sind gleich hervorgehoben; längere Hinweise bleiben
bei hoher GUI-Skalierung scrollbar.

| Modus | Tabliste | RP-Chat | Namensschilder | Kennenlernen |
| --- | --- | --- | --- | --- |
| Komfort | Spielernamen, Titel | Verfügbare RP-Namen, sonst Spielernamen | Spielernamen, normale Sichtbarkeit | RP-Namen automatisch nutzen |
| Ausgewogen | Verfügbare RP-Namen und Titel, sonst Spielernamen | Verfügbare RP-Namen, sonst Spielernamen | RP-Namen, sonst Spielernamen; nur bei freier Sicht | RP-Namen automatisch nutzen |
| Immersiv | Bekannte RP-Namen, Fremde unbekannt; keine Titel | Bekannte RP-Namen, Fremde unbekannt | Ausgeblendet | Schleichen + Rechtsklick auf eine Person |

Komfort zeigt beim Betrachten auch Spielernamen; Ausgewogen und Immersiv blenden
sie dort aus. Hilfe-, Offtopic- und OOC-Kanäle verwenden weiterhin Spielernamen.
Die Tabelle beschreibt die Ausgangswerte. Im zweiten Schritt können elf Schalter
in drei Bereichen angepasst werden:

- **Chat & Tabliste:** RP-Namen im RP-Chat, RP-Namen und Titel in der Tabliste.
- **Namensschilder:** Anzeige, freie Sicht, RP-Namen, Spielernamen und Titel.
  Bei ausgeschalteter Anzeige sind die vier Detailoptionen deaktiviert; ihre
  Werte bleiben für das Wiedereinschalten erhalten.
- **Kennenlernen:** erst kennenlernen, Spielernamen bei Fremden und beim Betrachten.
  Kennenlernen hat bei RP-Namen Vorrang. Die Option für Spielernamen bei Fremden
  erlaubt dabei nur eine zusätzliche Namensschildzeile.

Die Modi liefern jeweils einen eigenen Entwurf. Änderungen bleiben beim Wechsel
zwischen Bereichen, beim Zurückgehen zur Modusauswahl und beim Zurückgehen zu den
Datenschutzentscheidungen erhalten. Die aktive Config bleibt dabei unverändert.
Die Darstellung kann auch später unter RP-Namen und Namensschilder in den
OttoExtra-Einstellungen geändert werden.

**Überspringen** und Escape schließen den Assistenten ohne Anwendung eines Modus
ab. Die zuvor getroffenen Datenschutzentscheidungen werden dabei gespeichert.
Zurück aus den Details führt zur Modusauswahl; Zurück aus der Auswahl führt zur
letzten Datenschutzentscheidung. Auch bearbeitete Entwürfe werden beim
Überspringen oder bei Escape verworfen.

Der bestehende Abschlussmarker `consent.onboardingVersion` wird erst nach
Übernehmen oder Überspringen gespeichert. Bereits abgeschlossene Installationen
bekommen durch dieses Feature keinen erneuten Assistenten. Ein vorzeitig beendeter
Erststart beginnt beim nächsten Start wieder mit den Hinweisen.

Die Presets verändern ausschließlich Anzeigeoptionen in `rpnames` und `nametags`.
Bestehende Profil-/Bekanntschaftsdaten, eigene Farben, Platzhalter, andere Module
und Netzwerkentscheidungen bleiben erhalten.

Validierung: `gradlew.bat build -x dist`, `RoleplayPresetTest` und
`RoleplayChoicesTest` prüfen Build,
Namensauflösung, API-importierte Identitäten, Wechsel der Presets, Config-Restore
und den Erhalt unabhängiger Einstellungen. Ein Client-GameTest im Workspace unter
`test-results/rp-onboarding-20260907` prüft zusätzlich die tatsächlichen Buttons,
Vorschauen, Zurück, Übernehmen/Speichern, Überspringen, Escape, das Zurückkehren ins
Hauptmenü und das Layout bei 320 × 240 GUI-Pixeln. Der erweiterte Client-Test unter
`test-results/rp-setup-details-20260907` prüft die horizontale Buttonreihe,
alle Detailbereiche, An/Aus samt deaktivierten Schaltern, Erhalt der Entwürfe
beim Zurückgehen bis zur Einwilligung, angepasstes Übernehmen/Speichern und
Verwerfen durch Überspringen/Escape.
