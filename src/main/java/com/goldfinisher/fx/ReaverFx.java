package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.FinisherProps;
import com.goldfinisher.GoldFinisher.Finisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
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
 * Reaver, timed from the video: a void portal opens under the body, shadow claws sweep up out of it,
 * a violet energy pillar erupts and swallows the body, then dissolves and the portal closes.
 */
public final class ReaverFx {
	private static final int CLAWS = 5;

	private ReaverFx() {}

	public static void start(ClientLevel level, Entity body, Finisher f) {
		int id = body.getId();
		Fx.disc(level, id, f.pos(), Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), true);  // glowing rim
		Fx.disc(level, id, f.pos(), Blocks.BLACK_CONCRETE.defaultBlockState(), false);       // void inside
		for (int i = 0; i < CLAWS; i++) FinisherProps.item(level, id, f.pos(), new ItemStack(Items.NETHERITE_HOE), false);
		FinisherProps.block(level, id, f.pos(), Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), true);  // outer pillar
		FinisherProps.block(level, id, f.pos(), Blocks.MAGENTA_STAINED_GLASS.defaultBlockState(), true); // inner pillar
		Fx.sound(level, f.pos(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 1F, 0.6F);
		Fx.sound(level, f.pos(), SoundEvents.SOUL_ESCAPE.value(), 2F, 0.8F);
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		RandomSource r = level.random;
		List<Display> props = FinisherProps.of(body.getId());
		if (props.size() < 4 + CLAWS + 2) return;
		Vec3 p = f.pos();
		float u = t / (float) total, h = body.getBbHeight(), w = body.getBbWidth();
		int n = Fx.scale(cfg);

		// portal: opens fast, holds, shrinks shut at the end
		float portal = cfg.portalRing ? Fx.easeOut(Fx.window(u, 0F, 0.1F)) * (1 - Fx.smooth(Fx.window(u, 0.85F, 1F))) : 0;
		float spin = t * 0.03F;
		Fx.moveDisc(new Display.BlockDisplay[] { (Display.BlockDisplay) props.get(0), (Display.BlockDisplay) props.get(1) }, 0.01F, 2.0F * portal, spin);
		Fx.moveDisc(new Display.BlockDisplay[] { (Display.BlockDisplay) props.get(2), (Display.BlockDisplay) props.get(3) }, 0.015F, 1.6F * portal, -spin);

		// shadow claws sweep up out of the portal around the body, then sink back
		float claw = cfg.souls ? Fx.window(u, 0.15F, 0.36F) : 0;
		float clawSize = claw > 0 && claw < 1 ? 1.4F * Mth.sin(claw * Mth.PI) : 0;
		for (int i = 0; i < CLAWS; i++) {
			float a = i * Mth.TWO_PI / CLAWS + 0.3F;
			float rise = Fx.easeOut(claw);
			Quaternionf rot = new Quaternionf().rotationY(-a + Mth.HALF_PI).rotateZ((float) Math.toRadians(-45 + rise * 160)); // blade arcs up and over
			FinisherProps.move(props.get(4 + i), new Vector3f(Mth.cos(a) * (w + 0.4F), -0.8F + rise * 1.8F, Mth.sin(a) * (w + 0.4F)), rot,
					new Vector3f(clawSize, clawSize, 0.3F));
		}
		if (cfg.souls && Fx.at(t, total, 0.16)) Fx.sound(level, p, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.5F);

		// violet pillar: erupts at 0.34, holds and swirls, then thins out and fades
		float erupt = Fx.easeOut(Fx.window(u, 0.34F, 0.4F));
		float fade = 1 - Fx.smooth(Fx.window(u, 0.68F, 0.9F));
		float pillar = cfg.pillar ? erupt : 0;
		float width = (w + 0.9F) * fade, height = h * 2.8F * pillar;
		FinisherProps.move(props.get(4 + CLAWS), new Vector3f(0, height / 2, 0), new Quaternionf().rotationY(t * 0.05F),
				new Vector3f(width * pillar, height, width * pillar));
		FinisherProps.move(props.get(5 + CLAWS), new Vector3f(0, height / 2, 0), new Quaternionf().rotationY(-t * 0.08F),
				new Vector3f(width * 0.6F * pillar, height * 1.05F, width * 0.6F * pillar));
		if (cfg.pillar && Fx.at(t, total, 0.34)) {
			Fx.sound(level, p, SoundEvents.BEACON_ACTIVATE, 1.5F, 0.6F);
			Fx.sound(level, p, SoundEvents.WARDEN_SONIC_BOOM, 0.4F, 1.4F);
		}
		if (Fx.at(t, total, 0.85)) {
			Fx.sound(level, p, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.7F);
			Fx.sound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1F, 0.5F);
		}

		// particles: violet swirl around the body, souls spiralling in, ink at the portal rim
		DustParticleOptions violet = new DustParticleOptions(ARGB.scaleRGB(f.color(), 2.6F), 1.3F);
		if (cfg.souls && u < 0.85F) {
			double radius = (w + 0.6) * (1 - u * 0.5);
			for (int i = 0; i < n * 3; i++) {
				double a = t * 0.5 + i * Math.PI * 2 / (n * 3);
				Vec3 at = p.add(Math.cos(a) * radius, r.nextDouble() * h * (1 + pillar * 1.6), Math.sin(a) * radius);
				Fx.particle(level, i % 3 == 0 ? ParticleTypes.SOUL : violet, at, -Math.sin(a) * 0.06, 0.04, Math.cos(a) * 0.06);
			}
		}
		if (portal > 0) {
			Fx.ring(level, ParticleTypes.REVERSE_PORTAL, p.add(0, 0.05, 0), 1.0 * portal, 5 * n, t * 0.2);
			if (t % 2 == 0) Fx.ring(level, ParticleTypes.SQUID_INK, p.add(0, 0.05, 0), 0.8 * portal, 2 * n, -t * 0.2);
		}
		if (pillar > 0 && fade > 0.2F) {
			PowerParticleOption breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1F);
			for (int i = 0; i < n * 2; i++) {
				Vec3 at = p.add((r.nextDouble() - 0.5) * width, r.nextDouble() * height, (r.nextDouble() - 0.5) * width);
				Fx.particle(level, i % 2 == 0 ? ParticleTypes.WITCH : breath, at, 0, 0.1, 0);
			}
		}
	}

	public static void end(ClientLevel level, Entity body, Finisher f) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		int n = Fx.scale(FinisherConfig.INSTANCE);
		for (int i = 0; i < 10 * n; i++) {
			Fx.particle(level, ParticleTypes.LARGE_SMOKE, p.add((r.nextDouble() - 0.5) * 0.8, 0.1, (r.nextDouble() - 0.5) * 0.8), 0, 0.04, 0);
			Fx.particle(level, ParticleTypes.PORTAL, p.add(0, 0.3, 0), (r.nextDouble() - 0.5) * 2, r.nextDouble(), (r.nextDouble() - 0.5) * 2);
		}
	}
}
