# Placeholders

With [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) installed, these placeholders can be used in
scoreboards, tab lists, holograms and other plugins.

## Of the player

| Placeholder | Value |
|---|---|
| `%woodcutter_enabled%` | `false` if the player has switched WoodCutter off |
| `%woodcutter_level%` | Level of the enchantment of the item in the main hand, `0` without it |
| `%woodcutter_max_blocks%` | Logs the player cuts at once with the item in the main hand |
| `%woodcutter_delay%` | Seconds until the next tree, `0` if they can cut one right away |
| `%woodcutter_skill_level%` | Woodcutting level |
| `%woodcutter_skill_xp%` | Woodcutting experience |
| `%woodcutter_skill_xp_needed%` | Experience missing for the next level, `0` at the highest level |
| `%woodcutter_trees%` | Trees the player has cut |

## Ranking

These need no player, e.g. for a hologram of the best woodcutters. `<rank>` is `1` for the best player.

| Placeholder | Value |
|---|---|
| `%woodcutter_top_<rank>_name%` | Name of the player |
| `%woodcutter_top_<rank>_level%` | Their woodcutting level |
| `%woodcutter_top_<rank>_xp%` | Their experience |
| `%woodcutter_top_<rank>_trees%` | Their cut trees |

Example for DecentHolograms:

```
/dh create woodcutters &9The best woodcutters
/dh line add woodcutters 1 &71. &f%woodcutter_top_1_name% &8| &7Level &9%woodcutter_top_1_level%
/dh line add woodcutters 1 &72. &f%woodcutter_top_2_name% &8| &7Level &9%woodcutter_top_2_level%
```
