package com.owlmaddie.utils;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * The {@code TextureLoader} class registers and returns texture identifiers for resources
 * contained for this mod. UI and Entity icons. Missing textures are logged once.
 */
public class TextureLoader {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Set<String> missingTextures = new HashSet<>();

    public TextureLoader() {
    }

    public GpuTexture GetUI(String name) {
        String texturePath = "textures/ui/" + name + ".png";
        Identifier textureId = Identifier.of("creaturechat", texturePath);
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(textureId);

        if (resource.isPresent()) {
            return loadTexture(textureId);
        } else {
            // Resource not found
            logMissingTextureOnce(texturePath);
            return null;
        }
    }

    private GpuTexture loadTexture(Identifier textureId) {
        // Bind texture, and return Identity
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        try {
            return textureManager.getTexture(textureId).getGlTexture();
        } catch (IllegalStateException e) {
            ResourceTexture texture = new ResourceTexture(textureId);
            try {
                texture.loadContents(MinecraftClient.getInstance().getResourceManager());
            } catch (IOException ee) {
                throw new RuntimeException(ee);
            }
            textureManager.registerTexture(textureId, texture);

            return texture.getGlTexture();
        }

    }
    public GpuTexture GetEntity(String texturePath) {
        Identifier textureId = Identifier.of("creaturechat", texturePath);
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(textureId);

        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();

        if (resource.isPresent()) {
            // Texture found, bind it and return the Identifier
            return loadTexture(textureId);
        } else {
            // Texture not found, log a message and return the "not_found" texture Identifier
            Identifier notFoundTextureId = Identifier.of("creaturechat", "textures/entity/not_found.png");
            logMissingTextureOnce(texturePath);
            return loadTexture(notFoundTextureId);
        }
    }

    private void logMissingTextureOnce(String texturePath) {
        // Check if the missing texture has already been logged
        if (!missingTextures.contains(texturePath)) {
            LOGGER.info(texturePath + " was not found");
            missingTextures.add(texturePath);
        }
    }
}