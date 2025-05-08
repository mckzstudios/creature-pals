package com.owlmaddie.mixin.client;

import java.util.UUID;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.owlmaddie.utils.ChatProcessor;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;

@Mixin(ChatHud.class)
public final class MixinChatOnMessage {
    // @Inject(method = "onGameMessage", at = @At("HEAD"))
    // private void onGameMessage(MessageType type, Text message, CallbackInfo ci) {
    // String value = message.getString();
    // if (ChatProcessor.isFormatted(value)) {

    // System.out.println("ACTUALLY CANCELLING MSG BECAUSE IT IS FORMATTED");
    // ci.cancel();
    // }

    // }
    // private void onChatMessage(SignedMessage message, GameProfile sender,
    // MessageType.Parameters params, CallbackInfo ci) {
    // String value = message.getContent().getString();

    // if (ChatProcessor.isFormatted(value)) {
    // if (ci.isCancellable()) {
    // System.out.println("ACTUALLY CANCELLING MSG BECAUSE IT IS FORMATTED");
    // ci.cancel();
    // }
    // }
    // }
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", at = @At("HEAD"), cancellable = true)
    public void addMessage(Text message, MessageSignatureData signature, int ticks, MessageIndicator indicator,
            boolean refresh, CallbackInfo ci) {
        if (ChatProcessor.isFormatted(message.getString())) {
            System.out.println("ACTUALLY CANCELLING MSG BECAUSE IT IS FORMATTED");
            if(ci.isCancellable()){
                ci.cancel();
            }
        }
    }
}