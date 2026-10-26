package com.goldfinisher;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GoldFinisher implements ClientModInitializer {
	public static final String MOD_ID = "goldfinisher";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final DustParticleOptions VOID_DUST = new DustParticleOptions(0x7A2BBF, 1.2F);

	/**
	 * One running finisher. start is game time (not wall clock) so replays/pausing stay in sync.
	 * pos/yRot are captured on the death event, before the server's knockback arrives, and the body is drawn there.
	 * color is RGB resolved at kill time (ignored when rainbow).
	 */
	public record Finisher(long start, FinisherConfig.Type type, int color, boolean rainbow, Vec3 pos, float yRot,
			@Nullable Identifier texture) {}

	/** entityId -> running finisher. */
	public static final Map<Integer, Finisher> ACTIVE = new HashMap<>();
	/** entityId -> removal the server asked for, held back until the finisher ends. */
	public static final Map<Integer, Entity.RemovalReason> DEFERRED_REMOVALS = new HashMap<>();

	private static KeyMapping openMenu;
	private static ClientLevel lastLevel;
	private static CameraType previewRestoreCamera;

	@Override
	public void onInitializeClient() {
		FinisherConfig.load();
		openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.goldfinisher.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G,
				KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"))));
		ClientTickEvents.END_CLIENT_TICK.register(GoldFinisher::tick);
	}

	/** Called from the death entity-event (byte 3), which both live play and replay playback deliver. */
	public static void onDeath(LivingEntity entity) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		if (!cfg.enabled || !shouldTrigger(entity, cfg.trigger)) return;
		start(entity);
	}

	private static boolean shouldTrigger(LivingEntity entity, FinisherConfig.Trigger trigger) {
		if (trigger == FinisherConfig.Trigger.ALL_DEATHS) return true;
		// Filled from ClientboundDamageEventPacket, includes the shooter for projectile kills.
		DamageSource source = entity.getLastDamageSource();
		Entity killer = source == null ? null : source.getEntity();
		if (!(killer instanceof Player)) return false;
		return trigger == FinisherConfig.Trigger.PLAYER_KILLS || killer == Minecraft.getInstance().player;
	}

	public static void start(LivingEntity entity) {
		FinisherConfig cfg = FinisherConfig.INSTANCE;
		FinisherConfig.Color material = cfg.color == FinisherConfig.Color.RANDOM
				? FinisherConfig.Color.CLASSIC[entity.getRandom().nextInt(FinisherConfig.Color.CLASSIC.length)]
				: cfg.color;
		int color = material == FinisherConfig.Color.CUSTOM ? cfg.customColor : material.rgb;
		Finisher f = new Finisher(entity.level().getGameTime(), cfg.type, color,
				material == FinisherConfig.Color.RAINBOW, entity.position(), entity.yBodyRot, material.texture);
		ACTIVE.put(entity.getId(), f);
		Level level = entity.level();
		if (cfg.totemSound) playSound(level, f.pos(), SoundEvents.TOTEM_USE, 0.8F, 1.0F);
		if (!cfg.sounds) return;
		if (f.type() == FinisherConfig.Type.REAVER) {
			playSound(level, f.pos(), SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.8F);
			playSound(level, f.pos(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.35F, 0.5F);
		} else {
			playSound(level, f.pos(), SoundEvents.BELL_RESONATE, 0.8F, 1.4F);
			playSound(level, f.pos(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.8F);
		}
	}

	/** Shows the finisher on the local player in third person. */
	public static void preview() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		previewRestoreCamera = mc.options.getCameraType();
		mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		start(mc.player);
	}

	/** 0..1 progress, or -1 when the entity isn't finishing. */
	public static float progress(Entity entity, float partialTick) {
		Finisher f = ACTIVE.get(entity.getId());
		if (f == null) return -1;
		float elapsed = entity.level().getGameTime() - f.start() + partialTick;
		return Math.min(1F, elapsed / FinisherConfig.INSTANCE.durationTicks);
	}

	/** RGB to draw this finisher with right now (rainbow cycles smoothly). */
	public static int color(Finisher f, Entity entity, float partialTick) {
		if (!f.rainbow()) return f.color();
		float hue = ((entity.level().getGameTime() - f.start() + partialTick) / 30F + entity.getId() * 0.13F) % 1F;
		return Mth.hsvToRgb(hue, 0.85F, 1F) & 0xFFFFFF;
	}

	private static void tick(Minecraft mc) {
		while (openMenu.consumeClick()) mc.setScreen(new FinisherScreen(null));

		if (mc.level != lastLevel) { // world change or replay seek: old ids are meaningless
			lastLevel = mc.level;
			ACTIVE.clear();
			DEFERRED_REMOVALS.clear();
		}
		if (mc.level == null || mc.isPaused()) return;

		FinisherConfig cfg = FinisherConfig.INSTANCE;
		long now = mc.level.getGameTime();
		Iterator<Map.Entry<Integer, Finisher>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Finisher> e = it.next();
			Finisher f = e.getValue();
			Entity entity = mc.level.getEntity(e.getKey());
			long elapsed = now - f.start();
			if (entity == null || elapsed < 0) { // gone, or time jumped backwards in a replay
				it.remove();
				DEFERRED_REMOVALS.remove(e.getKey());
				continue;
			}
			if (elapsed < cfg.durationTicks) {
				if (f.type() == FinisherConfig.Type.REAVER) reaverTick(mc.level, entity, f, elapsed, cfg);
				else championsTick(mc.level, entity, f, cfg);
				continue;
			}
			it.remove();
			if (f.type() == FinisherConfig.Type.REAVER) reaverEnd(mc.level, entity, f, cfg);
			else burst(mc.level, entity, f, cfg);
			Entity.RemovalReason reason = DEFERRED_REMOVALS.remove(e.getKey());
			if (reason != null) mc.level.removeEntity(e.getKey(), reason);
			if (entity == mc.player && previewRestoreCamera != null) {
				mc.options.setCameraType(previewRestoreCamera);
				previewRestoreCamera = null;
			}
		}
	}

	private static int particleScale(FinisherConfig cfg) {
		return switch (cfg.particles) {
			case LOW -> 1;
			case MEDIUM -> 2;
			case HIGH -> 4;
		};
	}

	// Particles use f.pos(): the server still knocks the real entity around after death.
	private static void championsTick(ClientLevel level, Entity e, Finisher f, FinisherConfig cfg) {
		RandomSource r = level.random;
		DustParticleOptions dust = new DustParticleOptions(color(f, e, 0), 1.2F);
		Vec3 p = f.pos();
		int n = particleScale(cfg);
		double w = e.getBbWidth();
		for (int i = 0; i < n; i++) {
			level.addParticle(dust, p.x + (r.nextDouble() - 0.5) * w * 1.5, p.y + r.nextDouble() * e.getBbHeight(),
					p.z + (r.nextDouble() - 0.5) * w * 1.5, 0, 0.05, 0);
		}
		if (cfg.beam) {
			for (int i = 0; i < n; i++) {
				level.addParticle(ParticleTypes.END_ROD, p.x + (r.nextDouble() - 0.5) * 0.2, p.y + r.nextDouble() * 4,
						p.z + (r.nextDouble() - 0.5) * 0.2, 0, 0.08, 0);
			}
		}
	}

	private static void burst(ClientLevel level, Entity e, Finisher f, FinisherConfig cfg) {
		RandomSource r = level.random;
		DustParticleOptions dust = new DustParticleOptions(color(f, e, 0), 1.2F);
		// Body centre at the end of the slanted rise: drifted back along the path, plus the lean (matches LivingEntityRendererMixin)
		double angle = Math.toRadians(cfg.riseAngle), yaw = Math.toRadians(f.yRot());
		double back = cfg.riseHeight * Math.tan(Math.min(angle, Math.toRadians(78))) + e.getBbHeight() / 2 * Math.sin(angle);
		Vec3 p = f.pos();
		double cx = p.x + Math.sin(yaw) * back, cz = p.z - Math.cos(yaw) * back;
		double cy = p.y + e.getBbHeight() / 2 * Math.cos(angle) + cfg.riseHeight;
		int n = 20 * particleScale(cfg);
		for (int i = 0; i < n; i++) {
			level.addParticle(i % 3 == 0 ? ParticleTypes.WAX_ON : dust, cx, cy, cz,
					(r.nextDouble() - 0.5) * 0.6, (r.nextDouble() - 0.3) * 0.6, (r.nextDouble() - 0.5) * 0.6);
		}
		if (cfg.sounds) {
			playSound(level, p, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 1.2F);
			playSound(level, p, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
		}
	}

	/** Reaver: dragged into the void. Inward spiral around the body + a widening ink/portal ring on the ground. */
	private static void reaverTick(ClientLevel level, Entity e, Finisher f, long t, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		int n = particleScale(cfg);
		float progress = Math.min(1F, (float) t / cfg.durationTicks);
		double radius = (e.getBbWidth() * 1.2 + 0.4) * (1 - progress * 0.8);
		for (int i = 0; i < n * 3; i++) {
			double a = t * 0.6 + i * Math.PI * 2 / (n * 3);
			double x = Math.cos(a) * radius, z = Math.sin(a) * radius;
			// tangential + inward velocity so it visibly swirls toward the body
			double vx = (-Math.sin(a) - Math.cos(a) * 0.5) * 0.08, vz = (Math.cos(a) - Math.sin(a) * 0.5) * 0.08;
			ParticleOptions type = switch (i % 3) {
				case 0 -> ParticleTypes.SOUL;
				case 1 -> ParticleTypes.SMOKE;
				default -> VOID_DUST;
			};
			level.addParticle(type, p.x + x, p.y + r.nextDouble() * e.getBbHeight(), p.z + z, vx, -0.02, vz);
		}
		double ring = 0.5 + progress * 2.0;
		for (int i = 0; i < n * 4; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			level.addParticle(i % 2 == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.SQUID_INK,
					p.x + Math.cos(a) * ring, p.y + 0.05, p.z + Math.sin(a) * ring, 0, 0, 0);
		}
	}

	private static void reaverEnd(ClientLevel level, Entity e, Finisher f, FinisherConfig cfg) {
		RandomSource r = level.random;
		Vec3 p = f.pos();
		double cy = p.y + e.getBbHeight() / 2;
		int n = 10 * particleScale(cfg);
		for (int i = 0; i < n; i++) {
			level.addParticle(ParticleTypes.LARGE_SMOKE, p.x + (r.nextDouble() - 0.5) * 0.6, cy + (r.nextDouble() - 0.5),
					p.z + (r.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
		}
		for (int i = 0; i < n * 3; i++) {
			level.addParticle(ParticleTypes.PORTAL, p.x, cy, p.z,
					(r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5) * 2);
		}
		if (cfg.sounds) playSound(level, p, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.5F);
	}

	private static void playSound(Level level, Vec3 pos, SoundEvent sound, float volume, float pitch) {
		level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch, false);
	}
}
