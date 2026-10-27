package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.FinisherProps;
import com.goldfinisher.GoldFinisher.Finisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Champions finishers, timed from the 2022 and 2023 videos (beat sheets in docs/CUSTOMIZING.md).
 * 2023: ground glyph, gold beam, ring of gold panels + emblem above; the body rises into the ring, which opens like wings.
 * 2022: white flash, dark dome with a lightning web, a gold weapon drops point-down into the body, shatters.
 */
public final class ChampionsFx {
	private static final int PANELS = 6;

	private ChampionsFx() {}

	private static boolean v2022() {
		return FinisherConfig.INSTANCE.championsVersion == FinisherConfig.ChampionsVersion.V2022;
	}

	public static void start(ClientLevel level, Entity body, Finisher f) {
		if (v2022()) {
			FinisherProps.item(level, body.getId(), f.pos(), new ItemStack(Items.GOLDEN_SWORD), true);
			for (int i = 0; i < 6; i++) FinisherProps.block(level, body.getId(), f.pos(), Blocks.GOLD_BLOCK.defaultBlockState(), true);
		} else {
			Fx.disc(level, body.getId(), f.pos(), Blocks.BLACK_CONCRETE.defaultBlockState(), false);
			for (int i = 0; i < 2; i++) FinisherProps.block(level, body.getId(), f.pos(), Blocks.GOLD_BLOCK.defaultBlockState(), true); // glyph X
			for (int i = 0; i < PANELS; i++) FinisherProps.block(level, body.getId(), f.pos(), Blocks.GOLD_BLOCK.defaultBlockState(), true);
			FinisherProps.item(level, body.getId(), f.pos(), new ItemStack(Items.NETHER_STAR), true); // emblem
		}
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total, int rgb) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		float u = t / (float) total;
		if (v2022()) tick2022(level, body, f, t, total, u, rgb, cfg);
		else tick2023(level, body, f, t, total, u, rgb, cfg);
		beatbox(level, f.pos(), t, total);
	}

	// ---------------------------------------------------------------- 2023

	private static void tick2023(ClientLevel level, Entity body, Finisher f, int t, int total, float u, int rgb, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float h = body.getBbHeight();
		List<Display> props = FinisherProps.of(body.getId());
		if (props.size() < 2 + 2 + PANELS + 1) return;

		// ground glyph: dark disc + a gold X, appears at 0.1
		float appear = Fx.smooth(Fx.window(u, 0.08F, 0.16F)) * (1 - Fx.window(u, 0.93F, 1F));
		Fx.moveDisc(new Display.BlockDisplay[] { (Display.BlockDisplay) props.get(0), (Display.BlockDisplay) props.get(1) },
				0.01F, 2.2F * appear, 0);
		for (int i = 0; i < 2; i++) {
			FinisherProps.move(props.get(2 + i), new Vector3f(0, 0.03F, 0),
					new Quaternionf().rotationY((float) Math.toRadians(45 + i * 90)), new Vector3f(1.9F * appear, 0.02F, 0.16F * appear));
		}

		// ring of panels above the head: slowly orbiting; at the climax it opens out and up like wings
		float open = Fx.smooth(Fx.window(u, 0.68F, 0.92F));
		float ringIn = Fx.easeOut(Fx.window(u, 0.1F, 0.22F)) * (1 - Fx.window(u, 0.95F, 1F));
		float radius = 0.95F + open * 1.4F;
		float ringY = h + 0.55F + open * 1.2F;
		float spin = t * 0.04F;
		for (int i = 0; i < PANELS; i++) {
			float a = spin + i * Mth.TWO_PI / PANELS;
			Quaternionf rot = new Quaternionf().rotationY(-a + Mth.HALF_PI).rotateX(-open * 1.1F); // face inward, tilt out
			FinisherProps.move(props.get(4 + i), new Vector3f(Mth.cos(a) * radius, ringY, Mth.sin(a) * radius), rot,
					new Vector3f(0.62F * ringIn, 0.82F * ringIn, 0.05F * ringIn));
		}
		// emblem above the ring
		float emblem = ringIn * (1 - open * 0.3F);
		FinisherProps.move(props.get(4 + PANELS), new Vector3f(0, ringY + 1.0F + open * 0.6F, 0),
				new Quaternionf().rotationY(t * 0.05F), new Vector3f(0.9F * emblem, 0.9F * emblem, 0.9F * emblem));

		int n = Fx.scale(cfg);
		DustParticleOptions dust = new DustParticleOptions(rgb, 1.3F);
		if (u > 0.08F && u < 0.95F) {
			for (int i = 0; i < n; i++) { // sparkles around the statue
				Fx.particle(level, dust, p.add((r.nextDouble() - 0.5) * 1.2, r.nextDouble() * h * 1.4, (r.nextDouble() - 0.5) * 1.2), 0, 0.03, 0);
			}
			if (cfg.beam) { // gold light beam from the glyph up through the ring
				for (int i = 0; i < n * 2; i++) {
					Fx.particle(level, ParticleTypes.END_ROD, p.add((r.nextDouble() - 0.5) * 0.5, r.nextDouble() * (ringY + 1), (r.nextDouble() - 0.5) * 0.5), 0, 0.06, 0);
				}
			}
			Fx.ring(level, dust, p.add(0, 0.05, 0), 1.05, 6 * n, t * 0.1);
		}
		if (Fx.at(t, total, 0.08)) { // the burst of gold light when it starts
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFD86A), p.add(0, h / 2, 0), 0, 0, 0);
			Fx.sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.7F);
		}
	}

	// ---------------------------------------------------------------- 2022

	private static void tick2022(ClientLevel level, Entity body, Finisher f, int t, int total, float u, int rgb, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float h = body.getBbHeight();
		List<Display> props = FinisherProps.of(body.getId());
		if (props.size() < 7) return;
		int n = Fx.scale(cfg);

		if (Fx.at(t, total, 0.1)) { // white star flash on the body
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), p.add(0, h / 2, 0), 0, 0, 0);
			Fx.sound(level, p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1F, 1.2F);
		}
		// dark dome above, with a white geometric web spreading over it
		if (u > 0.15F && u < 0.85F) {
			DustParticleOptions dome = new DustParticleOptions(0x3A0A12, 2.2F);
			double radius = 2.6 * Fx.easeOut(Fx.window(u, 0.15F, 0.35F));
			for (int i = 0; i < n * 6; i++) {
				double a = r.nextDouble() * Math.PI * 2, b = r.nextDouble() * Math.PI / 2;
				Fx.particle(level, dome, p.add(Math.cos(a) * Math.cos(b) * radius, 0.4 + Math.sin(b) * radius, Math.sin(a) * Math.cos(b) * radius), 0, 0, 0);
			}
			if (t % 3 == 0) { // one web strand: a chord between two points on the dome
				Vec3 a = domePoint(r, p, radius), b = domePoint(r, p, radius);
				Fx.line(level, ParticleTypes.ELECTRIC_SPARK, a, b, 10);
			}
		}

		// the gold weapon drops point-down from the sky into the body, then breaks into shards
		Display sword = props.get(0);
		float drop = Fx.easeIn(Fx.window(u, 0.42F, 0.55F));
		float swordSize = 2.2F * (u > 0.42F && u < 0.72F ? 1 : 0);
		FinisherProps.move(sword, new Vector3f(0, (float) Mth.lerp(drop, h + 7, h * 0.6), 0), Fx.tipDown(0), new Vector3f(swordSize, swordSize, 0.4F));
		if (Fx.at(t, total, 0.55)) { // the strike: lightning everywhere
			Fx.sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.3F);
			Fx.sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1F, 1F);
		}
		if (u > 0.55F && u < 0.72F) {
			for (int i = 0; i < n * 3; i++) { // jagged bolts radiating from the strike
				Vec3 from = p.add(0, h * 0.6, 0);
				Vec3 to = from.add((r.nextDouble() - 0.5) * 4, (r.nextDouble() - 0.3) * 3, (r.nextDouble() - 0.5) * 4);
				Fx.line(level, ParticleTypes.ELECTRIC_SPARK, from, to, 8);
			}
		}
		float shatter = Fx.window(u, 0.72F, 0.95F);
		for (int i = 0; i < 6; i++) { // wireframe shards flying out flat
			float a = i * Mth.TWO_PI / 6 + 0.4F;
			float d = 0.3F + Fx.easeOut(shatter) * 2.2F;
			float s = shatter > 0 && shatter < 1 ? 1 - shatter * 0.7F : 0;
			FinisherProps.move(props.get(1 + i), new Vector3f(Mth.cos(a) * d, h * 0.6F + shatter * 0.4F, Mth.sin(a) * d),
					new Quaternionf().rotationY(-a).rotateZ(shatter * 3), new Vector3f(0.7F * s, 0.1F * s, 0.1F * s));
		}
		if (Fx.at(t, total, 0.72)) {
			BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState());
			for (int i = 0; i < 30 * n; i++) {
				Fx.particle(level, gold, p.add(0, h * 0.6, 0), (r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.5, (r.nextDouble() - 0.5) * 0.8);
			}
			Fx.sound(level, p, SoundEvents.GLASS_BREAK, 1F, 0.8F);
		}
	}

	private static Vec3 domePoint(RandomSource r, Vec3 p, double radius) {
		double a = r.nextDouble() * Math.PI * 2, b = r.nextDouble() * Math.PI / 2;
		return p.add(Math.cos(a) * Math.cos(b) * radius, 0.4 + Math.sin(b) * radius, Math.sin(a) * Math.cos(b) * radius);
	}

	/** Champions "beatbox": kick / snare / hats over 32 steps, then a rising chime run and a bell hit at the climax. */
	private static void beatbox(ClientLevel level, Vec3 p, int t, int total) {
		for (int step = 0; step < 32; step++) {
			if (t != Math.round(step / 32F * total)) continue;
			if (step % 8 == 0 || step % 8 == 6) Fx.sound(level, p, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.2F, 0.7F);
			if (step % 8 == 4) Fx.sound(level, p, SoundEvents.NOTE_BLOCK_SNARE.value(), 1F, 1F);
			if (step % 2 == 1 && step < 28) Fx.sound(level, p, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6F, 1.6F);
			if (step >= 24 && step <= 27) Fx.sound(level, p, SoundEvents.NOTE_BLOCK_PLING.value(), 0.9F, Fx.note(new int[] { 6, 10, 13, 18 }[step - 24]));
			if (step == 30) {
				Fx.sound(level, p, SoundEvents.NOTE_BLOCK_BELL.value(), 1.2F, Fx.note(18));
				Fx.sound(level, p, SoundEvents.NOTE_BLOCK_CHIME.value(), 1F, Fx.note(22));
			}
		}
	}

	public static void end(ClientLevel level, Entity body, Finisher f, int rgb) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float h = body.getBbHeight();
		int n = Fx.scale(FinisherConfig.INSTANCE);
		Vec3 c = v2022() ? p.add(0, h * 0.6, 0) : p.add(0, h + 0.55 + 1.2 * FinisherConfig.INSTANCE.riseHeight, 0);
		Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | rgb), c, 0, 0, 0);
		DustParticleOptions dust = new DustParticleOptions(rgb, 1.4F);
		for (int i = 0; i < 25 * n; i++) {
			Fx.particle(level, i % 3 == 0 ? ParticleTypes.WAX_ON : dust, c, (r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.3) * 0.7, (r.nextDouble() - 0.5) * 0.7);
		}
		Fx.sound(level, p, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5F, 1.2F);
		Fx.sound(level, p, SoundEvents.FIREWORK_ROCKET_BLAST, 1F, 1F);
	}
}
