---
title: FAQ
---

# FAQ

**Nothing happens when I break a log.**
Sneak while you break it (`require-sneaking`), and use an axe with the enchantment, or enable the `goodie`. Check
`disabled-worlds`, the WorldGuard flag, and whether you have switched it off with `/woodcutter toggle`. A delay
between two trees is shown over the action bar.

**Only some logs are cut.**
The limit of logs is reached (`maxblocks`, levels, `woodcutter.maxblocks.<amount>`), the axe is almost broken
(`protect-axe`) or a protection plugin denies some logs.

**My log cabin is not cut. Good, but why?**
Logs placed by players are remembered and never cut (`destroy-fake-trees: false`). Logs without leaves are no tree
(`require-leaves: true`).

**Does it work on Spigot?**
No. WoodCutter needs Paper or Folia 1.21 or newer.

**Does it work with Folia?**
Yes, with the same jar.

**How do I translate the texts?**
Set `language` and change the texts in `messages_<language>.yml`. See [Messages & languages](messages.md).

**Where are the levels saved?**
In `plugins/WoodCutter/levels.yml`. It is written every 5 minutes and when the server stops.

**Can I see statistics of all servers?**
Yes, on [bStats](https://bstats.org/plugin/bukkit/WoodCutter/3789). The statistics are anonymous and can be
disabled in `plugins/bStats/config.yml`.
