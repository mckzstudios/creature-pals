package com.owlmaddie.ui;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.skin.PlayerCustomTexture;
import com.owlmaddie.utils.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The {@code BubbleRenderer} class provides static methods to render the chat
 * UI bubble, entity icons,
 * text, friendship status, and other UI-related rendering code.
 */
public class BubbleRenderer {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    protected static TextureLoader textures = new TextureLoader();
    public static int DISPLAY_PADDING = 2;
    public static int animationFrame = 0;
    public static long lastTick = 0;
    public static int light = 15728880;
    public static int overlay = OverlayTexture.DEFAULT_UV;
    // TODO: Consider moving these to a config file
    public static List<String> whitelist = new ArrayList<>();
    public static List<String> blacklist = new ArrayList<>();
    private static int queryEntityDataCount = 0;
    private static List<LivingEntity> relevantEntities;

    public static void drawTextBubbleBackground(String base_name, MatrixStack matrices, VertexConsumerProvider provider, RenderLayer renderLayer, float x, float y, float width, float height, int friendship) {
        float z = 0.01F;
        VertexConsumer buffer = provider.getBuffer(renderLayer);

        // Draw UI text background (based on friendship)
        // Draw TOP
        if (friendship == -3 && !base_name.endsWith("-player")) {
            RenderSystem.setShaderTexture(0, textures.GetUI(base_name + "-enemy").getGlId());
        } else if (friendship == 3 && !base_name.endsWith("-player")) {
            RenderSystem.setShaderTexture(0, textures.GetUI(base_name + "-friend").getGlId());
        } else {
            RenderSystem.setShaderTexture(0, textures.GetUI(base_name).getGlId());
        }
        drawTexturePart(matrices, buffer, x - 50, y, z, 228, 40, 0, 0, 1, 1);

        // Draw MIDDLE
        RenderSystem.setShaderTexture(0, textures.GetUI("text-middle").getGlId());
        drawTexturePart(matrices, buffer, x, y + 40, z, width, height, 0, 0, 1, 1);

        // Draw BOTTOM
        RenderSystem.setShaderTexture(0, textures.GetUI("text-bottom").getGlId());
        drawTexturePart(matrices, buffer, x, y + 40 + height, z, width, 5, 0, 0, 1, 1);
    }

    private static void drawTexturePart(MatrixStack matrices, VertexConsumer buffer, float x, float y, float z, float width, float height, float u1, float v1, float u2, float v2) {
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(u1, v2).light(light).overlay(overlay).next(); // bottom left
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(u2, v2).light(light).overlay(overlay).next(); // bottom right
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(u2, v1).light(light).overlay(overlay).next(); // top right
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(u1, v1).light(light).overlay(overlay).next(); // top left
    }

    private static void drawIcon(String ui_icon_name, MatrixStack matrices, VertexConsumerProvider provider, RenderLayer renderLayer, float x, float y, float width, float height) {
        GpuTexture button_texture = textures.GetUI(ui_icon_name);
        RenderSystem.setShaderTexture(0, button_texture.getGlId());
        VertexConsumer buffer = provider.getBuffer(renderLayer);
        drawTexturePart(matrices, buffer, x, y, 0.0F, width, height, 0, 0, 1, 1);
    }

    private static void drawFriendshipStatus(MatrixStack matrices, VertexConsumerProvider provider, RenderLayer renderLayer, float x, float y, float width, float height, int friendship) {
        String ui_icon_name = "friendship" + friendship;
        GpuTexture button_texture = textures.GetUI(ui_icon_name);
        RenderSystem.setShaderTexture(0, button_texture.getGlId());
        VertexConsumer buffer = provider.getBuffer(renderLayer);
        drawTexturePart(matrices, buffer, x, y, -0.01F, width, height, 0, 0, 1, 1);
    }

