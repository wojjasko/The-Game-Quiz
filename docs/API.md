# The Game Quiz – API contract

This document is the contract between the Spring backend and the browser frontend.
All identifiers are English. User-facing texts in the UI stay Polish.

## Concepts

- **Game** – one room, identified by a 5-character `code` (e.g. `K7M2Q`). Several games can run in parallel.
- **Host** – the quiz master. Creating a game returns a secret `hostToken`. Every host action must send it in the
  `X-Host-Token` header. Only the host can change game state.
- **Players** – join a game with a nickname and choose a team. Joining returns a secret `playerId`
  (used only to leave the game / rejoin). Players only watch the screen (buzzers are a future feature).
- **Teams** – exactly two: `BLUE` and `RED` (enum `Team`). Always send/receive the upper-case names.
- **Phases** – `LOBBY`, `STAGE_1`, `STAGE_2`, `STAGE_3`, `FINISHED` (enum `GamePhase`). The host switches them; every
  screen follows automatically.

## Realtime (STOMP over SockJS)

- Endpoint: `/ws-quiz` (SockJS). Libraries: `sockjs-client` 1.5.1 and `stomp.js` 2.3.3 (CDN, as in the legacy pages).
- `/topic/game/{code}/state` – a full `GameSnapshot` after every state change.
- `/topic/game/{code}/timers` – a `TimersSnapshot`, sent every second while a stage-1 timer is running.
- Client flow: connect -> subscribe to both topics -> `GET /api/games/{code}` for the initial snapshot.
- Players and the projector screen never receive secrets (e.g. the correct answer of a stage-3 card) – those are only
  available from the host endpoint.

## Errors

Every error response has the shape `{ "error": "human readable message" }`:

| Status | Meaning |
|--------|---------|
| 400 | invalid input (bad value, missing field, name too long, ...) |
| 403 | missing / wrong `X-Host-Token`, or wrong access code |
| 404 | game (or player) not found |
| 409 | action not allowed in the current state (name taken, team full, lifeline already used, ...) |

## Data shapes

### GameSnapshot (public)

Exactly one of `stage1` / `stage2` / `stage3` is non-null – the one matching `phase`. In `LOBBY` and `FINISHED` all three
are `null`.

```json
{
  "code": "K7M2Q",
  "phase": "STAGE_1",
  "teams": {
    "BLUE": { "name": "KANGURY Z KOSMOSU", "score": 0, "players": ["Ala", "Olek"] },
    "RED":  { "name": "BUNIE",             "score": 0, "players": ["Kasia"] }
  },
  "stage1": { },
  "stage2": null,
  "stage3": null
}
```

### Stage1View

```json
{
  "board": [["DEFAULT", "BLUE", "RED", "INCORRECT"], ["..."], ["..."], ["..."]],
  "categories": ["Kategoria 1", "Kategoria 2", "Kategoria 3", "Kategoria 4"],
  "question": "Text currently shown on the screen",
  "imageUrl": "/media/images/stage1/mario.jpg",
  "soundUrl": "/media/sounds/smb_powerup.wav",
  "imageZoomed": false,
  "timers": {
    "BLUE": { "remainingSeconds": 360, "running": false },
    "RED":  { "remainingSeconds": 360, "running": false }
  },
  "steals": { "BLUE": 2, "RED": 2 },
  "completedLines": [ [[0,0],[0,1],[0,2]], [[0,0],[1,1],[2,2]] ]
}
```

- `board[row][col]`, 4x4. Row `i` is worth `5, 10, 15, 20` points (row 0 = 5).
- `TileStatus`: `DEFAULT`, `BLUE`, `RED`, `INCORRECT`, `BLUE_STEAL`, `RED_STEAL`, `SELECTED`.
- `imageUrl` / `soundUrl` are `null` when there is no media.
- `completedLines`: every line of 3 tiles that already gave its team the 5-point bonus; each line is a list of
  `[row, col]` cells. The screen highlights a line for 3 seconds the first time it sees it.

### TimersSnapshot (`/timers` topic)

