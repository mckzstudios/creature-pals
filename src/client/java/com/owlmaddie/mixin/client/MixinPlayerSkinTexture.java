package com.owlmaddie.mixin.client;

import com.owlmaddie.skin.IPlayerSkinTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code MixinPlayerSkinTexture} class injects code into the PlayerSkinTexture class, to make a copy
 * of the player's skin native image, so we can later use it for pixel checking (black/white key) for
 * loading custom player icons in the unused UV coordinates of the player skin image.
 */
@Mixin(PlayerSkinTextureDownloader.class)
public abstract class MixinPlayerSkinTexture implements IPlayerSkinTexture {
    @Unique
    @Mutable
    private static NativeImage cachedSkinImage;


    @Inject(method = "registerTexture", at = @At("HEAD"))
    private static void captureNativeImage(Identifier textureId, NativeImage image, CallbackInfoReturnable<CompletableFuture<Identifier>> cir) {
        // Instead of image.copy(), we do a manual clone
        cachedSkinImage = cloneNativeImage(image);
    }

    @Override
    public NativeImage getLoadedImage() {
        return cachedSkinImage;
    }

    // Example of the utility method in the same class (or in a separate helper):
    @Unique
    private static NativeImage cloneNativeImage(NativeImage source) {
        NativeImage copy = new NativeImage(
                source.getFormat(),
                source.getWidth(),
                source.getHeight(),
                false
        );
        copy.copyFrom(source);
        return copy;
    }
}

