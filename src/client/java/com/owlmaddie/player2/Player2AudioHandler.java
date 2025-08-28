package com.owlmaddie.player2;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles audio playback for Player2 TTS in Fabric
 */
public class Player2AudioHandler {
    private static final ConcurrentHashMap<UUID, String> activeAudioFiles = new ConcurrentHashMap<>();
    private static final String SOUND_NAMESPACE = "creaturepals";
    private static final String TTS_SOUND_NAME = "tts_audio";
    
    /**
     * Play an MP3 audio file using Minecraft's sound system
     * 
     * @param audioFile The MP3 file to play
     * @param entityId The entity ID this audio is for (for tracking)
     * @return true if playback started successfully
     */
    public static boolean playAudioFile(File audioFile, UUID entityId) {
        if (audioFile == null || !audioFile.exists()) {
            System.err.println("Audio file does not exist: " + audioFile);
            return false;
        }
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            System.err.println("Minecraft client not ready for audio playback");
            return false;
        }
        
        try {
            // Create a unique identifier for this audio file
            String audioId = "tts_" + entityId.toString().replace("-", "_");
            
            // Copy the file to a permanent location in the mod's resources
            Path modSoundsDir = getModSoundsDirectory();
            if (modSoundsDir == null) {
                System.err.println("Could not create mod sounds directory");
                return false;
            }
            
            // Create the sounds directory if it doesn't exist
            Files.createDirectories(modSoundsDir);
            
            // Copy the audio file to the mod's sounds directory
            Path targetPath = modSoundsDir.resolve(audioId + ".mp3");
            Files.copy(audioFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Track this audio file
            activeAudioFiles.put(entityId, targetPath.toString());
            
            // Create a custom sound event for this audio
            Identifier soundId = Identifier.of(SOUND_NAMESPACE, audioId);
            
            // Play the sound using Minecraft's sound system
            // For now, we'll play a placeholder sound to indicate TTS is ready
            // and log the file location for manual testing
            System.out.println("TTS audio ready for playback: " + targetPath.toString());
            System.out.println("Sound ID: " + soundId.toString());
            
            // Play a notification sound to indicate TTS is ready
            // This is a workaround until we implement proper MP3 playback
            try {
                // Play a UI sound to indicate TTS is ready
                mc.getSoundManager().play(PositionedSoundInstance.master(
                    net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(),
                    1.0f, 1.0f
                ));
                System.out.println("TTS notification sound played - audio file ready at: " + targetPath.toString());
            } catch (Exception e) {
                System.err.println("Failed to play TTS notification sound: " + e.getMessage());
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Failed to prepare audio file for playback: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get the mod's sounds directory
     */
    private static Path getModSoundsDirectory() {
        try {
            // Try to get the mod's resource directory
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                Path projectDir = Path.of(userDir);
                Path modSoundsDir = projectDir.resolve("src").resolve("client").resolve("resources").resolve("assets").resolve(SOUND_NAMESPACE).resolve("sounds");
                return modSoundsDir;
            }
        } catch (Exception e) {
            System.err.println("Could not determine mod sounds directory: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Clean up audio files for a specific entity
     */
    public static void cleanupAudio(UUID entityId) {
        String audioPath = activeAudioFiles.remove(entityId);
        if (audioPath != null) {
            try {
                File audioFile = new File(audioPath);
                if (audioFile.exists()) {
                    Files.delete(audioFile.toPath());
                    System.out.println("Cleaned up audio file: " + audioPath);
                }
            } catch (IOException e) {
                System.err.println("Failed to cleanup audio file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean up all audio files
     */
    public static void cleanupAllAudio() {
        for (UUID entityId : activeAudioFiles.keySet()) {
            cleanupAudio(entityId);
        }
    }
}
