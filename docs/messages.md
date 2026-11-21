# Messages & languages

All texts players see are in `plugins/WoodCutter/messages.yml`, or `messages_<language>.yml` for another language.
New texts are added after an update, your texts stay. An empty text (`''`) is not sent.

## Languages

`language` in the `config.yml`: `en` (English), `de` (German), `fr` (French), `es` (Spanish), `pl` (Polish), `zh`
(Chinese), `ru` (Russian), `uk` (Ukrainian), `cs` (Czech) or `pt` (Portuguese). Improvements to the
translations are welcome as an
[issue](https://github.com/itsthechris07/WoodCutter/issues) or a pull request.

## Colors

- **Color codes:** `&a`, `&l`, … and hex colors like `&#ff8800`.
- **MiniMessage:** `<gold>`, `<bold>`, `<gradient:#5e9c3a:#c9a46b>Timber!</gradient>`, … A text with MiniMessage
  tags must not contain `&` codes, so that texts like `<player>` in the help stay as they are.

`%prefix%` is replaced with the text of `prefix`.

## Texts worth changing

| Text | Shown |
|---|---|
| `timber` | As a subtitle when a tree tips over (animation `topple`, with `animation.topple.timber: true`) |
| `economy_reward` | In the chat when a player gets money for a tree, empty by default, e.g. `'%prefix%&a+%money% &7for %logs% logs'` |
| `skill_xp` | Over the action bar after every tree |
| `skill_level_up_title` | As a title when a player reaches a level |
| `sneak_hint` | Once over the action bar, if a player breaks a log without sneaking |
