# ReportX — Implementation Plan

An advanced-but-simple reporting plugin for Paper networks with Velocity proxy support. Built on the Solo framework. Multi-module Maven project.

---

## Guiding principles

- **Simple for players**: `/report <player> [reason]` → pick a category in a GUI → submit. Done.
- **Advanced for staff**: unclaimed queue, admin audit view, one-at-a-time handling with a full case file, combined log timeline, evidence, notes, audit log.
- **Fully configurable**: everything in categorised YAML files. Sensible defaults, nothing hardcoded.
- **Clean architecture**: Solo modules enforce subsystem boundaries. Storage and messaging are swappable via interfaces.
- **One at a time**: a staff member handles exactly one report at a time. `/rh` opens the current file if one is active, otherwise starts a new one.

---

## Commands

### Player
| Command | Aliases | Description |
| --- | --- | --- |
| `/report <player> [reason]` | — | Opens category picker GUI. `reason` is optional free-text detail attached to the report. |

### Staff — queue browsing
| Command | Aliases | Description |
| --- | --- | --- |
| `/reports` | `/unclaimedreports` | GUI of all **unclaimed** cases. Click to claim and begin handling. |
| `/claimedreports` | — | GUI of all **claimed** cases. Admin-oriented. Click a case → see audit log (status changes, notes, evidence edits, handoffs, etc). |

### Staff — handling
| Command | Aliases | Description |
| --- | --- | --- |
| `/reporthandle` | `/rh`, `/handle` | If not currently handling: category picker → auto-claims oldest unhandled → opens file GUI. If currently handling: skips straight to the file GUI. |
| `/rh release` | — | Releases the current case back to the unclaimed queue. Audit-logged. |
| `/rh handoff <staff>` | — | Transfers the current case to another staff member. Audit-logged. |

Granular permission per command and per file-GUI action (see "Permissions" below).

---

## Category picker GUI

Used in both `/report` (player side) and `/rh` (staff side).

- Each configured category = a slot with icon, name, lore.
- Player flow: click → optionally prompt for detail text → submit.
- Staff flow (`/rh`): click → auto-claims oldest unhandled in that category → opens file GUI.

### Empty-queue behavior (staff side only)
- If the staff clicks a category with **no unhandled reports**, the slot:
  1. Swaps to iron bars with configured name/lore (default: "No reports available").
  2. Plays configurable error sound.
  3. After 2 seconds (configurable), reverts to the normal category icon.

### Default categories
Ship with three, config supports up to five:

1. **Hacking**
2. **Chat**
3. **Other**

Each category in YAML defines: id, display name, lore, icon material, priority, evidence buffer window override.

---

## The case file GUI (the heart of the plugin)

Opened via `/rh` when a staff is handling a case. Also opened (read-only for non-claimers) via `/reports` claim flow and `/claimedreports` inspection.

### Layout (6-row chest GUI)

**Row 1 — Player info panel**
- Player head (live skin)
- Name, UUID
- Current server, world, coordinates
- Online/offline, ping
- Playtime (from Bukkit statistic by default, configurable)
- **Reputation badge**: tier name + short description
  - Example: `Reputation: Outstanding — This player has had 1 report against their account.`
  - Example: `Reputation: Bad — This player has had 15 reports against their account.`

**Row 2-3 — Case info & reports**
- Case category, priority, claim status, age
- List of all reports merged into this case (reporter name, timestamp, detail)

**Row 4 — Action buttons**
- **Teleport** → safe teleport to target (cross-proxy via Velocity if needed)
- **View Logs** → opens combined log timeline sub-GUI
- **Attach Evidence** → chat-input prompt for label + content
- **View Evidence** → list of evidence entries, click to delete (if author or admin)
- **Add Note** → chat-input for note body
- **View Notes** → list of notes (date/time, author, last edited), click own note to edit/delete
- **Resolve — Accept** → prompts for reason
- **Resolve — Deny** → prompts for reason
- **Release / Handoff** → if configured as buttons

**Row 5-6 — Filler / reserved**

