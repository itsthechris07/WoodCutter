# Branding

Images for the plugin pages on SpigotMC, Modrinth and Hangar. The texts for these pages (summary, tags, changelog) are in
[listing.md](listing.md).

| File | Size | Use |
|---|---|---|
| `icon.png` | 512×512 | Project icon (Modrinth, Hangar, SpigotMC) |
| `banner.png` | 1600×500 | Header at the top of the description |
| `how-it-works.png` | 1600×900 | Gallery / description: the steps of cutting a tree + the levels of the enchantment |
| `features.png` | 1600×900 | Gallery / description: feature overview |
| `skill.png` | 1600×900 | Gallery / description: the woodcutting skill (experience, ranking, perks) |

## Editing

The sources are in `design/` (one `.dc.html` per image, `canvas.json` holds the sizes). They are plain HTML with
inline styles in the same format as CatchTheBeacon and SurvivalGames (fonts: Chakra Petch, Barlow and Pixelify Sans
for the item tooltip and the chat texts from Google Fonts); the pixel art (axe, log, tree) is inline SVG.

After a change, render the PNGs again (needs Chrome or Edge):

```powershell
.\branding\render.ps1
```

Colors: background `#0F1410`, panels `#171E18`, borders `#2B382D`, leaf green `#6CC24A`, amber `#F5B83D`,
enchantment violet `#9B6BFF`, diamond `#4FD6C8`, blue `#4B95FF`, red `#E5484D`.

The numbers in the images are the defaults of `config.yml` (30 levels, 40 % chance, 120 / 180 / 240 logs, 50 skill levels and the values of the perks) – change
them here as well if the defaults change.
