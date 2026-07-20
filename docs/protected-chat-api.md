# Geschützte Chat-Originale: API-Vertrag

TranslateUtils kann den Originaltext einer übersetzten Chatnachricht für
verifizierte OttoExtra-Nutzer als Hover freigeben. Der öffentliche Minecraft-
Chat enthält ausschließlich die normale Übersetzung. Die Zuordnung wird von
berechtigten Clients über die API-Inbox geladen.

## Voraussetzungen

- Beide Routen verwenden den bestehenden Mojang-verifizierten Bearer-Token.
- Antworten werden wie die übrigen `/v2/*`-Routen mit der vorhandenen
  Ed25519-Antwortsignatur versehen.
- Usernamen werden beim Anlegen serverseitig auf UUIDs aufgelöst. Die
  Berechtigungsprüfung darf nicht dauerhaft anhand veränderbarer Usernamen
  erfolgen.

## `POST /v2/chat-translations`

Request:

```json
{
  "original": "Der nicht öffentlich sichtbare Originaltext",
  "translations": ["Der öffentlich gesendete übersetzte Chattext"],
  "access": "all",
  "allowedUsernames": []
}
```

Für eine Allowlist wird `access` auf `allowlist` gesetzt und
`allowedUsernames` enthält höchstens 64 Minecraft-Namen. Der Server übernimmt
die UUID aus dem Bearer-Token als Besitzer, löst die Namen zu UUIDs auf und
antwortet mit:

```json
{
  "id": "zufaellige_url_sichere_id",
  "expiresAt": "2026-07-20T12:00:00Z"
}
```

Empfohlen: mindestens 128 Bit Zufall für `id`, maximal 5.000 Zeichen Text,
TTL 24 Stunden, Rate-Limit je Besitzer sowie keine Protokollierung des
Originaltexts.

## `GET /v2/chat-translations/{id}`

Die Route liefert nur bei gültigem Bearer-Token und erfüllter Freigabe:

```json
{
  "original": "Der nicht öffentlich sichtbare Originaltext",
  "senderUuid": "...",
  "expiresAt": "2026-07-20T12:00:00Z"
}
```

- `all`: jeder Mojang-verifizierte OttoExtra-Client darf lesen.
- `allowlist`: Besitzer oder eine beim Anlegen gespeicherte Empfänger-UUID darf
  lesen.
- Nicht berechtigt: `403`; unbekannt oder abgelaufen: `404`/`410`.

Die Datenbank benötigt eine Tabelle für `id`, Besitzer-UUID, verschlüsselten
Originaltext, Zugriffsmodus und Ablaufzeit sowie eine Kindtabelle mit den
erlaubten Empfänger-UUIDs. Ein regelmäßiger Cleanup entfernt abgelaufene
Datensätze. Verschlüsselung at rest ist empfohlen, da der Text absichtlich
nicht im öffentlichen Minecraft-Chat steht.

## `GET /v2/chat-translations/inbox`

Liefert für den authentifizierten Nutzer ausschließlich freigegebene,
markerlose Zuordnungen der letzten zehn Minuten. OttoExtra-Clients gleichen
`translation` mit dem sichtbaren Minecraft-Chat ab und ergänzen den Hover nur
lokal. Nicht-Mod-Clients erhalten keinerlei Referenzzeichen.
