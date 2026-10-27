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
 * Aemondir, timed from the video: a light streak falls from the sky onto the body (gold fire impact, the body
 * crumples), a second streak, red crystals fan up out of the ground, a huge gold explosion, and a giant gold sword
 * is left planted blade-down where the body was, with a red pennant and molten ground, before it fades.
 */
public final class AemondirFx {
	/** Body pink glow, also used by the renderer. */
	public static final int BODY_COLOR = 0xFF7AD0;
	private static final int CRYSTALS = 7;
	private static final float STREAK_1 = 0.08F, STREAK_2 = 0.33F, CRYSTAL = 0.38F, BOOM = 0.55F, PLANT = 0.58F;

	private AemondirFx() {}

	public static void start(ClientLevel level, Entity body, Finisher f) {
		int id = body.getId();
		FinisherProps.item(level, id, f.pos(), new ItemStack(Items.GOLDEN_SWORD), true);              // 0 sword
		FinisherProps.block(level, id, f.pos(), Blocks.RED_WOOL.defaultBlockState(), true);           // 1 pennant
		Fx.disc(level, id, f.pos(), Blocks.MAGMA_BLOCK.defaultBlockState(), true);                    // 2,3 molten ground
		for (int i = 0; i < CRYSTALS; i++) FinisherProps.block(level, id, f.pos(), Blocks.RED_STAINED_GLASS.defaultBlockState(), true);
	}

	/** True once the explosion has taken the body. */
	public static boolean bodyGone(float u) {
		return u >= BOOM + 0.02F;
	}

	public static void tick(ClientLevel level, Entity body, Finisher f, int t, int total) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		RandomSource r = level.random;
		List<Display> props = FinisherProps.of(body.getId());
		if (props.size() < 4 + CRYSTALS) return;
		Vec3 p = f.pos();
		float u = t / (float) total, h = body.getBbHeight();
		int n = Fx.scale(cfg);

		streak(level, p, h, t, total, u, STREAK_1, n);
		streak(level, p, h, t, total, u, STREAK_2, n);
		if (u > STREAK_1 + 0.03F && u < BOOM) { // gold fire at the feet
			for (int i = 0; i < n; i++) {
				Fx.particle(level, i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
						p.add((r.nextDouble() - 0.5) * 0.9, 0.1, (r.nextDouble() - 0.5) * 0.9), 0, 0.03, 0);
			}
		}

		// crystals fan up out of the ground around the body, then shatter in the explosion
		float grow = cfg.crystals ? Fx.easeOut(Fx.window(u, CRYSTAL, CRYSTAL + 0.07F)) * (u < BOOM ? 1 : 0) : 0;
		for (int i = 0; i < CRYSTALS; i++) {
			float a = i * Mth.TWO_PI / CRYSTALS + 0.2F;
			float tilt = (float) Math.toRadians(18 + (i % 3) * 12);
			float len = (0.9F + (i % 2) * 0.5F) * grow;
			Quaternionf rot = new Quaternionf().rotationY(-a).rotateZ(-tilt); // lean outward from the body
			FinisherProps.move(props.get(4 + i), new Vector3f(Mth.cos(a) * 0.45F, len / 2, Mth.sin(a) * 0.45F), rot,
					new Vector3f(0.16F * grow, len, 0.16F * grow));
		}
		if (cfg.crystals && Fx.at(t, total, CRYSTAL)) Fx.sound(level, p, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.5F, 0.6F);

		if (Fx.at(t, total, BOOM)) explode(level, p, h, n, cfg);

