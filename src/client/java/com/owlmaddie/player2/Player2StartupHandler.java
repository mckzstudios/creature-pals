package com.owlmaddie.player2;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.screen.ScreenTexts;
import com.owlmaddie.player2.Player2OAuthHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles Player2 API key validation and setup on Minecraft startup
 */
public class Player2StartupHandler {
    private static boolean hasCheckedApiKey = false;
    private static boolean isApiKeyValid = false;
    
    /**
     * Check if the Player2 API key is set and valid
     * This should be called on Minecraft startup
     */
    public static void checkApiKeyOnStartup() {
        if (hasCheckedApiKey) {
            return; // Already checked
        }
        
        hasCheckedApiKey = true;
        System.out.println("Player2StartupHandler: Checking API key on startup...");
        
        // Check if API key is set (try environment variable first, then system property)
        String apiKey = System.getenv("PLAYER2_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getProperty("PLAYER2_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            // No API key set, show setup screen
            System.out.println("Player2StartupHandler: No API key found, showing setup screen");
            showApiKeySetupScreen();
            return;
        }
        
        System.out.println("Player2StartupHandler: API key found, validating...");
        
        // API key is set, validate it
        validateApiKey(apiKey);
    }
    
    /**
     * Validate the provided API key by sending a test request
     */
    private static void validateApiKey(String apiKey) {
        CompletableFuture.runAsync(() -> {
            try {
                // Test the API key with a simple heartbeat
                Player2APIService.sendHeartbeat();
                isApiKeyValid = true;
                System.out.println("Player2 API key validated successfully");
            } catch (Exception e) {
                isApiKeyValid = false;
                System.err.println("Player2 API key validation failed: " + e.getMessage());
                
                // Show error screen on main thread
                MinecraftClient.getInstance().execute(() -> {
                    showApiKeyErrorScreen(e.getMessage());
                });
            }
        });
    }
    
