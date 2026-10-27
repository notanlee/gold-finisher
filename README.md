# Gold Finisher

A client-side Fabric mod for Minecraft **1.21.11** that plays Valorant-style finishers when a player kills something. The choreography and timing of each finisher were rebuilt frame by frame from the Valorant originals, using the Minecraft body model and particles.

- **Champions 2023:** a gold glyph opens on the ground. The body turns into an ore statue and floats in a beam of light, then rises up and bursts into light.
- **Champions 2022:** a white flash, then a dark dome with a lightning web forms over the body. A gold streak drives down into it with lightning, and it shatters into gold shards.
- **Reaver:** a void portal opens under the body, shadow claws sweep up out of it, and a violet energy pillar erupts and swallows the body before the portal closes.
- **Aemondir:** light streaks fall from the sky and red crystal spikes burst out of the ground around the body. A huge gold explosion follows, and a giant glowing sword drawn in gold particles is left planted where the body was, with a red pennant and embers.

Champions plays a note-block **beatbox** jingle that builds to a chime run and a bell hit. Each death also pops a totem, and totem particles keep rising for the whole finisher. The body stays where it died (no knockback from the last hit). Dead players are replaced by a client-side stand-in, so the finisher plays in full even on servers that respawn instantly.

Only you need the mod. It works on any server (vanilla included) and inside **Flashback / ReplayMod** replays and exported videos.

## Download

Get the latest `goldfinisher-x.y.z.jar` from the [Releases](../../releases) page and put it in `.minecraft/mods` together with Fabric API.

## Mods

| Mod | Needed? | Why |
|---|---|---|
| [Fabric Loader](https://fabricmc.net/use/) 0.20.1+ | Required | Mod loader |
| [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11 | Required | Keybind, tick and render hooks |
| [Mod Menu](https://modrinth.com/mod/modmenu) | Optional | Config button in the mod list (otherwise use **G**) |
| [Entity Model Features](https://modrinth.com/mod/entity-model-features) + [Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures) | Optional | EMF animates the finisher poses and gives resource packs [animation variables](docs/CUSTOMIZING.md#2-emf-animations-resource-packs). Without it, the built-in poses are used. |
| [Flashback](https://modrinth.com/mod/flashback) / [ReplayMod](https://modrinth.com/mod/replaymod) | Optional | Finishers play back in replays. Set Trigger to "Any player kill" or "All deaths" there. |

## Settings

Press **G** in game (rebindable under Controls → Gold Finisher) or use Mod Menu → Config. The menu has three tabs:

**General:** Enabled, Finisher (Champions / Reaver / Aemondir), Trigger (My kills / Any player kill / All deaths), Animation speed, Preview on me.

**Finisher** (shows only the options of the finisher you picked):

| Finisher | Options |
|---|---|
| Champions | Version (2022 / 2023), Duration, Color (Gold, Diamond, Emerald, Amethyst, Redstone with the real block texture; Rainbow; Custom RGB; Random ore), Style (Solid statue / Color tint), Rise height, Horizontal angle (−180°…180°), Vertical angle (−60°…60°) |
| Reaver | Duration, Reaver color (Void purple / Blood red / Abyss blue / Custom RGB), Style, Sink depth, Sink start, Shiver, Horizontal / Vertical angle, Portal, Claws & souls, Energy pillar |
| Aemondir | Duration, Style, Sword scale, Crystals, Explosion |

**Effects & Sound:** Particles (Low / Medium / High), Sounds, Totem sound, Totem particles, Glow (Champions), Light beam (Champions).

Settings are saved in `config/goldfinisher.json`.

## Customizing (resource packs and developers)

See **[docs/CUSTOMIZING.md](docs/CUSTOMIZING.md)** for:
- every config field;
- the EMF variables (`goldfinisher_progress`, `goldfinisher_type`, `goldfinisher_variant`, `goldfinisher_active`, `goldfinisher_speed`) and the shipped `.jem` pose files;
- each finisher's timeline;
- where poses, particles and sounds live in the code, and how to add a finisher.

## Known limits
- Minecraft can't reproduce Valorant's shaders or meshes. Finishers match the original choreography, timing and colors using the vanilla body model and particles.
- Armor follows the pose but keeps its own colors.
- Non-humanoid mobs (pigs, spiders, …) get the statue and effects without a limb pose (EMF packs can add one).
- Not tested with Iris shaders.

## Building

```bash
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runClient` starts a dev client with Mod Menu, EMF and ETF. See [docs/CUSTOMIZING.md](docs/CUSTOMIZING.md#dev-setup) for the optional Flashback dev setup and a Windows note.

## License

[MIT](LICENSE)
