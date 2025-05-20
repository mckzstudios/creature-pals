package com.owlmaddie.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.*;
import net.minecraft.util.dynamic.Codecs;

import java.util.stream.Stream;

/**
 * The {@code LeadParticleEffect} class allows for an 'angle' to be passed along with the Particle, to rotate it in the direction of LEAD behavior.
 */
static class LeadParticle implements ParticleEffect {
    private final float angle;


    public static final MapCodec<LeadParticle> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            Codecs.POSITIVE_FLOAT.fieldOf("angle").forGetter(effect -> effect.angle), SCALE_CODEC.fieldOf("scale").forGetter(AbstractDustParticleEffect::getScale)
                    )
                    .apply(instance, LeadParticle::new)
    );
    public static final PacketCodec<RegistryByteBuf, LeadParticle> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, effect -> effect.angle, LeadParticle::new
    );
    LeadParticle(float angle) {
        this.angle = angle;
    }

    @Override
    public ParticleType<LeadParticle> getType() {
        return LEAD_PARTICLE;
    }

}
public static final ParticleType<LeadParticle> LEAD_PARTICLE = FabricParticleTypes.complex(
        type -> LeadParticle.CODEC,
        type -> LeadParticle.PACKET_CODEC
);

