---
title: Integrations
---

# Integrations

All of these plugins are optional. WoodCutter works without them.

## Protection, logging and skill plugins

Every log of a tree is broken as the player, like a normal block break. So other plugins handle each log on its
own:

- **Protection plugins** (WorldGuard, GriefPrevention, Lands, Towny, …) can deny single logs.
- **Logging plugins** like CoreProtect record every log, and rollbacks restore the tree.
- **Jobs Reborn, mcMMO, AuraSkills** and similar plugins give their money and experience for every log.

Saplings are planted as the player too, so protection plugins can deny them and logging plugins record them.

## WorldGuard

The region flag `woodcutter` is allowed by default. To switch WoodCutter off in a region (players break single logs
there):

```
/rg flag <region> woodcutter deny
```

Or list the regions in the `config.yml` – ids with `*` for any characters, `__global__` for everywhere outside of
regions:

```yaml
worldguard-regions:
  mode: blacklist   # whitelist: WoodCutter only works in the listed regions
  list:
    - spawn
    - plot_*
```

## Custom items (ItemsAdder, Nexo, Oraxen, MMOItems)

Items of these plugins become axes for WoodCutter with `custom-axes` – they cut trees and get the enchantment at
the enchanting table and the anvil:

```yaml
custom-axes:
  items:
    - itemsadder:mypack:ruby_axe
    - nexo:onyx_axe
    - oraxen:obsidian_axe
    - mmoitems:AXE:LUMBER_AXE
  only: false   # true: only these items are axes, not the axes of Minecraft
```

No plugin is required: WoodCutter asks the ones that are installed. Blocks of mods on hybrid servers are cut with
`extra-logs` and `extra-leaves`, their saplings are set with `saplings`.

## Vault (economy)

With Vault and an economy plugin (EssentialsX, CMI, …), trees can earn and cost money:

```yaml
  costs:
    money: 10
  economy:
    reward-per-log: 0.5
```

Players in creative mode pay nothing and get nothing. The skill perk `costs` makes trees cheaper.

## PlaceholderAPI

See [Placeholders](placeholders.md).

## Developer API

`com.christian34.woodcutter.events.WoodCutterEvent` is called before a player cuts a tree. Cancel it to let the player
break only the single block.

```java
@EventHandler
public void onWoodCutter(WoodCutterEvent event) {
    if (event.getBlock().getWorld().getName().equals("spawn")) event.setCancelled(true);
}
```

While a tree is cut, the logs are broken with `BlockBreakEvent`s of the player.
