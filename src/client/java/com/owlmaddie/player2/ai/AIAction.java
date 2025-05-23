package com.owlmaddie.player2.ai;

/**
 * Represents a single action returned by the LLM.
 */
public class AIAction {
    private final String name;
    private final String arguments;

    public AIAction(String name, String arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        if (arguments != null && !arguments.isEmpty()) {
            return name + "(" + arguments + ")";
        }
        return name;
    }
}
