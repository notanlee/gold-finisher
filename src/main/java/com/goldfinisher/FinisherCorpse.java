package com.goldfinisher;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Client-only stand-in body for a dead player. Servers respawn players with the SAME entity id (PlayerList.respawn),
 * and instant-respawn servers do it right away, which replaced the finishing body with the live player.
 * The stand-in isn't server-tracked, so nothing can remove or replace it before the finisher ends.
 */
public final class FinisherCorpse {
	/** Real player ids hidden while their stand-in plays the finisher. */
	public static final Set<Integer> HIDDEN = new HashSet<>();
	/** Fake ids count up from MIN_VALUE: the server never hands out negative ids. */
	private static int nextId = Integer.MIN_VALUE;

	private FinisherCorpse() {}

	public static AbstractClientPlayer spawn(AbstractClientPlayer original) {
		ClientLevel level = (ClientLevel) original.level();
		PlayerSkin skin = original.getSkin();
		// own UUID: the client level refuses duplicate UUIDs, and the skin is normally looked up by UUID
		AbstractClientPlayer corpse = new RemotePlayer(level, new GameProfile(UUID.randomUUID(), original.getGameProfile().name())) {
			{
				getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, original.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
			}

			@Override
			public PlayerSkin getSkin() {
				return skin;
			}
		};
		corpse.setId(nextId++);
		corpse.snapTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
		corpse.yBodyRot = corpse.yBodyRotO = original.yBodyRot;
		corpse.yHeadRot = corpse.yHeadRotO = original.yHeadRot;
		corpse.setPose(original.getPose() == Pose.DYING ? Pose.STANDING : original.getPose());
		for (EquipmentSlot slot : EquipmentSlot.values()) corpse.setItemSlot(slot, original.getItemBySlot(slot).copy());
		level.addEntity(corpse);
		HIDDEN.add(original.getId());
		return corpse;
	}
}
