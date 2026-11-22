# Commands & permissions

## Commands

| Command | Description | Permission |
|---|---|---|
| `/woodcutter menu` | Menu: switch WoodCutter on and off, level, perks and the best players. Admins change settings there | none |
| `/woodcutter toggle` | Switch WoodCutter on or off for yourself | `woodcutter.use` |
| `/woodcutter level [player]` | Woodcutting level, cut trees and perks | none |
| `/woodcutter top` | The best woodcutters | none |
| `/woodcutter give <player> [book\|axe] [level]` | Give an enchanted book or axe, also from the console | `woodcutter.admin` |
| `/woodcutter enchant [level]` | Enchant the axe in the main hand | `woodcutter.admin` |
| `/woodcutter reload` | Reload the files | `woodcutter.admin` |

Aliases: `/wcutter`, `/woodc`

## Permissions

The defaults work without a permission plugin: everyone can use WoodCutter and operators manage it.

| Permission | Description | Default |
|---|---|---|
| `woodcutter.use` | Get the enchantment at the enchanting table and cut trees with it | everyone |
| `woodcutter.goodie` | Cut trees with every axe (with `goodie: permission`) | nobody |
| `woodcutter.bypass.delay` | No delay between two trees | nobody |
| `woodcutter.delay.<seconds>` | A shorter delay, e.g. `woodcutter.delay.10` for a rank | nobody |
| `woodcutter.maxblocks.<amount>` | A higher limit of logs, e.g. `woodcutter.maxblocks.300` (at most 2000) | nobody |
| `woodcutter.replant.break` | Break saplings WoodCutter has planted (with `replant.protect: true`) | operators |
| `woodcutter.admin` | Reload, enchant, give and the settings in the menu | operators |