```json
{ "BLUE": { "remainingSeconds": 359, "running": true }, "RED": { "remainingSeconds": 360, "running": false } }
```

### Stage2View

```json
{
  "selectedQuestion": 1,
  "outcomes": ["PENDING", "BLUE", "RED", "NONE", "PENDING", "PENDING", "PENDING", "PENDING"],
  "images": [
    { "index": 0, "url": "/media/images/stage2/p1_1.jpg", "revealed": false },
    { "index": 1, "url": "/media/images/stage2/p1_2.jfif", "revealed": true },
    { "index": 2, "url": null, "revealed": false }
  ],
  "focusedImageIndex": null
}
```

- 8 questions, 3 images each. `images` describes **only the selected question**.
- `outcomes[n-1]` for question `n`: `PENDING` (in progress, white), `BLUE`, `RED`, `NONE` (nobody answered, gray).
- A non-revealed image is shown blurred. `focusedImageIndex` (0..2 or `null`) is the image the host zoomed to the
  center of the screen; focusing an image also reveals it permanently.
- `url` can be `null` (image file missing on the server) – show a placeholder.

### Stage3View

```json
{
  "table": [ { "id": 7, "category": "Zmiany pogody", "used": false, "result": "NONE" } ],
  "multipliers": { "BLUE": 15, "RED": 20 },
  "lifelineUsed": { "BLUE": false, "RED": true },
  "question": {
    "category": "Zmiany pogody",
    "text": "W której grze ...?",
    "answers": ["COD Ghosts", "GTA V", "Battlefield 4", "Crysis 3"],
    "hiddenAnswers": ["A", "D"]
  }
}
```

- `table` has 10 cards (one per grid slot, in order). `result`: `NONE`, `BLUE_CORRECT`, `BLUE_WRONG`, `RED_CORRECT`,
  `RED_WRONG`. `used` is `true` once the card was opened and closed (also when closed without points).
- `question` is `null` when no card is open. `answers` are in order A, B, C, D. `hiddenAnswers` lists the letters
  removed by the 50:50 lifeline. **The correct answer is never part of the public snapshot.**
- `multipliers`: points the team gets for its next correct answer (starts at 15, +5 per correct answer, reset to 15
  after a wrong one). `lifelineUsed`: 50:50 lifeline already spent.

### HostSnapshot (host only)

```json
{
  "game": { "...GameSnapshot..." },
  "players": [ { "id": "3f2c...", "name": "Ala", "team": "BLUE" } ],
  "stage1Categories": ["Kategoria 1", "Kategoria 2", "Kategoria 3", "Kategoria 4"],
  "stage1Questions": [
    [ { "text": "Pytanie 1", "image": "", "sound": "" }, "...4 per row..." ]
  ],
  "stage3": { "activeCardId": 7, "correctAnswer": "C", "correctAnswerText": "Battlefield 4" }
}
```

- `stage1Categories` and `stage1Questions[row][col]` (the 16-question database of stage 1; `image` / `sound` are file names, may be empty) are present in every phase, so the host can prepare stage 1 in the lobby.
- `stage3` is `null` when no card is open.

## Public endpoints

| Method & path | Body | Response |
|---|---|---|
| `POST /api/games` | `{ "accessCode": "..." }` | `201 { "code", "hostToken" }` – `accessCode` is required only when the server has `quiz.access-code` configured |
| `GET /api/games/{code}` | – | `GameSnapshot` |
| `POST /api/games/{code}/players` | `{ "name": "Ala", "team": "BLUE" }` | `201 { "playerId", "name", "team" }` |
| `DELETE /api/games/{code}/players/{playerId}` | – | `204` (player leaves; the host uses the same call to kick) |

Name rules: 1–24 characters after trimming, unique in the game (case-insensitive), max 8 players per team.

## Host endpoints

All require `X-Host-Token`. All return a `HostSnapshot` unless noted. Every successful call also broadcasts the new
public `GameSnapshot` on `/topic/game/{code}/state`.

