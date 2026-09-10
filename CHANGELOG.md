# Changelog

One release line per Minecraft version. The tag and the jar name say which
game a build targets. This branch is 1.18.2; the 26.2 notes are on `main`.

---

## v1.0.0-mc1.18.2 — 1.18.2

First release. Fabric API is the only dependency.

Quests:

- 126 main quests, 6 phases, unlocking in a fixed order. Parent is whatever
  was declared before it, phase boundaries included.
- 30 side challenges, off the main chain.
- 455 objectives.
- No rewards.
- Locked quests are readable, not completable. Server re-checks the lock on
  every checkmark packet. This gates the quest list only; nothing stops you
  doing any of it early, and the chain ticks through behind you when it does
  unlock.
- Counts sized to what the next step eats, plus a buffer.
- Static hand-written guides. Required-tools list per quest.

Detection:

- Task types: `item`, `craft`, `kill`, `structure`, `enchant`, `position`,
  `stat`, `use`, `checkmark`.
- Sticky item progress. Spend the iron, keep the tick.
- Structure tasks ask the game, not the blocks. Your spruce hut with a cauldron
  in it is not a Swamp Hut. Block scanning is the fallback for the things
  1.18.2 does not model as a structure at all: lit portal, geode, cave biome.
- One-second poll, not event hooks. Progress made before a quest unlocked
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
  choice. Never draws in the world.
- Locator bar. Compass strip, other players by bearing, own dimension only.
  Single-player broadcasts nothing.
- Layout editor. Both HUD elements drag and resize on their own. Positions are
  a fraction of the screen, so a resolution change keeps the layout.
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
