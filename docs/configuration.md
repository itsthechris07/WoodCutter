---
title: Configuration
---

# Configuration

All settings are in `plugins/WoodCutter/config.yml`, under `config:`. After an update, new settings are added with
their comments and your values stay. `/woodcutter reload` loads the file again, and admins can switch many settings
in `/woodcutter menu`.

## General

| Setting | Default | Description |
|---|---|---|
| `language` | `en` | Language of the texts: `en`, `de`, `fr`, `es`, `pl` or `zh` |
| `disabled-worlds` | `Lobby`, … | Worlds without WoodCutter. `*` stands for any characters, so `sg_*` matches `sg_1` |
| `require-sneaking` | `true` | Players have to sneak to cut a whole tree |
| `goodie` | `none` | `none`: the enchantment is needed. `permission`: players with `woodcutter.goodie` cut with every axe. `everyone`: all players do |

## Enchantment

| Setting | Default | Description |
|---|---|---|
| `level` | `30` | Level of the enchanting table offer that can give the enchantment |
| `chance` | `40` | Chance in percent to get it with such an offer |
| `maxblocks` | `120` | Logs WoodCutter I cuts at once. Ranks can get more with `woodcutter.maxblocks.<amount>` |
| `levels.max` | `3` | Highest level of the enchantment, `1` disables the levels |
| `levels.additional-blocks` | `60` | Logs every level adds to `maxblocks` |
| `sources.librarian` | `3` | Chance in percent that a new librarian trade is a book with WoodCutter |
| `sources.wandering-trader` | `3` | The same for wandering traders |
| `sources.price` | `20` | Emeralds a book costs per level |
| `sources.loot` | `3` | Chance in percent that a loot chest contains such a book |

## Trees

| Setting | Default | Description |
|---|---|---|
| `destroy-fake-trees` | `false` | `false`: logs placed by players are never cut |
| `require-leaves` | `true` | Logs without leaves (or wart blocks in the Nether) are no tree |
| `mushrooms` | `true` | Huge mushrooms are cut when their stem is broken with an axe |
| `excluded-logs` | `[]` | Logs that are never cut, by name or pattern, e.g. `stripped_*` or `cherry_log` |
| `fast-leaf-decay` | `true` | The leaves of a cut tree decay within seconds |
| `replant.enabled` | `true` | A new sapling is planted, four of them for trees with a 2x2 trunk |
| `replant.free` | `false` | `false`: the sapling is taken from the inventory of the player |
| `protect-axe` | `true` | Cutting stops before the hit that would break the axe |

## Animation

| Setting | Default | Description |
|---|---|---|
| `animation.enabled` | `true` | The tree does not just disappear |
| `animation.mode` | `layers` | `layers`: log by log from the bottom. `topple`: the tree tips over away from the player |
| `animation.blocks-per-tick` | `2` | Logs per tick with `layers` (20 ticks are a second) |
| `animation.topple.drops-where-it-lands` | `true` | The items drop where the tree hits the ground (not with `drops-to-inventory`) |
| `animation.topple.knockback` | `true` | Mobs and players the tree falls on are pushed away |
| `animation.topple.damage` | `0` | Damage the tree does to them (2 = a heart), as the player who cut it, so PvP and protection plugins can deny it |
| `animation.topple.timber` | `false` | Shows "Timber!" (message `timber`) as a subtitle to the player who cut the tree |

## Drops

| Setting | Default | Description |
|---|---|---|
| `drops-to-inventory` | `false` | Logs go into the inventory of the player, what does not fit is dropped |
| `bonus-drops.chance` | `0` | Chance in percent that a log drops a second time |
| `experience-drops.per-log` | `0.25` | Experience points every log drops |

## Delay and costs

| Setting | Default | Description |
|---|---|---|
| `delay.enabled` | `true` | Players wait between two trees |
| `delay.time` | `30` | Seconds. `woodcutter.delay.<seconds>` gives a rank a shorter delay, `woodcutter.bypass.delay` none |
| `delay.message` | … | Shown over the action bar, `''` disables it |
| `costs.hunger` | `0` | Food points a tree costs (10 fill the whole bar) |
| `costs.experience` | `0` | Experience levels a tree costs |
| `costs.money` | `0` | Money a tree costs, needs Vault and an economy plugin |
| `economy.reward-per-log` | `0` | Money players get for every log of a tree, not in creative mode |

Players in creative mode pay nothing. The skill perk `costs` makes all costs lower.

## Woodcutting skill

See [Woodcutting skill](skill.md) for `skill.*`.
