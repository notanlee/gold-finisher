package com.goldfinisher.fx;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.FinisherProps;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Small helpers shared by the finisher choreographies. Times are fractions u = tick / duration. */
final class Fx {
	private Fx() {}

	/** True on the one tick where the timeline crosses fraction {@code frac}. */
	static boolean at(int t, int total, double frac) {
		return t == (int) Math.round(frac * total);
	}

	/** 0..1 progress of u through [a, b], clamped. */
	static float window(float u, float a, float b) {
		return Mth.clamp((u - a) / (b - a), 0F, 1F);
	}

	static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}

	static float easeOut(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	static float easeIn(float x) {
		return x * x * x;
	}

	static int scale(FinisherConfig cfg) {
		return switch (cfg.particles) {
			case LOW -> 1;
			case MEDIUM -> 2;
			case HIGH -> 4;
		};
	}

	static void sound(ClientLevel level, Vec3 p, SoundEvent s, float volume, float pitch) {
		if (FinisherConfig.INSTANCE.sounds) level.playLocalSound(p.x, p.y, p.z, s, SoundSource.PLAYERS, volume, pitch, false);
	}

	/** Note-block pitch for a semitone offset from F#3 (0..24 like note blocks). */
	static float note(int semitones) {
		return (float) Math.pow(2, (semitones - 12) / 12.0);
	}

	static void particle(ClientLevel level, ParticleOptions o, Vec3 p, double vx, double vy, double vz) {
		level.addParticle(o, true, false, p.x, p.y, p.z, vx, vy, vz);
	}

	/** {@code n} particles evenly along the segment a→b. */
	static void line(ClientLevel level, ParticleOptions o, Vec3 a, Vec3 b, int n) {
		for (int i = 0; i <= n; i++) particle(level, o, a.lerp(b, i / (double) n), 0, 0, 0);
	}

	/** Particles on a horizontal circle. */
	static void ring(ClientLevel level, ParticleOptions o, Vec3 centre, double radius, int n, double phase) {
		for (int i = 0; i < n; i++) {
			double a = phase + i * Math.PI * 2 / n;
			particle(level, o, centre.add(Math.cos(a) * radius, 0, Math.sin(a) * radius), 0, 0, 0);
		}
	}

	/** Flat "disc": two squares rotated 45° from each other read as an octagon. */
	static Display.BlockDisplay[] disc(ClientLevel level, int owner, Vec3 origin, BlockState state, boolean bright) {
		return new Display.BlockDisplay[] {
				FinisherProps.block(level, owner, origin, state, bright),
				FinisherProps.block(level, owner, origin, state, bright) };
	}

	static void moveDisc(Display.BlockDisplay[] disc, float y, float size, float spin) {
		for (int i = 0; i < disc.length; i++) {
			FinisherProps.move(disc[i], new Vector3f(0, y + i * 0.002F, 0),
					new Quaternionf().rotationY(spin + i * Mth.HALF_PI / 2), new Vector3f(size, 0.02F, size));
		}
	}

	/** Item sprites are drawn diagonally (handle bottom-left, tip top-right): this turns the tip straight down. */
	static Quaternionf tipDown(float yaw) {
		return new Quaternionf().rotationY(yaw).rotateZ((float) Math.toRadians(-135));
	}
}
