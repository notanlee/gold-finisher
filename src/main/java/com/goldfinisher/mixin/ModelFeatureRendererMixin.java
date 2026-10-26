package com.goldfinisher.mixin;

import com.goldfinisher.FinisherPose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Poses humanoid limbs at draw time, after every subclass's setupAnim (zombie arms, player model). Covers armor too. */
@Mixin(ModelFeatureRenderer.class)
public abstract class ModelFeatureRendererMixin {
	@Inject(method = "renderModel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
	private <S> void goldfinisher$pose(SubmitNodeStorage.ModelSubmit<S> modelSubmit, RenderType renderType, VertexConsumer vertexConsumer,
			OutlineBufferSource outlineBufferSource, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
		FinisherPose.apply(modelSubmit.model(), modelSubmit.state());
	}
}
