package com.owlmaddie.ui;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.skin.PlayerCustomTexture;
import com.owlmaddie.utils.EntityHeights;
import com.owlmaddie.utils.EntityRendererUUID;
import com.owlmaddie.utils.TextureLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.owlmaddie.ui.BubblePipeline.getBubbleLayer;


public abstract class BubbleRenderer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends FeatureRenderer<S, M> {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    protected static TextureLoader TEXTURES = new TextureLoader();
    public static int DISPLAY_PADDING = 2;
    public static int ANIMATION_FRAME = 0;
    public static long LAST_TICK = 0;
    public static int OVERLAY = OverlayTexture.DEFAULT_UV;
    public static List<Identifier> WHITELIST = new ArrayList<>();
    public static List<Identifier> BLACKLIST = new ArrayList<>();
    private static int QUERY_ENTITY_DATA_COUNT = 0;
    private static List<LivingEntity> RELEVANT_ENTITIES;
    public BubbleRenderer(M model) {
        super(new BubbleRendererContext<S,M>() {
            @Override
            public M getModel() {
                return model;
            }
        });
    }

    public static void drawTextBubbleBackground(String base_name, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, float x, float y, float width,
                                                float height, int friendship, int light) {
        // Set shader & texture
        //GlStateManager._setShader(GameRenderer::getPositionColorTexLightmapProgram);

        // Enable depth test and blending
        GlStateManager._enableBlend();
        //GlStateManager._defaultBlendFunc();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);

        float z = 0.01F;

        // Draw UI text background (based on friendship)
        // Draw TOP
        if (friendship == -3 && !base_name.endsWith("-player")) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI(base_name + "-enemy")));
            drawTexturePart(matrices, vertexConsumer, model,x - 50, y, z, 228, 40, light);
        } else if (friendship == 3 && !base_name.endsWith("-player")) {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI(base_name + "-friend")));
            drawTexturePart(matrices, vertexConsumer, model,x - 50, y, z, 228, 40, light);

        } else {
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI(base_name)));
            drawTexturePart(matrices, vertexConsumer, model,x - 50, y, z, 228, 40,light);

        }


        // Draw MIDDLE

        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI("text-middle")));
        drawTexturePart(matrices,vertexConsumer,model, x, y + 40, z, width, height,light);

        // Draw BOTTOM
        vertexConsumer = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI("text-bottom")));
        drawTexturePart(matrices,vertexConsumer,model, x, y + 40 + height, z, width, 5,light);

        // Disable blending and depth test
        GlStateManager._disableBlend();
        GlStateManager._disableDepthTest();
    }

    private static void drawTexturePart(MatrixStack matrices, VertexConsumer buffer, EntityModel<?> model, float x, float y, float z,
                                        float width, float height, int light) {

        // Define the vertices with color, texture, light, and overlay
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Begin drawing quads with the correct vertex format

        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1); // bottom left
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1); // bottom right
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0); // top right
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0); // top
        // left
        model.render(matrices, buffer, light, OVERLAY);

    }

    public static void drawIcon(String ui_icon_name, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, float x, float y, float width, float height, int light) {
        // Draw button icon

        matrices.push();
        RenderPhase.Texture button_texture = TEXTURES.GetUI(ui_icon_name);
        // Set shader & texture

        RenderSystem.setShaderTexture(0, MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.of("creaturechat", "textures/ui/" + ui_icon_name)).getGlTexture());
        //RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(MinecraftClient.getInstance().getTextureManager().getTexture(Identifier.of("creaturechat", "textures/ui/" + ui_icon_name)).getGlTexture(), OptionalInt.of(0xFFFFFFFF));
        // Enable depth test and blending
        GlStateManager._enableBlend();
        //GlStateManager.defaultBlendFunc();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);

        VertexConsumer buffer = vertexConsumerProvider.getBuffer(getBubbleLayer(button_texture));
        // Get the current matrix position
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Begin drawing quads with the correct vertex format

        buffer.vertex(matrix4f, x, y + height, 0.0F).color(255, 255, 255, 255).texture(0, 1).light(light)
                .overlay(OVERLAY); // bottom left
        buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(255, 255, 255, 255).texture(1, 1).light(light)
                .overlay(OVERLAY); // bottom right
        buffer.vertex(matrix4f, x + width, y, 0.0F).color(255, 255, 255, 255).texture(1, 0).light(light)
                .overlay(OVERLAY); // top right
        buffer.vertex(matrix4f, x, y, 0.0F).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(OVERLAY); // top left

        model.render(matrices, buffer, light, OVERLAY);

        matrices.pop();

        // Disable blending and depth test
        GlStateManager._disableBlend();
        GlStateManager._disableDepthTest();
    }


    private static void drawFriendshipStatus(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, float x, float y, float width, float height,
                                             int friendship, int light) {
        // dynamically calculate friendship ui image name
        String ui_icon_name = "friendship" + friendship;

        // Set shader
        //GlStateManager._setShader(GameRenderer::getPositionColorTexLightmapProgram);

        // Set texture

        // Enable depth test and blending
        GlStateManager._enableBlend();
        //GlStateManager._defaultBlendFunc();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);

        VertexConsumer bufferBuilder = vertexConsumerProvider.getBuffer(getBubbleLayer(TEXTURES.GetUI(ui_icon_name)));

        // Get the current matrix position
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Begin drawing quads with the correct vertex format

        float z = -0.01F;
        bufferBuilder.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1); // bottom left
        bufferBuilder.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1); // bottom right
        bufferBuilder.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0); // top right
        bufferBuilder.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0); // top left

        // Disable blending and depth test
        GlStateManager._disableBlend();
        GlStateManager._disableDepthTest();
        model.render(matrices, bufferBuilder, light, OVERLAY);
    }

    private static void drawEntityIcon(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, LivingEntityRenderState entity, MinecraftClient client, float x, float y, float width,
                                       float height, int light) {
        // Get entity renderer
        String entity_icon_path = getTextureIdentifier(client,entity).getPath();


        // Draw face icon
        RenderPhase.Texture entity_id = TEXTURES.GetEntity(entity_icon_path);
        if (entity_id == null) {
            return;
        }

        // Set shader & texture
        //GlStateManager._setShader(GameRenderer::getPositionColorTexLightmapProgram);

        // Enable depth test and blending
        GlStateManager._enableBlend();
        // GlStateManager._defaultBlendFunc();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);

        VertexConsumer bufferBuilder = vertexConsumerProvider.getBuffer(getBubbleLayer(entity_id));

        // Get the current matrix position
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        // Begin drawing quads with the correct vertex format

        float z = -0.01F;
        bufferBuilder.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light)
                .overlay(OVERLAY); // bottom left
        bufferBuilder.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light)
                .overlay(OVERLAY); // bottom right
        bufferBuilder.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light)
                .overlay(OVERLAY); // top right
        bufferBuilder.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(OVERLAY); // top left

        System.out.println("Drawing entity icon: " + entity_icon_path);


        // Disable blending and depth test
        GlStateManager._disableBlend();
        GlStateManager._disableDepthTest();
        model.render(matrices, bufferBuilder, light, OVERLAY);

    }

    private static void drawPlayerIcon(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, LivingEntityRenderState entityRenderState, MinecraftClient client, float x, float y, float width,
                                       float height, int light) {
        // Get player skin texture
        Identifier playerTexture = getTextureIdentifier(client, entityRenderState);

        // Check for black and white pixels (using the Mixin-based check)
        boolean customSkinFound = PlayerCustomTexture.hasCustomIcon(playerTexture);

        // Set shader & texture
        //GlStateManager.setShader(GameRenderer::getPositionColorTexLightmapProgram);

        RenderSystem.setShaderTexture(0, MinecraftClient.getInstance().getTextureManager().getTexture(playerTexture).getGlTexture());

        // Enable depth test and blending
        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);

        VertexConsumer bufferBuilder = vertexConsumerProvider.getBuffer(getBubbleLayer(new RenderPhase.Texture(playerTexture, TriState.FALSE, false)));

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float z = -0.01F;

        if (customSkinFound) {
            // Hidden icon UV coordinates
            float[][] newCoordinates = {
                    { 0.0F, 0.0F, 8.0F, 8.0F, 0F, 0F }, // Row 1 left
                    { 24.0F, 0.0F, 32.0F, 8.0F, 8F, 0F }, // Row 1 middle
                    { 32.0F, 0.0F, 40.0F, 8.0F, 16F, 0F }, // Row 1 right
                    { 56.0F, 0.0F, 64.0F, 8.0F, 0F, 8F }, // Row 2 left
                    { 56.0F, 20.0F, 64.0F, 28.0F, 8F, 8F }, // Row 2 middle
                    { 36.0F, 16.0F, 44.0F, 20.0F, 16F, 8F }, // Row 2 right top
                    { 56.0F, 16.0F, 64.0F, 20.0F, 16F, 12F }, // Row 2 right bottom
                    { 56.0F, 28.0F, 64.0F, 36.0F, 0F, 16F }, // Row 3 left
                    { 56.0F, 36.0F, 64.0F, 44.0F, 8F, 16F }, // Row 3 middle
                    { 56.0F, 44.0F, 64.0F, 48, 16F, 16F }, // Row 3 top right
                    { 12.0F, 48.0F, 20.0F, 52, 16F, 20F }, // Row 3 bottom right
            };
            float scaleFactor = 0.77F;

            for (float[] coords : newCoordinates) {
                float newU1 = coords[0] / 64.0F;
                float newV1 = coords[1] / 64.0F;
                float newU2 = coords[2] / 64.0F;
                float newV2 = coords[3] / 64.0F;

                float offsetX = coords[4] * scaleFactor;
                float offsetY = coords[5] * scaleFactor;
                float scaledX = x + offsetX;
                float scaledY = y + offsetY;
                float scaledWidth = (coords[2] - coords[0]) * scaleFactor;
                float scaledHeight = (coords[3] - coords[1]) * scaleFactor;

                bufferBuilder.vertex(matrix4f, scaledX, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU1, newV2).light(light).overlay(OVERLAY);
                bufferBuilder.vertex(matrix4f, scaledX + scaledWidth, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU2, newV2).light(light).overlay(OVERLAY);
                bufferBuilder.vertex(matrix4f, scaledX + scaledWidth, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU2, newV1).light(light).overlay(OVERLAY);
                bufferBuilder.vertex(matrix4f, scaledX, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU1, newV1).light(light).overlay(OVERLAY);
            }
        } else {
            // make skin appear smaller and centered
            x += 2;
            y += 2;
            width -= 4;
            height -= 4;

            // Normal face coordinates
            float u1 = 8.0F / 64.0F;
            float v1 = 8.0F / 64.0F;
            float u2 = 16.0F / 64.0F;
            float v2 = 16.0F / 64.0F;

            bufferBuilder.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(u1, v2).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(u2, v2).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(u2, v1).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(u1, v1).light(light).overlay(OVERLAY);

            // Hat layer
            float hatU1 = 40.0F / 64.0F;
            float hatV1 = 8.0F / 64.0F;
            float hatU2 = 48.0F / 64.0F;
            float hatV2 = 16.0F / 64.0F;

            z -= 0.01F;

            bufferBuilder.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV2).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV2).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV1).light(light).overlay(OVERLAY);
            bufferBuilder.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV1).light(light).overlay(OVERLAY);
        }

        // Disable blending and depth test
        GlStateManager._disableBlend();
        GlStateManager._disableDepthTest();
        model.render(matrices, bufferBuilder, light, OVERLAY);
    }

    private static void drawMessageText(Matrix4f matrix, List<String> lines, int starting_line, int ending_line,
                                        VertexConsumerProvider immediate, float lineSpacing, int fullBright, float yOffset) {
        TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
        int currentLineIndex = 0; // We'll use this to track which line we're on

        for (String lineText : lines) {
            // Only draw lines that are within the specified range
            if (currentLineIndex >= starting_line && currentLineIndex < ending_line) {
                fontRenderer.draw(lineText, -fontRenderer.getWidth(lineText) / 2f, yOffset, 0xffffff,
                        false, matrix, immediate, TextRenderer.TextLayerType.NORMAL, 0, fullBright);
                yOffset += fontRenderer.fontHeight + lineSpacing;
            }
            currentLineIndex++;

            if (currentLineIndex > ending_line) {
                break;
            }
        }
    }

    private static void drawEntityName(LivingEntityRenderState entityRenderState, Matrix4f matrix, VertexConsumerProvider immediate,
                                       int fullBright, float yOffset, boolean truncate) {
        TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;

        // Get Name of entity
        String nameText = "";
        EntityType<?> entityType = entityRenderState.entityType;
        if (entityType.getBaseClass().isInstance(MobEntity.class)) {
            // Custom Name Tag (MobEntity)
            if (entityRenderState.customName != null) {
                nameText = entityRenderState.customName.getString();
            }
        } else if (entityRenderState instanceof PlayerEntityRenderState) {
            // Player Name
            if (((PlayerEntityRenderState) entityRenderState).playerName != null) {
                nameText = ((PlayerEntityRenderState) entityRenderState).playerName.getString();
            }
        }

        // Truncate long names
        if (nameText.length() > 14 && truncate) {
            nameText = nameText.substring(0, 14) + "...";
        }

        fontRenderer.draw(nameText, -fontRenderer.getWidth(nameText) / 2f, yOffset, 0xffffff,
                false, matrix, immediate, TextRenderer.TextLayerType.NORMAL, 0, fullBright);
    }

    protected void renderEntity(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, S entityRenderState, float entityYaw, M model, int light ) {
        float lineSpacing = 1F;
        float textHeaderHeight = 40F;
        float textFooterHeight = 5F;
        int fullBright = 0xF000F0;
        double renderDistance = 11.0;
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer fontRenderer = client.textRenderer;

        EntityType<?> entityType = entityRenderState.entityType;
        float paddingAboveEntity = 0.4f;

        float height = EntityHeights.getAdjustedEntityHeight(entityType) + paddingAboveEntity;

        Vec3d entityPosition = new Vec3d(entityRenderState.x, entityRenderState.y + height, entityRenderState.z);
        Vec3d difference = client.cameraEntity.getEyePos().relativize(entityPosition);

        // Get entity height (adjust for specific classes)

        // Interpolate entity position (smooth motion)
        // Determine the chat bubble position
        Vec3d bubblePosition;

        if (entityType == EntityType.ENDER_DRAGON) {
            // Interpolate the head position
            bubblePosition = new Vec3d(0,height,0);
        } else {
            // Calculate the forward offset based on the entity's yaw
            float entityYawRadians = (float) Math.toRadians(entityRenderState.bodyYaw);
            Vec3d forwardOffset = new Vec3d(-Math.sin(entityYawRadians), 0.0, Math.cos(entityYawRadians));

            // Calculate the forward offset based on the entity's yaw, scaled to 80% towards
            // the front edge
            Vec3d scaledForwardOffset = forwardOffset.multiply(entityRenderState.width / 2.0 * 0.8);

            // Calculate the position of the chat bubble: above the head and 80% towards the
            // front
            bubblePosition = new Vec3d(0,height,0).add(scaledForwardOffset);
        }

        // Translate to the chat bubble's position




        matrices.translate(bubblePosition);

        UUID entityUUID = ((EntityRendererUUID) entityRenderState).getEntityUUID();
        // Use the body yaw for LivingEntityRenderState


        // Calculate the yaw angle
        double yaw = (Math.atan2(-difference.z, -difference.x) + Math.PI / 2D);

        // Convert yaw to Quaternion
        float halfYaw = (float) yaw * 0.5f;
        double sinHalfYaw = MathHelper.sin(halfYaw);
        double cosHalfYaw = MathHelper.cos(halfYaw);
        Quaternionf yawRotation = new Quaternionf(0, sinHalfYaw, 0, cosHalfYaw);

        // Apply the yaw rotation to the matrix stack
        matrices.multiply(yawRotation);

        // Obtain the horizontal distance to the entity
        double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
        // Calculate the pitch angle based on the horizontal distance and the y
        // difference
        double pitch = Math.atan2(-difference.y,-horizontalDistance );

        // Convert pitch to Quaternion
        float halfPitch = (float) pitch * 0.5f;
        double sinHalfPitch = MathHelper.sin(halfPitch);
        double cosHalfPitch = MathHelper.cos(halfPitch);
        Quaternionf pitchRotation = new Quaternionf(sinHalfPitch, 0, 0, -cosHalfPitch);

        // Apply the pitch rotation to the matrix stack
        matrices.multiply(pitchRotation);



        // Get position matrix
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Get the player
        ClientPlayerEntity player = MinecraftClient.getInstance().player;


        // Get chat message (if any)
        EntityChatData chatData = null;
        PlayerData playerData = null;

        Optional<UUID> otherPlayerUUID = Optional.empty();
        if (entityRenderState instanceof PlayerEntityRenderState) {
            String otherPlayerName = ((PlayerEntityRenderState) entityRenderState).name;

            assert client.world != null;
            Optional<AbstractClientPlayerEntity> playerEntity = client.world.getPlayers().stream().filter(entity -> {
                return entity.getName().getString().equals(otherPlayerName);
            }).findAny();

            if (playerEntity.isEmpty()) {
                LOGGER.warn("Could not find player entity with name: " + otherPlayerName);
                matrices.pop();
                return; // No player entity found, exit early
            } else {
                otherPlayerUUID = Optional.of(playerEntity.get().getUuid());
            }
        }
        if (entityRenderState instanceof PlayerEntityRenderState) {
            chatData = PlayerMessageManager.getMessage(otherPlayerUUID.get());
            playerData = new PlayerData(); // no friendship needed for player messages
        } else {
            chatData = ChatDataManager.getClientInstance().getOrCreateChatData(entityUUID);
            if (chatData != null && player != null) {
                playerData = chatData.getPlayerData(player.getUuid());
            }
        }

        float minTextHeight = (ChatDataManager.DISPLAY_NUM_LINES * (fontRenderer.fontHeight + lineSpacing))
                + (DISPLAY_PADDING * 2);
        float scaledTextHeight = 0;

        if (chatData != null) {
            // Set the range of lines to display
            List<String> lines = chatData.getWrappedLines();
            float linesDisplayed = 0;
            int starting_line = chatData.currentLineNumber;
            int ending_line = Math.min(chatData.currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES,
                    lines.size());

            // Determine max line length
            linesDisplayed = ending_line - starting_line;

            // Calculate size of text scaled to world
            scaledTextHeight = linesDisplayed * (fontRenderer.fontHeight + lineSpacing);
            scaledTextHeight = Math.max(scaledTextHeight, minTextHeight);

            // Update Bubble Data for Click Handling using UUID (account for scaling)
            BubbleLocationManager.updateBubbleData(entityUUID, entityPosition,
                    128F / (1 / 0.02F), (scaledTextHeight + 25F) / (1 / 0.02F), yaw, entityRenderState.pitch);

            // Scale down before rendering textures (otherwise font is huge)
            matrices.scale(-0.02F, -0.02F, 0.02F);

            // Translate above the entity
            matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

            // Check if conversation has started
            if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                // AAA if chatData.status == ChatDataManage.ChatStatus.None
                // Draw 'start chat' button
                drawIcon("button-chat", matrices, vertexConsumerProvider, model, -16, textHeaderHeight, 32, 17, light);

                // Draw Entity (Custom Name)
                drawEntityName(((LivingEntityRenderState) entityRenderState), matrix, vertexConsumerProvider, fullBright, 24F + DISPLAY_PADDING, true);

            } else if (chatData.status == ChatDataManager.ChatStatus.PENDING) {
                // Draw 'pending' button
                drawIcon("button-dot-" + ANIMATION_FRAME, matrices, vertexConsumerProvider, model,-16, textHeaderHeight, 32, 17, light);

            } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT
                    && chatData.status != ChatDataManager.ChatStatus.HIDDEN) {
                // Draw Entity (Custom Name)
                drawEntityName(( entityRenderState), matrix, vertexConsumerProvider, fullBright, 24F + DISPLAY_PADDING, true);

                // Draw text background (no smaller than 50F tall)
                drawTextBubbleBackground("text-top", matrices, vertexConsumerProvider, model,-64, 0, 128, scaledTextHeight,
                        playerData.friendship,light);

                // Draw face icon of entity
                drawEntityIcon(matrices, vertexConsumerProvider, model, entityRenderState,client, -82, 7, 32, 32, light);

                // Draw Friendship status
                drawFriendshipStatus(matrices, vertexConsumerProvider, model,51, 18, 31, 21, playerData.friendship, light);

                // Draw 'arrows' & 'keyboard' buttons
                if (chatData.currentLineNumber > 0) {
                    drawIcon("arrow-left", matrices, vertexConsumerProvider, model,-63, scaledTextHeight + 29, 16, 16, light);
                }
                if (!chatData.isEndOfMessage()) {
                    drawIcon("arrow-right", matrices, vertexConsumerProvider, model,47, scaledTextHeight + 29, 16, 16, light);
                } else {
                    drawIcon("keyboard", matrices, vertexConsumerProvider, model,47, scaledTextHeight + 28, 16, 16, light);
                }

                // Render each line of the text
                drawMessageText(matrix, lines, starting_line, ending_line, vertexConsumerProvider, lineSpacing, fullBright,
                        40.0F + DISPLAY_PADDING);

            } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT
                    && chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                // Draw Entity (Custom Name)
                drawEntityName((LivingEntityRenderState) entityRenderState, matrix, vertexConsumerProvider, fullBright, 24F + DISPLAY_PADDING, false);

                // Draw 'resume chat' button
                if (playerData.friendship == 3) {
                    // Friend chat bubble
                    drawIcon("button-chat-friend", matrices, vertexConsumerProvider,model,-16, textHeaderHeight, 32, 17, light);
                } else if (playerData.friendship == -3) {
                    // Enemy chat bubble
                    drawIcon("button-chat-enemy", matrices, vertexConsumerProvider,model,-16, textHeaderHeight, 32, 17, light);
                } else {
                    // Normal chat bubble
                    drawIcon("button-chat", matrices, vertexConsumerProvider,model,-16, textHeaderHeight, 32, 17, light);
                }

            } else if (chatData.sender == ChatDataManager.ChatSender.USER
                    && chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                // Draw Player Name
                drawEntityName(entityRenderState, matrix, vertexConsumerProvider, fullBright, 24F + DISPLAY_PADDING, true);

                // Draw text background
                drawTextBubbleBackground("text-top-player", matrices, vertexConsumerProvider,model,-64, 0, 128, scaledTextHeight,
                        playerData.friendship, light);

                // Draw face icon of player
                drawPlayerIcon(matrices, vertexConsumerProvider, model, entityRenderState, client, -75, 14, 18, 18, light);

                // Render each line of the player's text
                drawMessageText(matrix, lines, starting_line, ending_line, vertexConsumerProvider, lineSpacing, fullBright,
                        40.0F + DISPLAY_PADDING);
            }

        } else if (entityType == EntityType.PLAYER) {
            // Scale down before rendering textures (otherwise font is huge)
            matrices.scale(-0.02F, -0.02F, 0.02F);

            boolean showPendingIcon = false;
            if (PlayerMessageManager.isChatUIOpen(entityUUID)) {
                showPendingIcon = true;
                scaledTextHeight += minTextHeight; // raise height of player name and icon
            } else {
                scaledTextHeight -= 15; // lower a bit more (when no pending icon is visible)
            }

            // Translate above the player
            matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

            // Draw Player Name (if not self and HUD is visible)
            if (!MinecraftClient.getInstance().options.hudHidden) {
                drawEntityName(entityRenderState, matrices.peek().getPositionMatrix(), vertexConsumerProvider, fullBright,
                        24F + DISPLAY_PADDING, true);

                if (showPendingIcon) {
                    // Draw 'pending' button (when Chat UI is open)
                    drawIcon("button-dot-" + ANIMATION_FRAME, matrices,vertexConsumerProvider, model,-16, textHeaderHeight, 32, 17, light);
                }
            }
        }

        // Calculate animation frames (0-8) every X ticks
        ANIMATION_FRAME++;
        if (ANIMATION_FRAME > 8) {
            ANIMATION_FRAME = 0;
        }


    }

    private static <T extends LivingEntity, S extends LivingEntityRenderState> Identifier getTextureIdentifier(MinecraftClient client,S state) {

        EntityRenderer<? , ? super S> renderer = client.getEntityRenderDispatcher().getRenderer(state);

        // This is a test method to check if the renderer is working correctly.
        // It can be used to debug rendering issues or to verify that the renderer
        // is set up correctly.
        if (renderer instanceof LivingEntityRenderer) {
            return ((LivingEntityRenderer<? super T, ? super S, ?>) renderer).getTexture((state));
        } else {
            LOGGER.warn("Renderer is not an instance of LivingEntityRenderer for entity: {}", state.customName.getString());
            return Identifier.of("creaturechat", "textures/entity/not_found.png");
        }
    }
}
