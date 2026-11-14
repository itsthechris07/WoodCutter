---
title: Commands & permissions
---

# Commands & permissions

## Commands

| Command | Description | Permission |
|---|---|---|
| `/woodcutter menu` | Menu: switch WoodCutter on and off, level, perks and the best players. Admins change settings there | – |
| `/woodcutter toggle` | Switch WoodCutter on or off for yourself | `woodcutter.use` |
| `/woodcutter level [player]` | Woodcutting level, cut trees and perks | – |
| `/woodcutter top` | The best woodcutters | – |
| `/woodcutter give <player> [book\|axe] [level]` | Give an enchanted book or axe, also from the console | `woodcutter.admin` |
| `/woodcutter enchant [level]` | Enchant the axe in the main hand | `woodcutter.admin` |
| `/woodcutter reload` | Reload the files | `woodcutter.admin` |

Aliases: `/wcutter`, `/woodc`

## Permissions

The defaults work without a permission plugin: everyone can use WoodCutter and operators manage it.

| Permission | Description | Default |
|---|---|---|
| `woodcutter.use` | Get the enchantment at the enchanting table and cut trees with it | everyone |
| `woodcutter.goodie` | Cut trees with every axe (with `goodie: permission`) | – |
| `woodcutter.bypass.delay` | No delay between two trees | – |
| `woodcutter.delay.<seconds>` | A shorter delay, e.g. `woodcutter.delay.10` for a rank | – |
| `woodcutter.maxblocks.<amount>` | A higher limit of logs, e.g. `woodcutter.maxblocks.300` (at most 2000) | – |
| `woodcutter.admin` | Reload, enchant, give and the settings in the menu | operators |
