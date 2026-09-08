# Local Courier server harness

This starts an isolated **Paper 1.21.11 build 132** server with the unchanged supplied `input/courier-2.1.0.jar`. The helper controls test setup and reads actual Bukkit/Courier state. It does not implement `/letter`, `/post`, announcements, pagination, or delivery.

Run from `OttoExtra`:

```powershell
./tools/courier-e2e/server/build.ps1
./tools/courier-e2e/server/start.ps1
Invoke-RestMethod http://127.0.0.1:25580/health
# Stop after tests:
Invoke-RestMethod -Method Post http://127.0.0.1:25580/stop
```

Preparation copies the existing Paper bootstrap, libraries, cache and versions from `../OttonienItems-local-server`. It copies the existing `eula=true` file with its user-acceptance comment dated 2026-07-26. If that evidence is unavailable, preparation fails rather than creating a new acceptance. No original server files are changed. Runtime, world, plugins, logs and reports are all under `build/courier-e2e/server`.

Minecraft listens on **127.0.0.1:25579**, offline mode. HTTP listens on **127.0.0.1:25580**. Java 21 is read from the installed Adoptium directory in the scripts. Start validates the actual runtime properties: loopback address, fixed Minecraft port, offline mode, and disabled RCON/query. Ambiguous or duplicate property keys fail before Java starts. Preparation and start check the cached Mojang/Paper core JARs and Paper API library; an incomplete copy reports the exact missing path. Start also refuses occupied ports and verifies the Courier SHA-256:

`9310A0ACAEAED0123A8FD2007502065090C0864DBFE277E779D266601F789B4F`

The first login of each UUID per server process receives a safe test platform, permissions, 1000 test coins and one supply. Reconnecting within that process **preserves the inventory and partially written book**, recreating only the permission attachment for the new Bukkit Player instance. Explicit `/reset` performs a fresh test setup.

## Test-only differences

