package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.GoldFinisher.Finisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Aemondir, timed from the video, particles only: a light streak falls from the sky onto the body (gold fire impact),
 * a second streak, red crystal spikes fan up out of the ground, a huge gold explosion, and a giant gold sword
 * (drawn in particles) is left planted blade-down where the body was, with a red pennant and embers, before it fades.
 */
public final class AemondirFx {
	/** Body pink glow, also used by the renderer. */
	public static final int BODY_COLOR = 0xFF7AD0;
	private static final int CRYSTALS = 7;
	private static final float STREAK_1 = 0.08F, STREAK_2 = 0.33F, CRYSTAL = 0.38F, BOOM = 0.55F, PLANT = 0.58F;
	private static final DustParticleOptions GOLD = new DustParticleOptions(0xFFC23A, 1.4F);
	private static final DustParticleOptions BRIGHT = new DustParticleOptions(0xFFE9A0, 1.0F);
	private static final DustParticleOptions RED = new DustParticleOptions(0xD0202A, 1.3F);

	private AemondirFx() {}

	/** True once the explosion has taken the body. */
	public static boolean bodyGone(float u) {
		return u >= BOOM + 0.02F;
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float u = t / (float) total, h = body.getBbHeight();
		int n = Fx.scale(cfg);

		streak(level, p, h, t, total, u, STREAK_1, n);
		streak(level, p, h, t, total, u, STREAK_2, n);
		if (u > STREAK_1 + 0.03F && u < BOOM && t % 2 == 0) { // gold fire at the feet
			for (int i = 0; i < n; i++) {
				Fx.particle(level, i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
						p.add((r.nextDouble() - 0.5) * 0.9, 0.1, (r.nextDouble() - 0.5) * 0.9), 0, 0.03, 0);
			}
		}

		// red crystal spikes fan up out of the ground around the body until the explosion
		float grow = cfg.crystals && u < BOOM ? Fx.easeOut(Fx.window(u, CRYSTAL, CRYSTAL + 0.07F)) : 0;
		if (grow > 0 && t % 2 == 0) {
			for (int i = 0; i < CRYSTALS; i++) {
				double a = i * Math.PI * 2 / CRYSTALS + 0.2, tilt = Math.toRadians(18 + (i % 3) * 12);
				double len = (0.9 + (i % 2) * 0.5) * grow;
				Vec3 base = p.add(Math.cos(a) * 0.45, 0.05, Math.sin(a) * 0.45);
				Vec3 tip = base.add(Math.cos(a) * Math.sin(tilt) * len, Math.cos(tilt) * len, Math.sin(a) * Math.sin(tilt) * len);
				Fx.line(level, RED, base, tip, 3 + n);
			}
		}
		if (cfg.crystals && Fx.at(t, total, CRYSTAL)) Fx.sound(level, p, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5F, 0.6F);

		if (Fx.at(t, total, BOOM)) explode(level, p, h, n, cfg);

		// the giant sword drops in and stays planted; drawn every other tick so it reads as a steady glowing shape
		float plant = Fx.easeIn(Fx.window(u, PLANT, PLANT + 0.04F));
		float stay = u >= PLANT ? 1 - Fx.smooth(Fx.window(u, 0.9F, 1F)) : 0;
		if (stay > 0.05F && t % 2 == 0) {
			double drop = (1 - plant) * 4;
			sword(level, p.add(0, drop, 0), f.yRot(), cfg.swordScale * stay, t, n);
		}
		if (Fx.at(t, total, PLANT + 0.04F)) {
			Fx.sound(level, p, SoundEvents.ANVIL_LAND, 0.7F, 0.6F);
			Fx.sound(level, p, SoundEvents.BELL_BLOCK, 1F, 0.8F);
		}
		if (stay > 0 && u > BOOM && t % 2 == 0) { // embers and lava around the planted sword
			for (int i = 0; i < n; i++) {
				Fx.particle(level, i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME,
						p.add((r.nextDouble() - 0.5) * 1.6, 0.05, (r.nextDouble() - 0.5) * 1.6), 0, 0.02, 0);
			}
		}
	}

