# Namen aus dem Chat lernen

Bei aktivem Kennenlernen öffnet **Linksklick** auf einen unbekannten
Sprecher im Chat das vorhandene Kennenlernfenster. Der Hover-Hinweis lautet
„OttoExtra: Linksklick auf Unbekannt, um den Namen zu lernen.“ Bei einem eigenen
Platzhalter wie `???` wird dieser im Hinweis verwendet.

Jeder Namensabschnitt behält die Zuordnung zum tatsächlichen Minecraft-Konto,
auch wenn mehrere Personen als „Unbekannt“ erscheinen. Der Hover-Hinweis zeigt
keinen bereits von der API erhaltenen RP-Namen. Shift-Linksklick funktioniert
weiterhin. Ein einfacher Klick auf bekannte Namen behält die vorhandene Chataktion.

Die Aktion benötigt nicht die separate Option „Shift-Klick öffnet Personenbuch“.
Diese bleibt für bekannte Namen zuständig. Shift-Rechtsklick auf den Spieler
nutzt denselben Kennenlernablauf; bei „IM KAMPF“ bleibt das Fenster geschlossen.

Vorhandene lokale Namensvorschläge öffnen das Fenster sofort. Andernfalls wird
mit bekannter UUID das vorhandene Spielerprofil abgefragt. Ohne UUID oder bei
einem API-Fehler kann der Name im lokalen Fenster eingetragen werden. Eine
Antwort nach einem Chat- oder Weltwechsel öffnet kein veraltetes Fenster.

Erst die Bestätigung speichert die Bekanntschaft. Abbrechen erhält den bisherigen
Stand. Der Chatentwurf bleibt erhalten, und gespeicherte Namen werden auch in
bereits sichtbaren Nachrichten aktualisiert.
