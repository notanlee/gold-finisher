# Customizing Gold Finisher

There are three ways to change how the finishers look, from no code at all to editing the mod:

1. **Players**: the in-game menu (key **G**, or Mod Menu → Config). Settings are saved to `config/goldfinisher.json`.
2. **Resource pack makers**: EMF `.jem` animations driven by the mod's animation variables. No Java needed.
3. **Developers**: the Java code. Pose angles, colors, particles, sounds, or new finisher types.

---

## 1. Config file (`config/goldfinisher.json`)

Every menu option is a field here. You can edit the file while the game is closed.

**Shared**

| Field | Type / range | Default | What it does |
|---|---|---|---|
| `enabled` | bool | `true` | Turns the finisher on or off |
| `type` | `CHAMPIONS`, `REAVER` | `CHAMPIONS` | Which finisher plays |
| `trigger` | `MY_KILLS`, `PLAYER_KILLS`, `ALL_DEATHS` | `PLAYER_KILLS` | Which deaths get a finisher. Use `PLAYER_KILLS` or `ALL_DEATHS` in replays. |
| `durationTicks` | 20–100 (20 = 1 s) | `40` | Length of the whole finisher |
| `animationSpeed` | 0.25–3 | `1.0` | How fast limbs and lean move into the pose. At 1 the pose is reached at 40% of the finisher. |
| `particles` | `LOW`, `MEDIUM`, `HIGH` | `MEDIUM` | Particle amount |
| `sounds` | bool | `true` | Finisher sounds |
| `totemSound` | bool | `true` | Totem pop sound on death |
| `totemParticles` | bool | `true` | Totem pop particles on death |

**Champions**

| Field | Type / range | Default | What it does |
|---|---|---|---|
| `color` | `GOLD`, `DIAMOND`, `EMERALD`, `AMETHYST`, `REDSTONE`, `RAINBOW`, `CUSTOM`, `RANDOM` | `GOLD` | Ore colors draw the real block texture. `RANDOM` picks an ore on each kill. |
| `customColor` | int RGB (e.g. `255` = blue, `16766538` = `0xFFD24A`) | `0xFFD24A` | Color used when `color` is `CUSTOM` |
| `style` | `SOLID_GOLD` ("Solid statue"), `GOLD_TINT` ("Color tint") | `SOLID_GOLD` | Solid statue texture, or the entity's own skin tinted |
| `riseHeight` | 0–2 blocks | `1.0` | How high the body floats |
| `championsHorizontalAngle` | −180…180° | `0` | Which way the slant points around the body: 0 = backwards, ±90 = to a side, 180 = forwards |
| `championsVerticalAngle` | −60…60° | `25` | How much the rise path slants from straight up and the body leans; negative flips it |
| `glow` | bool | `true` | Full-bright statue |
| `beam` | bool | `true` | Light beam |

**Reaver**

| Field | Type / range | Default | What it does |
|---|---|---|---|
| `reaverColor` | `VOID`, `BLOOD`, `ABYSS`, `CUSTOM` | `VOID` | Body color (Reaver is never lit up) |
| `reaverCustomColor` | int RGB | `0x2A0F3D` | Color used when `reaverColor` is `CUSTOM` |
| `sinkDepth` | 0.5–3 blocks | `2.3` | How deep the body is dragged |
| `sinkStart` | 0.1–0.8 | `0.35` | Fraction of the finisher spent shivering before the sink |
| `shiver` | 0–2 | `1.0` | Shiver and arm-tremble strength |
| `reaverHorizontalAngle` / `reaverVerticalAngle` | −180…180° / −60…60° | `0` / `0` | Same as Champions, for the sink path |
| `portalRing` | bool | `true` | Widening void ring on the ground |
| `souls` | bool | `true` | Soul, smoke and dust swirl |

---

## 2. EMF animations (resource packs)

