# RPNAMES-01: Wartende RP-Namen-Uploads starten noch nach Widerruf

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Serielle Executor-Aufrufkette statisch bestätigt; keine echten Uploads ausgelöst.

## Auslöser und Folge

Upload A blockiert vorübergehend; B und C warten im einzelnen Upload-Thread. Der Benutzer widerruft die Upload-Einwilligung und speichert. Sobald A endet, starten B und C trotzdem neue HTTP-POSTs.

Dies betrifft noch nicht gestartete Requests, nicht die unvermeidbare Fertigstellung einer bereits laufenden Übertragung.

## Codebeleg

- [RpNameUploadService.java:32](../../../src/client/java/de/ottoextra/rpnames/upload/RpNameUploadService.java#L32): Ein einzelner Executor-Thread arbeitet die Warteschlange ab.
- [RpNameUploadService.java:66](../../../src/client/java/de/ottoextra/rpnames/upload/RpNameUploadService.java#L66): Zustimmung wird nur vor dem Einreihen mit `supplyAsync` geprüft.
- [RpNameUploadService.java:160](../../../src/client/java/de/ottoextra/rpnames/upload/RpNameUploadService.java#L160): `send()` prüft die inzwischen gültige Zustimmung nicht erneut.
- [SettingsRegistry.java:227](../../../src/client/java/de/ottoextra/config/settings/SettingsRegistry.java#L227): Der Widerruf ändert das Consent-Feld.

## Fixansatz und Regressionstest

Zustimmung unmittelbar vor Versand erneut prüfen und wartende Jobs bei Widerruf invalidieren. Deduplikationszustand übersprungener Jobs mitbehandeln. Ein Fake-Transport soll A anhalten, B/C einreihen, Consent widerrufen und A freigeben. Für B/C dürfen keine Requests starten.

## Umsetzung am 06.09.2026

Wartende Uploads prüfen die Einwilligung vor Abarbeitung und unmittelbar vor HTTP-Versand. Widerruf erhöht eine Consent-Generation, sodass erneutes Zustimmen alte Jobs nicht wieder freigibt. Übersprungene Jobs werden aus der Deduplikation entfernt; Settings, Willkommen und Config-Restore verwenden den Widerrufspfad.

Regression: [RpNameConsentQueueTest.java](../../../src/test/java/de/ottoextra/rpnames/upload/RpNameConsentQueueTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
