package com.owlmaddie.mixin.client;

import com.owlmaddie.utils.EntityRendererUUID;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin{

    @Shadow @Final protected EntityRenderDispatcher dispatcher;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private <S extends EntityRenderState>  void cancelRenderLabel(S state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        ci.cancel(); // Hide Entity Custom Names
    }


    @Inject(method = "updateRenderState", at = @At("HEAD"))
    private <T extends Entity, S extends EntityRenderState> void addUUID(T entity, S state, float tickProgress, CallbackInfo ci) {
        // This is a workaround to add the UUID to the EntityRenderState
        // so that it can be used in the BubbleRenderer.
        if (state != null && entity != null) {
            ((EntityRendererUUID) state).setEntityUUID(entity.getUuid());
        }
    }
}
