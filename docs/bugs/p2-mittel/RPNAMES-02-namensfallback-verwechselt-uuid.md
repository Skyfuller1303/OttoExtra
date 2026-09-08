# RPNAMES-02: Wiedervergebener Minecraft-Name übernimmt ein fremdes RP-Profil

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: UUID-/Namensindex-Pfad mit gültigen Eingaben statisch bestätigt.

## Auslöser und Folge

Ein bekannter Account mit UUID A gibt seinen Namen „Anna“ auf. Bevor dessen neuer Name lokal erfasst wird, erscheint ein anderer Account mit UUID B als „Anna“. OttoExtra ordnet B das gespeicherte Personenprofil von A zu, einschließlich RP-Name, Titel und Bekanntschaft.

## Codebeleg

- [LocalRpIdentityStore.java:374](../../../src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java#L374): Nach verfehlter UUID-Suche fällt `find()` ohne Prüfung einer widersprechenden gespeicherten UUID auf den Namen zurück.
- [LocalRpIdentityStore.java:399](../../../src/client/java/de/ottoextra/rpnames/store/LocalRpIdentityStore.java#L399): `ensureSeen()` ersetzt eine UUID nur, wenn die bisherige leer ist; das falsche Profil bleibt bestehen.
- [RpNamesModule.java:383](../../../src/client/java/de/ottoextra/rpnames/RpNamesModule.java#L383): Die Tablistenverarbeitung liefert echte UUID-/Namenspaare an diesen Pfad.

## Fixansatz und Regressionstest

Bei bekannter, abweichender UUID keinen Namensfallback zulassen. Dauerhafte Identität über UUID führen und Namensindex beim Umbenennen aktualisieren. Test: Profil `(A, Anna)` speichern und `(B, Anna)` beobachten; B darf weder Profil noch Bekanntschaft oder private Notizen von A erben.

## Umsetzung am 06.09.2026

Ein Namensfallback mit widersprechender UUID wird abgelehnt. Beide UUID-Profile bleiben im Speicher und in der Datei erhalten; ein späteres Umbenennen entfernt nur den eigenen Namensindex. Die letzte Beobachtung bestimmt nach dem Laden den aktuellen Besitzer eines wiedervergebenen Namens.

Regression: [IdentityReuseTest.java](../../../src/test/java/de/ottoextra/rpnames/store/IdentityReuseTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
