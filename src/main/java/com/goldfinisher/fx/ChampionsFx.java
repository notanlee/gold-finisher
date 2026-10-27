package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.GoldFinisher.Finisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Champions finishers, timed from the 2022 and 2023 videos (beat sheets in docs/CUSTOMIZING.md). Particles only.
 * 2023: a gold glyph on the ground and a gold beam; the body hovers in it, then rises up and bursts.
 * 2022: white flash, dark dome with a lightning web, a gold streak drives down into the body, it shatters.
 */
public final class ChampionsFx {
	private ChampionsFx() {}

	private static boolean v2022() {
		return FinisherConfig.INSTANCE.championsVersion == FinisherConfig.ChampionsVersion.V2022;
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total, int rgb) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		float u = t / (float) total;
		if (v2022()) tick2022(level, body, f, t, total, u, cfg);
		else tick2023(level, body, f, t, total, u, rgb, cfg);
		beatbox(level, f.pos(), t, total);
	}

	// ---------------------------------------------------------------- 2023

	private static void tick2023(ClientLevel level, Entity body, Finisher f, int t, int total, float u, int rgb, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float h = body.getBbHeight();
		int n = Fx.scale(cfg);
		DustParticleOptions dust = new DustParticleOptions(rgb, 1.3F);
		if (Fx.at(t, total, 0.08)) { // the burst of gold light when it starts
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFD86A), p.add(0, h / 2, 0), 0, 0, 0);
			Fx.sound(level, p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.7F);
		}
		if (u < 0.08F || u > 0.95F || t % 2 != 0) return; // kept light: a few particles every other tick
		// ground glyph: a gold circle with an X through it
		Fx.ring(level, dust, p.add(0, 0.05, 0), 1.0, 4 * n, t * 0.05);
		for (int k = 0; k < 2; k++) {
			double a = Math.PI / 4 + k * Math.PI / 2;
			Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a)).scale(0.9);
			Fx.line(level, dust, p.add(d).add(0, 0.05, 0), p.subtract(d).add(0, 0.05, 0), 3 * n);
		}
		for (int i = 0; i < n; i++) { // sparkles around the statue
			Fx.particle(level, dust, p.add((r.nextDouble() - 0.5) * 1.0, r.nextDouble() * h * 1.3, (r.nextDouble() - 0.5) * 1.0), 0, 0.03, 0);
		}
		if (cfg.beam) { // gold light beam straight up from the glyph
			for (int i = 0; i < n; i++) {
				Fx.particle(level, ParticleTypes.END_ROD, p.add((r.nextDouble() - 0.5) * 0.35, r.nextDouble() * (h + 3), (r.nextDouble() - 0.5) * 0.35), 0, 0.05, 0);
			}
		}
	}

	// ---------------------------------------------------------------- 2022

	private static void tick2022(ClientLevel level, Entity body, Finisher f, int t, int total, float u, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float h = body.getBbHeight();
		int n = Fx.scale(cfg);

		if (Fx.at(t, total, 0.1)) { // white star flash on the body
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), p.add(0, h / 2, 0), 0, 0, 0);
			Fx.sound(level, p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1F, 1.2F);
		}
		// dark dome above, with a white geometric web spreading over it
		if (u > 0.15F && u < 0.85F && t % 2 == 0) { // kept sparse so the body stays readable
			DustParticleOptions dome = new DustParticleOptions(0x3A0A12, 2.2F);
			double radius = 2.6 * Fx.easeOut(Fx.window(u, 0.15F, 0.35F));
			for (int i = 0; i < n * 2; i++) Fx.particle(level, dome, domePoint(r, p, radius), 0, 0, 0);
			if (t % 6 == 0) { // one web strand: a chord between two points on the dome
				Fx.line(level, ParticleTypes.ELECTRIC_SPARK, domePoint(r, p, radius), domePoint(r, p, radius), 10);
			}
		}

		// a gold streak drives down from the sky into the body (where the weapon falls in the original)
		float drop = Fx.window(u, 0.42F, 0.55F);
		if (drop > 0 && drop < 1) {
			Vec3 top = p.add(0, h + 8, 0), head = top.lerp(p.add(0, h * 0.6, 0), Fx.easeIn(drop));
			Fx.line(level, ParticleTypes.END_ROD, top.lerp(head, 0.7), head, 5 * n);
			Fx.line(level, new DustParticleOptions(0xFFC23A, 1.6F), top.lerp(head, 0.85), head, 3 * n);
		}
		if (Fx.at(t, total, 0.55)) { // the strike: lightning everywhere
			Fx.sound(level, p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.3F);
			Fx.sound(level, p, SoundEvents.LIGHTNING_BOLT_IMPACT, 1F, 1F);
		}
		if (u > 0.55F && u < 0.72F) {
			for (int i = 0; i < n; i++) { // jagged bolts radiating from the strike
				Vec3 from = p.add(0, h * 0.6, 0);
				Vec3 to = from.add((r.nextDouble() - 0.5) * 4, (r.nextDouble() - 0.3) * 3, (r.nextDouble() - 0.5) * 4);
				Fx.line(level, ParticleTypes.ELECTRIC_SPARK, from, to, 8);
			}
		}
		if (Fx.at(t, total, 0.72)) { // shatters into gold shards flying out flat
			Vec3 c = p.add(0, h * 0.6, 0);
			DustParticleOptions shard = new DustParticleOptions(0xFFC23A, 1.6F);
			BlockParticleOption bits = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState());
			for (int i = 0; i < 25 * n; i++) {
				double a = r.nextDouble() * Math.PI * 2, speed = 0.3 + r.nextDouble() * 0.4;
				Fx.particle(level, i % 2 == 0 ? shard : bits, c, Math.cos(a) * speed, r.nextDouble() * 0.2, Math.sin(a) * speed);
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
		Vec3 c = v2022() ? p.add(0, h * 0.6, 0) : p.add(0, h / 2 + 2.75 * FinisherConfig.INSTANCE.riseHeight, 0);
		Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFF000000 | rgb), c, 0, 0, 0);
		DustParticleOptions dust = new DustParticleOptions(rgb, 1.4F);
		for (int i = 0; i < 25 * n; i++) {
			Fx.particle(level, i % 3 == 0 ? ParticleTypes.WAX_ON : dust, c, (r.nextDouble() - 0.5) * 0.7, (r.nextDouble() - 0.3) * 0.7, (r.nextDouble() - 0.5) * 0.7);
		}
		Fx.sound(level, p, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5F, 1.2F);
		Fx.sound(level, p, SoundEvents.FIREWORK_ROCKET_BLAST, 1F, 1F);
	}
}
