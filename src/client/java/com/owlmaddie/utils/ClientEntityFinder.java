package com.owlmaddie.utils;

import com.owlmaddie.ui.BubbleEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.chat.ChatDataManager.ChatStatus;
import com.owlmaddie.ui.PlayerMessageManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * The {@code ClientEntityFinder} class is used to find a specific MobEntity by
 * UUID, since
 * there is not a built-in method for this. Also has a method for client
 * PlayerEntity lookup.
 */
public class ClientEntityFinder {
    public static boolean isChattableEntity(EntityType<?> entityType) {
        if (!(entityType == EntityType.PLAYER || entityType.isSummonable())) {
            return false;
        }

        Identifier entityId = Registries.ENTITY_TYPE.getId(entityType);

        if (BubbleEntityRenderer.BLACKLIST.contains(entityId) || (!BubbleEntityRenderer.WHITELIST.isEmpty() && !BubbleEntityRenderer.WHITELIST.contains(entityId))) {
            return false;
        }
        return true;
    }
    public static MobEntity getEntityByUUID(ClientWorld world, UUID uuid) {
        for (Entity entity : world.getEntities()) {
            if (entity.getUuid().equals(uuid) && entity instanceof MobEntity) {
                return (MobEntity) entity;
            }
        }
        return null; // Entity not found
    }

    public static PlayerEntity getPlayerEntityFromUUID(UUID uuid) {
        return MinecraftClient.getInstance().world.getPlayers().stream()
                .filter(player -> player.getUuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets entity closest to player that has chat bubble open.
     */
    public static Optional<LivingEntity> getClosestEntityToPlayerWithChatBubbleOpen() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        List<LivingEntity> entities = List.of();
        MinecraftClient.getInstance().world.getEntities().forEach(entity -> {
            if (!(entity instanceof LivingEntity)) {
                return;
            }
            if (isChattableEntity(entity.getType())) {
                entities.add((LivingEntity) entity);
            }
        });


        Optional<LivingEntity> closest = entities.stream()
                .filter(entity -> {
                    if (!(entity instanceof MobEntity))
                        return false;
                    EntityChatData chatData = ChatDataManager.getClientInstance()
                            .getOrCreateChatData(entity.getUuid());
                    return chatData.status != ChatStatus.HIDDEN && chatData.status != ChatStatus.NONE;
                })
                .min(Comparator.comparingDouble(e -> e.getPos().distanceTo(player.getPos())));
        return closest;
    }
}
