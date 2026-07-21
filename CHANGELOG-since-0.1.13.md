# OttoExtra 0.1.15.1 – Changelog seit 0.1.13

Zeitraum: 15. bis 21. Juli 2026. Zielversion: Minecraft 1.21.11.

## 0.1.15.1 – Worldmap-Daten und Tablisten-Sortierung

- Frische Installationen laden Regions- und Lehensdaten über die signierte,
  authentifizierte v2-API statt über den inkompatiblen Legacy-Fallback.
- Wappen-PNGs werden vom vertrauenswürdigen API-Origin geladen; fremde Hosts,
  Cross-Origin-Weiterleitungen und ungültige Signaturen bleiben blockiert.
- Optionale Tablisten-Sortierung nach Lehen mit Leitung, Titelhierarchie und
  sichtbarem Namen als Unterordnung.
- BetterTab 2.1.5 wird direkt unterstützt; Server-Wappen, Badges und formatierte
  Namen bleiben unverändert.
- Regions-Bootstrap ignoriert verspätete Antworten alter Verbindungen und
  aktualisiert Fraktions-Aliase nach Änderungen zuverlässig.

## 0.1.15 – RP-Untersuchung, geschützte Originaltexte und Chatfarben

- Chatfarben-Verwaltung im dunklen RP-Personenbuch-Design mit Hexfeldern,
  Live-Farbfeldern, Einzelreset und „Alle auf Original“.
- Farbe und Kursivdarstellung für `*Emotes*` und `(OOC)` getrennt einstellbar.
- Kanalname, Nachrichtentext, Emotes und OOC sind gemeinsam in einer kompakten
  Farbverwaltung erreichbar.
- Leere Kanal-Farbfelder übernehmen weiterhin unverändert den Serverstil.
- „Titel in der Tabliste“ gilt für alle Spieler, unabhängig vom
  Bekanntschaftsstatus. Der separate Unbekannten-Schalter entfällt; Chat und
  Nametags bleiben davon unberührt.
- Fehlende Lupentextur in der JAR ergänzt und durch Asset-Tests abgesichert.
- Lupentextur, Post-Effect und Shader werden durch Tests dauerhaft auf ihre
  vollständige Aufnahme in die JAR geprüft.
- Fehlende Beschriftung des Öffnen-Buttons ergänzt.
- Doppelten Screen-Blur und den Crash „Can only blur once per frame“ behoben.
- Chatname und Nachrichtentext lassen sich für Sprechen, Flüstern, Murmeln,
  Rufen, Brüllen, Offtopic und Hilfe getrennt einfärben.
- OttoExtra gibt sämtliche Chatfarben zentral vor; TranslateUtils übernimmt
  dieselben Regeln für Übersetzungen, Hover-Lore und lokale Originalzeilen.
- Die Kanalanzeige neben der Chat-Eingabe verwendet ebenfalls die jeweils
  konfigurierte Farbe des Chatnamens.
- Mojang-authentifizierte API-Anbindung für zeitlich begrenzte,
  empfängergebundene Chat-Originaltexte.
- Markerlose API-Inbox: Nur berechtigte OttoExtra-Nutzer laden die Zuordnung
  zwischen Übersetzung und Original; Nicht-Mod-Nutzer sehen keine Zusatzzeichen.
- Addon-Hook für Hover-Texte und lokal eingeblendete Originalnachrichten.
- Signierte Backend-Antworten und gemeinsamer OttoExtra-API-Client für Addons.
- OOC-Standardfarbe auf `#B4BEC6` angepasst.
- Verschachtelte OOC-/Emote-Syntax bleibt auch über lange Teilnachrichten erhalten.
- Bereits sichtbare Chatzeilen werden beim Neuaufbau erneut durch registrierte
  Addons verarbeitet, damit nachträgliche Hover- und Originaldaten erhalten bleiben.
- Normale Chatnachrichten behalten wieder die Farbe ihres Serverkanals.
- Nur `*Emotes*` und `(OOC-Kommentare)` werden speziell formatiert.
- Nach formatierten Passagen kehrt der Text zuverlässig zum vorherigen
  Kanal- beziehungsweise Emote-Stil zurück.
- Offtopic und Hilfe bleiben von der automatischen RP-Formatierung ausgenommen.
- Neuer, frei belegbarer Alt-Untersuchungsmodus mit Lupe, Zoom,
  Randunschärfe, Fortschrittsring und RP-Gedanken.
- Sichtbare Informationen zu Spielern, Händen, Rüstung, Gegenständen,
  Kreaturen, Schildern, Rahmen, Lesepulten, Bannern und ausgewählten Blöcken.
- Rein clientseitig: keine versteckten Inventare oder geheimen Serverwerte.
- `*Emotes*` werden hellgrau und kursiv, `(OOC-Kommentare)` farblich dargestellt.
- Lange RP-Nachrichten führen offene Emote- und OOC-Bereiche korrekt über
  mehrere Teilnachrichten fort.
- Verkündungs-Auto-Optimize ist standardmäßig aus und nur über die erweiterten
  Briefeinstellungen aktivierbar; ein Warnhinweis nennt mögliche Umbruch-,
  Formatierungs- und Seitenlayoutfehler.

## 0.1.13-2a – RP-Namen, Tabliste und Regions-API

- Inkrementeller Regions-Sync beim Öffnen der Xaero-Weltkarte.
- Regions-API von `regions.skyfuller.de` auf `api.ottoextra.dev` migriert.
- Bestehende Konfigurationen werden automatisch auf die neue Domain umgestellt.
- Bereits sichtbare Chatzeilen werden nach Änderungen im RP-Personenbuch neu aufgebaut.
- RP-Namen, Titel und persönliche Farben greifen einheitlich in Chat,
  Tabliste und Nametags, ohne Rich-Text-Komponenten oder Hoverdaten zu verlieren.
- Titelanzeige und automatische Titelaktualisierung der Tabliste wurden getrennt.
- Gemeinsame Chat-Eingabefläche verhindert Überlagerungen durch Addons.
- Zuordnung über UUID beziehungsweise Spieler-Kopf hat Vorrang vor
  gleichnamigen RP-Profilen.
- Änderungen im Personenbuch werden beim Wechseln und Schließen zuverlässig gespeichert.
- Sichtbare Ressourcenpfade, falsche Farben und große Leerbereiche beim
  Chat-Umschreiben wurden behoben.
- `Unbekannt` wird ausschließlich auf echte Spieler angewendet.

## Zugehöriges TranslateUtils

- `0.1.1`: Sprachindikator, Kanalstatus, dynamische OttoExtra-Anbindung und
  zuverlässiger Schutz von Emotes, Farben, URLs und OOC-Passagen.
- `0.1.2`: Geschützte Originaltexte per Hover, Zugriff für alle oder Allowlist,
  Username-Liste mit Tab-Vervollständigung.
- `0.1.3`: Datenschutzhinweis, signierte geschützte Chat-API, optionale lokale
  Originalzeile, markerlose API-Inbox, Debug-Nutzung außerhalb Ottoniens und
  zentrale Formatierung durch OttoExtra.