    private static void drawEntityIcon(MatrixStack matrices, VertexConsumerProvider provider, RenderLayer renderLayer, LivingEntity entity, float x, float y, float width, float height) {
        String entity_icon_path = getTextureIdentifier(entity).getPath();
        GpuTexture entity_id = textures.GetEntity(entity_icon_path);
        if (entity_id == null) {
            return;
        }
        RenderSystem.setShaderTexture(0, entity_id.getGlId());
        VertexConsumer buffer = provider.getBuffer(renderLayer);
        drawTexturePart(matrices, buffer, x, y, -0.01F, width, height, 0, 0, 1, 1);
    }

    private static <T extends LivingEntity, S extends LivingEntityRenderState> Identifier getTextureIdentifier(LivingEntity entity) {

        EntityRenderer<? super T, ?> renderer = EntityRendererAccessor.getEntityRenderer(entity);
        // This is a test method to check if the renderer is working correctly.
        // It can be used to debug rendering issues or to verify that the renderer
        // is set up correctly.
        if (renderer instanceof LivingEntityRenderer) {
            // TODO: This is a temporary fix for the render state issue.
            // EntityRenderState state = renderer.createRenderState();
            // if (state instanceof LivingEntityRenderState) {
            // return ((LivingEntityRenderer<? super T, ? super LivingEntityRenderState,
            // ?>) renderer).getTexture(((LivingEntityRenderState) state));
            // }
            // else {
            // LOGGER.warn("Renderer state is not an instance of LivingEntityRenderState for
            // entity: {}", entity.getName().getString());
            // return Identifier.of("creaturechat", "textures/entity/not_found.png");
            // }
            // For now, let's just use the getTexture method directly.
            return renderer.getTexture((T) entity);
        } else {
            LOGGER.warn("Renderer is not an instance of LivingEntityRenderer for entity: {}", entity.getName().getString());
            return Identifier.of("creaturechat", "textures/entity/not_found.png");
        }
    }

