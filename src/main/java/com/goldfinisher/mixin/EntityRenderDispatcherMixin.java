package com.goldfinisher.mixin;

import com.goldfinisher.FinisherCorpse;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hides a dead player whose stand-in corpse is playing the finisher. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private <E extends Entity> void goldfinisher$hideReplaced(E entity, Frustum frustum, double x, double y, double z,
			CallbackInfoReturnable<Boolean> cir) {
		if (FinisherCorpse.HIDDEN.contains(entity.getId())) cir.setReturnValue(false);
	}
}
