package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.utils.ChatProcessor;
import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.concurrent.ThreadLocalRandom;

// for each entity, have one of these (server only, used for stacking LLM calls)
public class EventQueueData {
    String entityId;
    Entity entity;
    Deque<MessageData> eventQueue;
    long lastTimePolled;
    long randomInterval;
    MessageData lastMessageData;

    private class MessageData {
        public String userLanguage;
        public String userMessage;
        public boolean is_auto_message;
        public ServerPlayerEntity player;

        public MessageData(String userLanguage, ServerPlayerEntity player, String userMessage,
                boolean is_auto_message) {
            this.is_auto_message = is_auto_message;
            this.userLanguage = userLanguage;
            this.player = player;
            this.userMessage = userMessage;
        }
    }

    public EventQueueData(String entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        eventQueue = new ArrayDeque<>();
        lastTimePolled = System.nanoTime();
        // poll between 45 ms -> 55 ms
        randomInterval = ThreadLocalRandom.current().nextLong(45_000_000L, 55_000_001L);
    }

    public void updateUUID(String newUUID) {
        this.entityId = newUUID;
    }

    public boolean shouldPoll() {
        boolean shouldPoll = entity != null &&
                lastMessageData != null &&
                lastMessageData.player != null &&
                !eventQueue.isEmpty() &&
                !EventQueueManager.llmProcessing &&
                System.nanoTime() > lastTimePolled + randomInterval &&
                lastMessageData.player.distanceTo(entity) < EventQueueManager.maxDistance;
        if (!shouldPoll) {
            return false;
        }
        // start polling:
        lastTimePolled = System.nanoTime();
        return true;
    }

    public void add(MessageData toAdd) {
        if (eventQueue.size() > 3) {
            // discard if queue size is too much
            eventQueue.poll();
        }
        eventQueue.addLast(toAdd);
    }

    // MAKE SURE THAT EXTERNAL ENTITY IS DIFFERENT FROM CURRENT WHEN CALLING THIS:
    public void addExternalEntityMessage(String userLanguage, ServerPlayerEntity player, String entityMessage,
            String entityName) {
        String newMessage = String.format("another creature named %s said %s", entityName, entityMessage);
        MessageData toAdd = new MessageData(userLanguage, player, newMessage, false);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void addUserMessage(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        LOGGER.info(String.format("EventQueueData/addUserMessage (%s) to entity (%s)", userMessage, entityId));
        MessageData toAdd = new MessageData(userLanguage, player, userMessage, is_auto_message);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void poll() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) polling event queue", entityId));
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        while (!eventQueue.isEmpty()) {
            // add all messages to queue if should poll
            MessageData cur = eventQueue.poll();
            chatData.addMessage(cur.userMessage, ChatDataManager.ChatSender.USER, cur.player, "system-chat");
        }
        // if (eventQueue.isEmpty()) {
        // if just added last message:

        LOGGER.info(
                String.format("EventQueueData/injectOnServerTick(entity %s) done polling, generating msg", entityId));
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);
        chatData.generateMessage(lastMessageData.userLanguage, lastMessageData.player,
                lastMessageData.is_auto_message, message -> {
                    EventQueueManager.llmProcessing = false;

                    LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) generated message (%s)",
                            entityId, message));
                }, errMsg -> {
                    EventQueueManager.llmProcessing = false;

                    LOGGER.info(String.format(
                            "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                            entityId, errMsg));
                });
    }

}
