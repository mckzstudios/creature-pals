package com.owlmaddie.utils;

public class ChatProcessor {
        public static String splitter = "AAA";

        public static boolean isFormatted(String input) {
                return input.contains(splitter);
        }

        public static String getBack(String input) {
                if (!isFormatted(input)) {
                        return input;
                }
                return input.split(splitter)[1].trim();
        }

        public static String getFront(String input) {
                if (!isFormatted(input)) {
                        return input;
                }
                return input.split(splitter)[0].trim();
        }

        public static String encode(String front, String back) {
                return front + splitter + back;
        }
}