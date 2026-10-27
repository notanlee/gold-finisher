package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.GoldFinisher.Finisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Reaver, timed from the video, particles only: a void portal opens under the body, shadow claws sweep up out of it,
 * a violet energy pillar erupts and swallows the body, then dissolves and the portal closes.
 */
public final class ReaverFx {
	private static final int CLAWS = 5;

	private ReaverFx() {}

	public static void start(ClientLevel level, Finisher f) {
		Fx.sound(level, f.pos(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 1F, 0.6F);
		Fx.sound(level, f.pos(), SoundEvents.SOUL_ESCAPE.value(), 2F, 0.8F);
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		RandomSource r = level.random;
		Vec3 p = f.pos();
		float u = t / (float) total, h = body.getBbHeight(), w = body.getBbWidth();
		int n = Fx.scale(cfg);
		DustParticleOptions violet = new DustParticleOptions(ARGB.scaleRGB(f.color(), 2.6F), 1.3F);

		// portal: a dark ring that opens fast, holds, and shrinks shut at the end
		float portal = cfg.portalRing ? Fx.easeOut(Fx.window(u, 0F, 0.1F)) * (1 - Fx.smooth(Fx.window(u, 0.85F, 1F))) : 0;
		if (portal > 0) {
			Fx.ring(level, ParticleTypes.REVERSE_PORTAL, p.add(0, 0.05, 0), 1.0 * portal, 5 * n, t * 0.2);
			Fx.ring(level, violet, p.add(0, 0.05, 0), 0.75 * portal, 3 * n, -t * 0.15);
			if (t % 2 == 0) Fx.ring(level, ParticleTypes.SQUID_INK, p.add(0, 0.05, 0), 0.5 * portal, 2 * n, t * 0.3);
		}

		// shadow claws: dark arcs sweeping up out of the portal around the body
		float claw = cfg.souls ? Fx.window(u, 0.15F, 0.36F) : 0;
		if (claw > 0 && claw < 1) {
			for (int i = 0; i < CLAWS; i++) {
				double a = i * Math.PI * 2 / CLAWS + 0.3, rad = w + 0.4;
				double y = -0.2 + Fx.easeOut(claw) * (h + 0.5);
				double lean = Fx.easeOut(claw) * 0.6; // tip curls in over the body
				Vec3 base = p.add(Math.cos(a) * rad, Math.max(0.05, y - 0.8), Math.sin(a) * rad);
				Vec3 tip = p.add(Math.cos(a) * rad * (1 - lean), y, Math.sin(a) * rad * (1 - lean));
				Fx.line(level, ParticleTypes.SQUID_INK, base, tip, 3 * n);
			}
		}
		if (cfg.souls && Fx.at(t, total, 0.16)) Fx.sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);

		// violet pillar: erupts at 0.34, holds and swirls, then thins out and fades
		float erupt = cfg.pillar ? Fx.easeOut(Fx.window(u, 0.34F, 0.4F)) : 0;
		float fade = 1 - Fx.smooth(Fx.window(u, 0.68F, 0.9F));
		if (erupt > 0 && fade > 0.05F) {
			double radius = (w + 0.9) / 2 * fade, height = h * 2.8 * erupt;
			PowerParticleOption breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1F);
			for (int i = 0; i < n * 4; i++) {
				double a = t * 0.4 + i * Math.PI * 2 / (n * 4);
				Vec3 at = p.add(Math.cos(a) * radius, r.nextDouble() * height, Math.sin(a) * radius);
				Fx.particle(level, i % 3 == 0 ? breath : i % 3 == 1 ? ParticleTypes.WITCH : violet, at, 0, 0.08, 0);
			}
		}
		if (cfg.pillar && Fx.at(t, total, 0.34)) {
			Fx.sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.5F, 0.6F);
			Fx.sound(level, p, SoundEvents.WARDEN_SONIC_BOOM, 0.4F, 1.4F);
		}
		if (Fx.at(t, total, 0.85)) {
			Fx.sound(level, p, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.7F);
			Fx.sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1F, 0.5F);
		}

		// souls spiralling in around the body
		if (cfg.souls && u < 0.85F && t % 2 == 0) {
			double radius = (w + 0.6) * (1 - u * 0.5);
			for (int i = 0; i < n; i++) {
				double a = t * 0.5 + i * Math.PI * 2 / n;
				Fx.particle(level, ParticleTypes.SOUL, p.add(Math.cos(a) * radius, r.nextDouble() * h, Math.sin(a) * radius),
						-Math.sin(a) * 0.06, 0.04, Math.cos(a) * 0.06);
			}
		}
	}

	public static void end(ClientLevel level, Finisher f) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		int n = Fx.scale(FinisherConfig.INSTANCE);
		for (int i = 0; i < 10 * n; i++) {
			Fx.particle(level, ParticleTypes.LARGE_SMOKE, p.add((r.nextDouble() - 0.5) * 0.8, 0.1, (r.nextDouble() - 0.5) * 0.8), 0, 0.04, 0);
			Fx.particle(level, ParticleTypes.PORTAL, p.add(0, 0.3, 0), (r.nextDouble() - 0.5) * 2, r.nextDouble(), (r.nextDouble() - 0.5) * 2);
		}
	}
}
