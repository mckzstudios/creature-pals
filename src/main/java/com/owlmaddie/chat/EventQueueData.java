package com.owlmaddie.chat;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.ArrayDeque;
import java.util.Deque;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.utils.Randomizer;

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

    private class MessageData {
        public String userLanguage;
        public String userMessage;
        public boolean is_auto_message;
        public ServerPlayerEntity player;
        public boolean is_gen_char_msg;

        public MessageData(String userLanguage, ServerPlayerEntity player, String userMessage,
                boolean is_auto_message, boolean is_gen_char_msg) {
            this.is_auto_message = is_auto_message;
            this.userLanguage = userLanguage;
            this.player = player;
            this.userMessage = userMessage;
            this.is_gen_char_msg = is_gen_char_msg;
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
        MessageData toAdd = new MessageData(userLanguage, player, newMessage, false, false);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public void addUserMessage(String userLanguage, ServerPlayerEntity player, String userMessage,
            boolean is_auto_message) {
        LOGGER.info(String.format("EventQueueData/addUserMessage (%s) to entity (%s)", userMessage, entityId));
        MessageData toAdd = new MessageData(userLanguage, player, userMessage, is_auto_message, false);
        add(toAdd);
        lastMessageData = toAdd;
    }

    public boolean isGreetingInQueue() {
        if (eventQueue.isEmpty())
            return false;
        return eventQueue.getFirst().is_gen_char_msg;
    }

    public void addGreetingIfNeeded(String userLanguage, EntityChatData chatData, ServerPlayerEntity player,
            MobEntity entity) {
        if (isGreetingInQueue() || !chatData.characterSheet.isEmpty()) {
            // if already have a greeting then dont generate.
            // also if its already in queue dont add another one.
            LOGGER.info("[EventQueueData/addGreetingIfNeeded]: Not generating greeting because one already exists.");
            return;
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
        MessageData toAdd = new MessageData(userLanguage, player, userMessageBuilder.toString(), false, true);
        lastMessageData = toAdd;
        eventQueue.addFirst(toAdd);

    }

    public void poll() {
        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) process event queue", entityId));
        EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entityId);
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(lastMessageData.player, (MobEntity) entity, 3.5F);
        EntityBehaviorManager.addGoal((MobEntity) entity, talkGoal, GoalPriority.TALK_PLAYER);
        if (isGreetingInQueue()) {
            String.format("EventQueueData/injectOnServerTick(entity %s) generating greeting", entityId);
            MessageData greetingMsg = eventQueue.poll();
            chatData.generateCharacter(greetingMsg.userLanguage, greetingMsg.player, greetingMsg.userMessage,
                    greetingMsg.is_auto_message, (message) -> {
                        EventQueueManager.llmProcessing = false;

                        LOGGER.info(String.format("EventQueueData/injectOnServerTick(entity %s) generated message (%s)",
                                entityId, message));

                    }, (errMsg) -> {
                        EventQueueManager.llmProcessing = false;

                        LOGGER.info(String.format(
                                "EventQueueData/injectOnServerTick(entity %s) ERROR GENERATING MESSAGE: errMsg: (%s)",
                                entityId, errMsg));
                        EventQueueManager.onError();
                    });
            // only generate greeting, wait for poll again
            lastTimePolled = System.nanoTime();
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
    }

}
