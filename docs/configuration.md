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
| `extra-logs` | `[]` | Blocks that are cut like logs, e.g. the logs of mods on hybrid servers (`*` stands for any characters) |
| `extra-leaves` | `[]` | Blocks that are removed with the leaves of a cut tree, e.g. `nether_wart_block` |
| `attached-blocks` | `[cocoa, vine, shelf_mushroom]` | Blocks on the logs that are removed with the tree, their items drop (and the vines below them) |
| `saplings` | `{}` | Saplings of logs that have none (or another one), as `log: sapling` |
| `custom-axes.items` | `[]` | Items of other plugins that are axes: `itemsadder:<namespace>:<id>`, `nexo:<id>`, `oraxen:<id>`, `mmoitems:<type>:<id>` – or items of Minecraft, e.g. `mace` |
| `custom-axes.only` | `false` | `true`: only these items are axes, not the axes of Minecraft |
| `bees.angry` | `true` | The bees of a nest in the tree fly out and attack the player who cut it |
| `bees.drop-nest` | `false` | The nest drops as an item (without its bees) |
| `worldguard-regions.mode` | `blacklist` | `blacklist`: WoodCutter is disabled in the listed regions. `whitelist`: only enabled in them |
| `worldguard-regions.list` | `[]` | Region ids, `*` stands for any characters, `__global__` is everywhere outside of regions |
| `fast-leaf-decay` | `true` | The leaves of a cut tree decay within seconds |
| `replant.enabled` | `true` | A new sapling is planted, four of them for trees with a 2x2 trunk – next to the stump if something is on it |
| `replant.free` | `false` | `false`: the sapling is taken from the inventory of the player |
| `replant.protect` | `false` | Players can't break the planted saplings until they grow (except with `woodcutter.replant.break`) |
| `protect-axe` | `true` | Cutting stops before the hit that would break the axe |
| `chopping.slowdown-per-log` | `0` | Percent every log of the tree makes breaking the log slower, e.g. `5`: a tree of 20 logs takes twice as long (`0` disables it) |
| `chopping.max-factor` | `6` | The most breaking the log can take, in times as long as a single log |
| `chopping.progress` | `bossbar` | How the progress is shown: `bossbar`, `actionbar` or `none` |

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
| `animation.topple.physics` | `rigid` | `rigid`: the tree falls in one piece, bounces and comes to rest on what is below it – walls and other trees stop it. `fall-apart`: it breaks into its blocks when it hits the ground. `simple`: the 90° fall of older versions |
| `animation.topple.direction` | `away` | `away` from the player, where the player is `facing`, down the `slope` (away from the player on even ground) or `random` |
| `animation.topple.float-on-water` | `true` | A tree that falls into water floats for a few seconds |
| `animation.topple.update-interval` | `2` | Ticks between two positions sent to the players (1–10) – higher values save bandwidth, the players' clients smooth the movement |
| `animation.topple.glow` | `''` | The falling tree glows in a color, e.g. `'#ff8800'` (`''` for none) |

### Effects

| Setting | Default | Description |
|---|---|---|
| `effects.sounds.chop` | `block.wood.break` | Played to the player for every log of the animation `layers` |
| `effects.sounds.creak` | `block.wooden_door.open`, `block.wood.break` | When a tree starts to tip over |
| `effects.sounds.impact` | `block.wood.break`, `entity.generic.big_fall` | When it hits the ground |
| `effects.sounds.splash` | `entity.generic.splash` | When it falls into water |
| `effects.sounds.leaves` | `block.grass.break` | When leaves decay |
| `effects.particles.falling-leaves` | `true` | Leaves fall from the crown of a tipping tree |
| `effects.particles.impact` | `true` | Dust of the logs and the ground where the tree lands |
| `effects.particles.leaves` | `true` | Particles of decaying leaves |

Every sound is a list of `'sound volume pitch'`, e.g. `'minecraft:block.wood.break 1 0.7'` – sounds of resource packs
work as well, and `[]` plays nothing.

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
