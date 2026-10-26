package com.goldfinisher.mixin;

import com.goldfinisher.GoldFinisher;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	private static final byte DEATH = 3, POOF = 60;

	@Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
	private void goldfinisher$onEntityEvent(byte event, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!self.level().isClientSide()) return;
		if (event == DEATH) GoldFinisher.onDeath(self);
		else if (event == POOF && GoldFinisher.ACTIVE.containsKey(self.getId())) ci.cancel(); // our burst replaces it
	}
}
