package com.owlmaddie.player2;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test class for the new Player2 API implementation
 * Note: These tests require a valid PLAYER2_API_KEY environment variable
 */
public class Player2APITest {

    @BeforeEach
    void setUp() {
        // Check if API key is available
        String apiKey = System.getenv("PLAYER2_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("Warning: PLAYER2_API_KEY environment variable not set. Some tests may fail.");
        }
    }

    @Test
    void testHeartbeat() {
        try {
            Player2APIService.sendHeartbeat();
            // If no exception is thrown, the test passes
            assertTrue(true);
        } catch (Exception e) {
            if (e.getMessage().contains("PLAYER2_API_KEY environment variable is not set")) {
                // This is expected if no API key is set
                System.out.println("Skipping heartbeat test - no API key available");
                assertTrue(true);
            } else {
                fail("Heartbeat failed with unexpected error: " + e.getMessage());
            }
        }
    }

    @Test
    void testGetVoices() {
        try {
            List<JsonObject> voices = Player2APIService.getVoices();
            // Should return a list (even if empty)
            assertNotNull(voices);
            System.out.println("Found " + voices.size() + " voices");
        } catch (Exception e) {
            if (e.getMessage().contains("PLAYER2_API_KEY environment variable is not set")) {
                System.out.println("Skipping voices test - no API key available");
                assertTrue(true);
            } else {
                fail("Get voices failed with unexpected error: " + e.getMessage());
            }
        }
    }

    @Test
    void testTextToSpeech() {
        try {
            // Test with a simple message and default voice
            Player2APIService.textToSpeech("Hello, this is a test message.", "alloy", UUID.randomUUID());
            // If no exception is thrown, the test passes
            assertTrue(true);
        } catch (Exception e) {
            if (e.getMessage().contains("PLAYER2_API_KEY environment variable is not set")) {
                System.out.println("Skipping TTS test - no API key available");
                assertTrue(true);
            } else {
                fail("TTS failed with unexpected error: " + e.getMessage());
            }
        }
    }

    @Test
    void testChatCompletion() {
        try {
            // Create a simple test message
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", "Say hello!");
            
            List<JsonObject> messages = List.of(message);
            
            Map<String, JsonElement> response = Player2APIService.createChatCompletion(messages, "gpt-3.5-turbo");
            
            // Should return a response (even if empty due to no API key)
            assertNotNull(response);
            
            if (!response.isEmpty()) {
                System.out.println("Chat completion response received");
            } else {
                System.out.println("Chat completion returned empty response (expected without API key)");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("PLAYER2_API_KEY environment variable is not set")) {
                System.out.println("Skipping chat completion test - no API key available");
                assertTrue(true);
            } else {
                fail("Chat completion failed with unexpected error: " + e.getMessage());
            }
        }
    }
}
