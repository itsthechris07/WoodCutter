---
title: Woodcutting skill
---

# Woodcutting skill

Players gain experience for every log of a tree they cut and reach levels. The experience, the cut trees and the
names of all players are kept in `plugins/WoodCutter/levels.yml`, so the ranking contains offline players as well.

| Setting | Default | Description |
|---|---|---|
| `skill.enabled` | `true` | `false` switches the skill off (trees are still counted) |
| `skill.max-level` | `50` | Highest level, at most 1000 |
| `skill.xp-per-log` | `1` | Experience for every log of a tree |
| `skill.xp-first-level` | `120` | Experience for the first level |
| `skill.xp-increase` | `40` | Every further level needs this much more than the one before |

## Perks

The perks grow evenly with every level, up to their value at the highest level. `0` disables a perk. Perks that
change nothing with the settings of the server are not shown to the players, e.g. `delay` while `delay.enabled` is
`false`, `speed` without the animation `layers` and `costs` while all costs are `0`.

| Perk | Default at the highest level | Description |
|---|---|---|
| `speed` | `4` | Additional logs per tick of the animation `layers` |
| `delay` | `20` | Seconds the delay between two trees is shorter |
| `max-blocks` | `120` | Additional logs on top of `maxblocks` |
| `bonus-drops` | `20` | Chance in percent on top of `bonus-drops.chance` |
| `axe-saver` | `30` | Chance in percent that a log does not damage the axe |
| `costs` | `50` | Percent the costs of a tree are lower |
| `forester-luck` | `3` | Chance in percent that a leaf drops one of the `lucky-drops` |

`skill.lucky-drops` is a list of `item:weight`, items with a higher weight drop more often. `sapling` is the sapling
of the cut tree.

## Unlocks

Levels that unlock features for a player, even if they are disabled for everyone (`0`: never).

| Unlock | Default | Feature |
|---|---|---|
| `skill.unlocks.replant` | `10` | A new sapling is planted for free |
| `skill.unlocks.drops-to-inventory` | `20` | Logs go into the inventory |

## Rewards

Commands the server runs when a player reaches a level, as `level: command`. `%player%` is the name of the player,
`%uuid%` their id and `%level%` the level.

```yaml
    rewards:
      - '10: give %player% golden_apple 3'
      - '25: eco give %player% 500'
      - '50: lp user %player% parent add forester'
```

A player who reaches several levels with one big tree gets the rewards of all of them.
