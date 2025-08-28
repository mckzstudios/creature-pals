package com.owlmaddie.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.owlmaddie.network.ServerPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ConfigurationHandler} class loads and saves configuration settings
 * for this mod. It first
 * checks for a config file in the world save folder, and if not found, falls
 * back to the root folder.
 * This allows for global/default settings, or optional server-specific
 * settings.
 */

public class ConfigurationHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturepals");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path serverConfigPath;
    private final Path defaultConfigPath;

    public ConfigurationHandler(MinecraftServer server) {
        this.serverConfigPath = server.getSavePath(WorldSavePath.ROOT).resolve("creaturepals.json");
        this.defaultConfigPath = Paths.get(".", "creaturepals.json"); // Assumes the default location is the server root
                                                                      // or a similar logical default
    }

    public Config loadConfig() {
        Config config = loadConfigFromFile(serverConfigPath);
        if (config == null) {
            config = loadConfigFromFile(defaultConfigPath);
        }
        return config != null ? config : new Config(); // Return new config if both are null
    }

    public boolean saveConfig(Config config, boolean useServerConfig) {
        Path path = useServerConfig ? serverConfigPath : defaultConfigPath;
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(config, writer);
            return true;
        } catch (IOException e) {
            String errorMessage = "Error saving `creaturepals.json`. Creature Pals config was not saved. "
                    + e.getMessage();
            LOGGER.error(errorMessage, e);
            ServerPackets.sendErrorToAllOps(ServerPackets.serverInstance, errorMessage);
            return false;
        }
    }

    private Config loadConfigFromFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            return null; // File does not exist or other IO errors
        }
    }

    public static class Config {
        private String apiKey = "";
        private String url = "https://api.player2.game/v1/chat/completions";
        private String model = "gpt-3.5-turbo";
        private int maxContextTokens = 16385;
        private int maxOutputTokens = 200;
        private double percentOfContext = 0.75;
        private int timeout = 20; // 20 second timeout
        private boolean chatBubbles = true;
        private List<String> whitelist = new ArrayList<>();
        private List<String> blacklist = new ArrayList<>();
        private String story = "";

        // Getters and setters for existing fields
        public String getApiKey() {
            String apiKey = System.getProperty("PLAYER2_API_KEY");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return apiKey.trim();
            }

            // Then try environment variable
            apiKey = System.getenv("PLAYER2_API_KEY");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return apiKey.trim();
            }

            // Finally try reading from file
            try {
                String minecraftDir = System.getProperty("user.home") + "/AppData/Roaming/.minecraft";
                java.io.File file = new java.io.File(minecraftDir, "p2key.txt");
                if (file.exists()) {
                    apiKey = new String(java.nio.file.Files.readAllBytes(file.toPath())).trim();
                    if (!apiKey.isEmpty()) {
                        // Store in system properties for this session
                        System.setProperty("PLAYER2_API_KEY", apiKey);
                        return apiKey;
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to read API key from file: " + e.getMessage());
            }
            return null;
        }

        public void setApiKey(String apiKey) {
            if (apiKey.startsWith("cc_") && apiKey.length() == 15) {
                // Update URL if a Creature Pals API key is detected
                setUrl("https://api.creaturepals.com/v1/chat/completions");
            } else if (apiKey.startsWith("sk-")) {
                // Update URL if an OpenAI API key is detected
                setUrl("https://api.openai.com/v1/chat/completions");
            } else if (apiKey.startsWith("p2_") || apiKey.length() >= 32) {
                // Update URL if a Player2 API key is detected
                setUrl("https://api.player2.game/v1/chat/completions");
            }
            this.apiKey = apiKey;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getMaxContextTokens() {
            return maxContextTokens;
        }

        public void setMaxContextTokens(int maxContextTokens) {
            this.maxContextTokens = maxContextTokens;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public double getPercentOfContext() {
            return percentOfContext;
        }

        public void setPercentOfContext(double percentOfContext) {
            this.percentOfContext = percentOfContext;
        }

        public List<String> getWhitelist() {
            return whitelist;
        }

        public void setWhitelist(List<String> whitelist) {
            this.whitelist = whitelist;
        }

        public List<String> getBlacklist() {
            return blacklist;
        }

        public void setBlacklist(List<String> blacklist) {
            this.blacklist = blacklist;
        }

        public String getStory() {
            return story;
        }

        public void setStory(String story) {
            this.story = story;
        }

        // Add getter and setter
        public boolean getChatBubbles() {
            return chatBubbles;
        }

        public void setChatBubbles(boolean chatBubblesEnabled) {
            this.chatBubbles = chatBubblesEnabled;
        }
    }
}