		// the giant sword drops in and stays planted; red pennant on the hilt; molten ground under it
		float plant = Fx.easeIn(Fx.window(u, PLANT, PLANT + 0.04F));
		float stay = u >= PLANT ? 1 - Fx.smooth(Fx.window(u, 0.9F, 1F)) : 0;
		float size = (float) cfg.swordScale * stay;
		float bladeHalf = size * 0.5F;
		float centreY = (float) Mth.lerp(plant, bladeHalf + 4, bladeHalf * 0.8F); // tip ends up a little in the ground
		FinisherProps.move(props.get(0), new Vector3f(0, centreY, 0), Fx.tipDown(0.4F), new Vector3f(size, size, size * 0.25F));
		float wave = Mth.sin(t * 0.35F) * 0.3F;
		FinisherProps.move(props.get(1), new Vector3f(0.25F * size * stay, centreY + bladeHalf * 0.85F, 0),
				new Quaternionf().rotationY(0.4F + wave), new Vector3f(0.5F * size / 3, 0.22F * size / 3, 0.02F));
		Fx.moveDisc(new Display.BlockDisplay[] { (Display.BlockDisplay) props.get(2), (Display.BlockDisplay) props.get(3) },
				0.01F, 1.7F * stay * (u >= BOOM ? 1 : 0), 0.2F);
		if (Fx.at(t, total, PLANT + 0.04F)) {
			Fx.sound(level, p, SoundEvents.ANVIL_LAND, 0.7F, 0.6F);
			Fx.sound(level, p, SoundEvents.BELL_BLOCK, 1F, 0.8F);
		}
		if (stay > 0 && u > BOOM) { // embers and lava around the planted sword
			for (int i = 0; i < n; i++) {
				Fx.particle(level, i % 3 == 0 ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME,
						p.add((r.nextDouble() - 0.5) * 1.6, 0.05, (r.nextDouble() - 0.5) * 1.6), 0, 0.02, 0);
			}
		}
	}

	/** A light streak from high above down onto the body around fraction {@code at}. */
	private static void streak(ClientLevel level, Vec3 p, float h, int t, int total, float u, float at, int n) {
		float s = Fx.window(u, at, at + 0.04F);
		if (s <= 0 || s >= 1) return;
		Vec3 top = p.add(1.5, 20, -1), bottom = p.add(0, h * 0.5, 0);
		Vec3 head = top.lerp(bottom, Fx.easeIn(s));
		Fx.line(level, ParticleTypes.END_ROD, top.lerp(head, 0.6), head, 6 * n);
		Fx.line(level, ParticleTypes.FIREWORK, top.lerp(head, 0.8), head, 3 * n);
		if (Fx.at(t, total, at)) Fx.sound(level, p, SoundEvents.TRIDENT_THUNDER.value(), 0.5F, 1.4F);
		if (Fx.at(t, total, at + 0.04)) { // impact
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFC040), bottom, 0, 0, 0);
			Fx.sound(level, p, SoundEvents.FIRECHARGE_USE, 1F, 0.8F);
			BlockParticleOption gold = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GOLD_BLOCK.defaultBlockState());
			for (int i = 0; i < 12 * n; i++) {
				Fx.particle(level, gold, p.add(0, 0.2, 0), (level.random.nextDouble() - 0.5) * 0.6, level.random.nextDouble() * 0.5, (level.random.nextDouble() - 0.5) * 0.6);
			}
		}
	}

	private static void explode(ClientLevel level, Vec3 p, float h, int n, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 c = p.add(0, h * 0.4, 0);
		if (cfg.explosion) {
			Fx.particle(level, ParticleTypes.EXPLOSION_EMITTER, c, 0, 0, 0);
			Fx.particle(level, ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFB020), c, 0, 0, 0);
			DustParticleOptions gold = new DustParticleOptions(0xFFC23A, 2.0F);
			for (int i = 0; i < 40 * n; i++) { // rays shooting out flat and up
				double a = r.nextDouble() * Math.PI * 2;
				double speed = 0.6 + r.nextDouble() * 0.6;
				Fx.particle(level, i % 4 == 0 ? ParticleTypes.FLAME : gold, c, Math.cos(a) * speed, r.nextDouble() * 0.5, Math.sin(a) * speed);
			}
			Fx.sound(level, p, SoundEvents.GENERIC_EXPLODE.value(), 1F, 1.1F);
		}
		if (cfg.crystals) {
			BlockParticleOption glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_STAINED_GLASS.defaultBlockState());
			for (int i = 0; i < 20 * n; i++) {
				Fx.particle(level, glass, p.add(0, 0.6, 0), (r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.8);
			}
			Fx.sound(level, p, SoundEvents.GLASS_BREAK, 1F, 0.7F);
		}
	}

	public static void end(ClientLevel level, Entity body, Finisher f) {
		RandomSource r = level.random;
		for (int i = 0; i < 12 * Fx.scale(FinisherConfig.INSTANCE); i++) {
			Fx.particle(level, ParticleTypes.LARGE_SMOKE, f.pos().add((r.nextDouble() - 0.5), r.nextDouble() * 2, (r.nextDouble() - 0.5)), 0, 0.03, 0);
		}
	}
}