	/** A sword planted blade-down: blade, crossguard, grip, pommel and a red pennant on the hilt, facing yaw. */
	private static void sword(ClientLevel level, Vec3 tip, float yaw, double size, int t, int n) {
		double a = Math.toRadians(yaw);
		Vec3 side = new Vec3(Math.cos(a), 0, Math.sin(a)); // crossguard runs across the body's left-right
		double blade = size * 0.75, guard = size * 0.22, grip = size * 0.18;
		Vec3 guardCentre = tip.add(0, blade, 0);
		Fx.line(level, GOLD, tip, guardCentre, (int) (6 * size) + n);                                  // blade
		Fx.line(level, BRIGHT, tip.add(0, blade * 0.15, 0), guardCentre.add(0, -blade * 0.1, 0), (int) (3 * size)); // glowing core
		Fx.line(level, GOLD, guardCentre.add(side.scale(guard)), guardCentre.add(side.scale(-guard)), (int) (3 * size) + 2); // crossguard
		Vec3 pommel = guardCentre.add(0, grip, 0);
		Fx.line(level, GOLD, guardCentre, pommel, 3);                                                  // grip
		Fx.particle(level, ParticleTypes.END_ROD, pommel, 0, 0, 0);                                     // pommel gem
		double wave = Math.sin(t * 0.35) * 0.08;                                                       // pennant flutters
		Vec3 flag = pommel.add(0, -grip * 0.3, 0);
		for (int i = 1; i <= 4; i++) {
			double d = i * size * 0.06;
			Fx.particle(level, RED, flag.add(side.scale(d)).add(0, wave * i, 0), 0, 0, 0);
		}
		if (t % 4 == 0) Fx.particle(level, ParticleTypes.WAX_ON, tip.add(0, Mth.nextDouble(level.random, 0, blade), 0), 0, 0.02, 0); // rune glints
	}

	/** A light streak from high above down onto the body around fraction {@code at}. */
	private static void streak(ClientLevel level, Vec3 p, float h, int t, int total, float u, float at, int n) {
		float s = Fx.window(u, at, at + 0.04F);
		if (s > 0 && s < 1) {
			Vec3 top = p.add(1.5, 20, -1), bottom = p.add(0, h * 0.5, 0);
			Vec3 head = top.lerp(bottom, Fx.easeIn(s));
			Fx.line(level, ParticleTypes.END_ROD, top.lerp(head, 0.6), head, 6 * n);
			Fx.line(level, ParticleTypes.FIREWORK, top.lerp(head, 0.8), head, 3 * n);
		}
		if (Fx.at(t, total, at)) Fx.sound(level, p, SoundEvents.TRIDENT_THUNDER.value(), 0.5F, 1.4F);
		if (Fx.at(t, total, at + 0.04)) { // impact
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFC040), p.add(0, h * 0.5, 0), 0, 0, 0);
			Fx.sound(level, p, SoundEvents.FIRECHARGE_USE, 1F, 0.8F);
			BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState());
			RandomSource r = level.random;
			for (int i = 0; i < 12 * n; i++) {
				Fx.particle(level, gold, p.add(0, 0.2, 0), (r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.5, (r.nextDouble() - 0.5) * 0.6);
			}
		}
	}

	private static void explode(ClientLevel level, Vec3 p, float h, int n, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 c = p.add(0, h * 0.4, 0);
		if (cfg.explosion) {
			Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, c, 0, 0, 0);
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFB020), c, 0, 0, 0);
			for (int i = 0; i < 40 * n; i++) { // rays shooting out flat and up
				double a = r.nextDouble() * Math.PI * 2;
				double speed = 0.6 + r.nextDouble() * 0.6;
				Fx.particle(level, i % 4 == 0 ? ParticleTypes.FLAME : GOLD, c, Math.cos(a) * speed, r.nextDouble() * 0.5, Math.sin(a) * speed);
			}
			Fx.sound(level, p, SoundEvents.GENERIC_EXPLODE.value(), 1F, 1.1F);
		}
		if (cfg.crystals) { // the crystals shatter in the blast
			BlockParticleOption glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_STAINED_GLASS.defaultBlockState());
			for (int i = 0; i < 20 * n; i++) {
				Fx.particle(level, glass, p.add(0, 0.6, 0), (r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.8);
			}
			Fx.sound(level, p, SoundEvents.GLASS_BREAK, 1F, 0.7F);
		}
	}

	public static void end(ClientLevel level, Finisher f) {
		RandomSource r = level.random;
		for (int i = 0; i < 12 * Fx.scale(FinisherConfig.INSTANCE); i++) {
			Fx.particle(level, ParticleTypes.LARGE_SMOKE, f.pos().add((r.nextDouble() - 0.5), r.nextDouble() * 2, (r.nextDouble() - 0.5)), 0, 0.03, 0);
		}
	}
}
