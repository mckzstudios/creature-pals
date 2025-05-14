package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
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
    String characterName = null;

    static enum MessageDataType {
        GreetingAndCharacter, Character, Normal
    }

    private class MessageData {
        public String userLanguage;
        public String userMessage;
        public boolean is_auto_message;
        public ServerPlayerEntity player;
        public MessageDataType type;

        public MessageData(String userLanguage, ServerPlayerEntity player, String userMessage,
                boolean is_auto_message, MessageDataType type) {
            this.is_auto_message = is_auto_message;
            this.userLanguage = userLanguage;
            this.player = player;
            this.userMessage = userMessage;
            this.type = type;
        }
    }

    public EventQueueData(String entityId, Entity entity) {
        this.entityId = entityId;
        this.entity = entity;
        eventQueue = new ArrayDeque<>();
        lastTimePolled = System.nanoTime();
        // poll between 1000 ms -> 1500s
        randomInterval = ThreadLocalRandom.current().nextLong(1_000_000_000L, 1_500_000_001L);
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
                lastMessageData.player.distanceTo(entity) < EventQueueManager.maxDistance &&
                !EventQueueManager.shouldWaitBecauseOfError();
        if (!shouldPoll) {
            return false;
        }
        // start polling:
        return true;
    }

    public void add(MessageData toAdd) {
        eventQueue.addLast(toAdd);
        // if there is a message, and we need to 
        if (needToGenCharacter() && !isGreetingInQueue()) {
            addCharacterAndMaybeGreeting(toAdd.userLanguage, toAdd.player, MessageDataType.Character);
        } else if (eventQueue.size() > 3) {
            // discard if queue size is too much
            eventQueue.poll();
        }
    }

    // MAKE SURE THAT EXTERNAL ENTITY IS DIFFERENT FROM CURRENT WHEN CALLING THIS:
    public void addExternalEntityMessage(String userLanguage, ServerPlayerEntity player, String entityMessage,
            String entityCustomName, String entityName) {
        String newMessage = String.format("[%s the %s] said %s", entityCustomName, entityName, entityMessage);
        MessageData toAdd = new MessageData(userLanguage, player, newMessage, false, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void addUserMessage(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        LOGGER.info(String.format("EventQueueData/addUserMessage (%s) to entity (%s)", userMessage, entityId));
        MessageData toAdd = new MessageData(userLanguage, player, userMessage, is_auto_message, MessageDataType.Normal);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public boolean isGreetingInQueue() {
        if (eventQueue.isEmpty())
            return false;
        return eventQueue.getFirst().type == MessageDataType.GreetingAndCharacter;
    }

    // use to check if queue will generate character
    public boolean queueContainsCharacterGen() {
        if (eventQueue.isEmpty()) {
            return false;
        }
        MessageDataType firstType = eventQueue.getFirst().type;
        return firstType == MessageDataType.GreetingAndCharacter || firstType == MessageDataType.Character;
    }

    public boolean needToGenCharacter() {
        // prerequisite before anything:
        return characterName == null || characterName.equals("N/A");
    }

    public void addGreeting(String userLanguage, ServerPlayerEntity player) {
        addCharacterAndMaybeGreeting(userLanguage, player, MessageDataType.GreetingAndCharacter);
    }

    private void addCharacterAndMaybeGreeting(String userLanguage, ServerPlayerEntity player,
            MessageDataType type) {
        if (type != MessageDataType.Character && type != MessageDataType.GreetingAndCharacter) {
            throw new Error("Tried to call addCharacterAndMaybeGreeting with wrong type!");
        }

        if (!needToGenCharacter()) {
            // if character has been set, do not send greeting/regenerate character
            LOGGER.warn("Character name already set! Cancelling generateCharacter request");
            return;
        }

        if (queueContainsCharacterGen()) {
            if (eventQueue.getFirst().type == MessageDataType.GreetingAndCharacter) {
                // already have greeting queued up
                return;
            }
            // removes character gen so that can be replaced with greeting
            eventQueue.pollFirst();
        }
        // make sure greeting is first in queue:
        String randomAdjective = Randomizer.getRandomMessage(Randomizer.RandomType.ADJECTIVE);
        String randomClass = Randomizer.getRandomMessage(Randomizer.RandomType.CLASS);
        String randomAlignment = Randomizer.getRandomMessage(Randomizer.RandomType.ALIGNMENT);
        String randomSpeakingStyle = Randomizer.getRandomMessage(Randomizer.RandomType.SPEAKING_STYLE);

        // Generate random name parameters
        String randomLetter = Randomizer.RandomLetter();
        int randomSyllables = Randomizer.RandomNumber(5) + 1;

        // Build the message
        StringBuilder userMessageBuilder = new StringBuilder();
        userMessageBuilder.append("Please generate a ").append(randomAdjective).append(" character. ");
        userMessageBuilder.append("This character is a ").append(randomClass).append(" class, who is ")
                .append(randomAlignment).append(". ");
        if (entity.getCustomName() != null && !entity.getCustomName().getString().equals("N/A")) {
            userMessageBuilder.append("Their name is '").append(entity.getCustomName().getString()).append("'. ");
        } else {
            userMessageBuilder.append("Their name starts with the letter '").append(randomLetter)
                    .append("' and is ").append(randomSyllables).append(" syllables long. ");
        }
        userMessageBuilder.append("They speak in '").append(userLanguage).append("' with a ")
                .append(randomSpeakingStyle).append(" style.");
        MessageData toAdd = new MessageData(userLanguage, player, userMessageBuilder.toString(), false, type);
        lastMessageData = toAdd;
        eventQueue.addFirst(toAdd);
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
            MessageData greetingMsg = eventQueue.poll();

            switch (greetingMsg.type) {
                case GreetingAndCharacter:
                    LOGGER.info(String.format(
                            "EventQueueData/injectOnServerTick(entity %s) generating greeting and Char", entityId));
                    LOGGER.info(String.format("... usr msg for greeting: (%s)", greetingMsg.userMessage));
                    chatData.generateCharacterAndSendGreeting(greetingMsg.userLanguage, greetingMsg.player,
                            greetingMsg.userMessage,
                            greetingMsg.is_auto_message, (characterName) -> {
                                EventQueueManager.llmProcessing = false;
                                LOGGER.info(String.format(
                                        "EventQueueData/injectOnServerTick(entity %s) generated greeting and char with name (%s)",
                                        entityId, characterName));
                                if (characterName != "N/A") {
                                    this.characterName = characterName;
                                }
                            }, (errMsg) -> {
                                EventQueueManager.llmProcessing = false;

                                LOGGER.info(String.format(
                                        "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                                        entityId, errMsg));
                                EventQueueManager.onError();
                            });
                    // only generate greeting, wait for poll again
                    lastTimePolled = System.nanoTime();
                    break;
                case Character:
                    chatData.generateCharacter(greetingMsg.userLanguage, greetingMsg.player, greetingMsg.userMessage,
                            greetingMsg.is_auto_message, (characterName) -> {
                                EventQueueManager.llmProcessing = false;
                                LOGGER.info(String.format(
                                        "EventQueueData/injectOnServerTick(entity %s) generated character with name (%s)",
                                        entityId, characterName));
                                if (characterName != "N/A") {
                                    this.characterName = characterName;
                                }
                            }, (errMsg) -> {
                                EventQueueManager.llmProcessing = false;
                                LOGGER.info(String.format(
                                        "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                                        entityId, errMsg));
                                EventQueueManager.onError();
                            });
                    // only generate greeting, wait for poll again
                    lastTimePolled = System.nanoTime();
                    break;
                default:
                    throw new Error("greetinMsg is not an actual greeting/characterGen");
            }

            return;
        }
        while (!eventQueue.isEmpty()) {
            // add all messages to queue if should poll
            MessageData cur = eventQueue.poll();
            chatData.addMessage(cur.userMessage, ChatDataManager.ChatSender.USER, cur.player, "system-chat");
        }
        // if (eventQueue.isEmpty()) {
        // if just added last message:

        LOGGER.info(
                String.format("EventQueueData/injectOnServerTick(entity %s) done polling, generating msg", entityId));

        chatData.generateMessage(lastMessageData.userLanguage, lastMessageData.player,
                lastMessageData.is_auto_message, message -> {
                    EventQueueManager.llmProcessing = false;
                    LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) generated message (%s)",
                            entityId, message));
                    lastTimePolled = System.nanoTime();
                }, errMsg -> {
                    EventQueueManager.llmProcessing = false;
                    LOGGER.info(String.format(
                            "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                            entityId, errMsg));
                    EventQueueManager.onError();
                    lastTimePolled = System.nanoTime();
                });
        chatData.logConversationHistory();
    }

}
