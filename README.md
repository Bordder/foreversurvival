# ForeverSurvival

A self-contained Fabric mod that turns a vanilla world into a strictly-locked,
months-long guided survival campaign.

No FTB Quests. No KubeJS. No FTB Teams. No questing library of any kind. The
only dependency is Fabric API.

156 quests, 455 objectives, zero rewards. The mod's whole job is to tell you
what to do next; everything you get, you get from vanilla.

## Downloads

Two builds, same mod, different Minecraft versions. Grab the jar for your
version from [Releases](../../releases) and drop it in `mods/` next to Fabric
API.

| Minecraft | Loader | Fabric API | Java | Branch | Release |
|---|---|---|---|---|---|
| 26.2 | 0.19.5 | 0.160.0+26.2 | 25 | `main` | `v1.0.0-mc26.2` |
| 1.18.2 | 0.15.11 | 0.77.0+1.18.2 | 17 | `1.18.2` | `v1.0.0-mc1.18.2` |

Both are feature-identical. The 26.2 build is a straight port — see
[PORTING-26.2.md](PORTING-26.2.md) for the mapping table if you are porting
something of your own and want the parts that are not obvious.

Mod ID `foreversurvival`, group `com.foreversurvival`, MIT licensed.

---

## What it does

**126 main quests** across 6 phases, welded into one strict linear chain, plus
**30 optional side challenges** that never block the main line. 455 individual
objectives in total.

| Phase | Name | Quests | Covers |
|---|---|---|---|
| 1 | The Struggle | 18 | first tree → first iron |
| 2 | The Establishment | 24 | iron, farms, village, mineshaft, diamonds, enchanting |
| 3 | The Expansion | 22 | all five Nether biomes, both structures, netherite |
| 4 | The Mastery | 24 | every remaining structure, the farms, the Wither |
| 5 | The Endgame | 18 | stronghold → dragon → End cities → Elytra |
| 6 | The Forever Goals | 20 | automation, collections, mega-builds |

Every quest is a real milestone of a survival world — no invented busywork.
Phase 3 walks the Crimson Forest, Warped Forest, Soul Sand Valley and Basalt
Deltas separately because that is how you actually explore the Nether. Phase 4
covers the gold farm, creeper farm, blaze farm, slime chunk, witch hut, trading
hall and item sorter because that is what the middle of a long world is.

**There are no rewards.** No items, no XP, no named trophies. The mod's only
job is to tell you what to do next — everything you get, you get from vanilla.

### Balance rule

An objective asks for roughly **what the next step actually consumes, plus a
small buffer** — 20 cobblestone covers a furnace and a tool set, 14 eyes of
ender covers a 12-frame portal at the 20% shatter rate, 4 ancient debris makes
exactly 1 netherite ingot. Length comes from the *number* of distinct
objectives, never from inflated stack counts. If a number looks like padding,
it's a bug.

### Strict locking, with previews

A quest's parent is the quest declared immediately before it — across phase
boundaries too.

Locked quests are **fully previewable**: you can click any quest ahead of you
and read its title, description, required tools, static guide and objective
list. They are drawn greyed with a padlock, and the "Mark objective complete"
button is absent.

They are never **doable**. `QuestManager` only evaluates progress for unlocked
quests and re-validates the lock on every checkmark packet, so previewing can
never turn into progress and a forged packet cannot skip ahead.

Completed quests get a green tick in the list, on each finished objective, and
next to each finished phase in the Summary tab.

### Static guides only

Each quest carries a hand-written `GuideText` and an explicit `RequiredTools`
list, both hand-checked against 1.18+ world generation. Nothing is computed
at runtime.
Guides are deliberately short: where to go, what tool you need, and the one
thing that will kill you if you get it wrong.

Examples of what is baked in:
- Diamond peaks at **Y=-59**, never above Y=16, and is *less* likely to
  generate exposed to air in 1.18+.
- Iron has two bands: underground peaking at **Y=16**, mountains peaking at
  **Y=232** (often on the surface in Jagged Peaks).
- Copper peaks at **Y=48**, gold at **Y=-16**, lapis at **Y=0**,
  redstone at **Y=-59**.
- Ancient Debris peaks at **Y=15** and never generates exposed to air.
- Amethyst geodes between **Y=-64 and Y=30**.
- Deepslate below Y=0, bedrock from Y=-64.

There are **no waypoints, no particle markers, no compass pointers and no
dynamic hints** anywhere in the mod.

### Death tracking

Every death records X / Y / Z, the dimension, the vanilla death message and a
snapshot of your inventory into the player's NBT:

```
Death #7: -450, 64, 200 [Overworld]
Steve fell from a high place
```

Click a death to see exactly what you were carrying, laid out like the
inventory screen. The list keeps the most recent 200 deaths; the most recent
**25** keep their inventory snapshot, older ones keep coordinates only so a
months-long world does not bloat player NBT.

No HUD element, no world marker, no recovery compass. Just the list.

### Stats