With [Entity Model Features](https://modrinth.com/mod/entity-model-features) installed, the mod registers these animation variables. You can use them in any `.jem` animation expression:

| Variable | Value |
|---|---|
| `goldfinisher_active` | `true` while this entity is playing a finisher |
| `goldfinisher_progress` | `0 → 1` over the finisher, smooth every frame (uses the partial tick). `0` when not finishing. |
| `goldfinisher_type` | `0` none, `1` Champions, `2` Reaver |
| `goldfinisher_speed` | the `animationSpeed` setting (1 = default) |

### The shipped pose files

The mod ships pose-only files in `assets/minecraft/optifine/cem/`: `player`, `zombie`, `husk`, `drowned`, `zombie_villager`, `skeleton`, `stray`, `wither_skeleton`, `bogged`, `piglin`, `piglin_brute` and `zombified_piglin`.

Each file attaches one empty part to `body` with `"attach": true`, so the **vanilla geometry and texture stay**. It only adds animation lines:

```json
"var.t": "clamp(goldfinisher_progress * goldfinisher_speed / 0.4, 0, 1)",
"var.w": "if(goldfinisher_active, if(var.t < 0.5, 4 * var.t * var.t * var.t, 1 - pow(2 - 2 * var.t, 3) / 2), 0)",
"var.r": "if(goldfinisher_type > 1.5, 1, 0)",
"right_arm.rx": "lerp(var.w, right_arm.rx, if(var.r > 0.5, -3.0, -3.05))"
```

- `var.t` is the pose timeline (0 → 1). `var.w` is that timeline eased (ease-in-out cubic), so the motion is smooth.
- Every line has the form `lerp(var.w, <vanilla value>, <finisher value>)`. Outside a finisher `var.w` is 0, so the mob animates exactly like vanilla.
- `var.r` is 1 for Reaver and 0 for Champions.

### Changing the pose

To tweak the pose, copy a `.jem` into your own resource pack at the same path, change the target angles (radians: `-3.05` ≈ arm straight up, positive `rx` swings forward), and put the pack above the mod's resources.

### Animating a mob that has no shipped file

Write a `.jem` for that mob and use the variables. Example: a pig spins while finishing.

```json
{
	"models": [
		{
			"part": "body",
			"id": "spin",
			"attach": true,
			"animations": [
				{ "body.ry": "if(goldfinisher_active, goldfinisher_progress * 12.56, body.ry)" }
			]
		}
	]
}
```

When EMF animates an entity, the mod's built-in Java pose turns off for it, so the two never stack. If your pack ships its own `.jem` for one of the mobs above, yours wins, and that mob only gets a finisher pose if your file uses the variables.

---

## 3. Code (developers)

| What | Where |
|---|---|
| Built-in limb pose (used without EMF), angles per finisher type | `FinisherPose.apply` |
| Pose timing, shared by built-in pose, lean and EMF | `FinisherPose.blendTime` |
| Freeze, frozen position (no knockback), rise / sink / lean, statue texture and tint | `mixin/LivingEntityRendererMixin` |
| Pose applied at draw time (armor follows) | `mixin/ModelFeatureRendererMixin` |
| Ore colors (RGB + block texture) | `FinisherConfig.Color` |
| Block textures tiled 4×4 so they're crisp on 64×64 skins | `OreTextures` |
| Particles and sounds per finisher type | `GoldFinisher.effectsTick` / `burst` / `start` |
| Which deaths trigger (works in replays) | `GoldFinisher.shouldTrigger`, `mixin/LivingEntityMixin` |
| Body kept until the finisher ends | `mixin/ClientLevelMixin` |
| Stand-in body for dead players (instant-respawn servers reuse the entity id), hides the real player meanwhile | `FinisherCorpse`, `mixin/EntityRenderDispatcherMixin` |
| Slanted path math (horizontal / vertical angle) shared by rise, sink and end bursts | `FinisherPose.slantDirection` / `slantOffset` |
| EMF variables | `compat/EmfCompat` |

### Add a menu option
1. Add a field to `FinisherConfig`.
2. Add one widget line in `FinisherScreen.init()` and its label in `assets/goldfinisher/lang/en_us.json`.
3. Read `FinisherConfig.INSTANCE.<field>` where you need it.

### Add a finisher type
1. Add a constant to `FinisherConfig.Type` and a lang key `goldfinisher.option.type.<name>`.
2. Add a branch for it in `FinisherPose.apply` (pose), in `LivingEntityRendererMixin.goldfinisher$rise` (movement) and in `goldfinisher$gold` (texture/color). Give it its own config fields and a menu section in `FinisherScreen.init()`, like Reaver.
3. Give it particles and sounds in `GoldFinisher.effectsTick`, `burst` and `start`.
4. For EMF, give it a `goldfinisher_type` number in `EmfCompat` and a branch in the `.jem` files.

### Add an ore color
Add `NAME(0xRRGGBB, "block_name")` to `FinisherConfig.Color`, add it to `CLASSIC` if Random should pick it, and add a lang key `goldfinisher.option.color.name`.

### Dev setup
- `./gradlew runClient` starts a client with Mod Menu, EMF and ETF.
- **Flashback (optional, for replay testing):** Flashback's jar-in-jar libraries aren't loaded in a dev runtime. Extract `mixinsquared-fabric-0.3.7-beta.1.jar`, `mixinconstraints-1.0.8.jar` and `lattice-1.3.1.jar` from the Flashback 0.39.10 (MC 1.21.11) jar's `META-INF/jars/` into `libs/dev/`. `build.gradle` picks Flashback up automatically once they exist.
- **Windows with a space in the user folder:** set `JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\tmp -Djava.io.tmpdir=C:\tmp` (any folder without spaces), or Gradle fails with "Unable to establish loopback connection".
