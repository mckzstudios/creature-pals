package com.owlmaddie.components;

import com.mojang.serialization.Codec;
import com.owlmaddie.commands.CreatureChatCommands;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;


public class Components {
    public static final ComponentType<UUID> ChatUUID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of("creaturechat", "CCUUID"),
            ComponentType.<UUID>builder().codec(Uuids.CODEC).build()
    );
    protected static void initialize() {
        CreatureChatCommands.LOGGER.info("Registering creaturechat components");
        // Technically this method can stay empty, but some developers like to notify
        // the console, that certain parts of the mod have been successfully initialized


    }
}