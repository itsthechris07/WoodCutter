---
title: Getting started
---

# Getting started

1. Put `WoodCutter-<version>.jar` into the `plugins` folder of the server and restart it.
2. The files `config.yml`, `messages.yml` and later `levels.yml` are created in `plugins/WoodCutter`.
3. Get an axe with the enchantment, either at the enchanting table (an offer of level 30, 40 % chance by default) or
   from an admin with `/woodcutter give <player> axe`.
4. Sneak and break the lowest log of a tree. The whole tree is cut.

## Getting the enchantment

- **Enchanting table:** axes and books get WoodCutter I (60 %), II (30 %) or III (10 %).
- **Anvil:** two items of the same level combine to the next level. A book puts the enchantment onto an axe.
- **Grindstone:** removes the enchantment.
- **Librarians, wandering traders and loot chests** can have books with WoodCutter (`sources` in the config).
- **Goodie:** with `goodie: everyone` or `goodie: permission` players cut trees with every axe, without the
  enchantment.

## What is cut

- All logs of Minecraft, including cherry, mangrove and the huge fungi of the Nether. Huge mushrooms are cut as well
  (`mushrooms`).
- Logs placed by players are never cut, so buildings next to trees stay (`destroy-fake-trees`).
- Logs without leaves are no tree (`require-leaves`). This also keeps buildings that are older than the plugin.
- Logs of `excluded-logs` are never cut, e.g. `stripped_*`.
- The amount of logs is limited: 120 with WoodCutter I, 60 more with each level (`maxblocks`, `levels`).

## Animations

- **layers:** the tree is cut log by log from the bottom to the top.
- **topple:** the tree tips over as a whole, away from the player, with its leaves. The items drop where the tree
  hits the ground, and mobs in the way are pushed away. Settings: `animation.topple`.

Without an animation (`animation.enabled: false`), the tree disappears at once.
