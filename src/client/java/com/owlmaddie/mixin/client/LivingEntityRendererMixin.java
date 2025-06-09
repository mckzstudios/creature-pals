package com.owlmaddie.mixin.client;

import com.owlmaddie.chat.ChatDataManager;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.chat.PlayerData;
import com.owlmaddie.ui.*;
import com.owlmaddie.utils.EntityHeights;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {


    @Unique @Mutable
    protected boolean featureRendererEnabled = false;
    @Shadow @Final protected List<FeatureRenderer<S, M>> features;

    @Shadow protected abstract boolean addFeature(FeatureRenderer<S, M> feature);

    @Shadow public abstract M getModel();

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)Z"))
    private void onRender(S livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {

        if (livingEntityRenderState.onFire) {
            System.out.println("FIRE");
        }
        EntityType<?> entityType = livingEntityRenderState.entityType;
        if (!(entityType == EntityType.PLAYER || entityType.isSummonable())) {
            return;
        }

        Identifier entityId = Registries.ENTITY_TYPE.getId(entityType);

        if (BubbleRenderer.BLACKLIST.contains(entityId) || (!BubbleRenderer.WHITELIST.isEmpty() && !BubbleRenderer.WHITELIST.contains(entityId))) {
            return;
        }

        BubbleEntityRenderer<S, M> entityRenderer = getBubbleEntityRendererFeature();

        if (!featureRendererEnabled) {

            this.addFeature(entityRenderer);
            featureRendererEnabled = true;
        }


    }

    @Unique
    private @NotNull BubbleEntityRenderer<S, M> getBubbleEntityRendererFeature() {
        M model = this.getModel();
        return new BubbleEntityRenderer<S, M>(model) {
            @Override
            public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance) {
                //VertexConsumer consumer = vertexConsumers.getBuffer(BubblePipeline.BUBBLE_LAYER);
                //Matrix4f matrix4f = matrices.peek().getPositionMatrix();

                System.out.println("Hello");
                renderEntity(matrices,vertexConsumers,state);
            }
        };
    }




}