| Method & path | Body |
|---|---|
| `GET /api/games/{code}/host` | – |
| `GET /api/games/{code}/host/media` | – (returns `{ "stage1Images": ["mario.jpg"], "sounds": ["smb_powerup.wav"] }`, file names available on the server) |
| `PUT /api/games/{code}/phase` | `{ "phase": "STAGE_1" }` |
| `PUT /api/games/{code}/teams/{team}/name` | `{ "name": "..." }` (1–30 chars) |
| `POST /api/games/{code}/teams/{team}/score` | `{ "delta": 5 }` (adds, may be negative) |
| `PUT /api/games/{code}/teams/{team}/score` | `{ "value": 0 }` (sets) |
| `PUT /api/games/{code}/players/{playerId}/team` | `{ "team": "RED" }` (move a player) |

### Stage 1 – question matrix (`/api/games/{code}/stage1/...`)

| Method & path | Body | Effect |
|---|---|---|
| `PUT categories` | `{ "categories": ["a","b","c","d"] }` | exactly 4 names |
| `PUT tiles/{row}/{col}` | `{ "status": "BLUE" }` | set tile status; completing a line of 3 owned tiles (`BLUE`/`BLUE_STEAL` or `RED`/`RED_STEAL`) gives +5 points once per line |
| `POST tiles/{row}/{col}/activate` | – | load the stored question + media of this tile onto the screen |
| `PUT questions/{row}/{col}` | `{ "text", "image", "sound" }` | store a question in the database (`image`/`sound` = file names, may be empty) |
| `PUT current-question` | `{ "text": "..." }` | show free text on the screen |
| `DELETE current-question` | – | clear text, image and sound from the screen |
| `PUT image-zoom` | `{ "zoomed": true }` | enlarge / shrink the question image |
| `POST timers/{team}/{action}` | – | `action` = `start`, `stop` or `reset` (back to 06:00) |
| `POST steals/{team}/spend` | – | -1 steal (min 0) |
| `POST steals/reset` | – | both teams back to 2 steals |
| `POST time-bonus/{team}` | – | adds `min(40, floor(remainingSeconds / 15) * 5)` points |
| `POST reset` | – | board, question, timers and steals back to initial state (scores untouched) |

### Stage 2 – "Co to za gra?" (`/api/games/{code}/stage2/...`)

| Method & path | Body | Effect |
|---|---|---|
| `PUT question` | `{ "number": 3 }` | select question 1..8 (also removes the image focus) |
| `PUT questions/{number}/outcome` | `{ "outcome": "BLUE" }` | `PENDING`, `BLUE`, `RED`, `NONE` |
| `PUT focus` | `{ "imageIndex": 0 }` | center + reveal an image of the selected question (0..2) |
| `DELETE focus` | – | put the image back (it stays revealed) |

### Stage 3 – cards (`/api/games/{code}/stage3/...`)

| Method & path | Body | Effect |
|---|---|---|
| `POST cards/{id}/swap` | – | replace an unused table card with the next reserve card (409 when the reserve is empty) |
| `POST cards/{id}/open` | – | open the card's question on the screen |
| `POST lifeline` | `{ "team": "BLUE" }` | use the 50:50 lifeline on the open question (once per team) |
| `POST answer` | `{ "team": "BLUE", "correct": true }` | resolve the open question: correct = team gets its multiplier points and the multiplier grows by 5, wrong = multiplier resets to 15; the card is marked and closed |
| `POST close` | – | close the open question without points (card becomes `used`) |

## Static pages and browser storage

| Page | Purpose |
|---|---|
| `/` (`index.html`) | create a game (host) or join a game (player) |
| `/screen.html?game=CODE` | what players and the projector see; renders lobby / stage 1 / stage 2 / stage 3 / final by `phase` |
| `/host.html?game=CODE` | host control panel |

`localStorage` keys: `quiz.hostToken.<CODE>` (host token) and `quiz.playerId.<CODE>` (player id).

Media is served from `/media/**` (`/media/images/stage1/<file>`, `/media/images/stage2/<file>`, `/media/sounds/<file>`).