    private static void drawPlayerIcon(MatrixStack matrices, VertexConsumerProvider provider, RenderLayer renderLayer, LivingEntity entity, float x, float y, float width, float height) {
        Identifier playerTexture = getTextureIdentifier(entity);
        boolean customSkinFound = PlayerCustomTexture.hasCustomIcon(playerTexture);

        RenderSystem.setShaderTexture(0, MinecraftClient.getInstance().getTextureManager().getTexture(playerTexture).getGlTextureId());
        VertexConsumer buffer = provider.getBuffer(renderLayer);
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

                buffer.vertex(matrix4f, scaledX, scaledY + scaledHeight, z).color(255, 255, 255, 255).texture(newU1, newV2).light(light).overlay(overlay).next();
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY + scaledHeight, z).color(255, 255, 255, 255).texture(newU2, newV2).light(light).overlay(overlay).next();
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY, z).color(255, 255, 255, 255).texture(newU2, newV1).light(light).overlay(overlay).next();
                buffer.vertex(matrix4f, scaledX, scaledY, z).color(255, 255, 255, 255).texture(newU1, newV1).light(light).overlay(overlay).next();
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

            buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(u1, v2).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(u2, v2).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(u2, v1).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(u1, v1).light(light).overlay(overlay).next();

            // Hat layer
            float hatU1 = 40.0F / 64.0F;
            float hatV1 = 8.0F / 64.0F;
            float hatU2 = 48.0F / 64.0F;
            float hatV2 = 16.0F / 64.0F;

            z -= 0.01F; // Render hat slightly above the face

            buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(hatU1, hatV2).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(hatU2, hatV2).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(hatU2, hatV1).light(light).overlay(overlay).next();
            buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(hatU1, hatV1).light(light).overlay(overlay).next();
        }
    }

    private static void drawMessageText(Matrix4f matrix, List<String> lines, int starting_line, int ending_line,
            VertexConsumerProvider immediate, float lineSpacing, int fullBright, float yOffset) {
        TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
        int currentLineIndex = 0; // We'll use this to track which line we're on

        for (String lineText : lines) {
            // Only draw lines that are within the specified range
            if (currentLineIndex >= starting_line && currentLineIndex < ending_line) {
                fontRenderer.draw(lineText, -fontRenderer.getWidth(lineText) / 2f, yOffset, 0xffffff,
                        false, matrix, immediate, TextLayerType.NORMAL, 0, fullBright);
                yOffset += fontRenderer.fontHeight + lineSpacing;
            }
            currentLineIndex++;

            if (currentLineIndex > ending_line) {
                break;
            }
        }
    }

    private static void drawEntityName(Entity entity, Matrix4f matrix, VertexConsumerProvider immediate,
            int fullBright, float yOffset, boolean truncate) {
        TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;

        // Get Name of entity
        String nameText = "";
        if (entity instanceof MobEntity) {
            // Custom Name Tag (MobEntity)
            if (entity.getCustomName() != null) {
                nameText = entity.getCustomName().getString();
            }
        } else if (entity instanceof PlayerEntity) {
            // Player Name
            nameText = entity.getName().getString();
        }

        // Truncate long names
        if (nameText.length() > 14 && truncate) {
            nameText = nameText.substring(0, 14) + "...";
        }

        fontRenderer.draw(nameText, -fontRenderer.getWidth(nameText) / 2f, yOffset, 0xffffff,
                false, matrix, immediate, TextLayerType.NORMAL, 0, fullBright);
    }

    public static void drawTextAboveEntities(WorldRenderContext context, long tick, float partialTicks) {
        // Set some rendering constants
        float lineSpacing = 1F;
        float textHeaderHeight = 40F;
        float textFooterHeight = 5F;
        int fullBright = 0xF000F0;
        double renderDistance = 11.0;

        // Get camera
        Camera camera = context.camera();
        Entity cameraEntity = camera.getFocusedEntity();
        if (cameraEntity == null)
            return;
        World world = cameraEntity.getEntityWorld();

        // Calculate radius of entities
        Vec3d pos = cameraEntity.getPos();
        Box area = new Box(pos.x - renderDistance, pos.y - renderDistance, pos.z - renderDistance,
                pos.x + renderDistance, pos.y + renderDistance, pos.z + renderDistance);

        // Init font render, matrix, and vertex producer
        TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider immediate = context.consumers();
        RenderLayer renderLayer = RenderLayer.getTranslucent(); // Or a more specific one if appropriate

        // Get camera position
        Vec3d interpolatedCameraPos = new Vec3d(camera.getPos().x, camera.getPos().y, camera.getPos().z);

        // Increment query counter
        queryEntityDataCount++;

        // This query count helps us cache the list of relevant entities. We can refresh
        // the list every 3rd call to this render function
        if (queryEntityDataCount % 3 == 0 || relevantEntities == null) {
            // Get all entities
            // Filter to include only MobEntity & PlayerEntity but exclude any camera 1st
            // person entity and any entities with passengers
            relevantEntities = world.<LivingEntity>getEntitiesByClass(LivingEntity.class, area, (entity) -> {
                if ((entity instanceof MobEntity || entity instanceof PlayerEntity) &&
                        !entity.hasPassengers() &&
                        !(entity.equals(cameraEntity) && !camera.isThirdPerson()) &&
                        !(entity.equals(cameraEntity) && entity.isSpectator())) {
                    if (entity instanceof PlayerEntity) {
                        return true;
                    }
                    Identifier entityId = Registries.ENTITY_TYPE.getId(entity.getType());
                    String entityIdString = entityId.toString();
                    // Check blacklist first
                    if (blacklist.contains(entityIdString)) {
                        return false;
                    }
                    // If whitelist is not empty, only include entities in the whitelist
                    return whitelist.isEmpty() || whitelist.contains(entityIdString);
                } else {
                    return false;
                }
            });


            queryEntityDataCount = 0;
        }

        for (LivingEntity entity : relevantEntities) {

            // Push a new matrix onto the stack.
            matrices.push();

            // Get entity height (adjust for specific classes)
            float entityHeight = EntityHeights.getAdjustedEntityHeight(entity);

            // Interpolate entity position (smooth motion)
            double paddingAboveEntity = 0.4D;
            Vec3d interpolatedEntityPos = new Vec3d(
                    MathHelper.lerp(partialTicks, entity.lastX, entity.getPos().x),
                    MathHelper.lerp(partialTicks, entity.lastY, entity.getPos().y),
                    MathHelper.lerp(partialTicks, entity.lastZ, entity.getPos().z));

            // Determine the chat bubble position
            Vec3d bubblePosition;
            if (entity instanceof EnderDragonEntity) {
                // Ender dragons a unique, and we must use the Head for position
                EnderDragonEntity dragon = (EnderDragonEntity) entity;
                EnderDragonPart head = dragon.head;

                // Interpolate the head position
                Vec3d headPos = new Vec3d(
                        MathHelper.lerp(partialTicks, head.lastX, head.getX()),
                        MathHelper.lerp(partialTicks, head.lastY, head.getY()),
                        MathHelper.lerp(partialTicks, head.lastZ, head.getZ()));

                // Just use the head's interpolated position directly
                bubblePosition = headPos.add(0, entityHeight + paddingAboveEntity, 0);
            } else {
                // Calculate the forward offset based on the entity's yaw
                float entityYawRadians = (float) Math.toRadians(entity.getYaw(partialTicks));
                Vec3d forwardOffset = new Vec3d(-Math.sin(entityYawRadians), 0.0, Math.cos(entityYawRadians));

                // Calculate the forward offset based on the entity's yaw, scaled to 80% towards
                // the front edge
                Vec3d scaledForwardOffset = forwardOffset.multiply(entity.getWidth() / 2.0 * 0.8);

                // Calculate the position of the chat bubble: above the head and 80% towards the
                // front
                bubblePosition = interpolatedEntityPos.add(scaledForwardOffset)
                        .add(0, entityHeight + paddingAboveEntity, 0);
            }

            // Translate to the chat bubble's position
            matrices.translate(bubblePosition.x - interpolatedCameraPos.x,
                    bubblePosition.y - interpolatedCameraPos.y,
                    bubblePosition.z - interpolatedCameraPos.z);

            // Calculate the difference vector (from entity + padding above to camera)
            Vec3d difference = interpolatedCameraPos.subtract(new Vec3d(interpolatedEntityPos.x,
                    interpolatedEntityPos.y + entityHeight + paddingAboveEntity, interpolatedEntityPos.z));

            // Calculate the yaw angle
            double yaw = -(Math.atan2(difference.z, difference.x) + Math.PI / 2D);

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
            double pitch = Math.atan2(difference.y, horizontalDistance);

            // Convert pitch to Quaternion
            float halfPitch = (float) pitch * 0.5f;
            double sinHalfPitch = MathHelper.sin(halfPitch);
            double cosHalfPitch = MathHelper.cos(halfPitch);
            Quaternionf pitchRotation = new Quaternionf(sinHalfPitch, 0, 0, cosHalfPitch);

            // Apply the pitch rotation to the matrix stack
            matrices.multiply(pitchRotation);

            // Get position matrix
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // Get the player
            ClientPlayerEntity player = MinecraftClient.getInstance().player;

            // Get chat message (if any)
            EntityChatData chatData = null;
            PlayerData playerData = null;
            if (entity instanceof MobEntity) {
                chatData = ChatDataManager.getClientInstance().getOrCreateChatData(entity.getUuid());
                if (chatData != null) {
                    playerData = chatData.getPlayerData(player.getUuid());
                }
            } else if (entity instanceof PlayerEntity) {
                chatData = PlayerMessageManager.getMessage(entity.getUuid());
                playerData = new PlayerData(); // no friendship needed for player messages
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
                BubbleLocationManager.updateBubbleData(entity.getUuid(), bubblePosition,
                        128F / (1 / 0.02F), (scaledTextHeight + 25F) / (1 / 0.02F), yaw, pitch);

                // Scale down before rendering textures (otherwise font is huge)
                matrices.scale(-0.02F, -0.02F, 0.02F);

                // Translate above the entity
                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                // Check if conversation has started
                if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                    // AAA if chatData.status == ChatDataManage.ChatStatus.None
                    // Draw 'start chat' button
                    drawIcon("button-chat", matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);

                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                } else if (chatData.status == ChatDataManager.ChatStatus.PENDING) {
                    // Draw 'pending' button
                    drawIcon("button-dot-" + animationFrame, matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT
                        && chatData.status != ChatDataManager.ChatStatus.HIDDEN) {
                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    // Draw text background (no smaller than 50F tall)
                    drawTextBubbleBackground("text-top", matrices, immediate, renderLayer, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    // Draw face icon of entity
                    drawEntityIcon(matrices, immediate, renderLayer, entity, -82, 7, 32, 32);

                    // Draw Friendship status
                    drawFriendshipStatus(matrices, immediate, renderLayer, 51, 18, 31, 21, playerData.friendship);

                    // Draw 'arrows' & 'keyboard' buttons
                    if (chatData.currentLineNumber > 0) {
                        drawIcon("arrow-left", matrices, immediate, renderLayer, -63, scaledTextHeight + 29, 16, 16);
                    }
                    if (!chatData.isEndOfMessage()) {
                        drawIcon("arrow-right", matrices, immediate, renderLayer, 47, scaledTextHeight + 29, 16, 16);
                    } else {
                        drawIcon("keyboard", matrices, immediate, renderLayer, 47, scaledTextHeight + 28, 16, 16);
                    }

                    // Render each line of the text
                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright,
                            40.0F + DISPLAY_PADDING);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT
                        && chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                    // Draw Entity (Custom Name)
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, false);

                    // Draw 'resume chat' button
                    if (playerData.friendship == 3) {
                        // Friend chat bubble
                        drawIcon("button-chat-friend", matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);
                    } else if (playerData.friendship == -3) {
                        // Enemy chat bubble
                        drawIcon("button-chat-enemy", matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);
                    } else {
                        // Normal chat bubble
                        drawIcon("button-chat", matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);
                    }

                } else if (chatData.sender == ChatDataManager.ChatSender.USER
                        && chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                    // Draw Player Name
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    // Draw text background
                    drawTextBubbleBackground("text-top-player", matrices, immediate, renderLayer, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    // Draw face icon of player
                    drawPlayerIcon(matrices, immediate, renderLayer, entity, -75, 14, 18, 18);

                    // Render each line of the player's text
                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright,
                            40.0F + DISPLAY_PADDING);
                }

            } else if (entity instanceof PlayerEntity) {
                // Scale down before rendering textures (otherwise font is huge)
                matrices.scale(-0.02F, -0.02F, 0.02F);

                boolean showPendingIcon = false;
                if (PlayerMessageManager.isChatUIOpen(entity.getUuid())) {
                    showPendingIcon = true;
                    scaledTextHeight += minTextHeight; // raise height of player name and icon
                } else {
                    scaledTextHeight -= 15; // lower a bit more (when no pending icon is visible)
                }

                // Translate above the player
                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                // Draw Player Name (if not self and HUD is visible)
                if (!entity.equals(cameraEntity) && !MinecraftClient.getInstance().options.hudHidden) {
                    drawEntityName(entity, matrices.peek().getPositionMatrix(), immediate, fullBright,
                            24F + DISPLAY_PADDING, true);

                    if (showPendingIcon) {
                        // Draw 'pending' button (when Chat UI is open)
                        drawIcon("button-dot-" + animationFrame, matrices, immediate, renderLayer, -16, textHeaderHeight, 32, 17);
                    }
                }
            }

            // Calculate animation frames (0-8) every X ticks
            if (lastTick != tick && tick % 5 == 0) {
                lastTick = tick;
                animationFrame++;
            }
            if (animationFrame > 8) {
                animationFrame = 0;
            }

            // Pop the matrix to return to the original state.
            matrices.pop();
        }

        // Get list of Entity UUIDs with chat bubbles rendered
        List<UUID> activeEntityUUIDs = relevantEntities.stream()
                .map(Entity::getUuid)
                .collect(Collectors.toList());

        // Purge entities that were not rendered
        BubbleLocationManager.performCleanup(activeEntityUUIDs);
    }

    public static List<LivingEntity> getRelevantEntities() {
        return relevantEntities;
    }
}
