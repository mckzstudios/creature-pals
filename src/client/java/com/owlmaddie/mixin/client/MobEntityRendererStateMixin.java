package com.owlmaddie.mixin.client;

import com.owlmaddie.utils.EntityRendererUUID;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

@Mixin(EntityRenderState.class)
public class MobEntityRendererStateMixin implements EntityRendererUUID {
    @Unique
    @Mutable
    private UUID entityUUID = null;

    @Override
    public UUID getEntityUUID() {
        return entityUUID;
    }

    @Override
    public void setEntityUUID(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }
}