- No MythicMobs plugin is available. The supported Courier configuration path `letter-item.ItemStack` is a PAPER item named `Pergament und Feder`; the `MythicItem` key is removed. Courier's ordinary material matching and consumption still execute.
- A test-only plugin named `Vault` hosts the HTTP controller and a deterministic economy service, loaded before Courier. Its two API types are unchanged official VaultAPI sources from commit `8bad2c479f531436b4b0694f5696b501d0afef3e`. It is **not the production Vault plugin or economy provider**. Sources retain their LGPL notices: [Economy.java](https://github.com/MilkBowl/VaultAPI/blob/8bad2c479f531436b4b0694f5696b501d0afef3e/src/main/java/net/milkbowl/vault/economy/Economy.java), [EconomyResponse.java](https://github.com/MilkBowl/VaultAPI/blob/8bad2c479f531436b4b0694f5696b501d0afef3e/src/main/java/net/milkbowl/vault/economy/EconomyResponse.java).
- Discord delivery is disabled. Announcement **submission** is measured through Courier's actual announcements list/config; external Discord publication is outside this local harness.
- The helper embeds unmodified classes from the official [JDA 5.6.1 release](https://github.com/discord-jda/JDA/releases/tag/v5.6.1), downloaded with pinned SHA-256 `9230BA05BDB4BD1027263DD8F1E4727630DF25521061563A2ED4C93527CB06F5`. Courier's `Announcement` class requires these types during verification even when Discord is disabled. Initial smoke failure logs and saved YAML are preserved as `reports/missing-jda-*`; the supplied Courier JAR remains unchanged. Preparation downloads this dependency on first run.
- Reading distortion defaults are zero for deterministic tests. The original announcement cooldown remains 3600 seconds; each explicit `/reset` clears that player's cooldown so separate scenarios stay independent. Default post/announcement prices remain 5/20, exercised through `/economy`.
- `/book` creates an explicitly labelled import fixture. Subsequent edits go through real client `/letter` commands.

The READY4 process used for the comprehensive client tests predates a correction of the replacement-default configuration key. Those runs grant `skills.reading.6`, whose actual replacement and shuffle percentages are both zero, so that fixture typo does not alter the tested text. Subsequent helper builds also set the correctly spelled default key to zero.

## HTTP protocol

Every player endpoint accepts `?player=CourierAudit`; omitting player selects the first online player. Mutations require POST. Request bodies use UTF-8 JSON. Calls are applied on the server thread before responding. Errors return HTTP 400 with an `error` string.

| Method/path | Purpose |
| --- | --- |
| `GET /health` | `{ok,courier,paper,players,economy,http}`; `ok` requires Courier enabled. |
| `GET /state` | Actual hand/inventory pages, commands, outgoing queue, announcement count and couriers. |
| `POST /reset?slot=0&supplies=1` | Clear inventory, own outgoing queue, announcements and cooldowns; remove existing test couriers; reset position, permissions, balance and in-memory command/interaction lists. Historical reports remain. |
| `POST /slot?slot=1` | Change selected hotbar slot (0–8). |
| `POST /supply?slot=0&amount=1` | Install matching supply in inventory slot 0–40, amount 1–64. |
| `POST /book?slot=0` | Body: `{ "pages":["Page one"], "title":"§7Brief von §6CourierAudit", "author":"CourierAudit", "sent":false }`. Title/author/sent are optional. A bare pages array also works. |
| `POST /permissions?letter=false&post=false&announce=false` | Change supplied permission flags. Other supported flags: `unread`, `announcements`. |
| `POST /economy?balance=0` | Set deterministic balance. |
| `GET /outgoing` | Actual Courier queue: `{player,count,letters:[item]}`. |
| `GET /announcements_count` | Actual Courier in-memory `count`, separate `persistedCount`, and persisted `announcements` records read afresh from `plugins/Courier/announcements.yml`. |
| `POST /deliver` | Teleport an existing courier BAT close to the player. **Does not deliver mail.** Client must interact normally with the entity. Self-post is supported by the supplied Courier implementation. |
| `POST /archive?label=V201` | Save a named ground-truth state snapshot. |
| `POST /disconnect` | Intentional client disconnect for recovery tests. |
| `POST /stop` | Graceful shutdown after returning the response. |

Example state (additional fields omitted):

```json
{
  "player": "CourierAudit",
  "slot": 0,
  "hand": {
    "type": "WRITTEN_BOOK",
    "amount": 1,
    "rawPages": ["Hallo "],
    "title": "§7Brief von §6CourierAudit",
    "author": "CourierAudit",
    "lore": [],
    "itemModel": "ottonien:item/brief",
    "slot": 0
  },
  "commands": ["/letter Hallo"],
  "outgoing": [],
  "announcements_count": 0,
  "couriers": [{"id": 42, "entityId": 42, "uuid": "...", "type": "BAT", "x": 0.8, "y": 66.0, "z": 0.5}]
}
```

`hand.rawPages` and every inventory/outgoing item's `rawPages` come from **real `BookMeta.getPages()`**, preserving Courier formatting and trailing spaces. `commands` comes from `PlayerCommandPreprocessEvent`, so it records commands that actually crossed the client/server boundary. It is not a prediction or a list supplied by the client harness.

State also includes `announcements_count` (Courier's in-memory list), `announcements_persisted_count` (YAML records on disk), and `announcements: [{uuid, player, created, announced, letter}]`. Each `letter` has the same real Bukkit item fields as `hand`, including `rawPages`, `title`, `author` and `lore`; it is deserialized from a fresh disk read, independent of Courier's cached configuration and in-memory list. `announced` describes external Discord publication, so it remains false for local submissions. Separate counts expose partial Courier failures that saved a record but did not finish submission.

## Evidence

- `reports/provenance.json`: source paths, hashes and configuration differences.
- `reports/commands.jsonl`: client command events with timestamp, tick and cancellation status.
- `reports/interactions.jsonl`: actual client entity interaction events.
- `reports/<runId>-NNNNNN-after-command.json`, `*-after-interaction.json`, `*-V201.json`, etc.: complete server state snapshots. A timestamp/UUID prefix and create-new writes preserve earlier processes' files. State and JSONL records include the same `runId`. Older READY4 and earlier process reports used unprefixed names; Full1 was archived separately under `reports/full1` before its server restart.
- `console.log`, `console.err.log`, `logs/latest.log`: Paper/Courier startup and runtime output.

Dispatch and inventory changes do not prove external Discord delivery. Outgoing queue readback and actual entity interaction demonstrate the local Courier send/receive path.
