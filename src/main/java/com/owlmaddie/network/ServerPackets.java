package com.owlmaddie.network;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.ChatDataSaverScheduler;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.goals.EntityBehaviorManager;
import com.owlmaddie.goals.GoalPriority;
import com.owlmaddie.goals.TalkPlayerGoal;
import com.owlmaddie.particle.LeadParticleEffect;
import com.owlmaddie.utils.Compression;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.utils.ServerEntityFinder;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The {@code ServerPackets} class provides methods to send packets to/from the client for generating greetings,
 * updating message details, and sending user messages.
 */
public class ServerPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static MinecraftServer serverInstance;
    public static ChatDataSaverScheduler scheduler = null;

    /**
     * Packet payload definitions used for network communication.
     */
    public record GreetingC2SPayload(String entityId, String language) implements CustomPayload {
        public static final Id<GreetingC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_greeting");
        public static final PacketCodec<PacketByteBuf, GreetingC2SPayload> CODEC =
                CustomPayload.codecOf(GreetingC2SPayload::write, GreetingC2SPayload::new);

        private GreetingC2SPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readString());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
            buf.writeString(this.language);
        }

        @Override
        public Id<GreetingC2SPayload> getId() { return ID; }
    }

    public record ReadNextC2SPayload(String entityId, int lineNumber) implements CustomPayload {
        public static final Id<ReadNextC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_read_next");
        public static final PacketCodec<PacketByteBuf, ReadNextC2SPayload> CODEC =
                CustomPayload.codecOf(ReadNextC2SPayload::write, ReadNextC2SPayload::new);

        private ReadNextC2SPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readInt());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
            buf.writeInt(this.lineNumber);
        }

        @Override
        public Id<ReadNextC2SPayload> getId() { return ID; }
    }

    public record SetStatusC2SPayload(String entityId, String status) implements CustomPayload {
        public static final Id<SetStatusC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_set_status");
        public static final PacketCodec<PacketByteBuf, SetStatusC2SPayload> CODEC =
                CustomPayload.codecOf(SetStatusC2SPayload::write, SetStatusC2SPayload::new);

        private SetStatusC2SPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readString());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
            buf.writeString(this.status);
        }

        @Override
        public Id<SetStatusC2SPayload> getId() { return ID; }
    }

    public record OpenChatC2SPayload(String entityId) implements CustomPayload {
        public static final Id<OpenChatC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_open_chat");
        public static final PacketCodec<PacketByteBuf, OpenChatC2SPayload> CODEC =
                CustomPayload.codecOf(OpenChatC2SPayload::write, OpenChatC2SPayload::new);

        private OpenChatC2SPayload(PacketByteBuf buf) {
            this(buf.readString());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
        }

        @Override
        public Id<OpenChatC2SPayload> getId() { return ID; }
    }

    public record CloseChatC2SPayload() implements CustomPayload {
        public static final Id<CloseChatC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_close_chat");
        public static final PacketCodec<PacketByteBuf, CloseChatC2SPayload> CODEC =
                CustomPayload.codecOf((buf, payload) -> {}, buf -> new CloseChatC2SPayload());

        @Override
        public Id<CloseChatC2SPayload> getId() { return ID; }
    }

    public record SendChatC2SPayload(String entityId, String message, String language) implements CustomPayload {
        public static final Id<SendChatC2SPayload> ID = CustomPayload.id("creaturechat:packet_c2s_send_chat");
        public static final PacketCodec<PacketByteBuf, SendChatC2SPayload> CODEC =
                CustomPayload.codecOf(SendChatC2SPayload::write, SendChatC2SPayload::new);

        private SendChatC2SPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readString(), buf.readString());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
            buf.writeString(this.message);
            buf.writeString(this.language);
        }

        @Override
        public Id<SendChatC2SPayload> getId() { return ID; }
    }

    public record EntityMessageS2CPayload(String entityId, String message, int lineNumber,
                                           String status, String sender,
                                           Map<String, PlayerData> players) implements CustomPayload {
        public static final Id<EntityMessageS2CPayload> ID = CustomPayload.id("creaturechat:packet_s2c_entity_message");
        public static final PacketCodec<PacketByteBuf, EntityMessageS2CPayload> CODEC =
                CustomPayload.codecOf(EntityMessageS2CPayload::write, EntityMessageS2CPayload::new);

        private EntityMessageS2CPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readString(), buf.readInt(), buf.readString(), buf.readString(), readPlayerDataMap(buf));
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.entityId);
            buf.writeString(this.message);
            buf.writeInt(this.lineNumber);
            buf.writeString(this.status);
            buf.writeString(this.sender);
            writePlayerDataMap(buf, this.players);
        }

        @Override
        public Id<EntityMessageS2CPayload> getId() { return ID; }
    }

    public record PlayerMessageS2CPayload(String senderUuid, String senderName, String message,
                                           boolean fromMinecraftChat) implements CustomPayload {
        public static final Id<PlayerMessageS2CPayload> ID = CustomPayload.id("creaturechat:packet_s2c_player_message");
        public static final PacketCodec<PacketByteBuf, PlayerMessageS2CPayload> CODEC =
                CustomPayload.codecOf(PlayerMessageS2CPayload::write, PlayerMessageS2CPayload::new);

        private PlayerMessageS2CPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readString(), buf.readString(), buf.readBoolean());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.senderUuid);
            buf.writeString(this.senderName);
            buf.writeString(this.message);
            buf.writeBoolean(this.fromMinecraftChat);
        }

        @Override
        public Id<PlayerMessageS2CPayload> getId() { return ID; }
    }

    public record LoginChunkS2CPayload(int sequenceNumber, int totalPackets, byte[] chunk) implements CustomPayload {
        public static final Id<LoginChunkS2CPayload> ID = CustomPayload.id("creaturechat:packet_s2c_login");
        public static final PacketCodec<PacketByteBuf, LoginChunkS2CPayload> CODEC =
                CustomPayload.codecOf(LoginChunkS2CPayload::write, LoginChunkS2CPayload::new);

        private LoginChunkS2CPayload(PacketByteBuf buf) {
            this(buf.readInt(), buf.readInt(), buf.readByteArray());
        }

        private void write(PacketByteBuf buf) {
            buf.writeInt(this.sequenceNumber);
            buf.writeInt(this.totalPackets);
            buf.writeByteArray(this.chunk);
        }

        @Override
        public Id<LoginChunkS2CPayload> getId() { return ID; }
    }

    public record WhitelistS2CPayload(List<String> whitelist, List<String> blacklist) implements CustomPayload {
        public static final Id<WhitelistS2CPayload> ID = CustomPayload.id("creaturechat:packet_s2c_whitelist");
        public static final PacketCodec<PacketByteBuf, WhitelistS2CPayload> CODEC =
                CustomPayload.codecOf(WhitelistS2CPayload::write, WhitelistS2CPayload::new);

        private WhitelistS2CPayload(PacketByteBuf buf) {
            this(readStringList(buf), readStringList(buf));
        }

        private void write(PacketByteBuf buf) {
            writeStringList(buf, this.whitelist);
            writeStringList(buf, this.blacklist);
        }

        @Override
        public Id<WhitelistS2CPayload> getId() { return ID; }
    }

    public record PlayerStatusS2CPayload(String playerUuid, boolean isChatOpen) implements CustomPayload {
        public static final Id<PlayerStatusS2CPayload> ID = CustomPayload.id("creaturechat:packet_s2c_player_status");
        public static final PacketCodec<PacketByteBuf, PlayerStatusS2CPayload> CODEC =
                CustomPayload.codecOf(PlayerStatusS2CPayload::write, PlayerStatusS2CPayload::new);

        private PlayerStatusS2CPayload(PacketByteBuf buf) {
            this(buf.readString(), buf.readBoolean());
        }

        private void write(PacketByteBuf buf) {
            buf.writeString(this.playerUuid);
            buf.writeBoolean(this.isChatOpen);
        }

        @Override
        public Id<PlayerStatusS2CPayload> getId() { return ID; }
    }

    private static List<String> readStringList(PacketByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new java.util.ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readString());
        }
        return list;
    }

    private static void writeStringList(PacketByteBuf buf, List<String> list) {
        buf.writeInt(list.size());
        for (String s : list) {
            buf.writeString(s);
        }
    }

    private static Map<String, PlayerData> readPlayerDataMap(PacketByteBuf buffer) {
        int size = buffer.readInt();
        Map<String, PlayerData> map = new java.util.HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buffer.readString();
            PlayerData data = new PlayerData();
            data.friendship = buffer.readInt();
            map.put(key, data);
        }
        return map;
    }


    public static void register() {
        // Register payload codecs
        PayloadTypeRegistry.playC2S().register(GreetingC2SPayload.ID, GreetingC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ReadNextC2SPayload.ID, ReadNextC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetStatusC2SPayload.ID, SetStatusC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(OpenChatC2SPayload.ID, OpenChatC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CloseChatC2SPayload.ID, CloseChatC2SPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SendChatC2SPayload.ID, SendChatC2SPayload.CODEC);

        PayloadTypeRegistry.playS2C().register(EntityMessageS2CPayload.ID, EntityMessageS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerMessageS2CPayload.ID, PlayerMessageS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LoginChunkS2CPayload.ID, LoginChunkS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WhitelistS2CPayload.ID, WhitelistS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerStatusS2CPayload.ID, PlayerStatusS2CPayload.CODEC);

        // Handle packet for Greeting
        ServerPlayNetworking.registerGlobalReceiver(GreetingC2SPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            String userLanguage = payload.language();

            ctx.server().execute(() -> {
                MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(ctx.player().getServerWorld(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getUuidAsString());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, ctx.player(), entity);
                    }
                }
            });
        });

        // Handle packet for reading lines of message
        ServerPlayNetworking.registerGlobalReceiver(ReadNextC2SPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            int lineNumber = payload.lineNumber();

            ctx.server().execute(() -> {
                MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(ctx.player().getServerWorld(), entityId);
                if (entity != null) {
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(ctx.player(), entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getUuidAsString());
                    LOGGER.debug("Update read lines to " + lineNumber + " for: " + entity.getType().toString());
                    chatData.setLineNumber(lineNumber);
                }
            });
        });

        // Handle packet for setting status of chat bubbles
        ServerPlayNetworking.registerGlobalReceiver(SetStatusC2SPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            String status_name = payload.status();

            ctx.server().execute(() -> {
                MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(ctx.player().getServerWorld(), entityId);
                if (entity != null) {
                    // Set talk to player goal (prevent entity from walking off)
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(ctx.player(), entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getUuidAsString());
                    LOGGER.debug("Hiding chat bubble for: " + entity.getType().toString());
                    chatData.setStatus(ChatDataManager.ChatStatus.valueOf(status_name));
                }
            });
        });

        // Handle packet for Open Chat
        ServerPlayNetworking.registerGlobalReceiver(OpenChatC2SPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            // AAA when you right click and open chat 
            ctx.server().execute(() -> {
                MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(ctx.player().getServerWorld(), entityId);
                if (entity != null) {
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(ctx.player(), entity, 7F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);
                }

                // Sync player UI status to all clients
                BroadcastPlayerStatus(ctx.player(), true);
            });
        });

        // Handle packet for Close Chat
        ServerPlayNetworking.registerGlobalReceiver(CloseChatC2SPayload.ID, (payload, ctx) -> {
            ctx.server().execute(() -> {
                // Sync player UI status to all clients
                BroadcastPlayerStatus(ctx.player(), false);
            });
        });

        // Handle packet for new chat message
        ServerPlayNetworking.registerGlobalReceiver(SendChatC2SPayload.ID, (payload, ctx) -> {
            UUID entityId = UUID.fromString(payload.entityId());
            String message = payload.message();
            String userLanguage = payload.language();

            ctx.server().execute(() -> {
                MobEntity entity = (MobEntity) ServerEntityFinder.getEntityByUUID(ctx.player().getServerWorld(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getUuidAsString());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, ctx.player(), entity);
                    } else {
                        // AAA server side generate llm response on entity
                        generate_chat(userLanguage, chatData, ctx.player(), entity, message, false);
                    }
                }
            });
        });

        // Send lite chat data JSON to new player (to populate client data)
        // Data is sent in chunks, to prevent exceeding the 32767 limit per String.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;

            // Send entire whitelist / blacklist to logged in player
            send_whitelist_blacklist(player);

            LOGGER.info("Server send compressed, chunked login message packets to player: " + player.getName().getString());
            // Get lite JSON data & compress to byte array
            String chatDataJSON = ChatDataManager.getServerInstance().GetLightChatData(player.getDisplayName().getString());
            byte[] compressedData = Compression.compressString(chatDataJSON);
            if (compressedData == null) {
                LOGGER.error("Failed to compress chat data.");
                return;
            }

            final int chunkSize = 32000; // Define chunk size
            int totalPackets = (int) Math.ceil((double) compressedData.length / chunkSize);

            // Loop through each chunk of bytes, and send bytes to player
            for (int i = 0; i < totalPackets; i++) {
                int start = i * chunkSize;
                int end = Math.min(compressedData.length, start + chunkSize);

                byte[] chunk = Arrays.copyOfRange(compressedData, start, end);
                ServerPlayNetworking.send(player, new LoginChunkS2CPayload(i, totalPackets, chunk));
            }
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            String world_name = world.getRegistryKey().getValue().getPath();
            if (world_name.equals("overworld")) {
                serverInstance = server;
                ChatDataManager.getServerInstance().loadChatData(server);

                // Start the auto-save task to save every X minutes
                scheduler = new ChatDataSaverScheduler();
                scheduler.startAutoSaveTask(server, 15, TimeUnit.MINUTES);
            }
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            String world_name = world.getRegistryKey().getValue().getPath();
            if (world_name == "overworld") {
                ChatDataManager.getServerInstance().saveChatData(server);
                serverInstance = null;

                // Shutdown auto scheduler
                scheduler.stopAutoSaveTask();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            String entityUUID = entity.getUuidAsString();
            if (entity.getRemovalReason() == Entity.RemovalReason.KILLED && ChatDataManager.getServerInstance().entityChatDataMap.containsKey(entityUUID)) {
                LOGGER.debug("Entity killed (" + entityUUID + "), updating death time stamp.");
                ChatDataManager.getServerInstance().entityChatDataMap.get(entityUUID).death = System.currentTimeMillis();
            }
        });

    }

    public static void send_whitelist_blacklist(ServerPlayerEntity player) {
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();

        List<String> whitelist = config.getWhitelist();
        List<String> blacklist = config.getBlacklist();

        WhitelistS2CPayload payload = new WhitelistS2CPayload(whitelist, blacklist);

        if (player != null) {
            LOGGER.info("Sending whitelist / blacklist packet to player: " + player.getName().getString());
            ServerPlayNetworking.send(player, payload);
        } else {
            for (ServerPlayerEntity serverPlayer : serverInstance.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(serverPlayer, payload);
            }
        }
    }

    public static void generate_character(String userLanguage, EntityChatData chatData, ServerPlayerEntity player, MobEntity entity) {
        // Set talk to player goal (prevent entity from walking off)
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        // Grab random adjective
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
        userMessageBuilder.append("This character is a ").append(randomClass).append(" class, who is ").append(randomAlignment).append(". ");
        if (entity.getCustomName() != null && !entity.getCustomName().getString().equals("N/A")) {
            userMessageBuilder.append("Their name is '").append(entity.getCustomName().getString()).append("'. ");
        } else {
            userMessageBuilder.append("Their name starts with the letter '").append(randomLetter)
                    .append("' and is ").append(randomSyllables).append(" syllables long. ");
        }
        userMessageBuilder.append("They speak in '").append(userLanguage).append("' with a ").append(randomSpeakingStyle).append(" style.");

        // Generate new character
        chatData.generateCharacter(userLanguage, player, userMessageBuilder.toString(), false);
    }

    public static void generate_chat(String userLanguage, EntityChatData chatData, ServerPlayerEntity player, MobEntity entity, String message, boolean is_auto_message) {
        // Set talk to player goal (prevent entity from walking off)
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        // Add new message
        chatData.generateMessage(userLanguage, player, message, is_auto_message);
    }

    // Writing a Map<String, PlayerData> to the buffer
    public static void writePlayerDataMap(PacketByteBuf buffer, Map<String, PlayerData> map) {
        buffer.writeInt(map.size()); // Write the size of the map
        for (Map.Entry<String, PlayerData> entry : map.entrySet()) {
            buffer.writeString(entry.getKey()); // Write the key (playerName)
            PlayerData data = entry.getValue();
            buffer.writeInt(data.friendship); // Write PlayerData field(s)
        }
    }

    // Send new message to all connected players
    public static void BroadcastEntityMessage(EntityChatData chatData) {
        // Log useful information before looping through all players
        LOGGER.info("Broadcasting entity message: entityId={}, status={}, currentMessage={}, currentLineNumber={}, senderType={}",
                chatData.entityId, chatData.status,
                chatData.currentMessage.length() > 24 ? chatData.currentMessage.substring(0, 24) + "..." : chatData.currentMessage,
                chatData.currentLineNumber, chatData.sender);

        for (ServerWorld world : serverInstance.getWorlds()) {
            // Find Entity by UUID and update custom name
            UUID entityId = UUID.fromString(chatData.entityId);
            MobEntity entity = (MobEntity)ServerEntityFinder.getEntityByUUID(world, entityId);
            if (entity != null) {
                String characterName = chatData.getCharacterProp("name");
                if (!characterName.isEmpty() && !characterName.equals("N/A") && entity.getCustomName() == null) {
                    LOGGER.debug("Setting entity name to " + characterName + " for " + chatData.entityId);
                    entity.setCustomName(Text.literal(characterName));
                    entity.setCustomNameVisible(true);
                    entity.setPersistent();
                }
            }

            // Make auto-generated message appear as a pending icon (attack, show/give, arrival)
            if (chatData.sender == ChatDataManager.ChatSender.USER && chatData.auto_generated > 0) {
                chatData.status = ChatDataManager.ChatStatus.PENDING;
            }

            // Iterate over all players and send the packet
            for (ServerPlayerEntity player : serverInstance.getPlayerManager().getPlayerList()) {
                EntityMessageS2CPayload payload = new EntityMessageS2CPayload(
                        chatData.entityId,
                        chatData.currentMessage,
                        chatData.currentLineNumber,
                        chatData.status.toString(),
                        chatData.sender.toString(),
                        chatData.players);
                ServerPlayNetworking.send(player, payload);
            }
            break;
        }
    }

    // Send new message to all connected players
    public static void BroadcastPlayerMessage(EntityChatData chatData, ServerPlayerEntity sender, boolean fromMinecraftChat) {
        // Log the specific data being sent
        LOGGER.info("Broadcasting player message: senderUUID={}, message={}", sender.getUuidAsString(),
                chatData.currentMessage);

        PlayerMessageS2CPayload payload = new PlayerMessageS2CPayload(
                sender.getUuidAsString(),
                sender.getDisplayName().getString(),
                chatData.currentMessage,
                fromMinecraftChat);

        for (ServerPlayerEntity serverPlayer : serverInstance.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    // Send new message to all connected players
    public static void BroadcastPlayerStatus(PlayerEntity player, boolean isChatOpen) {
        PlayerStatusS2CPayload payload = new PlayerStatusS2CPayload(player.getUuidAsString(), isChatOpen);

        for (ServerPlayerEntity serverPlayer : serverInstance.getPlayerManager().getPlayerList()) {
            LOGGER.debug("Server broadcast " + player.getName().getString() + " player status to client: " + serverPlayer.getName().getString() + " | isChatOpen: " + isChatOpen);
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }

    // Send a chat message to all players (i.e. death message)
    public static void BroadcastMessage(Text message) {
        for (ServerPlayerEntity serverPlayer : serverInstance.getPlayerManager().getPlayerList()) {
            serverPlayer.sendMessage(message, false);
        };
    }

    // Send a chat message to a player which is clickable (for error messages with a link for help)
    public static void SendClickableError(PlayerEntity player, String message, String url) {
        MutableText text = Text.literal(message)
                .formatted(Formatting.RED)
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                        .withUnderline(true));
        player.sendMessage(text, false);
    }

    // Send a clickable message to ALL Ops
    public static void sendErrorToAllOps(MinecraftServer server, String message) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // Check if the player is an operator
            if (server.getPlayerManager().isOperator(player.getGameProfile())) {
                ServerPackets.SendClickableError(player, message, "https://elefant.gg/discord");
            }
        }
    }
}
