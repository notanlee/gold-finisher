package com.goldfinisher.mixin;

import com.goldfinisher.GoldFinisher;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server removes dead mobs after 1s; keep the body client-side until the finisher is done. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
	private void goldfinisher$deferRemoval(int id, Entity.RemovalReason reason, CallbackInfo ci) {
		if (GoldFinisher.ACTIVE.containsKey(id)) {
			GoldFinisher.DEFERRED_REMOVALS.put(id, reason);
			ci.cancel();
		}
	}

	// A new entity reusing this id (e.g. replay seeking) must replace the old body, not be blocked by it.
	@Inject(method = "addEntity", at = @At("HEAD"))
	private void goldfinisher$dropStale(Entity entity, CallbackInfo ci) {
		GoldFinisher.ACTIVE.remove(entity.getId());
		GoldFinisher.DEFERRED_REMOVALS.remove(entity.getId());
	}
}
