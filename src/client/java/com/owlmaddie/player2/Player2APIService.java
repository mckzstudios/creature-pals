package com.owlmaddie.player2;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gson.*;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class Player2APIService {

    private static final String BASE_URL = "https://api.player2.game";
    
    /**
     * Get the Player2 API key from Player2StartupHandler
     * @return The API key, or null if not set
     */
    private static String getApiKey() {
        return com.owlmaddie.player2.Player2StartupHandler.getApiKey();
    }

    /**
     * Handles boilerplate logic for interacting with the Player2 API endpoint
     *
     * @param endpoint    The API endpoint (e.g., "/v1/chat/completions").
     * @param postRequest True -> POST request, False -> GET request
     * @param requestBody JSON payload to send.
     * @return A map containing JSON keys and values from the response.
     * @throws Exception If there is an error.
     */
    private static Map<String, JsonElement> sendRequest(String endpoint, boolean postRequest, JsonObject requestBody)
            throws Exception {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("PLAYER2_API_KEY environment variable is not set");
        }

        URL url = new URI(BASE_URL + endpoint).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(postRequest ? "POST" : "GET");

        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Accept", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);

        System.out.printf("Sending %s request to %s\n", postRequest ? "POST" : "GET", endpoint);

        if (postRequest && requestBody != null) {
            System.out.printf("Request Body: %s\n", requestBody);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        int responseCode = connection.getResponseCode();

        if (responseCode != 200) {
            // read error info:
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8));
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                errorReader.close();
                System.err.println("Error response: " + errorResponse);
            }
            throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        JsonObject jsonResponse = JsonParser.parseString(response.toString()).getAsJsonObject();
        Map<String, JsonElement> responseMap = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonResponse.entrySet()) {
            responseMap.put(entry.getKey(), entry.getValue());
        }
        System.out.printf("DONE %s request to %s \n", postRequest ? "POST" : "GET", endpoint);

        return responseMap;
    }

    /**
     * Send a heartbeat to check API status and get client version
     */
    public static void sendHeartbeat() {
        try {
            System.out.println("Sending Heartbeat to Player2 API");
            Map<String, JsonElement> responseMap = sendRequest("/v1/health", false, null);
            if (responseMap.containsKey("status")) {
                String status = responseMap.get("status").getAsString();
                System.out.println("Heartbeat Successful - Status: " + status);
            }
        } catch (Exception e) {
            System.err.printf("Heartbeat Fail: %s", e.getMessage());
        }
    }

    /**
     * Convert text to speech using Player2 TTS API
     * 
     * @param message The text message to convert to speech
     * @param voiceId The voice ID to use for TTS
     */
    public static void textToSpeech(String message, String voiceId) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("text", message);
            requestBody.addProperty("voice_id", voiceId);
            requestBody.addProperty("model", "tts-1"); // Using OpenAI TTS model
            requestBody.addProperty("response_format", "mp3");
            requestBody.addProperty("speed", 1.0);
            
            System.out.println("Sending TTS request: " + message);
            Map<String, JsonElement> response = sendRequest("/v1/audio/speech", true, requestBody);
            
            // Handle the audio response - Player2 returns audio data
            if (response.containsKey("audio_url")) {
                String audioUrl = response.get("audio_url").getAsString();
                System.out.println("TTS audio generated: " + audioUrl);
                // TODO: Implement audio playback logic
            }
        } catch (Exception e) {
            System.err.println("TTS request failed: " + e.getMessage());
        }
    }

    /**
     * Get available voices from Player2 API
     * 
     * @return List of available voice objects
     */
    public static List<JsonObject> getVoices() {
        try {
            Map<String, JsonElement> responseMap = sendRequest("/v1/audio/voices", false, null);
            JsonElement voicesJsonElement = responseMap.get("voices");
            if (voicesJsonElement != null && voicesJsonElement.isJsonArray()) {
                JsonArray voicesJsonArray = voicesJsonElement.getAsJsonArray();

                List<JsonObject> voiceIds = new ArrayList<>();
                for (JsonElement voiceElement : voicesJsonArray) {
                    JsonObject voiceObject = voiceElement.getAsJsonObject();
                    voiceIds.add(voiceObject);
                }
                return voiceIds;
            }
        } catch (Exception e) {
            System.err.println("Failed to get voices: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Create a chat completion using Player2 Chat API
     * 
     * @param messages List of chat messages
     * @param model The model to use (e.g., "gpt-4", "gpt-3.5-turbo")
     * @return The chat completion response
     */
    public static Map<String, JsonElement> createChatCompletion(List<JsonObject> messages, String model) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.add("messages", new Gson().toJsonTree(messages));
            requestBody.addProperty("max_tokens", 1000);
            requestBody.addProperty("temperature", 0.7);
            
            System.out.println("Sending chat completion request");
            return sendRequest("/v1/chat/completions", true, requestBody);
        } catch (Exception e) {
            System.err.println("Chat completion request failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Create an image using Player2 DALL-E API
     * 
     * @param prompt The image generation prompt
     * @param size The image size (e.g., "1024x1024", "1792x1024", "1024x1792")
     * @return The image generation response
     */
    public static Map<String, JsonElement> createImage(String prompt, String size) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("prompt", prompt);
            requestBody.addProperty("model", "dall-e-3");
            requestBody.addProperty("size", size);
            requestBody.addProperty("quality", "standard");
            requestBody.addProperty("n", 1);
            
            System.out.println("Sending image generation request: " + prompt);
            return sendRequest("/v1/images/generations", true, requestBody);
        } catch (Exception e) {
            System.err.println("Image generation request failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Transcribe audio using Player2 Whisper API
     * 
     * @param audioData Base64 encoded audio data
     * @param model The Whisper model to use (e.g., "whisper-1")
     * @return The transcription response
     */
    public static Map<String, JsonElement> transcribeAudio(String audioData, String model) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("file", audioData);
            requestBody.addProperty("model", model);
            requestBody.addProperty("response_format", "json");
            requestBody.addProperty("language", "en");
            
            System.out.println("Sending audio transcription request");
            return sendRequest("/v1/audio/transcriptions", true, requestBody);
        } catch (Exception e) {
            System.err.println("Audio transcription request failed: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
