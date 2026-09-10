# ForeverSurvival

A Fabric mod that lays a months-long guided survival campaign over a vanilla
world.

Fabric API is the only dependency.

156 quests, 455 objectives, no rewards. The mod tells you what to do next.

## Downloads

Two builds. Pick the one for your Minecraft version from
[Releases](../../releases) and drop the jar in `mods/` alongside Fabric API.

| Minecraft | Loader | Fabric API | Java | Branch | Release |
|---|---|---|---|---|---|
| 26.2 | 0.19.5 | 0.160.0+26.2 | 25 | `main` | `v1.0.0-mc26.2` |
| 1.18.2 | 0.15.11 | 0.77.0+1.18.2 | 17 | `1.18.2` | `v1.0.0-mc1.18.2` |

The builds are feature-identical.

Mod ID `foreversurvival`, group `com.foreversurvival`. MIT licensed.

---

## What it does

**126 main quests** across 6 phases, unlocking in a fixed order, plus
**30 optional side challenges** that sit outside that order. 455 individual
objectives in total.

| Phase | Name | Quests | Covers |
|---|---|---|---|
| 1 | The Struggle | 18 | first tree → first iron |
| 2 | The Establishment | 24 | iron, farms, village, mineshaft, diamonds, enchanting |
| 3 | The Expansion | 22 | all five Nether biomes, both structures, netherite |
| 4 | The Mastery | 24 | every remaining structure, the farms, the Wither |
| 5 | The Endgame | 18 | stronghold → dragon → End cities → Elytra |
| 6 | The Forever Goals | 20 | automation, collections, mega-builds |

Phase 3 walks the five Nether biomes separately. Phase 4 covers the gold farm,
creeper farm, blaze farm, slime chunk, witch hut, trading hall and item sorter.

### Balance rule

An objective asks for roughly **what the next step actually consumes, plus a
small buffer** — 20 cobblestone covers a furnace and a tool set, 14 eyes of
ender covers a 12-frame portal at the 20% shatter rate, 4 ancient debris makes
exactly 1 netherite ingot. Length comes from the *number* of distinct
objectives, never from inflated stack counts.

### Unlock order, with previews

A quest's parent is the quest declared immediately before it — across phase
boundaries too. One main quest is active at a time: the first incomplete one.

**This gates the quest list, not the world.** Nothing stops you mining
netherite in your first hour. Locking decides which quest is on screen and
being evaluated, and that is all it decides.

Locked quests are **fully previewable**: you can click any quest ahead of you
and read its title, description, required tools, static guide and objective
list. They are drawn greyed with a padlock, and the "Mark objective complete"
button is absent. `QuestManager` re-validates the lock on every checkmark
packet, so the button being absent is not the only thing stopping you.

Because detection reads your inventory and your lifetime vanilla statistics,
work you did before a quest unlocked counts the moment it does. Play far ahead
of the chain and it will tick through behind you, several quests at a time.
The mod is a suggested order to follow, not a gate.

Completed quests get a green tick in the list, on each finished objective, and
next to each finished phase in the Summary tab.

### Static guides only

Each quest carries a hand-written `GuideText` and an explicit `RequiredTools`
list, both hand-checked against 1.18+ world generation. Nothing is computed at
runtime. Guides are short: where to go, what tool you need, and the one thing
that will kill you if you get it wrong.

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

No waypoints or dynamic hints anywhere in the mod.

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

### Stats

A **Stats** tab reads the vanilla statistics — playtime, distance walked /
sprinted / flown, deaths, mob kills, damage taken, jumps, nights slept — plus
quests completed and quests per hour. The client only receives vanilla stats
when it asks for them, so the handful shown here rides along with the quest
sync packet.

### Celebration

Finishing a quest gives a small one: a line on the action bar and a brief puff
of sparks. Finishing a **phase** gives a title card, and the major phases
(3, 4, 5, 6) add a firework burst. Visual only.

The vanilla "Advancement Made!" pop-up is suppressed by default, toggleable in
Config. The advancement is still granted; only the toast is hidden.

