package com.owlmaddie.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Particles {
    public static final SimpleParticleType HEART_SMALL_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType HEART_BIG_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FIRE_SMALL_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FIRE_BIG_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType ATTACK_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FLEE_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FOLLOW_FRIEND_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType FOLLOW_ENEMY_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType PROTECT_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType LEAD_FRIEND_PARTICLE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType LEAD_ENEMY_PARTICLE = FabricParticleTypes.simple(true);

    public static void register() {

        // Register custom particles
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("creaturepals", "heart_small"), HEART_SMALL_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "heart_big"), HEART_BIG_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "fire_small"), FIRE_SMALL_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "fire_big"), FIRE_BIG_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "attack"), ATTACK_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "flee"), FLEE_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "follow_enemy"), FOLLOW_ENEMY_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "follow_friend"), FOLLOW_FRIEND_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "protect"), PROTECT_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "lead_enemy"), LEAD_ENEMY_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE,  Identifier.of("creaturepals", "lead_friend"), LEAD_FRIEND_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("creaturepals", "lead"), LeadParticleEffect.TYPE);
    }
}
