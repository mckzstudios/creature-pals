package com.owlmaddie.mixin.client;

import com.owlmaddie.ui.*;
import com.owlmaddie.utils.ClientEntityFinder;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {


    @Unique @Mutable
    private static HashSet<EntityType<?>> ENTITY_TYPES_WITH_MIXIN_ENABLED = new HashSet<>();
    @Shadow @Final protected List<FeatureRenderer<S, M>> features;

    @Shadow protected abstract boolean addFeature(FeatureRenderer<S, M> feature);

    @Shadow public abstract M getModel();

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;)Z"))
    private void onRender(S livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        EntityType<?> entityType = livingEntityRenderState.entityType;
        if (!ClientEntityFinder.isChattableEntity(livingEntityRenderState)) {
            return;
        }

        BubbleRenderer<S, M> entityRenderer = getBubbleEntityRendererFeature();

        if (!ENTITY_TYPES_WITH_MIXIN_ENABLED.contains(entityType)) {
            this.addFeature(entityRenderer);
            ENTITY_TYPES_WITH_MIXIN_ENABLED.add(entityType);
            System.out.println("Added BubbleEntityRenderer for " + entityType.getName().getString());
        }
    }

    @Unique
    private @NotNull BubbleRenderer<S, M> getBubbleEntityRendererFeature() {
        M model = this.getModel();
        return new BubbleRenderer<S, M>(model) {
            @Override
            public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance) {
                //VertexConsumer consumer = vertexConsumers.getBuffer(BubblePipeline.BUBBLE_LAYER);
                //Matrix4f matrix4f = matrices.peek().getPositionMatrix();



                if (ClientEntityFinder.isChattableEntity(state)) {
                    matrices.push();
                    renderEntity(matrices,vertexConsumers,state,limbAngle,this.getContextModel(), light);
                    matrices.pop();

                }
            }
        };
    }




}
