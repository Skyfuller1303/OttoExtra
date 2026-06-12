# OttoExtra — Implementierungsarchitektur (Stand: Grundgerüst)

Begleitdokument zur Umsetzung. Die **fachliche Hauptquelle** bleibt `docs/`
(Spec, ADRs, OpenAPI). Dieses Dokument hält fest, *wie* der Code die Spec umsetzt
und welche Entscheidungen beim Scaffolding getroffen wurden.

## Zielplattform (autoritativ aus CLAUDE.md)

| | Version |
|---|---|
| Minecraft | **1.21.11** |
| Fabric Loader | 0.18.1 |
| Fabric Loom | 1.14 |
| Java | 21 |

> Die 1.20.4-Angaben in `docs/legacy-source-docs/` und den alten Mod-Docs sind
> **Legacy-Referenz**. Mixin-Ziele, Intermediary-Namen, Events und Rendering-APIs
> sind für 1.21.11 neu zu prüfen (siehe „Offene Punkte").

## Modulgrenzen

```
de.ottoextra
├─ OttoExtra              (main: Konstanten, Logger, id() — client-neutral)
├─ OttoExtraClient        (ClientModInitializer — nur Komposition + Lifecycle)
├─ OttoExtraModule        (Modul-Vertrag: id/enabled/Lifecycle)
├─ OttoExtraContext       (geteilte Singletons: Config + API)
├─ OttoExtraGate          (Ottonien-Server-Erkennung)
├─ api/                   (EINE API-Schicht — ADR-001)
│  ├─ OttoExtraApiClient        (Interface)
│  ├─ HttpOttoExtraApiClient    (java.net.http, async, off-thread)
│  ├─ OttoExtraApiRoutes        (einziger Ort für rohe URLs)
│  ├─ ApiProblem                (einheitliche Fehler statt Stacktraces)
│  └─ model/                    (DTOs, snake_case = JSON, tolerant)
├─ config/                (OttoExtraConfig, OttoExtraPaths, ModMenu-Stub)
├─ map/        regions/   rpnames/   nametags/   letter/   chat/
│              └─ je ein <X>Module (Stub mit enabled-Gate + Lifecycle)
└─ mixin/                 (noch leer — ottoextra.mixins.json: client: [])
```

### Leitprinzipien (aus den `.claude/rules` + ADRs)

1. **Eine API, kein Duplikat** (ADR-001). Module sehen nie rohe URLs, nur den
   `OttoExtraApiClient`/Services über den `OttoExtraContext`.
2. **Keine KI-Texthelfer** (ADR-002). Kein Gemini/OpenAI, kein Prompting, keine
   automatische RP-Textgenerierung, keine Schlüssel-/Token-Felder. Negativ-Audit
   gegen `docs/16` läuft sauber.
3. **Mixins sind letzte Wahl, dünn.** Erst Fabric-Events/Services. Jeder Mixin
   ruft genau eine Service-Methode und endet (rendering-mixin-compat-rules).
4. **Client/Server-Trennung.** Client-only Mod via `splitEnvironmentSourceSets()`;
   `net.minecraft.client.*` nur im `client`-SourceSet.
5. **Nichts blockiert den Tick/Render-Thread.** HTTP läuft auf eigenem Daemon-Pool;
   alle API-Methoden sind `CompletableFuture`.
6. **Tolerant lesen, streng schreiben.** Config + API-Parser ignorieren Unbekanntes;
   Config-Save ist atomar; Downloads haben Größenlimit (5 MB) und Timeout.
7. **Privacy.** `rpnames.uploadLearnedNames` standardmäßig `false`; zentrale
   Cache-Pfade unter `config/ottoextra/cache/`.

## API-Vertrag (Kurzfassung, Details: `docs/03` + `openapi/`)

- Basis: `https://regions.skyfuller.de/` (konfigurierbar), Multiplex über
  `api/index.php?action=...`.
- Routen in `OttoExtraApiRoutes`; DTOs in `api/model` exakt gegen reale
  `endpoints/*.json`-Dumps verifiziert (inkl. `region_capabilities`, `region_info`,
  `mapped`).
- Kein Auth-Header, keine Query-Secrets — nur öffentliche Spiel-/Serverdaten.

## Lifecycle-Fluss

```
onInitializeClient: Config laden → API-Client bauen → Module init (enabled-Gate)
ClientPlayConnectionEvents.JOIN → OttoExtraGate prüft "ottonien" → module.onServerJoin
ClientPlayConnectionEvents.DISCONNECT → module.onDisconnect
ClientLifecycleEvents.CLIENT_STOPPING → module.onClientStop → api.close()
```
Fehler pro Modul werden gefangen (`runSafe`) — ein defektes Modul reißt den Client nicht ab.

## Was ersetzt wird (entfernte/ignorierte Altlasten)

| Alt (Legacy) | OttoExtra |
|---|---|
| Mehrere HTTP-Clients (ottomap, ottoregions, ottochat-rpnames, ottoplus) | eine `api`-Schicht |
| ottoplus `AIApiClient`, `RoleplayCommand`-KI, `ChatHistoryManager`, Varianten-Auswahl | **entfernt** |
| Klartext-Gemini-Key in `ottotalk.json` | **nicht übernommen** (keine Keys) |
| Dupliziertes Karten-/Briefsystem in ottoplus | konsolidiert in `map`/`letter` |
| `mcp-api.ottonien.com` (ottoplus) | nur eine Basis-URL (Regions-API) |

## Offene Punkte / vor dem ersten Build zu prüfen

- [ ] `gradle/wrapper/gradle-wrapper.jar` fehlt (kein Gradle-CLI vorhanden). Per
      IDE-Import oder `gradle wrapper` erzeugen.
- [ ] `yarn_mappings` + `fabric_version` in `gradle.properties` gegen real
      verfügbare Maven-Artefakte der Zielversion verifizieren.
- [ ] Mixin-Ziele (`ChatHud.addMessage`, `PlayerEntityRenderer.renderLabelIfPresent`,
      `EntityRenderer.hasLabel`, `ClientPlayNetworkHandler`) gegen 1.21.11-Mappings prüfen.
- [ ] `ServerInfo.address`-Feldname in 1.21.11-Yarn verifizieren (OttoExtraGate).
- [ ] `assets/ottoextra/icon.png` ergänzen (in fabric.mod.json referenziert).
- [ ] Build/Test: `./gradlew build` (siehe CLAUDE.md „Vor Commit/PR").

## Projektort & Build-Ausgabe

- Projektwurzel: `OttoExtra/` (Code + Gradle). Spec/Docs bleiben in `../Mods/docs/`.
- `./gradlew build` → `build/libs/`. Der `dist`-Task (läuft via `finalizedBy build`)
  spiegelt die remappten JARs nach `OttoExtra/dist/`. JARs sind in `.gitignore`
  ausgenommen, der Ordner bleibt erhalten.

## Status

Grundgerüst steht: 30 Java-Dateien, valide Metadaten/Lang-JSON, No-AI-Audit sauber.
Feature-Logik der Module folgt phasenweise gemäß `docs/10-implementation-roadmap.md`.
**Noch nicht kompiliert** (Minecraft/Fabric-Abhängigkeiten offline nicht auflösbar).
