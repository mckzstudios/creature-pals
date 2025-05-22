package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.owlmaddie.chat.MessageData.MessageDataType;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.network.ServerPackets;
import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public class EventQueueData {
    String entityId;
    Entity entity;
    Deque<MessageData> queue;
    long lastTimePolled;
    long randomInterval;
    MessageData lastMessageData;
    String characterName;

    public EventQueueData(String entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        queue = new ArrayDeque<>();
        lastTimePolled = System.nanoTime();
        // poll between 1000 ms -> 1500s
        randomInterval = ThreadLocalRandom.current().nextLong(1_000_000_000L, 1_500_000_001L);
        lastMessageData = null;
    }

    public boolean shouldPoll() {
        boolean shouldPoll = entity != null &&
                entityId != EventQueueManager.blacklistedEntityId &&
                lastMessageData != null &&
                lastMessageData.player != null &&
                !queue.isEmpty() &&
                !EventQueueManager.llmProcessing && // technically dont need this but in case
                System.nanoTime() > lastTimePolled + randomInterval &&
                lastMessageData.player.distanceTo(entity) < EventQueueManager.maxDistance &&
                !EventQueueManager.shouldWaitBecauseOfError();
        if (!shouldPoll) {
            return false;
        }
        // start polling:
        return true;
    }

    public void add(MessageData toAdd) {
        queue.addLast(toAdd);
        if (needToGenCharacter()) {
            // if we are calling add, then no need to send greeting.
            addOnlyCharacterToQueue(toAdd.userLanguage, toAdd.player);
        } else if (queue.size() > 3) {
            // discard if queue size is too much
            queue.poll();
        }
    }

    public void addOnlyCharacterToQueue(String userLanguage, ServerPlayerEntity player) {
        addCharacterAndMaybeGreeting(userLanguage, player, MessageDataType.GreetingAndCharacter);
    }

    // use to check if queue will generate character
    public boolean queueContainsCharacterGen() {
        if (queue.isEmpty()) {
            return false;
        }
        MessageDataType firstType = queue.getFirst().type;
        return firstType == MessageDataType.GreetingAndCharacter || firstType == MessageDataType.Character;
    }

    private void addCharacterAndMaybeGreeting(String userLanguage, ServerPlayerEntity player,
            MessageDataType type) {

        // GUARDS
        if (type != MessageDataType.Character && type != MessageDataType.GreetingAndCharacter) {
            throw new Error("Tried to call addCharacterAndMaybeGreeting with wrong type!");
        }
        if (!needToGenCharacter()) {
            // if character has been set, do not send greeting/regenerate character
            LOGGER.warn("Character name already set! Cancelling generateCharacter request");
            return;
        }

        if (queueContainsCharacterGen()) {
            if (queue.getFirst().type == MessageDataType.Character) {
                // already have Character queued up
                return;
            }
            // removes character gen so that can be replaced with greeting
            queue.pollFirst();
        }
        MessageData toAdd = MessageData.genCharacterAndOrGreetingMessage(userLanguage, player, entity, type);
        lastMessageData = toAdd;
        queue.addFirst(toAdd);
    }

    private boolean needToGenCharacter() {
        // prerequisite before anything:
        return characterName == null || characterName.equals("N/A");
    }

    // MAKE SURE THAT EXTERNAL ENTITY IS DIFFERENT FROM CURRENT WHEN CALLING THIS:
    public void addExternalEntityMessage(String userLanguage, ServerPlayerEntity player, String entityMessage,
            String entityCustomName, String entityName) {
        EventQueueManager.blacklistedEntityId = null;
        String newMessage = String.format("[%s the %s] said %s", entityCustomName, entityName, entityMessage);
        MessageData toAdd = new MessageData(userLanguage, player, newMessage, false, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void addUserMessage(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        EventQueueManager.blacklistedEntityId = null;
        LOGGER.info(String.format("EventQueueData/addUserMessage (%s) to entity (%s)", userMessage, entityId));
        MessageData toAdd = new MessageData(userLanguage, player, userMessage, is_auto_message, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public boolean shouldDelete() {
        if (lastMessageData != null && lastMessageData.player != null) {
            return ServerEntityFinder.getEntityByUUID(lastMessageData.player.getServerWorld(),
                    UUID.fromString(entityId)) == null;
        }
        return false;
    }

    public void poll() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) process event queue", entityId));
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);

        if (queueContainsCharacterGen()) {
            
        }
    }

    // SIDE EFFECTS
    public void onMessageGenerated(String message) {
        // TODO: BROADCAST ENTITY MESSAGE HERE:
    }

    public void onGreetingGenerated(String message) {
        onMessageGenerated(message);
    }

    public void onError(String errorMessage) {
        ServerPackets.SendClickableError(lastMessageData.player, errorMessage, "https://elefant.gg/discord");
    }
}