A **Stats** tab reads the vanilla statistics — playtime, distance walked /
sprinted / flown, deaths, mob kills, damage taken, jumps, nights slept — plus
quests completed and quests per hour. The client only receives vanilla stats
when it asks for them, so the handful shown here rides along with the quest
sync packet.

### Celebration

Finishing a quest gives a small one: a line on the action bar and a brief puff
of sparks. Finishing a **phase** gives a title card, and the major phases
(3, 4, 5, 6) add a firework burst. Visual only — no sound, no entity, no
lingering marker.

The vanilla "Advancement Made!" pop-up is suppressed by default (toggleable in
Config), since the mod runs its own progression and shows its own feedback. The
advancement is still granted — only the toast is hidden.

---

## Custom quests

Drop JSON files in `config/foreversurvival/quests/`. They are **additive**: the
156 built-in quests stay in Java, and a JSON file adds new quests or overrides
a built-in one by reusing its id. See [example-quests/](example-quests/) for a
working file.

Task types are `item`, `craft`, `kill`, `structure` and `checkmark`. A
`structure` task takes either a `structure` id (accurate) or a `blocks` list
(scan). Overriding a **main** quest must declare its own `parent`, or the
strict chain loses a link. A malformed file is logged and skipped; it never
stops the game loading.

---

## Controls

| Key | Action |
|---|---|
| `U` | Open / close the Quest Book |
| `'` | Toggle the overlay and locator bar |

Both defaults are keys vanilla leaves unbound, and both are rebindable under
Options → Controls → ForeverSurvival.

Tabs: **Main**, **Side**, **Deaths**, **Stats**, **Summary**, **Config**.

A small magnifier at the top of the quest list opens a search box — it filters
on title, description and guide text.

### Two layouts

A **List / Tree** button sits next to the search box (and in the tree header),
and there's a matching toggle in Config. The choice is saved.

**List** is the default: scrolling quest list on the left, full detail pane on
the right.

**Tree** shows one phase per page as a snake grid of icon nodes, so a whole
phase is visible at once:

```
  ◄  Phase 3: The Expansion  ►         22 quests - 9 done    [Tree]

    [✔]──[✔]──[✔]──[✔]──[✔]──[✔]──[✔]──┐
                                         │
    ┌────────────────────────────────────┘
    [✔]──[✔]──[◆]──[▒]──[▒]──[▒]──[▒]──┐
                                         │
    ┌────────────────────────────────────┘
    [▒]──[▒]──[▒]──[▒]──[▒]──[▒]──[▒]──[▒]
  ───────────────────────────────────────────────────────────
   Ghasts and Magma                              [ Open ]
   Kill Ghasts 1/3  -  Magma Cubes 4/8  -  Ghast Tears 0/2
```

Rows alternate direction so the chain reads continuously, and the connector
line turns green behind completed quests. Green node = done (with a tick), gold
= your current objective, grey + padlock = locked. Hover any node to preview it
in the footer, click to select, **Open** returns to the list layout. `◄ ►` page
between phases; Side Challenges get a single page.

Any quest can be **pinned** to the overlay from its detail pane. A pinned side
challenge shows its objectives; a pinned main quest is only a line of text,
since the main line is already on screen above it.
Mouse wheel scrolls the list and the detail pane independently. Every scroll
region is clipped with a GL scissor box, so rows are cut at the panel edge.

---

## On-screen overlay

A small panel showing the current main quest, its phase, and each objective
with a checkbox. It is a UI panel, not a world marker: it never points at
anything, never draws in the world, and never reveals a locked quest.

## Locator bar

A compass-style strip showing where the other players are. Each player is a
coloured tick placed by the angle between where you are looking and where they
actually are; look at someone and their name and distance appear beneath the
bar. Players outside the bar's arc clamp to the nearest edge and draw dimmer,
so you always know which way to turn.

Only players in **your own dimension** are shown — a bearing to someone in the
Nether while you are in the Overworld would mean nothing.

The vanilla client only knows about entities inside its tracking range, so the
server broadcasts every player's position 4 times a second to feed this. On a
single-player world nothing is sent at all.

---

## Settings

The **Settings** tab has real sliders — click or drag anywhere on the track:

