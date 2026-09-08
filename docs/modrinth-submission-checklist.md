# Modrinth submission checklist

## Version metadata

- Project type: Mod
- Environment: Client-side only
- Loader: Fabric
- Game version: Minecraft 1.21.11
- Primary file: the remapped `ottoextra-<version>.jar` from the validated release
  build; verify its internal `fabric.mod.json` version before uploading.
- The current testing version is **0.1.17-e (Alpha)**. See
  [release validation](releases/0.1.17-e.md) and
  [tester notes](../release-notes-0.1.17-e.md). No upload is part of this build.
  Do not reuse a JAR from an older `dist` build.

## Dependencies on the Modrinth version

Add these linked project dependencies:

| Project | Dependency type | Reason |
|---|---|---|
| Fabric API | Required | OttoExtra uses Fabric lifecycle, rendering, command, screen, networking, and event APIs. |
| Mod Menu | Optional | Adds a convenient entry point to OttoExtra settings. OttoExtra works without it. |
| Xaero's Minimap | Optional | Enables minimap borders and banner integration. |
| Xaero's World Map | Optional | Enables the world-map overlay and fief views. |

Do not add:

- XaeroLib: current Fabric releases bundle it inside the Xaero map mods.
- DevAuth: development runtime only; not included in the release JAR.
- Xaero development JARs from `libs/`: compile-time only; not bundled.
- OttoTalk, OttoNames, or OttoChat RP Names: legacy imports/conflict detection only.
- Java or Fabric Loader as linked Modrinth projects. Select Fabric as loader and
  Minecraft 1.21.11 as game version; loader and Java requirements are already
  declared in `fabric.mod.json`.

## Project page

- Paste `docs/modrinth-description.md` into the project description.
- Keep both German and English sections.
- Suggested summary:
  `Client-side Ottonien mod for RP names, chat, letters, maps, regions, and consent-based optional downloads.`
- Keep Source and Issues links public.
- Mention that bundled map images are used with permission.
- Do not upload `OttoExtra-0.1.15.4-player-link.zip` as a normal additional file.
- Upload source JAR only if desired and mark it as a source file.