### View Logs sub-GUI
- **Combined timeline**: chat messages, commands, and connections (joins/leaves) interleaved chronologically.
- Each entry shows type icon, timestamp, content.
- Scrollable (Solo's scrollable layout).

---

## Dedup / case grouping

Reports are grouped into **Cases** by default.

- **Grouping rule** (configurable, default): same target + same category + within 5 minutes → merged into one case.
- Once a case is claimed, new matching reports still merge into it and the claimer gets a notification.
- Once a case is resolved, new matching reports create a **new** case.

The `dedup` config block controls:
- Window duration
- Whether category must match
- Whether merging continues after claim

---

## Reputation

Based on **number of reports against the target player**, not the reporter.

- **Decays over time** — configurable half-life, default 30 days.
- Shown in file GUI as **tier + short description**.
- Tiers fully configurable in `reputation.yml`. Default ladder:

| Tier | Range | Color |
| --- | --- | --- |
| Outstanding | 0 | green |
| Good | 1-2 | light green |
| Neutral | 3-5 | yellow |
| Questionable | 6-10 | orange |
| Bad | 11+ | red |

Thresholds, names, colors, and description templates all configurable.

**Explicitly NOT tracked**: reporter abuse / false-report scoring. Out of scope.

---

## Evidence

- **Fields**: label (free-text), content (free-text), author UUID, created-at, last-edited-at.
- **Content type**: by default anything. Config flag `evidence.require-url` enforces content must contain a URL.
- **Multiple entries** per case.
- **Deletion**: author can delete own; admins can delete any. All audit-logged.

---

## Notes

- **Fields**: body, author UUID, created-at, last-edited-at.
- **Visibility**: all staff with permission.
- **Edit rights**: **only the author** can edit their own notes.
- **Audit**: creates, edits, and deletes all logged.

---

## Audit log

Per-case, append-only, only state-changing events recorded (not GUI views or navigation).

Logged events:
- Case created (from report)
- Report merged into case
- Claimed / released / handed off (with actor)
- Evidence added / edited / deleted
- Note added / edited / deleted
- Resolved — accepted (with reason)
- Resolved — denied (with reason)
- Reopened (with reason)

Visible via `/claimedreports` click-through. Configurable retention (default: forever).

---

## Configuration files (all under `plugins/ReportX/`)

Categorised YAML. Everything configurable, sensible defaults. Files: `config.yml`, `storage.yml`, `messaging.yml`, `categories.yml`, `reputation.yml`, `gui.yml`, `messages.yml`.

### `config.yml`
Plugin-wide behavior.
```yaml
dedup:
  enabled: true
  window-seconds: 300
  same-category-required: true
  merge-after-claim: true

reports:
  cooldown-seconds: 60
  max-per-day: 20
  min-account-age-hours: 1
  require-reason: false

handle:
  empty-queue-revert-seconds: 2
  stale-claim-reclaim-minutes: 15

gui:
  click-sound:
    # Plays on valid clicks only (action buttons, category picks, nav).
    # Does not play on fillers, disabled slots, or the empty-queue iron bars.
    enabled: true
    sound: UI_BUTTON_CLICK
    volume: 1.0
    pitch: 1.0

evidence:
  require-url: false

logs:
  buffer:
    chat-max-messages: 200
    chat-retention-hours: 24
    commands-max: 100
    commands-retention-hours: 24
    connections-retention-days: 30
  persist: true
  rolling: true

proxy:
  enabled: false
  # when true, requires Redis messaging
```

### `storage.yml`
```yaml
backend: sqlite          # sqlite | mysql | postgres
sqlite:
  file: reports.db
mysql:
  host: localhost
  port: 3306
  database: reportx
  username: reportx
  password: ""
  pool-size: 10
postgres:
  host: localhost
  port: 5432
  database: reportx
  username: reportx
  password: ""
  pool-size: 10
```

### `messaging.yml`
```yaml
transport: local         # local | redis
redis:
  host: localhost
  port: 6379
  password: ""
  channel-prefix: reportx
```

### `categories.yml`
```yaml
categories:
  - id: hacking
    display: "<red>Hacking</red>"
    icon: IRON_SWORD
    lore:
      - "<gray>Reports about cheating, hacks, exploits."
    priority: 100
    evidence-window-seconds: 60
  - id: chat
    display: "<yellow>Chat</yellow>"
    icon: PAPER
    lore:
      - "<gray>Reports about spam, toxicity, harassment."
    priority: 50
    evidence-window-seconds: 30
  - id: other
    display: "<gray>Other</gray>"
    icon: BOOK
    lore:
      - "<gray>Anything that doesn't fit the above."
    priority: 25
    evidence-window-seconds: 30

empty-slot:
  icon: IRON_BARS
  display: "<red>No reports available</red>"
  lore:
    - "<gray>Check back later."
  sound: BLOCK_NOTE_BLOCK_BASS
```

### `reputation.yml`
```yaml
decay:
  enabled: true
  half-life-days: 30

tiers:
  - id: outstanding
    min-reports: 0
    max-reports: 0
    display: "<green>Outstanding</green>"
    description: "This player has had no reports against their account."
  - id: good
    min-reports: 1
    max-reports: 2
    display: "<dark_green>Good</dark_green>"
    description: "This player has had {count} report(s) against their account."
  - id: neutral
    min-reports: 3
    max-reports: 5
    display: "<yellow>Neutral</yellow>"
    description: "This player has had {count} reports against their account."
  - id: questionable
    min-reports: 6
    max-reports: 10
    display: "<gold>Questionable</gold>"
    description: "This player has had {count} reports against their account."
  - id: bad
    min-reports: 11
    max-reports: -1
    display: "<red>Bad</red>"
    description: "This player has had {count} reports against their account."
```

### `gui.yml`
All GUI layouts — titles (with placeholders), sizes, slots, materials, items, filler, border, pagination nav. One file so tweaks don't require hunting across configs.

**Placeholder support**: each GUI documents the placeholders it accepts in its title and item fields. Shared across most GUIs: `{target}`, `{id}`, `{category}`, `{status}`, `{staff}`, `{count}`, `{page}`, `{maxpage}`. Category picker adds `{category_id}`. Case file adds `{reputation_tier}`, `{created}`, `{claimed_at}`.

**Item schema** (reused for all buttons):
```yaml
slot: 13
material: PLAYER_HEAD
display: "<yellow>{target}"
lore:
  - "<gray>UUID: <white>{uuid}"
  - "<gray>Server: <white>{server}"
enchanted: false        # adds glow
custom-model-data: 0    # optional
```

```yaml
# Click sound fallback lives in config.yml (gui.click-sound). Override per-GUI if needed.

category-picker-player:
  title: "<dark_gray>Report a Player"
  rows: 3
  filler:
    material: GRAY_STAINED_GLASS_PANE
    display: " "
  # Category items are pulled from categories.yml; slots here control placement.
  category-slots: [11, 13, 15]    # for 3 categories; expand for up to 5
  close-button:
    slot: 22
    material: BARRIER
    display: "<red>Cancel"

category-picker-staff:
  title: "<dark_gray>Handle a Report"
  rows: 3
  filler:
    material: BLACK_STAINED_GLASS_PANE
    display: " "
  category-slots: [11, 13, 15]
  # Slots show live unhandled count as lore.
  item-lore-append:
    - ""
    - "<gray>Unhandled: <white>{unhandled_count}"
  close-button:
    slot: 22
    material: BARRIER
    display: "<red>Close"

unclaimed-queue:
  title: "<dark_gray>Unclaimed Reports <gray>({count})"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43]
  case-item:
    material: PLAYER_HEAD
    display: "<yellow>{target} <gray>— <white>{category}"
    lore:
      - "<gray>Reports: <white>{report_count}"
      - "<gray>Waiting: <white>{age}"
      - "<gray>Reputation: {reputation_tier}"
      - ""
      - "<green>Click to claim and handle."
  nav:
    previous:
      slot: 48
      material: ARROW
      display: "<yellow>Previous"
    next:
      slot: 50
      material: ARROW
      display: "<yellow>Next"
  filler:
    material: BLACK_STAINED_GLASS_PANE
    display: " "

claimed-queue:
  title: "<dark_gray>Claimed Reports <gray>({count})"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43]
  case-item:
    material: PLAYER_HEAD
    display: "<yellow>{target} <gray>— <white>{category}"
    lore:
      - "<gray>Claimed by: <white>{staff}"
      - "<gray>Claimed at: <white>{claimed_at}"
      - "<gray>Reports: <white>{report_count}"
      - ""
      - "<aqua>Click to view case file & audit log."
  nav:
    previous: { slot: 48, material: ARROW, display: "<yellow>Previous" }
    next:     { slot: 50, material: ARROW, display: "<yellow>Next" }
  filler:
    material: BLACK_STAINED_GLASS_PANE
    display: " "

case-file:
  title: "<dark_gray>Case #{id} <gray>— <yellow>{target}"
  rows: 6
  player-info:
    slot: 4
    material: PLAYER_HEAD
    display: "<yellow>{target}"
    lore:
      - "<gray>UUID: <white>{uuid}"
      - "<gray>Server: <white>{server}"
      - "<gray>World: <white>{world}"
      - "<gray>Coords: <white>{x}, {y}, {z}"
      - "<gray>Ping: <white>{ping}ms"
      - "<gray>Playtime: <white>{playtime}"
      - ""
      - "<gray>Reputation: {reputation_tier}"
      - "<gray>{reputation_description}"
  reports-info:
    slot: 13
    material: WRITABLE_BOOK
    display: "<yellow>Reports ({report_count})"
    lore:
      - "<gray>Category: <white>{category}"
      - "<gray>Opened: <white>{created}"
      - "<gray>Claimed by: <white>{staff}"
  buttons:
    teleport:
      slot: 19
      material: ENDER_PEARL
      display: "<aqua>Teleport to target"
    view-logs:
      slot: 20
      material: BOOK
      display: "<yellow>View logs"
      lore:
        - "<gray>Chat, commands, connections."
    attach-evidence:
      slot: 21
      material: MAP
      display: "<green>Attach evidence"
    view-evidence:
      slot: 22
      material: ITEM_FRAME
      display: "<green>View evidence <gray>({evidence_count})"
    add-note:
      slot: 23
      material: PAPER
      display: "<yellow>Add note"
    view-notes:
      slot: 24
      material: WRITTEN_BOOK
      display: "<yellow>View notes <gray>({note_count})"
    release:
      slot: 39
      material: HOPPER
      display: "<gold>Release back to queue"
    handoff:
      slot: 40
      material: NAME_TAG
      display: "<gold>Hand off"
    resolve-accept:
      slot: 41
      material: LIME_WOOL
      display: "<green>Resolve — Accept"
    resolve-deny:
      slot: 42
      material: RED_WOOL
      display: "<red>Resolve — Deny"
  filler:
    material: GRAY_STAINED_GLASS_PANE
    display: " "
  border:
    material: BLACK_STAINED_GLASS_PANE
    display: " "

log-timeline:
  title: "<dark_gray>Logs <gray>— <yellow>{target}"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43]
  entry-chat:
    material: PAPER
    display: "<white>[{time}] <yellow>CHAT"
    lore:
      - "<gray>{content}"
  entry-command:
    material: COMMAND_BLOCK
    display: "<white>[{time}] <aqua>CMD"
    lore:
      - "<gray>{content}"
  entry-connection:
    material: ENDER_EYE
    display: "<white>[{time}] <light_purple>{connection_type}"
    lore:
      - "<gray>{content}"
  back-button: { slot: 49, material: ARROW, display: "<yellow>Back" }

evidence-list:
  title: "<dark_gray>Evidence <gray>— <yellow>Case #{id}"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]
  entry:
    material: MAP
    display: "<yellow>{label}"
    lore:
      - "<gray>{content}"
      - ""
      - "<gray>Author: <white>{author}"
      - "<gray>Added: <white>{created}"
      - "<gray>Edited: <white>{edited}"
      - ""
      - "<red>Shift-click to delete (if allowed)."
  back-button: { slot: 49, material: ARROW, display: "<yellow>Back" }

notes-list:
  title: "<dark_gray>Notes <gray>— <yellow>Case #{id}"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25]
  entry:
    material: PAPER
    display: "<yellow>Note by {author}"
    lore:
      - "<gray>{body}"
      - ""
      - "<gray>Added: <white>{created}"
      - "<gray>Edited: <white>{edited}"
      - ""
      - "<aqua>Click to edit (author only)."
  back-button: { slot: 49, material: ARROW, display: "<yellow>Back" }

audit-log:
  title: "<dark_gray>Audit Log <gray>— <yellow>Case #{id}"
  rows: 6
  content-slots: [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34]
  entry:
    material: KNOWLEDGE_BOOK
    display: "<yellow>{event_type}"
    lore:
      - "<gray>{summary}"
      - ""
      - "<gray>Actor: <white>{actor}"
      - "<gray>When: <white>{created}"
  back-button: { slot: 49, material: ARROW, display: "<yellow>Back" }
```

### `messages.yml`
All user-facing strings. Supports **MiniMessage AND legacy** (`&c`) color codes — plugin detects and parses both so Spigot servers without native Adventure still work.
```yaml
prefix: "<gray>[<aqua>ReportX</aqua>]</gray> "
report:
  submitted: "<green>Your report has been submitted. Thank you."
  on-cooldown: "<red>Please wait {seconds}s before reporting again."
  resolved-notification: "<green>Thanks — a report you filed has been actioned."
staff:
  new-report: "<yellow>New report filed against <white>{target}</white> (<gray>{category}</gray>)."
  case-claimed: "<aqua>{staff}</aqua> claimed case <gray>#{id}</gray>."
  case-released: "<aqua>{staff}</aqua> released case <gray>#{id}</gray>."
  handoff: "<aqua>{from}</aqua> handed off case <gray>#{id}</gray> to <aqua>{to}</aqua>."
  resolve-accepted: "<green>Case <gray>#{id}</gray> resolved as accepted."
  resolve-denied: "<red>Case <gray>#{id}</gray> resolved as denied."
# ... etc for every string
```

### `permissions.yml` *(optional — or defined in plugin metadata)*
Documents the granular permission set.

---

## Permissions (granular)

| Node | Grants |
| --- | --- |
| `reportx.report` | `/report` |
| `reportx.staff.reports` | `/reports`, `/unclaimedreports` |
| `reportx.staff.claimedreports` | `/claimedreports` + audit log view |
| `reportx.staff.handle` | `/rh`, `/reporthandle`, `/handle` |
| `reportx.staff.release` | `/rh release` |
| `reportx.staff.handoff` | `/rh handoff` |
| `reportx.staff.teleport` | Teleport button in file GUI |
| `reportx.staff.logs` | View Logs button |
| `reportx.staff.evidence.add` | Attach evidence |
| `reportx.staff.evidence.delete.own` | Delete own evidence |
| `reportx.staff.evidence.delete.any` | Delete any evidence (admin) |
| `reportx.staff.note.add` | Add notes |
| `reportx.staff.note.edit.own` | Edit own notes |
| `reportx.staff.note.delete.any` | Delete any note (admin) |
| `reportx.staff.resolve` | Resolve accept/deny |
| `reportx.admin.reopen` | Reopen resolved cases |
| `reportx.admin.*` | Wildcard admin |

---

## Project layout (Maven multi-module)

```
ReportX/
├── pom.xml                              (parent, <packaging>pom</packaging>)
│
├── reportx-core/                        (pure Java — no platform deps)
│   └── src/main/java/com/reportx/core/
│       ├── model/                       (Report, Case, Evidence, Note, AuditEntry, Reputation, Category)
│       ├── storage/                     (ReportRepository, CaseRepository, EvidenceRepository, NoteRepository, AuditRepository, LogBufferRepository interfaces)
│       ├── messaging/                   (MessageBus interface, event types)
│       ├── config/                      (config loading/models)
│       └── util/                        (MiniMessage+legacy parser, etc.)
│
├── reportx-storage-sqlite/              (SQLite impl)
├── reportx-storage-sql/                 (MySQL/Postgres impl via HikariCP)
│
├── reportx-messaging-local/             (in-JVM bus for standalone)
├── reportx-messaging-redis/             (Lettuce-based pub/sub)
│
├── reportx-paper/                       (Paper plugin — Solo-based)
│   └── src/main/java/com/reportx/paper/
│       ├── ReportXPlugin.java           (extends SoloPlugin)
│       └── modules/
│           ├── ConfigModule.java
│           ├── StorageModule.java
│           ├── MessagingModule.java
│           ├── LogBufferModule.java     (in-memory rolling buffer, persists to DB)
│           ├── ReportModule.java        (/report, category picker GUI)
│           ├── EvidenceModule.java      (attach/delete evidence logic)
│           ├── CaseModule.java          (dedup/grouping, claim/release/handoff, resolve/reopen)
│           ├── StaffUIModule.java       (/reports, /claimedreports, /rh, file GUI)
│           ├── ReputationModule.java    (tier calculation + decay)
│           └── AuditModule.java         (append-only audit log)
│
├── reportx-velocity/                    (Velocity proxy plugin)
│   └── handles cross-server teleport routing, report event relay
│
└── reportx-dist/                        (shading → final jars)
```

**Why the split**
- `core` is pure Java → fast, easy to test, no platform leak.
- Storage/messaging impls pluggable via config without recompiling.
- Platform modules (`paper`, `velocity`) are thin: bootstrap + event translation.
- `dist` produces shaded jars for drop-in install.

---

## Solo modules (Paper plugin)

Each Solo module is an **internal subsystem**, not a user-facing toggle. Solo's dependency graph ensures correct load order.

| Module | Depends on | Responsibilities |
| --- | --- | --- |
| **ConfigModule** | — | Loads `config.yml`, `storage.yml`, `messaging.yml`, `categories.yml`, `reputation.yml`, `gui.yml`, `messages.yml`. Exposes config models via ServiceRegistry. `/reportx reload` support. |
| **StorageModule** | Config | Spins up DB. Exposes all repositories. |
| **MessagingModule** | Config | Spins up transport (local/Redis). Exposes MessageBus. |
| **LogBufferModule** | Storage | In-memory rolling buffer of chat/commands/connections per player. Persists to DB on configured rotation and on report submit. |
| **AuditModule** | Storage, Messaging | Subscribes to all state-change events and writes audit entries. |
| **ReputationModule** | Storage | Computes tier + decayed score for a target on demand. |
| **ReportModule** | Storage, Messaging, Config | `/report` command, category picker, submission flow. |
| **EvidenceModule** | Storage, Messaging | Evidence CRUD, URL validation if enforced. |
| **CaseModule** | Storage, Messaging, LogBuffer | Dedup/grouping, claim/release/handoff, resolve, reopen. Snapshots log buffer to case on creation. |
| **StaffUIModule** | Case, Reputation, Evidence, Config | `/reports`, `/claimedreports`, `/rh`, file GUI, log timeline GUI. |

---

## Data model (core)

### Report
```
id              UUID
caseId          UUID
reporterId      UUID
targetId        UUID
category        String
detail          String (nullable)
serverName      String
reporterCoords  (world, x, y, z)
createdAt       Instant
```

### Case
```
id              UUID
targetId        UUID
category        String
status          UNCLAIMED | CLAIMED | RESOLVED_ACCEPTED | RESOLVED_DENIED
claimedBy       UUID (nullable)
claimedAt       Instant (nullable)
resolvedBy      UUID (nullable)
resolvedAt      Instant (nullable)
resolutionReason String (nullable)
createdAt       Instant
lastActivityAt  Instant
```

### Evidence
```
id              UUID
caseId          UUID
label           String
content         String
authorId        UUID
createdAt       Instant
editedAt        Instant (nullable)
```

### Note
```
id              UUID
caseId          UUID
body            String
authorId        UUID
createdAt       Instant
editedAt        Instant (nullable)
```

### AuditEntry
```
id              UUID
caseId          UUID
actorId         UUID (nullable — system-generated if null)
eventType       String (enum)
payload         JSON (event-specific details)
createdAt       Instant
```

### LogBufferEntry
```
playerId        UUID
type            CHAT | COMMAND | CONNECTION
content         String
serverName      String
createdAt       Instant
```
Rolling window, auto-pruned per `config.yml` retention.

### ReputationSnapshot (derived, not stored long-term)
```
targetId        UUID
rawCount        int
decayedScore    double
tierId          String
```
Computed on demand from decayed report count.

---

## Cross-server events (via MessageBus)

- `ReportCreated(report)`
- `CaseCreated(case)`
- `ReportMergedIntoCase(reportId, caseId)`
- `CaseClaimed(caseId, staffId, serverName)`
- `CaseReleased(caseId, staffId)`
- `CaseHandedOff(caseId, fromStaffId, toStaffId)`
- `CaseResolved(caseId, outcome, staffId, reason)`
- `CaseReopened(caseId, staffId, reason)`
- `EvidenceAdded/Edited/Deleted(evidenceId, caseId, actorId)`
- `NoteAdded/Edited/Deleted(noteId, caseId, actorId)`
- `TeleportToTargetRequest(staffId, targetId)` — Velocity routes cross-server.

Local (standalone) mode uses a synchronous in-JVM bus. Network mode uses Redis pub/sub. Same API.

---

## Build & tooling

- **Java 21**
- **Maven** multi-module, parent manages versions
- **Paper API 1.21.x** (provided)
- **Velocity API 3.x** (provided, proxy module)
- **Solo** for Paper module
- **HikariCP + JDBC** (shaded)
- **Lettuce** for Redis (shaded)
- **Jackson** for event serialization
- **MiniMessage** (Adventure bundled with Paper; shaded for legacy Spigot fallback)
- **Lombok** (provided — consistent with Solo)
- **JUnit 5 + Mockito** for core tests
- **Testcontainers** for SQL + Redis integration tests
- Shading into `com.reportx.shadow.*`

---

## Delivery milestones

### Milestone 1 — Core foundation
- Parent POM + module skeletons
- `reportx-core` models, repositories, event types
- `reportx-storage-sqlite` full impl
- `reportx-messaging-local` full impl
- ConfigModule + YAML loading (all files)
- Unit tests for core

### Milestone 2 — Paper MVP (standalone)
- StorageModule, MessagingModule, LogBufferModule, AuditModule
- ReportModule with `/report` + category picker GUI
- CaseModule with dedup/grouping, claim/release/handoff, resolve/reopen
- EvidenceModule, ReputationModule
- StaffUIModule with `/reports`, `/claimedreports`, `/rh` + full file GUI + log timeline
- End-to-end flow working on single Paper server
- **Deliverable**: single-server Paper plugin that beats existing report plugins

### Milestone 3 — Network support
- `reportx-storage-sql` (MySQL + Postgres)
- `reportx-messaging-redis`
- `reportx-velocity` proxy plugin (teleport routing, event relay)
- Tested with 2 Paper backends + 1 Velocity
- **Deliverable**: production-ready for networks

### Milestone 4 — Polish
- Full message catalog + localization-ready
- `/reports-stats` leaderboard
- Admin commands for config reload, case search/export
- Performance benchmarks at 1k/10k cases in DB
- **Deliverable**: v1.0 release

### Post-1.0 (deferred)
- Punishment plugin bridge (LiteBans, AdvancedBan, LibertyBans)
- HistoryX integration
- Discord webhook module
- Web panel
- Bungee support
- Screenshot attachment flow

---

## Open items to confirm before coding

1. **Package name** — `com.reportx` or `com.github.willrees23.reportx` (matching Solo's convention)?
2. **Group ID / artifact naming** — `com.reportx:reportx-parent` etc.?
3. **License** — MIT? Apache 2? GPL (matches Spigot ecosystem)?
4. **Repo structure** — monorepo (current plan), or separate repos per module?

---

## Next concrete step

Scaffold the Maven parent POM + `reportx-core` module skeleton with models and repository interfaces. Everything else stacks on those.
