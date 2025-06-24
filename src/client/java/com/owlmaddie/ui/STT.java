package com.owlmaddie.ui;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class STT {
    
    public static KeyBinding sttKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chatclef.sttKey",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z, // by default, the key is Z
                "category.chatclef.keybindings"));    
}
