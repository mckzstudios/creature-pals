package com.owlmaddie.utils;

import static com.owlmaddie.network.ServerPackets.LOGGER;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatProcessor {

        // "[... the entity_name] said message" (message could be empty)
        public static boolean isFormatted(String input) {
                return input.matches("^\\[.+\\s+the\\s+[^\\]]+\\] said .*$");
        }

        // (everything before the last " the " inside brackets)
        public static String getCustomName(String input) {
                if (!isFormatted(input))
                        return null;
                String bracketContent = input.substring(1, input.indexOf(']'));
                int lastTheIndex = bracketContent.lastIndexOf(" the ");
                if (lastTheIndex == -1)
                        return null;
                return bracketContent.substring(0, lastTheIndex).trim();
        }

        // (everything after the last " the " inside brackets)
        public static String getEntityName(String input) {
                if (!isFormatted(input))
                        return null;
                String bracketContent = input.substring(1, input.indexOf(']'));
                int lastTheIndex = bracketContent.lastIndexOf(" the ");
                if (lastTheIndex == -1)
                        return null;
                return bracketContent.substring(lastTheIndex + 5).trim(); // 5 = " the ".length()
        }

        // everything after "[...] said "
        public static String getMessage(String input) {
                if (!isFormatted(input))
                        return null;
                int saidIndex = input.indexOf("] said ");
                return input.substring(saidIndex + 7); // 7 = "] said ".length()
        }

        public static String encode(String customName, String entityName, String message) {
                if (entityName.contains(" the ")) {
                        throw new Error(String.format("ENTITY NAME (%s) MUST NOT CONTAIN ' the '", entityName));
                }
                return "[" + customName + " the " + entityName + "] said " + message;
        }
}