---

## Custom quests

Drop JSON files in `config/foreversurvival/quests/`. They are **additive**: the
156 built-in quests stay in Java, and a JSON file adds new quests or overrides
a built-in one by reusing its id. See [example-quests/](example-quests/) for a
working file.

Task types are `item`, `craft`, `kill`, `structure` and `checkmark`. A
`structure` task takes either a `structure` id (accurate) or a `blocks` list
(scan). Overriding a **main** quest must declare its own `parent`, or the
chain loses a link. A malformed file is logged and skipped.

---

## Controls

| Key | Action |
|---|---|
| `U` | Open / close the Quest Book |
| `'` | Toggle the overlay and locator bar |

Both are rebindable under Options → Controls → ForeverSurvival.

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
challenge shows its objectives; a pinned main quest is only a line of text.

Mouse wheel scrolls the list and the detail pane independently.

---

## On-screen overlay

A small panel showing the current main quest, its phase, and each objective
with a checkbox. It never draws in the world and never reveals a locked quest.

## Locator bar

A compass-style strip showing where the other players are. Each player is a
coloured tick placed by the angle between where you are looking and where they
actually are; look at someone and their name and distance appear beneath the
bar. Players outside the bar's arc clamp to the nearest edge and draw dimmer,
so you always know which way to turn.

Only players in **your own dimension** are shown.

The vanilla client only knows about entities inside its tracking range, so the
server broadcasts every player's position 4 times a second to feed this. On a
single-player world nothing is sent.

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

Saved to `config/foreversurvival.properties`. Client-side only. Both HUD
elements hide with F1 and while the debug screen is open.

---

## How detection works

| Task type | Detection |
|---|---|
| `ItemTask` | Scans the player's inventory, armour and offhand. Progress is **sticky** — spending the items never undoes the objective. |
| `CraftTask` | Reads the vanilla `minecraft.crafted` statistic. |
| `KillTask` | Reads the vanilla `minecraft.killed` statistic, so only kills credited to you count. |
| `StructureTask` | Two modes. **Structure mode** asks the game directly whether you are inside a generated structure (`StructureManager`), checking every configured variant, so your own spruce base with a cauldron can never satisfy it. **Block mode** scans ±12 blocks horizontally / ±6 vertically for a set of blocks, used only for things the game does not model as a structure: a player-built shelter, a lit nether portal, an amethyst geode, a cave biome. |
| `CheckmarkTask` | Manual. Click the checkbox in the detail pane to tick **or un-tick** it; the server re-validates that the quest is unlocked before accepting either. Once the whole quest is complete it is final. |

Everything is evaluated on a **one-second poll** against your inventory and
lifetime statistics, so progress you made before a quest unlocked still
counts.

---

## Persistence

All state — completed quests, per-task progress, celebrated phases and the
death log — is written into the vanilla player NBT via a mixin on
`ServerPlayer#addAdditionalSaveData` / `readAdditionalSaveData`, and copied
across on respawn with `ServerPlayerEvents.COPY_FROM`.

---

## Building

`main` builds for 26.2 and wants JDK 25. The `1.18.2` branch wants JDK 17. You
do not need Gradle; the wrapper fetches it.

```bash
gradlew build
```

Output:

```
build/libs/foreversurvival-mc26.2-1.0.0.jar
build/libs/foreversurvival-mc1.18.2-1.0.0.jar
```

Ignore the `-sources` jar; the plain one goes in `mods/`.

There is one trap on the 26.2 branch. Loom 1.17 needs a JVM of 21 or newer to
run Gradle, which is separate from the JDK 25 it compiles against. With an
older `JAVA_HOME` the build fails before it reaches your code. Repoint
`JAVA_HOME`, or uncomment `org.gradle.java.home` in `gradle.properties`.

Built against Loom 1.17.20 with Microsoft OpenJDK 25.0.4 (26.2), and Loom
0.12.56 / Gradle 7.4.1 with Microsoft OpenJDK 17.0.20 (1.18.2).

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
