package com.goldfinisher;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Render-state keys shared by the renderer mixins and EMF compat, plus the built-in finisher limb pose and path math. */
public final class FinisherPose {
	/** 0..1 progress, null when not finishing. */
	public static final RenderStateDataKey<Float> PROGRESS = RenderStateDataKey.create(() -> "goldfinisher:progress");
	/** The running finisher (type, color, frozen pos), null when not finishing. */
	public static final RenderStateDataKey<GoldFinisher.Finisher> FINISHER = RenderStateDataKey.create(() -> "goldfinisher:finisher");
	/** True when EMF animates this entity's model: our .jem does the pose, so the built-in one stays off (armor included). */
	public static final RenderStateDataKey<Boolean> EMF_POSED = RenderStateDataKey.create(() -> "goldfinisher:emf_posed");
	/** Steepest slant allowed, so tan() stays finite. */
	private static final double MAX_SLANT = 78;

	private FinisherPose() {}

	/** Blends a humanoid's limbs toward the finisher pose. Call right after the model's setupAnim. */
	public static void apply(Object model, Object renderState) {
		if (!(model instanceof HumanoidModel<?> m) || !(renderState instanceof LivingEntityRenderState state)) return;
		Float p = state.getData(PROGRESS);
		GoldFinisher.Finisher f = state.getData(FINISHER);
		if (p == null || f == null || Boolean.TRUE.equals(state.getData(EMF_POSED))) return;
		float t = blendTime(p);
		float w = t < 0.5F ? 4 * t * t * t : 1 - (-2 * t + 2) * (-2 * t + 2) * (-2 * t + 2) / 2; // ease-in-out cubic
		// hat, sleeves and pants are children of head/arms/legs, so they follow
		if (f.type() == FinisherConfig.Type.REAVER) { // dragged down: reaching up, trembling
			float tremble = 0.08F * (float) FinisherConfig.INSTANCE.shiver * Mth.sin(p * 40);
			blend(m.rightArm, w, -3.0F, tremble);
			blend(m.leftArm, w, -3.0F, -tremble);
			blend(m.head, w, -0.8F, m.head.zRot);
			blend(m.rightLeg, w, m.rightLeg.xRot, 0.12F);
			blend(m.leftLeg, w, m.leftLeg.xRot, -0.12F);
		} else { // champions: one hand raised to the sky, other arm loose, looking up at it
			blend(m.rightArm, w, -3.05F, -0.12F); // straight up, a touch outward
			blend(m.leftArm, w, 0.2F, 0.12F);     // hangs slightly back and out
			blend(m.head, w, -0.55F, m.head.zRot);
			blend(m.rightLeg, w, 0, 0);
			blend(m.leftLeg, w, 0.15F, 0);        // trailing foot
		}
	}

	/** 0..1 how far into the pose transition: reaches 1 at 40% of the finisher, sooner/later with animation speed. */
	public static float blendTime(float progress) {
		return Math.min(1F, progress * (float) FinisherConfig.INSTANCE.animationSpeed / 0.4F);
	}

	/** Horizontal unit direction the slant points to: hDeg around from "behind" the body (0 = back, 180 = front). */
	public static Vec3 slantDirection(float yRotDeg, double hDeg) {
		double a = Math.toRadians(yRotDeg + hDeg);
		return new Vec3(Math.sin(a), 0, -Math.cos(a));
	}

	/**
	 * World offset after moving {@code vertical} blocks (negative = down) along a path tilted vDeg from vertical
	 * toward {@link #slantDirection}; negative vDeg tilts the other way.
	 */
	public static Vec3 slantOffset(float yRotDeg, double hDeg, double vDeg, double vertical) {
		double v = Mth.clamp(vDeg, -MAX_SLANT, MAX_SLANT);
		double side = Math.abs(vertical) * Math.tan(Math.toRadians(v));
		return slantDirection(yRotDeg, hDeg).scale(side).add(0, vertical, 0);
	}

	private static void blend(ModelPart part, float w, float xRot, float zRot) {
		part.xRot = Mth.lerp(w, part.xRot, xRot);
		part.zRot = Mth.lerp(w, part.zRot, zRot);
	}
}
