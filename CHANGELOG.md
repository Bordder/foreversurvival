# Changelog

One release line per Minecraft version. The builds are feature-identical; the
tag and the jar name tell you which game it targets.

---

## v1.0.0-mc26.2 — 26.2

Port of the 1.18.2 release. Same 156 quests, same balance, same everything
below. Only the game changed.

26.1 shipped unobfuscated and Yarn went away with it, so this was not a
mappings bump.

Changed:

- Toolchain: Loom 1.17, Java 25, Fabric Loader 0.19.5, Fabric API 0.160.0+26.2.
  No `mappings` line and no `modImplementation`, since nothing needs remapping
  any more.
- Yarn names to Mojang names across the whole tree. `ServerPlayerEntity` to
  `ServerPlayer`, `NbtCompound` to `CompoundTag`, `Text` to `Component`,
  `MathHelper` to `Mth`, and roughly forty others.
- The GUI is retained-mode now. `Screen.render(MatrixStack, …)` is gone;
  `extractRenderState(GuiGraphicsExtractor, …)` replaces it. Quest screen, HUD,
  locator bar and layout editor all had their draw calls rebuilt. Key handling
  moved to `keyPressed(KeyEvent)`.
- Item NBT to the components system.
- Structure detection rewritten. `StructureFeature` and
  `ConfiguredStructureFeature` were deleted and folded into `Structure`;
  `StructureAccessor` is now `StructureManager`.
- Mixin targets.
- Jar names are `foreversurvival-mc<mcversion>-<modversion>.jar`, so a 1.18.2
  build and a 26.2 build stop overwriting each other in `build/libs`.

Fixed:

- The quest screen drew its own background on top of the one 26.2 already
  draws, which came out muddy.
- The first build compiled and then crashed on launch. Mixin targets had moved.

Two mappings that are not what you would guess, and cost the most time:
`Identifier` kept its name and only moved package, to
`net.minecraft.resources`. `GuiGraphics` is not in 26.2 at all. The rest of the
verified table is in [PORTING-26.2.md](PORTING-26.2.md).

Not verified: the guides have not been played through on 26.2. Ore heights are
still correct, since those bands have not moved since 1.18, but nothing else
world-generation specific has been re-checked.

---

## v1.0.0-mc1.18.2 — 1.18.2

First release. Fabric API is the only dependency.

Quests:

- 126 main quests, 6 phases, one strict linear chain. Parent is whatever was
  declared before it, phase boundaries included.
- 30 side challenges. Off the chain, never block it.
- 455 objectives.
- No rewards. None.
- Locked quests are readable, not completable. Server re-checks the lock on
  every checkmark packet.
- Counts sized to what the next step eats, plus a buffer. Many small
  objectives, not a few huge ones.
- Static hand-written guides. Required-tools list per quest.

Detection:

- Task types: `item`, `craft`, `kill`, `structure`, `enchant`, `position`,
  `stat`, `use`, `checkmark`.
- Sticky item progress. Spend the iron, keep the tick.
- Structure tasks ask the game, not the blocks. Your spruce hut with a cauldron
  in it is not a Swamp Hut. Block scanning is the fallback for the things
  1.18.2 does not model as a structure at all: lit portal, geode, cave biome.
- One-second poll. No event hooks. Progress made before a quest unlocked
  counts.
- 28 checkmarks became real detection during development.

Interface:

- Quest Book on `U`. HUD toggle on `'`. Rebindable.
- Tabs: Main, Side, Deaths, Stats, Summary, Config.
- Search over title, description, guide text.
- List layout: quest list, detail pane.
- Tree layout: one phase per page, snake grid of icon nodes, connector line
  going green behind what you finished.
- Collapsible phase headers.
- Draggable scrollbars.
- Overlay with the current quest, its objectives, and one pinned quest of your
  choice. Panel only. It never draws in the world.
- Locator bar. Compass strip, other players by bearing, own dimension only.
  Single-player broadcasts nothing.
- Layout editor. Both HUD elements drag and resize on their own. Positions are
  a fraction of the screen, so changing resolution does not wreck them.
- Durability on item tooltips.

Logging:

- Deaths: coordinates, dimension, vanilla death message, inventory snapshot,
  into player NBT. Last 200. Snapshots on the last 25.
- Stats tab: vanilla statistics, quests completed, quests per hour, time per
  quest.
- Combat log by mob type.

Other:

- JSON in `config/foreversurvival/quests/` adds quests, or overrides a built-in
  one by id. Broken files get logged and skipped. Example in
  [example-quests/](example-quests/).
- State lives in vanilla player NBT, via a mixin on
  `ServerPlayerEntity#writeCustomDataToNbt` / `readCustomDataFromNbt`. Copied
  on respawn with `ServerPlayerEvents.COPY_FROM`.
