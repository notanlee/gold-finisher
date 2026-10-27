package com.goldfinisher;

import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-only block/item display entities used as finisher props (swords, panels, crystals, pillars...).
 * They sit at the finisher's frozen position; everything else is their transformation, which display entities
 * interpolate between ticks, so moving them once per tick still looks smooth.
 */
public final class FinisherProps {
	/** finisher entity id -> its props, removed when the finisher ends. */
	private static final Map<Integer, List<Display>> PROPS = new HashMap<>();
	/** Fake ids, below the corpse range; the server never hands out negative ids. */
	private static int nextId = Integer.MIN_VALUE / 2;

	private FinisherProps() {}

	public static Display.BlockDisplay block(ClientLevel level, int owner, Vec3 origin, BlockState state, boolean fullBright) {
		Display.BlockDisplay d = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
		d.setBlockState(state);
		return add(level, owner, origin, d, fullBright);
	}

	public static Display.ItemDisplay item(ClientLevel level, int owner, Vec3 origin, ItemStack stack, boolean fullBright) {
		Display.ItemDisplay d = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
		d.setItemStack(stack);
		d.setItemTransform(ItemDisplayContext.FIXED);
		return add(level, owner, origin, d, fullBright);
	}

	private static <T extends Display> T add(ClientLevel level, int owner, Vec3 origin, T d, boolean fullBright) {
		d.setId(nextId++);
		d.snapTo(origin.x, origin.y, origin.z, 0, 0);
		d.setViewRange(4F);
		if (fullBright) d.setBrightnessOverride(Brightness.FULL_BRIGHT);
		hide(d);
		level.addEntity(d);
		PROPS.computeIfAbsent(owner, k -> new ArrayList<>()).add(d);
		return d;
	}

	/** Zero scale: spawned props stay invisible until their first move. */
	public static void hide(Display d) {
		d.setTransformation(new Transformation(new Matrix4f().scale(0)));
	}

	/**
	 * Places a prop: centre offset from the finisher origin, rotation, and size in blocks.
	 * Block models span 0..1, so blocks are re-centred; item models are already centred.
	 * Interpolated over one tick by the client, so per-tick moves look continuous.
	 */
	public static void move(Display d, Vector3f centre, Quaternionf rotation, Vector3f size) {
		Matrix4f m = new Matrix4f().translate(centre).rotate(rotation);
		if (d instanceof Display.BlockDisplay) m.translate(-size.x / 2, -size.y / 2, -size.z / 2);
		m.scale(size);
		d.setTransformationInterpolationDelay(0);
		d.setTransformationInterpolationDuration(1);
		d.setTransformation(new Transformation(m));
	}

	public static List<Display> of(int owner) {
		return PROPS.getOrDefault(owner, List.of());
	}

	public static void remove(ClientLevel level, int owner) {
		List<Display> list = PROPS.remove(owner);
		if (list == null) return;
		for (Display d : list) level.removeEntity(d.getId(), Entity.RemovalReason.DISCARDED);
	}

	public static void clear() {
		PROPS.clear();
	}
}