| Setting | Type | Range |
|---|---|---|
| Show overlay / objectives / quest icon | toggle | — |
| Background opacity | slider | 0–100% (0 = text only, no panel) |
| Text opacity | slider | 20–100% (floored so it can't vanish) |
| Panel size | slider | 50–200% |
| Show locator bar / distance | toggle | — |
| Bar width | slider | 80–400px |
| Bar arc | slider | 30–180° |
| Bar size | slider | 50–200% |
| Move and resize… | button | opens the layout editor |
| Reset everything | button | — |

### Layout editor

**Move and resize…** opens a drag-and-drop editor. Drag either element to move
it, drag its bottom-right handle to resize it. Positions are stored as a
fraction of the screen, so a layout arranged at one resolution still looks
right at another.

Saved to `config/foreversurvival.properties`. Client-side only; nothing is
synced or stored in the world. Both HUD elements hide with F1 and while the
debug screen is open.

---

## How detection works

| Task type | Detection |
|---|---|
| `ItemTask` | Scans the player's inventory, armour and offhand. Progress is **sticky** — spending the items never undoes the objective. |
| `CraftTask` | Reads the vanilla `minecraft.crafted` statistic. |
| `KillTask` | Reads the vanilla `minecraft.killed` statistic, so only kills credited to you count. |
| `StructureTask` | Two modes. **Structure mode** asks the game directly whether you are inside a generated structure (`StructureAccessor`), checking every configured variant — so a Swamp Hut is a Swamp Hut, and your own spruce base with a cauldron can never satisfy it. **Block mode** scans ±12 blocks horizontally / ±6 vertically for a set of blocks, used only for things the game does not model as a structure: a player-built shelter, a lit nether portal, an amethyst geode, a cave biome. |
| `CheckmarkTask` | Manual. Click the checkbox in the detail pane to tick **or un-tick** it; the server re-validates that the quest is unlocked before accepting either. Once the whole quest is complete it is final — undoing part of a finished quest would strand the chain. |

Everything is evaluated on a **one-second poll** rather than a dozen event
hooks, which means progress you made before a quest unlocked still counts.

---

## Persistence

All state — completed quests, per-task progress, celebrated phases and the
death log — is written into the vanilla player NBT via a mixin on
`ServerPlayerEntity#writeCustomDataToNbt` / `readCustomDataFromNbt`, and copied
across on respawn with `ServerPlayerEvents.COPY_FROM`.

---

## Building

The `main` branch builds for 26.2 and needs **JDK 25**. The `1.18.2` branch
needs **JDK 17**. You do not need Gradle installed; the wrapper fetches it.

```bash
gradlew build
```

The jar carries its Minecraft version in the name, so the two builds can sit in
`build/libs` side by side without clobbering each other:

```
build/libs/foreversurvival-mc26.2-1.0.0.jar
build/libs/foreversurvival-mc1.18.2-1.0.0.jar
```

Ignore the `-sources` jar next to it. The plain one is what goes in `mods/`.

One gotcha on the 26.2 branch. Loom 1.17 wants a JVM of 21 or newer to run
Gradle. That is not the same thing as the JDK 25 it compiles against. If your
`JAVA_HOME` is older than 21 the build dies before it reaches any of your code.
Point `JAVA_HOME` at something newer, or set `org.gradle.java.home` in
`gradle.properties`.

26.2 was built against Loom 1.17.20 with Microsoft OpenJDK 25.0.4. 1.18.2 was
built against Loom 0.12.56 / Gradle 7.4.1 with Microsoft OpenJDK 17.0.20.

To launch a dev client:

```bash
gradlew runClient
```

---

## Project layout

```
src/main/java/com/foreversurvival/
├─ ForeverSurvival.java          Mod entrypoint, event wiring
├─ client/
│  ├─ ForeverSurvivalClient.java Keybinding + packet receivers
│  ├─ ClientQuestState.java      Client mirror of player data
│  ├─ ClientLocatorState.java    Latest player positions
│  ├─ QuestScreen.java           The five-tab GUI
│  ├─ QuestHud.java              On-screen objective overlay
│  ├─ LocatorBar.java            Compass strip of other players
│  ├─ HudEditScreen.java         Drag-to-move / drag-to-resize editor
│  ├─ HudRender.java             Shared scale/translate transform
│  └─ HudConfig.java             All display settings + properties file
├─ data/
│  ├─ PlayerQuestData.java       All persisted state
│  ├─ DeathRecord.java           One logged death
│  └─ QuestDataHolder.java       Mixin duck interface
├─ death/DeathLogger.java        Death event listener
├─ mixin/ServerPlayerEntityMixin.java  Player NBT read/write
├─ network/
│  ├─ ModNetworking.java         Sync + locations (S2C), checkmark (C2S)
│  └─ PlayerLocation.java        One player's broadcast position
└─ quest/
   ├─ Quest.java                 Definition + builder
   ├─ QuestPhase.java            6 phases + side bucket
   ├─ QuestManager.java          Singleton: registry, locking, evaluation
   ├─ QuestRegistry.java         Shared item groups + chaining helpers
   ├─ Phase1Quests.java …        One file per phase (18/24/22/24/18/20)
   ├─ SideQuests.java            The 30 optional challenges
   └─ task/                      ItemTask, CraftTask, StructureTask,
                                 KillTask, CheckmarkTask, TaskContext
```

## Adding your own quests

Quests live in the per-phase classes. `addMain(...)` chains onto the previous
main quest automatically; `addSide(...)` registers an always-open challenge.
Shared item groups (`LOGS`, `WOOL`, `BOATS`, …) live in `QuestRegistry`.

```java
addMain(Quest.builder("p2_25_my_quest", QuestPhase.PHASE_2)
        .title("My Quest")
        .desc("Short flavour line.")
        .icon(Items.IRON_INGOT)
        .guide("Static, hand-written instructions. Keep it short.")
        .tools("Iron Pickaxe", "Water Bucket")
        .task(new ItemTask("iron", "Collect Iron Ingots", 32, Items.IRON_INGOT)));
```
