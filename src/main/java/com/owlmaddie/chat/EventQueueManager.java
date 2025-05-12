package com.owlmaddie.chat;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

// SERVER ONLY
public class EventQueueManager {
    public static ConcurrentHashMap<String, EventQueueData> queueData = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static final long maxDistance = 12;
    public static boolean llmProcessing = false;

    public static void updateUUID(String oldUUID, String newUUID) {
        EventQueueData data = queueData.remove(oldUUID);
        if (data == null) {
            LOGGER.info("Unable to update chat data, UUID not found: " + oldUUID);
            return;
        }
        data.updateUUID(newUUID);
        queueData.put(newUUID, data);

    }

    public static EventQueueData getOrCreateQueueData(String entityId, Entity entity) {
        return queueData.computeIfAbsent(entityId, k -> {
            LOGGER.info(String.format("EventQueueManager/creating new queue data for ent id (%s)", entityId));
            return new EventQueueData(entityId, entity);
        });
    }

    public static void addUserMessage(Entity entity, String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        EventQueueData q = getOrCreateQueueData(entity.getUuidAsString(), entity);
        q.addUserMessage(userLanguage, player, userMessage, is_auto_message);
    }
    public static void injectOnServerTick(){
        if(llmProcessing) return;
    for (EventQueueData queueData : queueData.values()) {
        if (queueData.shouldPoll()) {
            llmProcessing = true;
            queueData.poll();
            // only process one at a time:
            break; 
        }
    }
    }
}
