package com.owlmaddie.chat;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventQueueManager {
    public static ConcurrentHashMap<String, EventQueueData> queueData = new ConcurrentHashMap<>();
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static final long maxDistance = 12;
    public static boolean llmProcessing = false;

    public static String blacklistedEntityId = null;

    private static long lastErrorTime = 0L;
    private static long waitTimeAfterError = 10_000_000_000L; // wait 3 sec after err before doing any polling

    public static void onError() {
        lastErrorTime = System.nanoTime();
    }

    public static boolean shouldWaitBecauseOfError() {
        return System.nanoTime() <= lastErrorTime + waitTimeAfterError;
    }

    // TODO : look for all stuff like this:
    // public static void updateUUID(String oldUUID, String newUUID) {
    // EventQueueData data = queueData.remove(oldUUID);
    // if (data == null) {
    // LOGGER.info("Unable to update chat data, UUID not found: " + oldUUID);
    // return;
    // }
    // data.updateUUID(newUUID);
    // queueData.put(newUUID, data);

    // }

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

    public static void addGreeting(Entity entity, String userLangauge, ServerPlayerEntity player) {
        EventQueueData q = getOrCreateQueueData(entity.getUuidAsString(), entity);
        q.addGreeting(userLangauge, player);
    }

    public static void addUserMessageToAllClose(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        for (EventQueueData curQueue : queueData.values()) {

            Entity entity = curQueue.entity;
            addUserMessage(curQueue.entity, userLanguage, player, userMessage, is_auto_message);
        }
    }

    public static void injectOnServerTick() {
        if (llmProcessing)
            return;

        // TODO: Figure out a smarter way to figure out which to poll?
        for (EventQueueData curQueue : queueData.values()) {
            // remove entity if despawn/died so dont poll and err:
            if (curQueue.shouldDelete()) { // if entity died, etc.
                queueData.remove(curQueue.entityId);
                continue;
            }
            if (curQueue.shouldPoll()) {
                llmProcessing = true;
                curQueue.poll();
                // only process one at a time:
                break;
            }
        }
    }
}