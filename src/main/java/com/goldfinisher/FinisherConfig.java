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

	public boolean enabled = true;
	public Type type = Type.CHAMPIONS;
	public Color color = Color.GOLD;
	public int customColor = 0xFFD24A;
	public boolean totemSound = true;
	public Trigger trigger = Trigger.PLAYER_KILLS;
	public int durationTicks = 40;
	public double riseHeight = 1.0;
	/** Champions: degrees the body leans back and the rise path slants backwards. */
	public double riseAngle = 25;
	/** How fast limbs and lean move into the pose: 1 = reaches it at 40% of the finisher. */
	public double animationSpeed = 1.0;
	public Style style = Style.SOLID_GOLD;
	public boolean glow = true;
	public boolean beam = true;
	public Particles particles = Particles.MEDIUM;
	public boolean sounds = true;

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
