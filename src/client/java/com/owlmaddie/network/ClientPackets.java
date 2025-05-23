package com.owlmaddie.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.ui.BubbleRenderer;
import com.owlmaddie.ui.PlayerMessageManager;
import com.owlmaddie.utils.ClientEntityFinder;
import com.owlmaddie.utils.Decompression;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code ClientPackets} class provides methods to send packets to/from the server for generating greetings,
 * updating message details, and sending user messages.
 */
public class ClientPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    static HashMap<Integer, byte[]> receivedChunks = new HashMap<>();

    public static void sendGenerateGreeting(Entity entity) {
        // Get user language
        String userLanguageCode = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        String userLanguageName = MinecraftClient.getInstance().getLanguageManager().getLanguage(userLanguageCode).getDisplayText().getString();

        ClientPlayNetworking.send(new ServerPackets.GreetingC2SPayload(entity.getUuidAsString(), userLanguageName));
    }

    public static void sendUpdateLineNumber(Entity entity, Integer lineNumber) {
        ClientPlayNetworking.send(new ServerPackets.ReadNextC2SPayload(entity.getUuidAsString(), lineNumber));
    }

    public static void sendOpenChat(Entity entity) {
        ClientPlayNetworking.send(new ServerPackets.OpenChatC2SPayload(entity.getUuidAsString()));
    }

    public static void sendCloseChat() {
        ClientPlayNetworking.send(new ServerPackets.CloseChatC2SPayload());
    }

    public static void setChatStatus(Entity entity, ChatDataManager.ChatStatus new_status) {
        ClientPlayNetworking.send(new ServerPackets.SetStatusC2SPayload(entity.getUuidAsString(), new_status.toString()));
    }

    public static void sendChat(Entity entity, String message) {
        // AAA use this to actually send a chat msg to an entity.
        // Get user language
        String userLanguageCode = MinecraftClient.getInstance().getLanguageManager().getLanguage();
        String userLanguageName = MinecraftClient.getInstance().getLanguageManager().getLanguage(userLanguageCode).getDisplayText().getString();

        ClientPlayNetworking.send(new ServerPackets.SendChatC2SPayload(entity.getUuidAsString(), message, userLanguageName));
    }


    public static void register() {
        PayloadTypeRegistry.playS2C().register(ServerPackets.EntityMessageS2CPayload.ID, ServerPackets.EntityMessageS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPackets.PlayerMessageS2CPayload.ID, ServerPackets.PlayerMessageS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPackets.LoginChunkS2CPayload.ID, ServerPackets.LoginChunkS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPackets.WhitelistS2CPayload.ID, ServerPackets.WhitelistS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerPackets.PlayerStatusS2CPayload.ID, ServerPackets.PlayerStatusS2CPayload.CODEC);

        // Client-side packet handler, message sync
        ClientPlayNetworking.registerGlobalReceiver(ServerPackets.EntityMessageS2CPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            String message = payload.message();
            int line = payload.lineNumber();
            ChatDataManager.ChatStatus status = ChatDataManager.ChatStatus.valueOf(payload.status());
            ChatDataManager.ChatSender sender = ChatDataManager.ChatSender.valueOf(payload.sender());
            Map<String, PlayerData> players = payload.players();

            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.world == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for entity '{}'.", entityId);
                    return;
                }

                // Get entity chat data for current entity & player
                ChatDataManager chatDataManager = ChatDataManager.getClientInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(entityId.toString());

                // Add entity message
                if (!message.isEmpty()) {
                    chatData.currentMessage = message;
                }
                chatData.currentLineNumber = line;
                chatData.status = status;
                chatData.sender = sender;
                chatData.players = players;

                // Play sound with volume based on distance (from player or entity)
                MobEntity entity = ClientEntityFinder.getEntityByUUID(client.world, entityId);
                if (entity != null) {
                    playNearbyUISound(client, entity, 0.2f);
                }
            });
        });

        // Client-side packet handler, message sync
        ClientPlayNetworking.registerGlobalReceiver(ServerPackets.PlayerMessageS2CPayload.ID, (payload, ctx) -> {
            UUID senderPlayerId = UUID.fromString(payload.senderUuid());
            String senderPlayerName = payload.senderName();
            String message = payload.message();
            boolean fromMinecraftChat = payload.fromMinecraftChat();

            // Update the chat data manager on the client-side
            client.execute(() -> { // Make sure to run on the client thread
                // Ensure client.player is initialized
                if (client.player == null || client.world == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for sender '{}'.", senderPlayerId);
                    return;
                }
                // AAA trigger for player message
                LOGGER.info("Player message" + message);

                // Add player message to queue for rendering
                PlayerMessageManager.addMessage(senderPlayerId, message, senderPlayerName, ChatDataManager.TICKS_TO_DISPLAY_USER_MESSAGE);

                // if the msg was from minecraft's chat, and this is the client for that player, then send to nearest entity with bubble open.
                if(fromMinecraftChat && senderPlayerName.equals(client.player.getName().getString())){
                    Optional<Entity> entityToSendChatTo = ClientEntityFinder.getClosestEntityToPlayerWithChatBubbleOpen();
                    entityToSendChatTo.ifPresent(entity -> {
                        ClientPackets.sendChat(entity, message);
                    });
                }
            });
        });

        // Client-side player login: get all chat data
        ClientPlayNetworking.registerGlobalReceiver(ServerPackets.LoginChunkS2CPayload.ID, (payload, ctx) -> {
            int sequenceNumber = payload.sequenceNumber();
            int totalPackets = payload.totalPackets();
            byte[] chunk = payload.chunk();

            client.execute(() -> { // Make sure to run on the client thread
                // Store the received chunk
                receivedChunks.put(sequenceNumber, chunk);

                // Check if all chunks have been received
                if (receivedChunks.size() == totalPackets) {
                    LOGGER.info("Reassemble chunks on client and decompress lite JSON data string");

                    // Combine all byte array chunks
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    for (int i = 0; i < totalPackets; i++) {
                        combined.write(receivedChunks.get(i), 0, receivedChunks.get(i).length);
                    }

                    // Decompress the combined byte array to get the original JSON string
                    String chatDataJSON = Decompression.decompressString(combined.toByteArray());
                    if (chatDataJSON == null || chatDataJSON.isEmpty()) {
                        LOGGER.warn("Received invalid or empty chat data JSON. Skipping processing.");
                        return;
                    }

                    // Parse JSON and update client chat data
                    Gson GSON = new Gson();
                    Type type = new TypeToken<ConcurrentHashMap<String, EntityChatData>>(){}.getType();
                    ChatDataManager.getClientInstance().entityChatDataMap = GSON.fromJson(chatDataJSON, type);

                    // Clear receivedChunks for future use
                    receivedChunks.clear();
                }
            });
        });

        // Client-side packet handler, receive entire whitelist / blacklist, and update BubbleRenderer
        ClientPlayNetworking.registerGlobalReceiver(ServerPackets.WhitelistS2CPayload.ID, (payload, ctx) -> {
            List<String> whitelist = payload.whitelist();
            List<String> blacklist = payload.blacklist();

            client.execute(() -> {
                BubbleRenderer.whitelist = whitelist;
                BubbleRenderer.blacklist = blacklist;
            });
        });

        // Client-side packet handler, player status sync
        ClientPlayNetworking.registerGlobalReceiver(ServerPackets.PlayerStatusS2CPayload.ID, (payload, ctx) -> {
            UUID playerId = UUID.fromString(payload.playerUuid());
            boolean isChatOpen = payload.isChatOpen();

            // Get player instance
            PlayerEntity player = ClientEntityFinder.getPlayerEntityFromUUID(playerId);

            // Update the player status data manager on the client-side
            client.execute(() -> {
                if (player == null) {
                    LOGGER.warn("Player entity is null. Skipping status update.");
                    return;
                }

                if (isChatOpen) {
                    PlayerMessageManager.openChatUI(playerId);
                    playNearbyUISound(client, player, 0.2f);
                } else {
                    PlayerMessageManager.closeChatUI(playerId);
                }
            });
        });
    }

    private static void playNearbyUISound(MinecraftClient client, Entity player, float maxVolume) {
        // Play sound with volume based on distance
        int distance_squared = 144;
        if (client.player != null) {
            double distance = client.player.squaredDistanceTo(player.getX(), player.getY(), player.getZ());
            if (distance <= distance_squared) {
                // Decrease volume based on distance
                float volume = maxVolume - (float)distance / distance_squared * maxVolume;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), volume, 0.8F);
            }
        }
    }
}

