# Changelog

Two lines of releases, one per Minecraft version. They are feature-identical;
the version in the tag and the jar name tells you which game it is for.

---

## v1.0.0-mc26.2 — 26.2

The port. No new features, no balance changes, no new quests: if you have been
playing on 1.18.2, this is the same 156 quests on a newer game. Everything in
the 1.18.2 notes below still applies.

26.1 dropped obfuscation and Yarn along with it, so this was not a mappings
bump. It is a rename of nearly every import in the tree plus a rewrite of the
client render path.

**What actually changed**

- Toolchain moved to Loom 1.17 with no `mappings` line and plain
  `implementation` instead of `modImplementation`, because there is nothing
  left to remap. Java 25, Fabric Loader 0.19.5, Fabric API 0.160.0+26.2.
- Every Yarn name became its Mojang equivalent — `ServerPlayerEntity` is
  `ServerPlayer`, `NbtCompound` is `CompoundTag`, `Text` is `Component`,
  `MathHelper` is `Mth`, and so on for about forty more.
- The GUI is no longer immediate-mode. `Screen.render(MatrixStack, …)` is gone
  and the replacement is `extractRenderState(GuiGraphicsExtractor, …)`, an
  extract-then-render model, so the quest screen, HUD, locator bar and layout
  editor all had their draw calls rebuilt rather than renamed. Key handling
  moved to `keyPressed(KeyEvent)` at the same time.
- Item NBT is gone; stack data now goes through the components system.
- `StructureFeature` and `ConfiguredStructureFeature` were deleted and folded
  into `Structure`, and `StructureAccessor` became `StructureManager`, so
  structure detection was rewritten against the new lookup.
- Mixin targets were corrected — the first build compiled fine and then failed
  to launch, which is the usual way you find out a mixin target moved.
- The screen no longer paints its own background. 26.2 draws one already, and
  doing both left the quest book looking muddy.
- Jars are named `foreversurvival-mc<version>-<mod version>.jar` so a 1.18.2
  build and a 26.2 build can share a `build/libs` without overwriting.

Two things that look like they should have changed and did not: `Identifier`
kept its name (only the package moved, to `net.minecraft.resources`), and
`GuiGraphics` does not exist in 26.2 at all. Both cost time to find out. The
full verified mapping table is in [PORTING-26.2.md](PORTING-26.2.md).

**Known limits**

The quest guides still quote 1.18-era ore heights. Those bands have not moved
since 1.18, so the numbers are correct, but anything else world-generation
specific in a guide has not been re-audited against 26.2 by playing through it.

---

## v1.0.0-mc1.18.2 — 1.18.2

First release. A guided survival campaign that depends on Fabric API and
nothing else.

**The campaign**

- 126 main quests across 6 phases, chained strictly linear — each quest's
  parent is the one declared immediately before it, across phase boundaries
  too. 30 optional side challenges sit outside the chain and never block it.
  455 objectives in total.
- No rewards. No items, no XP, no trophies. The mod tells you what to do next
  and vanilla gives you everything else.
- Locked quests are fully previewable and never completable. The server
  re-validates the lock on every checkmark packet, so reading ahead cannot turn
  into progress and a forged packet cannot skip the chain.
- Objective counts are sized to what the next step actually consumes plus a
  small buffer. Length comes from the number of distinct objectives, not from
  inflated stack counts.
- Hand-written static guides and an explicit required-tools list per quest.
  No waypoints, no particle markers, no compass pointers, no runtime hints.

**Task detection**

- `item`, `craft`, `kill`, `structure`, `enchant`, `position`, `stat`, `use`
  and manual `checkmark`. Item progress is sticky, so spending what you
  gathered does not undo the objective.
- Structure detection asks the game whether you are standing in a generated
  structure rather than sniffing blocks, so your own spruce hut with a cauldron
  in it is not a Swamp Hut. Block scanning is only used for things the game
  does not model as a structure at all — a lit portal, a geode, a cave biome.
- Everything runs on a one-second poll instead of a pile of event hooks, which
  means progress you made before a quest unlocked still counts.
- 28 objectives that started life as manual checkmarks were converted to real
  detection during development.

**Interface**

- Quest Book on `U`, HUD toggle on `'`, both rebindable, both on keys vanilla
  leaves free.
- Six tabs: Main, Side, Deaths, Stats, Summary, Config. Search filters on
  title, description and guide text.
- Two layouts. List is a scrolling quest list with a detail pane. Tree draws a
  whole phase at once as a snake grid of icon nodes, with the connector line
  turning green behind what you have finished. Phase headers collapse.
- On-screen overlay showing the current quest and its objectives, plus any one
  quest you pin. It is a UI panel, never a world marker.
- Locator bar: a compass strip showing where other players are, by bearing,
  same dimension only. On single-player nothing is broadcast at all.
- Both HUD elements can be dragged and independently resized in a layout
  editor, and positions are stored as a fraction of the screen so a layout
  arranged at one resolution survives a resolution change. Everything is
  client-side in `config/foreversurvival.properties`.
- Item tooltips gained a durability line.

**Logging**

- Every death records coordinates, dimension, the vanilla death message and an
  inventory snapshot into player NBT. The last 200 deaths are kept;
  the most recent 25 keep their snapshot so a months-long world does not bloat
  the player file.
- Stats tab reads vanilla statistics, plus quests completed, quests per hour
  and time-per-quest.
- Combat log breaks damage dealt down by mob type.

**Extending it**

- Drop JSON in `config/foreversurvival/quests/` to add quests or override a
  built-in one by reusing its id. Malformed files are logged and skipped, never
  fatal. There is a working example in [example-quests/](example-quests/).

**Persistence**

- All state lives in vanilla player NBT via a mixin on
  `ServerPlayerEntity#writeCustomDataToNbt` / `readCustomDataFromNbt`, and is
  carried across respawn with `ServerPlayerEvents.COPY_FROM`.
