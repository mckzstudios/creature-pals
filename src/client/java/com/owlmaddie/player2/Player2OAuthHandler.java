package com.owlmaddie.player2;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.screen.ScreenTexts;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handles Player2 Device Authorization Flow authentication
 */
public class Player2OAuthHandler {
    private static final String OAUTH_BASE_URL = "https://api.player2.game/v1";
    private static final String DEVICE_AUTH_URL = OAUTH_BASE_URL + "/login/device/new";
    private static final String OAUTH_TOKEN_URL = OAUTH_BASE_URL + "/login/device/token";
    private static final String CLIENT_ID = "01977e1e-0b52-7269-ac8d-97522d4c1c21";

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private static boolean isAuthenticating = false;

    /**
     * Start the Device Authorization Flow
     */
    public static void startOAuthFlow(Screen parentScreen) {
        if (isAuthenticating) {
            return; // Already authenticating
        }

        isAuthenticating = true;

        // Start the device authorization flow
        startDeviceAuthorization(parentScreen);
    }

    /**
     * Start the device authorization process
     */
    private static void startDeviceAuthorization(Screen parentScreen) {
        CompletableFuture.runAsync(() -> {
            try {
                // Step 1: Request device authorization
                JsonObject deviceRequest = new JsonObject();
                deviceRequest.addProperty("client_id", CLIENT_ID);
                deviceRequest.addProperty("scope", "api");

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(DEVICE_AUTH_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(deviceRequest.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Log the raw response for debugging
                System.out.println("Device authorization response status: " + response.statusCode());
                System.out.println("Device authorization response body: " + response.body());

                if (response.statusCode() == 200) {
                    JsonObject deviceResponse = JsonParser.parseString(response.body()).getAsJsonObject();

                    // Debug: Log the full response to see what fields are available
                    System.out.println("Device authorization response: " + deviceResponse.toString());

                    // Check if all required fields are present
                    if (!deviceResponse.has("deviceCode")) {
                        throw new RuntimeException("Missing 'deviceCode' field in response");
                    }
                    if (!deviceResponse.has("userCode")) {
                        throw new RuntimeException("Missing 'userCode' field in response");
                    }
                    if (!deviceResponse.has("verificationUri")) {
                        throw new RuntimeException("Missing 'verificationUri' field in response");
                    }
                    if (!deviceResponse.has("interval")) {
                        throw new RuntimeException("Missing 'interval' field in response");
                    }

                    // Extract device code and user code
                    String deviceCode = deviceResponse.get("deviceCode").getAsString();
                    String userCode = deviceResponse.get("userCode").getAsString();
                    String verificationUri = deviceResponse.get("verificationUri").getAsString();
                    int interval = deviceResponse.get("interval").getAsInt();

                    // Show the authentication screen with the user code
                    MinecraftClient.getInstance().execute(() -> {
                        MinecraftClient mcClient = MinecraftClient.getInstance();
                        if (mcClient != null) {
                            mcClient.setScreen(new Player2OAuthScreen(parentScreen, deviceCode, userCode, verificationUri, interval));
                        }
                    });

                    // Start polling for token
                    startTokenPolling(deviceCode, interval);

                } else {
                    System.err.println("Device authorization request failed with status " + response.statusCode());
                    System.err.println("Response body: " + response.body());
                    throw new RuntimeException("Device authorization request failed with status " + response.statusCode() + ": " + response.body());
                }

            } catch (Exception e) {
                System.err.println("Device authorization failed: " + e.getMessage());
                isAuthenticating = false;
            }
        });
    }

    /**
     * Poll for the access token
     */
    private static void startTokenPolling(String deviceCode, int interval) {
        executor.scheduleWithFixedDelay(() -> {
            try {
                JsonObject tokenRequest = new JsonObject();
                tokenRequest.addProperty("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
                tokenRequest.addProperty("client_id", CLIENT_ID);
                tokenRequest.addProperty("device_code", deviceCode);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OAUTH_TOKEN_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(tokenRequest.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                
                // Debug: Log the token polling response
                System.out.println("Token polling response status: " + response.statusCode());
                System.out.println("Token polling response body: " + response.body());

                if (response.statusCode() == 200) {
                    JsonObject tokenResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    
                    if (tokenResponse.has("p2Key")) {
                        String p2Key = tokenResponse.get("p2Key").getAsString();

                        // Store the token
                        Player2StartupHandler.setApiKey(p2Key);

                        // Stop polling
                        executor.shutdown();

                        System.out.println("Successfully obtained Player2 API key via Device Authorization Flow");
                        
                        // Notify the user on the main thread
                        MinecraftClient.getInstance().execute(() -> {
                            showSuccessNotification();
                        });

                    }
                } else if (response.statusCode() == 400) {
                    // Still pending, continue polling
                    JsonObject errorResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (errorResponse.has("error") && "authorization_pending".equals(errorResponse.get("error").getAsString())) {
                        // This is expected, continue polling
                        return;
                    }
                }

            } catch (Exception e) {
                System.err.println("Token polling failed: " + e.getMessage());
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    /**
     * Open the user's default web browser
     */
    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                // Fallback for systems without desktop support
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("cmd /c start " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to open browser: " + e.getMessage());
        }
    }


    /**
     * Check if we're currently in the authentication process
     */
    public static boolean isAuthenticating() {
        return isAuthenticating;
    }

    /**
     * Reset authentication state
     */
    public static void resetAuthenticationState() {
        isAuthenticating = false;
    }

    /**
     * Screen for Device Authorization Flow authentication
     */
    public static class Player2OAuthScreen extends Screen {
        private final Screen parent;
        private final String deviceCode;
        private final String userCode;
        private final String verificationUri;
        private final int interval;
        private ButtonWidget openBrowserButton;
        private ButtonWidget cancelButton;
        private ButtonWidget helpButton;

        public Player2OAuthScreen(Screen parent, String deviceCode, String userCode, String verificationUri, int interval) {
            super(Text.literal("Player2 Authentication"));
            this.parent = parent;
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.interval = interval;
        }

        @Override
        protected void init() {
            super.init();

            int centerX = width / 2;
            int centerY = height / 2;

            // Title
            this.addDrawableChild(new net.minecraft.client.gui.widget.TextWidget(
                    centerX - 100, centerY - 80, 200, 20,
                    Text.literal("Player2 Authentication Required"),
                    this.textRenderer
            ));

            // Open browser button
            openBrowserButton = ButtonWidget.builder(
                    Text.literal("Open Website"),
                    button -> openBrowser(verificationUri + "?user_code=" + userCode)
            ).dimensions(centerX - 100, centerY + 50, 200, 20).build();

            // Cancel button
            cancelButton = ButtonWidget.builder(
                    Text.literal("Cancel"),
                    button -> close()
            ).dimensions(centerX - 100, centerY + 80, 200, 20).build();

            // Help button
            helpButton = ButtonWidget.builder(
                    Text.literal("Help"),
                    button -> showHelp()
            ).dimensions(centerX - 100, centerY + 110, 200, 20).build();

            addDrawableChild(openBrowserButton);
            addDrawableChild(cancelButton);
            addDrawableChild(helpButton);
        }

        private void showHelp() {
            client.setScreen(new ConfirmScreen(
                    button -> client.setScreen(this),
                    Text.literal("Authentication Help"),
                    Text.literal("1. Click 'Open Player2 Website' to open the verification page\n2. Enter the verification code shown above: " + userCode + "\n3. Log in to your Player2 account if needed\n4. Authorize the 'creature-pals-minecraft-mod' application\n5. The mod will automatically detect when authorization is complete\n\nIf the browser didn't open, manually visit:\n" + verificationUri),
                    Text.literal("OK"),
                    ScreenTexts.CANCEL
            ));
        }

        @Override
        public void close() {
            Player2OAuthHandler.resetAuthenticationState();
            if (parent != null) {
                client.setScreen(parent);
            } else {
                client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
            }
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
    
    /**
     * Show a success notification when authentication is complete
     */
    private static void showSuccessNotification() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            // Always show success message, regardless of current screen
            client.setScreen(new ConfirmScreen(
                button -> {
                    // Go back to title screen
                    client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
                },
                Text.literal("Authentication Successful!"),
                Text.literal("Your Player2 API key has been automatically retrieved and saved.\n\nYou can now use all the mod features!"),
                Text.literal("Continue"),
                ScreenTexts.CANCEL
            ));
        }
    }
}
