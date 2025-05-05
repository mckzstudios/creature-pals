package com.owlmaddie.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.owlmaddie.utils.ChatProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;

@Mixin(ChatScreen.class)
public class MixinChatPreProcess {

    @Inject(at = @At("HEAD"), method = "sendMessage")
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfoReturnable<Boolean> cir) {
        if (chatText.contains(ChatProcessor.splitter)) {
            String visible = chatText.split(ChatProcessor.splitter)[0].trim();
            MinecraftClient.getInstance().player.sendMessage(Text.of("Chat message recieved: " + visible));
            if (cir.isCancellable()) {
                cir.cancel();
            }
        }
    }
}