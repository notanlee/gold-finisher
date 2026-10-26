package com.goldfinisher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Every menu option is one field here + one widget in {@link FinisherScreen}. */
public class FinisherConfig {
	public enum Trigger { MY_KILLS, PLAYER_KILLS, ALL_DEATHS }
	public enum Style { SOLID_GOLD, GOLD_TINT }
	public enum Particles { LOW, MEDIUM, HIGH }
	public enum Type { CHAMPIONS, REAVER }
	/**
	 * Ore colors draw the body with their real block texture (rgb only colors the particles);
	 * RAINBOW/CUSTOM tint the plain statue texture; RANDOM picks an ore per kill.
	 */
	public enum Color {
		GOLD(0xFFD24A, "gold_block"), DIAMOND(0x5DECF5, "diamond_block"), EMERALD(0x41F384, "emerald_block"),
		AMETHYST(0xB46CFF, "amethyst_block"), REDSTONE(0xFF3B30, "redstone_block"),
		RAINBOW(0xFFFFFF, null), CUSTOM(0xFFFFFF, null), RANDOM(0xFFFFFF, null);

		public static final Color[] CLASSIC = { GOLD, DIAMOND, EMERALD, AMETHYST, REDSTONE };
		public final int rgb;
		/** Block texture for the ore colors, null for the rest. */
		public final @Nullable Identifier texture;

		Color(int rgb, @Nullable String block) {
			this.rgb = rgb;
			this.texture = block == null ? null : Identifier.withDefaultNamespace("textures/block/" + block + ".png");
		}
	}

	/** Reaver body colors. */
	public enum ReaverColor {
		VOID(0x2A0F3D), BLOOD(0x4A0A0A), ABYSS(0x0B1A3D), CUSTOM(0x2A0F3D);

		public final int rgb;

		ReaverColor(int rgb) {
			this.rgb = rgb;
		}
	}

	// shared
	public boolean enabled = true;
	public Type type = Type.CHAMPIONS;
	public Trigger trigger = Trigger.PLAYER_KILLS;
	public int durationTicks = 40;
	/** How fast limbs and lean move into the pose: 1 = reaches it at 40% of the finisher. */
	public double animationSpeed = 1.0;
	public Particles particles = Particles.MEDIUM;
	public boolean sounds = true;
	public boolean totemSound = true;
	public boolean totemParticles = true;

	// champions
	public Color color = Color.GOLD;
	public int customColor = 0xFFD24A;
	public Style style = Style.SOLID_GOLD;
	public double riseHeight = 1.0;
	/**
	 * Slant of the path and lean of the body, in degrees. Vertical: tilt away from straight up/down (negative = the
	 * other side). Horizontal: which way the tilt points around the body, 0 = backwards, 90 = to its side, 180 = forwards.
	 */
	public double championsVerticalAngle = 25;
	public double championsHorizontalAngle = 0;
	public boolean glow = true;
	public boolean beam = true;

	// reaver
	public ReaverColor reaverColor = ReaverColor.VOID;
	public int reaverCustomColor = 0x2A0F3D;
	/** Blocks the body sinks into the ground. */
	public double sinkDepth = 2.3;
	/** Fraction of the finisher spent shivering before the sink starts. */
	public double sinkStart = 0.35;
	/** Shiver strength multiplier, 0 = still. */
	public double shiver = 1.0;
	public double reaverVerticalAngle = 0;
	public double reaverHorizontalAngle = 0;
	public boolean portalRing = true;
	public boolean souls = true;

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("goldfinisher.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static FinisherConfig INSTANCE = new FinisherConfig();

	public static void load() {
		if (!Files.exists(FILE)) return;
		try {
			FinisherConfig loaded = GSON.fromJson(Files.readString(FILE), FinisherConfig.class);
			if (loaded != null) INSTANCE = loaded;
		} catch (Exception e) {
			GoldFinisher.LOGGER.warn("Bad goldfinisher.json, using defaults", e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			GoldFinisher.LOGGER.error("Could not save goldfinisher.json", e);
		}
	}
}
