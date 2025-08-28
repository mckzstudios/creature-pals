package com.owlmaddie.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.owlmaddie.player2.Player2StartupHandler;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.MinecraftClient;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void onTitleScreenRender(CallbackInfo ci) {
        // Check Player2 API key when title screen is rendered
        // This ensures it happens after the mixin is fully loaded
        if (!Player2StartupHandler.hasCheckedApiKey()) {
            System.out.println("TitleScreenMixin: Title screen rendering, checking API key...");
            Player2StartupHandler.checkApiKeyOnStartup();
        }
    }
}
