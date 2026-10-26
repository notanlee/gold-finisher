# Gold Finisher

A client-side Fabric mod for Minecraft **1.21.11** that plays Valorant-style finishers when a player kills something.

- **Champions** (Champions-collection style, without the trophy, mixed with the Roblox gold death): the body freezes and turns into a statue made of gold, diamond, emerald, amethyst or redstone block (or rainbow, or any custom color). One hand rises to the sky, the body leans back and floats up at an angle in a beam of light, then shatters.
- **Reaver**: the body turns black-purple and reaches up while purple smoke and souls swirl around it. A dark portal opens and it's dragged down into the ground.

Each death also plays a totem pop. The body stays where it died: the knockback from the last hit is ignored on your screen. Dead players are replaced by a client-side stand-in body, so the finisher plays in full even on servers that respawn players instantly.

Only you need the mod. It works on any server (vanilla included) and inside **Flashback / ReplayMod** replays and exported videos.

## Download

Get the latest `goldfinisher-x.y.z.jar` from the [Releases](../../releases) page and put it in `.minecraft/mods` together with Fabric API.

## Mods

| Mod | Needed? | Why |
|---|---|---|
| [Fabric Loader](https://fabricmc.net/use/) 0.20.1+ | Required | Mod loader |
| [Fabric API](https://modrinth.com/mod/fabric-api) for 1.21.11 | Required | Keybind, tick and render hooks |
| [Mod Menu](https://modrinth.com/mod/modmenu) | Optional | Config button in the mod list (otherwise use **G**) |
| [Entity Model Features](https://modrinth.com/mod/entity-model-features) + [Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures) | Optional | EMF animates the finisher pose and gives resource packs [animation variables](docs/CUSTOMIZING.md#2-emf-animations-resource-packs). Without it, the built-in pose is used. |
| [Flashback](https://modrinth.com/mod/flashback) / [ReplayMod](https://modrinth.com/mod/replaymod) | Optional | Finishers play back in replays. Set Trigger to "Any player kill" or "All deaths" there. |

## Settings

Press **G** in game (rebindable under Controls → Gold Finisher) or use Mod Menu → Config. The menu shows the shared options plus only the options of the finisher you picked.

**Shared**

| Option | What it does |
|---|---|
| Enabled | Turns the finisher on or off |
| Finisher | **Champions** / **Reaver** (switches which options are shown) |
| Trigger | **My kills** / **Any player kill** (default) / **All deaths** |
| Duration | 1–5 s |
| Animation speed | 0.25×–3×: how fast the limbs move into the pose |
| Particles | Low / Medium / High |
| Sounds | Finisher sounds |
| Totem sound / Totem particles | Totem pop sound and particles on death |
| Preview on me | Plays the finisher on yourself in third person |

**Champions**

| Option | What it does |
|---|---|
| Color | Gold, Diamond, Emerald, Amethyst, Redstone (the real block texture), **Rainbow**, **Custom** (R/G/B sliders) or **Random** (a different ore each kill) |
| Style | **Solid statue** (the whole body in the material) / **Color tint** (keeps the skin, tinted) |
| Rise height | 0–2 blocks |
| Horizontal angle | −180°…180°: which way the rise slants and the body leans (0 = backwards, ±90 = to a side, 180 = forwards) |
| Vertical angle | −60°…60°: how much the rise slants and the body leans; negative flips it to the other side |
| Glow | Full-bright statue |
| Light beam | The light beam |

**Reaver**

| Option | What it does |
|---|---|
| Reaver color | Void purple, Blood red, Abyss blue or **Custom** (R/G/B sliders) |
| Style | **Solid statue** (the whole body in the color) / **Color tint** (keeps the skin, tinted) |
| Sink depth | 0.5–3 blocks into the ground |
| Sink start | 10–80%: how long it shivers before it gets dragged down |
| Shiver | 0–2×: shiver and arm tremble strength |
| Horizontal / Vertical angle | Same as Champions, for the sink path |
| Portal ring | The widening void ring on the ground |
| Souls & smoke | The swirl around the body |

Settings are saved in `config/goldfinisher.json`.

## Customizing (resource packs and developers)

See **[docs/CUSTOMIZING.md](docs/CUSTOMIZING.md)** for:
- every config field and its range;
- the EMF variables (`goldfinisher_progress`, `goldfinisher_type`, `goldfinisher_active`, `goldfinisher_speed`), the shipped `.jem` pose files, and how to change the pose or animate any mob;
- where in the code poses, colors, particles and sounds live, and how to add a menu option, an ore color or a new finisher type.

## Known limits
- Armor follows the pose but keeps its own colors.
- Non-humanoid mobs (pigs, spiders, …) become statues without a limb pose (EMF packs can add one).
- Baby zombie variants have no EMF pose file yet.
- Not tested with Iris shaders.

## Building

```bash
./gradlew build
```

The jar ends up in `build/libs/`. `./gradlew runClient` starts a dev client with Mod Menu, EMF and ETF. See [docs/CUSTOMIZING.md](docs/CUSTOMIZING.md#dev-setup) for the optional Flashback dev setup and a Windows note.

## License

[MIT](LICENSE)
