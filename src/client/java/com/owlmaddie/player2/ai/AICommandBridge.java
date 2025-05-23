package com.owlmaddie.player2.ai;

import java.util.List;

/**
 * Simple bridge that uses {@link OpenAIChatAI} to get actions and execute them.
 * This is only a minimal example and would need to be expanded to actually
 * perform Minecraft tasks.
 */
public class AICommandBridge {
    private final OpenAIChatAI ai;

    public AICommandBridge(OpenAIChatAI ai) {
        this.ai = ai;
    }

    /**
     * Send a chat message to the LLM and execute any returned actions.
     */
    public void handlePlayerChat(String message) {
        String response = ai.getCompletion(message);
        if (response == null || response.isEmpty()) {
            return;
        }
        List<AIAction> actions = CommandParser.parseActions(response);
        for (AIAction action : actions) {
            executeAction(action);
        }
    }

    private void executeAction(AIAction action) {
        // Placeholder for actual integration with in-game commands.
        System.out.println("[AICommandBridge] Executing action: " + action);
    }
}
