package com.owlmaddie.mixin;

import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TameableEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.Optional;

@Mixin(TameableEntity.class)
public abstract class TameableAccessor {
    @Shadow
    @Final
    @Mutable // only if you intend to modify it, not needed for read-only
    public static TrackedData<Optional<LazyEntityReference<LivingEntity>>> OWNER_UUID = null;
}