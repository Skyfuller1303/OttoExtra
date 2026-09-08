# OttoExtra 0.1.17-a – Alpha

8. September 2026. Diese Version ist für gemeinsame Tests vorgesehen.
Die Datei zum Weitergeben ist **`ottoextra-0.1.17-a.jar`**.

## Installation

1. Minecraft Java **1.21.11**, Java **21**, Fabric Loader **ab 0.18.1** und
   Fabric API **ab 0.141.4+1.21.11** verwenden.
2. Im verwendeten Spielprofil die bisherige OttoExtra-JAR durch diese Alpha
   ersetzen; nur eine OttoExtra-Version im `mods`-Ordner behalten.
3. Minecraft starten und Ottonien beitreten. OttoExtra wird nur auf dem Client
   installiert. Für die Brief-V2-Tests wird die angepasste Courier-Version
   **2.1.0** auf dem Server verwendet.

ModMenu, Xaero-Karten und Litematica sind optional. Vor einem Wechsel von einer
bestehenden Installation die lokale Konfiguration `config/ottoextra/` sichern,
damit für Vergleichstests auch der bisherige Stand verfügbar bleibt.

## Enthaltene Änderungen

- Courier-V2-Versand, Import, Wiederaufnahme und Kalibrierung mit automatisierten
  Tests gegen die bereitgestellte Courier-JAR. Der Editor zeigt Arbeitsseiten
  und voraussichtliche Buchseiten getrennt; die Vorschau nutzt denselben Plan.
- Tierinfo beim Anvisieren, Pferdeübersicht mit Suche, Filtern und Modellvorschau.
- RP-Ersteinrichtung, aktualisierte Titel/Farben und Erweiterungen für Personenbuch,
  Namensschilder und Tabliste.
- Karteninformationen auf Pergament, Regionen-Aktivitätsanzeige und
  Kompatibilitätskorrekturen für optionale Xaero-Versionen.
- Chatkanäle, RP-Formatierung und Langchat: Sendepausen gelten auch bei weiteren
  Eingaben, alte Teilnachrichten werden beim Verbindungswechsel verworfen und
  vollständige Nachrichten bleiben im Eingabeverlauf erreichbar.
- Kontoübersicht und Verknüpfung, Kampfanzeige, Vote-Erinnerung und
  optionale Fachwerk-Unterstützung für Litematica.
- Korrekturen bei Backup/Restore, RP-Datenspeicherung, Ressourcenpaket-Updates,
  veralteten Kontoantworten und der Freigabe von Kartentexturen.

Das **Chatprotokoll ist standardmäßig aktiv** und speichert Nachrichten lokal
in `logs/ottoextra-chat.log`. Abschalten: **Tweaks → Basis → Chatprotokoll**.
Die Datei bleibt über Neustarts erhalten. Debug-Logging ist standardmäßig aus.

## Bitte beim Testen prüfen

- Briefe im **V2-Modus**: längere Texte, Absätze, Umlaute, Formatierung,
  mehrere Seiten, Vorschau, Import und das tatsächlich erhaltene Buch vergleichen.
  Einmal während des Versands trennen, neu verbinden und die Wiederaufnahme prüfen.
- Editor bei eigener Auflösung und GUI-Skalierung: Seitenzahlen, Sperrhinweise,
  Scrollen und alle Aktionsbuttons prüfen.
- Langchat in RP und OOC, Pfeil-hoch-Verlauf und erneuten Login ausprobieren.
- RP-Namen, Titel, Wappen und Tabliste nach Login, Logout und Serverwechsel prüfen.
- Tier-/Pferdeansichten, Karte, Regionenanzeige und Erststart-Auswahl im eigenen
  Modpaket ausprobieren; vorhandene Xaero-/Litematica-Versionen mit angeben.
- Einstellungen ändern, verwerfen sowie ein Backup erstellen und wiederherstellen.

## Bekannte offene Beobachtungen

- Veraltete Tablisteneinträge nach Logout sind weiterhin nicht als behoben belegt.
- In einzelnen automatisierten Import-Aufnahmen fehlt die Editor-Aktionszeile,
  obwohl die Widgets vorhanden und anklickbar sind. Bei Auftreten bitte Auflösung,
  GUI-Skalierung und Screenshot festhalten.
- XaeroPlus-Minimap-Zeichnungen und echte Proxy-Wechsel benötigen weitere
  Sichttests im jeweiligen Modpaket. Eine vollständige Matrix aller Mods wurde
  nicht abgenommen.
- Konto-Antwortrennen und Paket-Quellenwechsel wurden gezielt technisch geprüft;
  produktive Konto-API und neuer externer HTTPS-Paketdownload gehören nicht zu
  diesem automatisierten Abnahmelauf.

## Teststand und Fehlermeldungen

Der zugrunde liegende Quellstand bestand **647 Unit-Tests** und **73 von 73
lokalen Server-/Clientfällen** mit der unveränderten angepassten Courier-2.1.0-JAR.
Der Alpha-Build und die Artefaktprüfung werden in
[der Release-Validierung](docs/releases/0.1.17-a.md) dokumentiert.
Die älteren Ergebnisse und Grenzen stehen in der
[Audit-Umsetzung](docs/audit-2026-09-08/UMSETZUNG.md).

Bei Fehlern bitte Version `0.1.17-a`, Minecraft-/Loader-/Modversionen, Schritte zur
Reproduktion, erwartetes und tatsächliches Verhalten sowie gegebenenfalls einen
Screenshot nennen. Relevante Logausschnitte beilegen; persönliche Chatnachrichten
und Kontotokens vorher entfernen.
