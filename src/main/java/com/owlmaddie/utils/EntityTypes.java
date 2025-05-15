package com.owlmaddie.utils;

import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class EntityTypes {
    // use for getting sheep, cow, ...
    public static String getEntityType(Entity entity) {
        Identifier id = Registries.ENTITY_TYPE.getId(entity.getType());
        return id.getPath();
    }

}
