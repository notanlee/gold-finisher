package com.goldfinisher;

import com.goldfinisher.fx.AemondirFx;
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
		// hat, sleeves and pants are children of head/arms/legs, so they follow. For limbs hanging down, a positive
		// right-limb zRot points outward; for an arm pointing up, negative zRot tilts it outward.
		switch (f.type()) {
			case REAVER -> { // swallowed by the void: reaching up, trembling
				float tremble = 0.08F * (float) FinisherConfig.INSTANCE.shiver * Mth.sin(p * 40);
				blend(m.rightArm, w, -3.0F, tremble);
				blend(m.leftArm, w, -3.0F, -tremble);
				blend(m.head, w, -0.8F, m.head.zRot);
				blend(m.rightLeg, w, m.rightLeg.xRot, 0.12F);
				blend(m.leftLeg, w, m.leftLeg.xRot, -0.12F);
			}
			case AEMONDIR -> { // stands frozen where it died until the explosion: no extra pose
			}
			case CHAMPIONS -> {
				if (FinisherConfig.INSTANCE.championsVersion == FinisherConfig.ChampionsVersion.V2022) {
					// lightning-struck: arms flung wide, head thrown back, a spasm while the weapon strikes
					float spasm = p > 0.55F && p < 0.72F ? 0.15F * Mth.sin(p * 220) : 0;
					blend(m.rightArm, w, -0.3F, 1.25F + spasm);
					blend(m.leftArm, w, -0.3F, -1.25F - spasm);
					blend(m.head, w, -0.7F + spasm, m.head.zRot);
					blend(m.rightLeg, w, 0, 0.15F);
					blend(m.leftLeg, w, 0, -0.15F);
				} else { // 2023 ascension: one hand raised to the sky, other arm loose, looking up at it
					blend(m.rightArm, w, -3.05F, -0.12F);
					blend(m.leftArm, w, 0.2F, 0.12F);
					blend(m.head, w, -0.55F, m.head.zRot);
					blend(m.rightLeg, w, 0, 0);
					blend(m.leftLeg, w, 0.15F, 0); // trailing foot
				}
			}
		}
	}

	/**
	 * Whole-body motion for one frame: {@code vertical} blocks along a path slanted by the angles, lean weight 0..1
	 * (of vAngle), a small extra jitter, and whether the body is still shown.
	 */
	public record Motion(double vertical, double hAngle, double vAngle, float leanWeight, Vec3 jitter, boolean visible) {}

	/** Timed from the reference videos (beat sheets in docs/CUSTOMIZING.md). */
	public static Motion motion(GoldFinisher.Finisher f, float p) {
		FinisherConfig c = FinisherConfig.INSTANCE;
		float lean = blendTime(p);
		return switch (f.type()) {
			case REAVER -> { // shivers on the portal, then sinks into it inside the pillar, gone when it closes
				float start = (float) c.sinkStart;
				Vec3 shiver = p < 0.8F ? new Vec3(0.02 * c.shiver * Mth.sin(p * 120), 0, 0.015 * c.shiver * Mth.cos(p * 97)) : Vec3.ZERO;
				float t = Mth.clamp((p - start) / Math.max(0.05F, 0.8F - start), 0, 1);
				yield new Motion(-c.sinkDepth * t * t * t, c.reaverHorizontalAngle, c.reaverVerticalAngle, lean, shiver, p < 0.82F);
			}
			case AEMONDIR -> { // stays standing, jolts when the strikes land; the explosion takes it
				Vec3 hit = p > 0.12F && p < 0.16F || p > 0.37F && p < 0.41F ? new Vec3(0, 0.03 * Mth.sin(p * 300), 0) : Vec3.ZERO;
				yield new Motion(0, 0, 0, 0, hit, !AemondirFx.bodyGone(p));
			}
			case CHAMPIONS -> {
				double rh = c.riseHeight;
				if (c.championsVersion == FinisherConfig.ChampionsVersion.V2022) { // slight lift, struck, shatters at 0.72
					float lift = easeOut(Mth.clamp((p - 0.1F) / 0.3F, 0, 1));
					Vec3 jolt = p > 0.55F && p < 0.72F ? new Vec3(0.04 * Mth.sin(p * 400), 0, 0.04 * Mth.cos(p * 330)) : Vec3.ZERO;
					yield new Motion(0.25 * rh * lift, c.championsHorizontalAngle, c.championsVerticalAngle, lean, jolt, p < 0.72F);
				}
				// 2023: hovers in the beam, then climbs up through the ring, which opens around it
				double hover = 0.35 * rh * easeOut(Mth.clamp((p - 0.08F) / 0.17F, 0, 1));
				double climb = 2.4 * rh * smooth(Mth.clamp((p - 0.68F) / 0.27F, 0, 1));
				yield new Motion(hover + climb, c.championsHorizontalAngle, c.championsVerticalAngle, lean, Vec3.ZERO, p < 0.97F);
			}
		};
	}

	private static float smooth(float x) {
		return x * x * (3 - 2 * x);
	}

	private static float easeOut(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
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
