package com.owlmaddie.player2.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to parse simple <ACTION args> tokens from LLM output.
 */
public class CommandParser {
    private static final Pattern ACTION_PATTERN = Pattern.compile("<([A-Z_]+)([^>]*)>");

    public static List<AIAction> parseActions(String input) {
        List<AIAction> actions = new ArrayList<>();
        Matcher matcher = ACTION_PATTERN.matcher(input);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String args = matcher.group(2) != null ? matcher.group(2).trim() : "";
            actions.add(new AIAction(name, args));
        }
        return actions;
    }
}
