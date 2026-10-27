package com.goldfinisher;

import com.goldfinisher.fx.AemondirFx;
import com.goldfinisher.fx.ChampionsFx;
import com.goldfinisher.fx.ReaverFx;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
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
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GoldFinisher implements ClientModInitializer {
	public static final String MOD_ID = "goldfinisher";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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
	/** stand-in corpse id -> the real (hidden) player id, see FinisherCorpse. */
	private static final Map<Integer, Integer> CORPSES = new HashMap<>();

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
		// Dead players: play on a client-only stand-in so an instant respawn (same entity id) can't cut it off.
		// (Health is still synced as > 0 here, the death event arrives first, so don't use isDeadOrDying.)
		LivingEntity body = entity;
		if (entity instanceof AbstractClientPlayer player) {
			body = FinisherCorpse.spawn(player);
			CORPSES.put(body.getId(), player.getId());
		}
		start(body);
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
		Finisher f;
		if (cfg.type == FinisherConfig.Type.REAVER) {
			int color = cfg.reaverColor == FinisherConfig.ReaverColor.CUSTOM ? cfg.reaverCustomColor : cfg.reaverColor.rgb;
			f = new Finisher(entity.level().getGameTime(), cfg.type, color, false, entity.position(), entity.yBodyRot, null);
		} else if (cfg.type == FinisherConfig.Type.AEMONDIR) {
			f = new Finisher(entity.level().getGameTime(), cfg.type, AemondirFx.BODY_COLOR, false, entity.position(), entity.yBodyRot, null);
		} else {
			FinisherConfig.Color material = cfg.color == FinisherConfig.Color.RANDOM
					? FinisherConfig.Color.CLASSIC[entity.getRandom().nextInt(FinisherConfig.Color.CLASSIC.length)]
					: cfg.color;
			int color = material == FinisherConfig.Color.CUSTOM ? cfg.customColor : material.rgb;
			f = new Finisher(entity.level().getGameTime(), cfg.type, color,
					material == FinisherConfig.Color.RAINBOW, entity.position(), entity.yBodyRot, material.texture);
		}
		ACTIVE.put(entity.getId(), f);
		ClientLevel level = (ClientLevel) entity.level();
		if (cfg.totemSound) level.playLocalSound(f.pos().x, f.pos().y, f.pos().z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.0F, false);
		if (cfg.totemParticles) totem(level, entity, f.pos(), 15 * particleScale(cfg));
		if (f.type() == FinisherConfig.Type.REAVER) ReaverFx.start(level, f);
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
		return Math.min(1F, elapsed / FinisherConfig.INSTANCE.duration(f.type()));
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
			CORPSES.clear();
			FinisherCorpse.HIDDEN.clear();
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
			int total = cfg.duration(f.type());
			if (entity == null || elapsed < 0) { // gone, or time jumped backwards in a replay
				it.remove();
				DEFERRED_REMOVALS.remove(e.getKey());
				dropCorpse(mc.level, e.getKey());
				continue;
			}
			if (elapsed < total) {
				int t = (int) elapsed;
				// totem particles keep rising around the body the whole time (thinned out after the opening pop)
				if (cfg.totemParticles && t % 2 == 0) totem(mc.level, entity, f.pos(), particleScale(cfg));
				switch (f.type()) {
					case CHAMPIONS -> ChampionsFx.tick(mc.level, entity, f, t, total, color(f, entity, 0));
					case REAVER -> ReaverFx.tick(mc.level, entity, f, t, total);
					case AEMONDIR -> AemondirFx.tick(mc.level, entity, f, t, total);
				}
				continue;
			}
			it.remove();
			switch (f.type()) {
				case CHAMPIONS -> ChampionsFx.end(mc.level, entity, f, color(f, entity, 0));
				case REAVER -> ReaverFx.end(mc.level, f);
				case AEMONDIR -> AemondirFx.end(mc.level, f);
			}
			Entity.RemovalReason reason = DEFERRED_REMOVALS.remove(e.getKey());
			if (reason != null) mc.level.removeEntity(e.getKey(), reason);
			dropCorpse(mc.level, e.getKey());
			if (entity == mc.player && previewRestoreCamera != null) {
				mc.options.setCameraType(previewRestoreCamera);
				previewRestoreCamera = null;
			}
		}
	}

	/** If id is a stand-in corpse: remove it and show the real player again. */
	private static void dropCorpse(ClientLevel level, int id) {
		Integer original = CORPSES.remove(id);
		if (original == null) return;
		FinisherCorpse.HIDDEN.remove(original);
		level.removeEntity(id, Entity.RemovalReason.DISCARDED);
	}

	/** Totem-of-undying particles bursting out of the frozen body, like a used totem. */
	private static void totem(Level level, Entity e, Vec3 p, int n) {
		RandomSource r = level.random;
		for (int i = 0; i < n; i++) {
			level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, p.x + (r.nextDouble() - 0.5) * e.getBbWidth(),
					p.y + r.nextDouble() * e.getBbHeight(), p.z + (r.nextDouble() - 0.5) * e.getBbWidth(),
					(r.nextDouble() - 0.5) * 0.8, r.nextDouble() * 0.6 + 0.2, (r.nextDouble() - 0.5) * 0.8);
		}
	}

	private static int particleScale(FinisherConfig cfg) {
		return switch (cfg.particles) {
			case LOW -> 1;
			case MEDIUM -> 2;
			case HIGH -> 4;
		};
	}
}
