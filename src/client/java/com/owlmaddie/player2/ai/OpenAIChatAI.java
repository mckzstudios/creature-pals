package com.owlmaddie.player2.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.owlmaddie.player2.Player2APIService;

import java.util.Map;

/**
 * Helper for interacting with the local OpenAI compatible endpoint.
 */
public class OpenAIChatAI {
    private final String model;

    public OpenAIChatAI(String model) {
        this.model = model;
    }

    /**
     * Send a single user message to the LLM and return the raw response text.
     */
    public String getCompletion(String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        JsonArray messages = new JsonArray();
        JsonObject m = new JsonObject();
        m.addProperty("role", "user");
        m.addProperty("content", message);
        messages.add(m);
        payload.add("messages", messages);

        try {
            Map<String, JsonElement> resp = Player2APIService.sendChatCompletion(payload);
            if (resp.containsKey("choices")) {
                JsonArray choices = resp.get("choices").getAsJsonArray();
                if (!choices.isEmpty()) {
                    JsonObject first = choices.get(0).getAsJsonObject();
                    JsonObject msg = first.getAsJsonObject("message");
                    return msg.get("content").getAsString();
                }
            }
        } catch (Exception e) {
            System.err.println("Chat completion failed: " + e.getMessage());
        }
        return null;
    }
}
