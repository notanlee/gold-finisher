package com.goldfinisher;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A 16x16 block texture stretched over a 64x64 entity UV layout looks blurry and low-res.
 * Tiling it 4x4 gives one block pixel per skin pixel, so the statue reads as "made of the block".
 * Built at runtime from the active resource packs, so no Mojang textures are shipped.
 */
public final class OreTextures {
	private static final Map<Identifier, Identifier> TILED = new HashMap<>();

	private OreTextures() {}

	/** Tiled version of the block texture; falls back to the plain block texture if it can't be read. */
	public static Identifier tiled(Identifier block) {
		return TILED.computeIfAbsent(block, OreTextures::build);
	}

	// ponytail: built once per session; a resource pack swap mid-game keeps the old tiles until restart
	private static Identifier build(Identifier block) {
		Minecraft mc = Minecraft.getInstance();
		try (InputStream in = mc.getResourceManager().open(block); NativeImage src = NativeImage.read(in)) {
			int w = src.getWidth(), h = src.getHeight();
			NativeImage tiled = new NativeImage(w * 4, h * 4, false);
			for (int y = 0; y < h * 4; y++) {
				for (int x = 0; x < w * 4; x++) {
					tiled.setPixel(x, y, src.getPixel(x % w, y % h));
				}
			}
			Identifier id = Identifier.fromNamespaceAndPath(GoldFinisher.MOD_ID, "tiled/" + block.getPath().replace('/', '_'));
			mc.getTextureManager().register(id, new DynamicTexture(id::toString, tiled));
			return id;
		} catch (Exception e) {
			GoldFinisher.LOGGER.warn("Couldn't tile {}, using it as-is", block, e);
			return block;
		}
	}
}