    /**
     * Show the API key setup screen
     */
    private static void showApiKeySetupScreen() {
        System.out.println("Player2StartupHandler: showApiKeySetupScreen called");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            // If we're on the title screen, show the OAuth screen directly
            // Otherwise, show it on the current screen
            Screen currentScreen = client.currentScreen;
            System.out.println("Player2StartupHandler: Current screen: " + (currentScreen != null ? currentScreen.getClass().getSimpleName() : "null"));
            if (currentScreen instanceof TitleScreen) {
                System.out.println("Player2StartupHandler: On title screen, starting OAuth flow");
                Player2OAuthHandler.startOAuthFlow(null);
            } else if (currentScreen != null) {
                System.out.println("Player2StartupHandler: On other screen, starting OAuth flow");
                Player2OAuthHandler.startOAuthFlow(currentScreen);
            }
        } else {
            System.out.println("Player2StartupHandler: Minecraft client is null!");
        }
    }
    
    /**
     * Show the API key error screen
     */
    private static void showApiKeyErrorScreen(String errorMessage) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.currentScreen != null) {
            client.setScreen(new Player2ApiKeyErrorScreen(client.currentScreen, errorMessage));
        }
    }
    
    /**
     * Check if the API key is currently valid
     */
    public static boolean isApiKeyValid() {
        return isApiKeyValid;
    }
    
    /**
     * Check if the API key has been checked
     */
    public static boolean hasCheckedApiKey() {
        return hasCheckedApiKey;
    }
    
    /**
     * Force revalidation of the API key
     */
    public static void revalidateApiKey() {
        hasCheckedApiKey = false;
        isApiKeyValid = false;
        checkApiKeyOnStartup();
    }
    
    /**
     * Clear the API key from system properties
     */
    public static void clearApiKey() {
        System.clearProperty("PLAYER2_API_KEY");
        hasCheckedApiKey = false;
        isApiKeyValid = false;
    }
    
    /**
     * Set the API key in system properties and file
     */
    public static void setApiKey(String apiKey) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            // Store in system properties for current session
            System.setProperty("PLAYER2_API_KEY", apiKey.trim());
            
            // Store in file for persistence
            try {
                String minecraftDir = null;
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    minecraftDir = System.getenv("APPDATA") + "\\.minecraft";
                } else if (os.contains("mac")) {
                    minecraftDir = System.getProperty("user.home") + "/Library/Application Support/minecraft";
                } else {
                    // Assume Linux/Unix
                    minecraftDir = System.getProperty("user.home") + "/.minecraft";
                }
                java.io.File file = new java.io.File(minecraftDir, "p2key.txt");
                java.nio.file.Files.write(file.toPath(), apiKey.trim().getBytes());
                System.out.println("Player2 API key saved to: " + file.getAbsolutePath());
            } catch (Exception e) {
                System.err.println("Failed to save API key to file: " + e.getMessage());
            }
            
            hasCheckedApiKey = false;
            isApiKeyValid = false;
        }
    }
    
    /**
     * Get the API key from system properties, environment variable, or file
     */
    public static String getApiKey() {
        // First try system properties (current session)
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
    
    /**
     * Screen for setting up the Player2 API key
     */
    public static class Player2ApiKeySetupScreen extends Screen {
        private final Screen parent;
        private TextFieldWidget apiKeyField;
        private ButtonWidget saveButton;
        private ButtonWidget skipButton;
        private ButtonWidget helpButton;
        
        public Player2ApiKeySetupScreen(Screen parent) {
            super(Text.literal("Player2 API Key Setup"));
            this.parent = parent;
            System.out.println("Player2ApiKeySetupScreen: Constructor called with parent: " + (parent != null ? parent.getClass().getSimpleName() : "null"));
        }
        
        @Override
        protected void init() {
            super.init();
            System.out.println("Player2ApiKeySetupScreen: init() called");
            
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Title
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Player2 API Key Required"),
                button -> {}
            ).dimensions(centerX - 100, centerY - 80, 200, 20).build());
            
            // Description
            addDrawableChild(ButtonWidget.builder(
                Text.literal("This mod requires a Player2 API key to function."),
                button -> {}
            ).dimensions(centerX - 150, centerY - 50, 300, 20).build());
            
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Get your key at: player2.game"),
                button -> {}
            ).dimensions(centerX - 150, centerY - 30, 300, 20).build());
            
            // API Key input field
            apiKeyField = new TextFieldWidget(
                textRenderer,
                centerX - 100,
                centerY,
                200,
                20,
                Text.literal("Enter your Player2 API key")
            );
            apiKeyField.setMaxLength(100);
            apiKeyField.setChangedListener(text -> {
                saveButton.active = !text.trim().isEmpty();
            });
            
            // Save button
            saveButton = ButtonWidget.builder(
                Text.literal("Save & Validate"),
                button -> saveApiKey()
            ).dimensions(centerX - 100, centerY + 30, 200, 20).build();
            saveButton.active = false;
            
            // Skip button
            skipButton = ButtonWidget.builder(
                Text.literal("Skip for now"),
                button -> close()
            ).dimensions(centerX - 100, centerY + 60, 200, 20).build();
            
            // Help button
            helpButton = ButtonWidget.builder(
                Text.literal("Help"),
                button -> showHelp()
            ).dimensions(centerX - 100, centerY + 90, 200, 20).build();
            
            addSelectableChild(apiKeyField);
            addDrawableChild(apiKeyField);
            addDrawableChild(saveButton);
            addDrawableChild(skipButton);
            addDrawableChild(helpButton);
            
            setInitialFocus(apiKeyField);
        }
        
        private void saveApiKey() {
            String apiKey = apiKeyField.getText().trim();
            if (apiKey.isEmpty()) {
                return;
            }
            
            // Set the API key in system property for this session
            try {
                System.setProperty("PLAYER2_API_KEY", apiKey);
                
                // Validate the key
                validateApiKey(apiKey);
                
                // Show success message
                client.setScreen(new ConfirmScreen(
                    this::onValidationComplete,
                    Text.literal("API Key Saved"),
                    Text.literal("Your Player2 API key has been saved for this session.\n\nNote: You'll need to set this as an environment variable for permanent storage."),
                    Text.literal("Continue"),
                    ScreenTexts.CANCEL
                ));
                
            } catch (Exception e) {
                client.setScreen(new ConfirmScreen(
                    button -> client.setScreen(this),
                    Text.literal("Error"),
                    Text.literal("Failed to save API key: " + e.getMessage()),
                    Text.literal("OK"),
                    ScreenTexts.CANCEL
                ));
            }
        }
        
        private void onValidationComplete(boolean confirmed) {
            if (confirmed) {
                close();
            }
        }
        
        private void showHelp() {
            client.setScreen(new ConfirmScreen(
                button -> client.setScreen(this),
                Text.literal("How to Set Player2 API Key"),
                Text.literal("1. Visit player2.game and sign up\n2. Get your API key from your account\n3. Set it as an environment variable:\n\nWindows (PowerShell):\n$env:PLAYER2_API_KEY=\"your_key\"\n\nWindows (CMD):\nset PLAYER2_API_KEY=your_key\n\nLinux/macOS:\nexport PLAYER2_API_KEY=\"your_key\"\n\n4. Restart Minecraft"),
                Text.literal("OK"),
                ScreenTexts.CANCEL
            ));
        }
        
        @Override
        public void close() {
            if (parent != null) {
                client.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                client.setScreen(new TitleScreen());
            }
        }
        
        @Override
        public boolean shouldPause() {
            return false;
        }
    }
    
    /**
     * Screen for showing API key validation errors
     */
    public static class Player2ApiKeyErrorScreen extends Screen {
        private final Screen parent;
        private final String errorMessage;
        
        public Player2ApiKeyErrorScreen(Screen parent, String errorMessage) {
            super(Text.literal("Player2 API Key Error"));
            this.parent = parent;
            this.errorMessage = errorMessage;
        }
        
        @Override
        protected void init() {
            super.init();
            
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Error title
            addDrawableChild(ButtonWidget.builder(
                Text.literal("API Key Validation Failed"),
                button -> {}
            ).dimensions(centerX - 100, centerY - 80, 200, 20).build());
            
            // Error message
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Error: " + errorMessage),
                button -> {}
            ).dimensions(centerX - 100, centerY - 50, 300, 20).build());
            
            // Retry button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Retry"),
                button -> retryValidation()
            ).dimensions(centerX - 100, centerY, 200, 20).build());
            
            // Setup button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Setup New Key"),
                button -> setupNewKey()
            ).dimensions(centerX - 100, centerY + 30, 200, 20).build());
            
            // Continue anyway button
            addDrawableChild(ButtonWidget.builder(
                Text.literal("Continue Anyway"),
                button -> close()
            ).dimensions(centerX - 100, centerY + 60, 200, 20).build());
        }
        
        private void retryValidation() {
            Player2StartupHandler.revalidateApiKey();
            close();
        }
        
        private void setupNewKey() {
            client.setScreen(new Player2ApiKeySetupScreen(this));
        }
        
        @Override
        public void close() {
            if (parent != null) {
                client.setScreen(parent);
            } else {
                // If no parent screen, go back to title screen
                client.setScreen(new TitleScreen());
            }
        }
        
        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
