package com.goldfinisher.compat;

import com.goldfinisher.FinisherConfig;
import com.goldfinisher.FinisherPose;
import com.goldfinisher.GoldFinisher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import traben.entity_model_features.EMFAnimationApi;
import traben.entity_model_features.models.animation.state.EMFEntityRenderState;
import traben.entity_model_features.models.animation.state.EMFState;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Optional Entity Model Features integration: animation variables for .jem files.
 * EMF classes are only touched behind {@link #LOADED}, so this class loads fine without EMF.
 */
public final class EmfCompat implements ClientModInitializer {
	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("entity_model_features");

	@Override
	public void onInitializeClient() {
		if (!LOADED) return;
		try {
			// Runs before the first resource reload, so the variables exist when .jem files are parsed.
			EMFAnimationApi.registerSingletonAnimationVariable(GoldFinisher.MOD_ID, "goldfinisher_progress",
					"Gold Finisher: finisher progress 0..1 (frame-smooth), 0 when not finishing", (Supplier<Float>) () -> {
						LivingEntityRenderState s = current();
						Float p = s == null ? null : s.getData(FinisherPose.PROGRESS);
						return p == null ? 0F : p;
					});
			EMFAnimationApi.registerSingletonAnimationVariable(GoldFinisher.MOD_ID, "goldfinisher_type",
					"Gold Finisher: 0 none, 1 champions, 2 reaver, 3 aemondir", (Supplier<Float>) () -> {
						LivingEntityRenderState s = current();
						GoldFinisher.Finisher f = s == null ? null : s.getData(FinisherPose.FINISHER);
						return f == null ? 0F : f.type().ordinal() + 1F;
					});
			EMFAnimationApi.registerSingletonAnimationVariable(GoldFinisher.MOD_ID, "goldfinisher_variant",
					"Gold Finisher: Champions version, 0 = 2022, 1 = 2023",
					(Supplier<Float>) () -> (float) FinisherConfig.INSTANCE.championsVersion.ordinal());
			EMFAnimationApi.registerSingletonAnimationVariable(GoldFinisher.MOD_ID, "goldfinisher_speed",
					"Gold Finisher: animation speed multiplier from the menu (1 = default)",
					(Supplier<Float>) () -> (float) FinisherConfig.INSTANCE.animationSpeed);
			EMFAnimationApi.registerSingletonAnimationVariable(GoldFinisher.MOD_ID, "goldfinisher_active",
					"Gold Finisher: true while the entity is finishing", (BooleanSupplier) () -> {
						LivingEntityRenderState s = current();
						return s != null && s.getData(FinisherPose.FINISHER) != null;
					});
		} catch (Exception e) {
			GoldFinisher.LOGGER.warn("Could not register EMF animation variables", e);
		}
	}

	/** True when EMF animates this model (a .jem with animations, ours or a resource pack's). */
	public static boolean isAnimating(EntityModel<?> model) {
		return LOADED && EMFAnimationApi.isModelAnimatedByEMF(model);
	}

	/** The vanilla render state EMF is animating right now; it carries our FinisherPose data. */
	private static LivingEntityRenderState current() {
		EMFEntityRenderState s = EMFState.state(); // same lookup as EMFAnimationApi.getCurrentEntity()
		return s != null && s.vanillaState() instanceof LivingEntityRenderState l ? l : null;
	}
}
