package com.owlmaddie.player2;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.spongepowered.include.com.google.gson.JsonArray;

import com.google.gson.JsonObject;

public class TTS {
    public static final ExecutorService ttsThread = Executors.newSingleThreadExecutor();
    public static ConcurrentHashMap<UUID, String> voiceIds = new ConcurrentHashMap<>();
    public static List<String> voices = null;
    private static final Random random = new Random();

    public static void speak(String message, UUID entityId) {
        ttsThread.submit(() -> {
            voiceIds.computeIfAbsent(entityId, id -> {
                if (voices == null) {
                    voices = Player2APIService.getVoices();
                }
                int index = random.nextInt(voices.size());
                return voices.get(index);
            });
            Player2APIService.textToSpeech(message, voiceIds.get(entityId));
        });
    }

}
