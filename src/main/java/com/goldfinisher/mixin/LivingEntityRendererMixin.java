package com.goldfinisher.mixin;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.FinisherPose;
import com.goldfinisher.GoldFinisher;
import com.goldfinisher.OreTextures;
import com.goldfinisher.compat.EmfCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static com.goldfinisher.FinisherPose.EMF_POSED;
import static com.goldfinisher.FinisherPose.FINISHER;
import static com.goldfinisher.FinisherPose.PROGRESS;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Shadow
	protected EntityModel<?> model;
	// Mixins may only hold private statics: shared keys live in FinisherPose.
	/** ARGB to draw with this frame, resolved at extract time (rainbow needs partialTick). */
	private static final RenderStateDataKey<Integer> COLOR = RenderStateDataKey.create(() -> "goldfinisher:color");
	// Near-white noise, multiplied by COLOR, so every model UV layout comes out evenly colored.
	private static final Identifier STATUE_TEXTURE = Identifier.fromNamespaceAndPath(GoldFinisher.MOD_ID, "textures/entity/statue.png");
	private static final int FULL_BRIGHT = 0xF000F0;

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void goldfinisher$extract(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		float p = GoldFinisher.progress(entity, partialTick);
		GoldFinisher.Finisher f = p < 0 ? null : GoldFinisher.ACTIVE.get(entity.getId());
		// states are reused between entities, always overwrite
		state.setData(PROGRESS, f == null ? null : p);
		state.setData(FINISHER, f);
		state.setData(EMF_POSED, f != null && EmfCompat.isAnimating(model));
		if (f == null) return;
		FinisherConfig.Type type = f.type();
		state.setData(COLOR, 0xFF000000 | GoldFinisher.color(f, entity, partialTick)); // reaver color is resolved at start
		// Draw where it died: the server still knocks the body back, the client ignores it.
		state.x = f.pos().x;
		state.y = f.pos().y;
		state.z = f.pos().z;
		state.bodyRot = f.yRot();
		state.yRot = 0;                   // head yaw is relative to the body, keep it straight
		state.shadowPieces.clear();       // computed relative to the knocked-back x/z, would be offset
		state.deathTime = 0;             // no fall-over
		state.hasRedOverlay = false;      // no red flash
		state.walkAnimationSpeed = 0;     // statue: freeze limbs
		state.ageInTicks -= p * FinisherConfig.INSTANCE.duration(type); // freeze idle sway at the death pose
		// reaver stays dark; champions glows when enabled; aemondir's body always glows
		boolean bright = type == FinisherConfig.Type.AEMONDIR || type == FinisherConfig.Type.CHAMPIONS && FinisherConfig.INSTANCE.glow;
		if (bright) state.lightCoords = FULL_BRIGHT;
	}

	// Layers (held items, custom heads, elytra) read the model as posed here, before the draw-time pose in ModelFeatureRendererMixin.
	@Inject(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Ljava/lang/Object;)V", shift = At.Shift.AFTER))
	private void goldfinisher$poseForLayers(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState camera, CallbackInfo ci) {
		FinisherPose.apply(model, state);
	}

	@Inject(method = "setupRotations", at = @At("HEAD"))
	private void goldfinisher$rise(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo ci) {
		Float p = state.getData(PROGRESS);
		GoldFinisher.Finisher f = state.getData(FINISHER);
		if (p == null || f == null) return;
		FinisherPose.Motion m = FinisherPose.motion(f, p);
		// pose is already scaled, so divide offsets by scale
		Vec3 o = FinisherPose.slantOffset(f.yRot(), m.hAngle(), m.vAngle(), m.vertical()).add(m.jitter());
		poseStack.translate(o.x / scale, o.y / scale, o.z / scale);
		// lean the top toward the slant direction, pivoting at the feet; axis k with k x up = direction
		Vec3 d = FinisherPose.slantDirection(f.yRot(), m.hAngle());
		float lean = (float) Math.toRadians(m.vAngle()) * m.leanWeight();
		poseStack.mulPose(new Quaternionf().rotationAxis(lean, (float) d.z, 0, (float) -d.x));
		if (!m.visible()) poseStack.scale(0.001F, 0.001F, 0.001F); // taken by the effect (explosion, pillar, shatter)
	}

	// Args: model, state, poseStack, renderType, light, overlay, tint, sprite, outline, crumbling
	@ModifyArgs(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
	private void goldfinisher$gold(Args args) {
		LivingEntityRenderState state = args.get(1);
		GoldFinisher.Finisher f = state.getData(FINISHER);
		Integer color = state.getData(COLOR);
		if (f == null || color == null) return;
		boolean reaver = f.type() == FinisherConfig.Type.REAVER;
		FinisherConfig.Style style = switch (f.type()) {
			case CHAMPIONS -> FinisherConfig.INSTANCE.style;
			case REAVER -> FinisherConfig.INSTANCE.reaverStyle;
			case AEMONDIR -> FinisherConfig.INSTANCE.aemondirStyle;
		};
		if (style == FinisherConfig.Style.GOLD_TINT) { // own skin, tinted (reaver's dark colors brightened so the skin stays readable)
			args.set(6, ARGB.multiply((int) args.get(6), reaver ? ARGB.scaleRGB(color, 2.5F) : color));
		} else if (f.type() == FinisherConfig.Type.CHAMPIONS && f.texture() != null) { // ore colors: the real block, untinted
			args.set(3, RenderTypes.entityCutoutNoCull(OreTextures.tiled(f.texture())));
			args.set(6, -1);
		} else { // reaver, aemondir, rainbow, custom: plain statue tinted
			args.set(3, RenderTypes.entityCutoutNoCull(STATUE_TEXTURE));
			args.set(6, color);
		}
	}
}
