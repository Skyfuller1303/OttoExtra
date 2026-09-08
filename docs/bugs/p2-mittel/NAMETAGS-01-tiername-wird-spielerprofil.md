# NAMETAGS-01: Benannte Tiere und NPCs erhalten das Profil eines gleichnamigen Spielers

Priorität: **P2 – mittel**. Status: behoben (06.09.2026). Ursprünglicher Nachweis: Vollständiger Entity-Renderpfad statisch bestätigt; kein Ingame-Test.

## Auslöser und Folge

Ein geladener Spieler heißt „Alice“. Ein Tier, Rüstungsständer oder NPC mit demselben Anzeigenamen wird angesehen, sodass dessen normales Namensschild sichtbar wäre. OttoExtra behandelt diesen Renderzustand wie Alices Spielerzustand: Das Schild enthält ihren RP-Namen/Titel oder „Unbekannt“. Profil- und Ausblendungsregeln können sogar das fremde Schild unterdrücken.

## Codebeleg

- [EntityRendererMixin.java:49](../../../src/client/java/de/ottoextra/mixin/EntityRendererMixin.java#L49): Der allgemeine Hook verarbeitet Renderzustände sämtlicher Entitäten.
- [NametagService.java:75](../../../src/client/java/de/ottoextra/nametags/NametagService.java#L75): Fallback auf `state.displayName.getString()`.
- [NametagLabelRenderer.java:56](../../../src/client/java/de/ottoextra/nametags/NametagLabelRenderer.java#L56): Das Ergebnis wird als Spieleridentität verwendet.
- [NametagLabelRenderer.java:103](../../../src/client/java/de/ottoextra/nametags/NametagLabelRenderer.java#L103): `findOnlinePlayer()` sucht nur nach einem passenden Namen, ohne die tatsächliche Entität des Renderzustands zu prüfen.

## Fixansatz und Regressionstest

Spieler über eine explizit erfasste Entity-/UUID-Zuordnung am Renderzustand erkennen. Namen allein nicht als Typ- oder Identitätsnachweis verwenden. Ein Test mit Spieler und gleichnamigem Tier muss das Tierschild unverändert lassen, während der echte Spieler weiterhin korrekt formatiert wird.


## Umsetzung am 06.09.2026

Die Renderhooks erfassen UUID und Account ausschließlich von tatsächlichen PlayerEntity-Instanzen. Andere Entitäten löschen die Zuordnung eines wiederverwendeten Renderzustands. Der allgemeine Labelrenderer sucht den erfassten Spieler über UUID; Anzeigenamen sind kein Identitätsnachweis mehr.

Regression: [NametagIdentityTest.java](../../../src/test/java/de/ottoextra/nametags/NametagIdentityTest.java). Vollständiger Testlauf und Build erfolgreich; Details und verbleibende Ingame-Prüfungen: [P2-UMSETZUNG.md](../P2-UMSETZUNG.md). Die Codebelege oben beschreiben den Auditstand vor diesem Fix.
