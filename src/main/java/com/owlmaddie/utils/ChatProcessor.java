package com.owlmaddie.utils;

public class ChatProcessor {

        // "playName wants to tell you [... the entity_name] said message" (message
        // could be empty)
        public static boolean isFormatted(String input) {
                return input.matches("^[^\\[]+ wants to tell you \\[.+\\s+the\\s+[^\\]]+\\] said .*$");
        }

        public static String getPlayerName(String input) {
                if (!isFormatted(input))
                        return null;
                return input.substring(0, input.indexOf(" wants to tell you")).trim();
        }

        // (everything before the last " the " inside brackets)
        public static String getCustomName(String input) {
                if (!isFormatted(input))
                        return null;
                int bracketStart = input.indexOf('[');
                int bracketEnd = input.indexOf(']');
                String bracketContent = input.substring(bracketStart + 1, bracketEnd);
                int lastTheIndex = bracketContent.lastIndexOf(" the ");
                if (lastTheIndex == -1)
                        return null;
                return bracketContent.substring(0, lastTheIndex).trim();
        }

        // (everything after the last " the " inside brackets)
        public static String getEntityName(String input) {
                if (!isFormatted(input))
                        return null;
                int bracketStart = input.indexOf('[');
                int bracketEnd = input.indexOf(']');
                String bracketContent = input.substring(bracketStart + 1, bracketEnd);
                int lastTheIndex = bracketContent.lastIndexOf(" the ");
                if (lastTheIndex == -1)
                        return null;
                return bracketContent.substring(lastTheIndex + 5).trim();
        }

        // everything after "[...] said "
        public static String getMessage(String input) {
                if (!isFormatted(input))
                        return null;
                int saidIndex = input.indexOf("] said ");
                return input.substring(saidIndex + 7); // 7 = "] said ".length()
        }

        public static String encode(String customName, String entityName, String message, String playerName) {
                if (entityName.contains(" the ")) {
                        throw new Error(String.format("ENTITY NAME (%s) MUST NOT CONTAIN ' the '", entityName));
                }
                return playerName + " wants to tell you [" + customName + " the " + entityName + "] said " + message;
        }
}