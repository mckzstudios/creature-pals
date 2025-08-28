package com.owlmaddie.player2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartbeatManager {
    public static final ExecutorService heartbeatManager = Executors.newSingleThreadExecutor();
    public static long lastHeartbeatTime = System.nanoTime();
    public static boolean isConnected = false;

    public static void sendHeartbeat() {
        heartbeatManager.submit(() -> {
            try {
                Player2APIService.sendHeartbeat();
                isConnected = true;
                System.out.println("Player2 API connection established");
            } catch (Exception e) {
                isConnected = false;
                System.err.println("Player2 API connection failed: " + e.getMessage());
            }
        });
    }

    // inject into ontick
    public static void injectIntoOnTick() {
        long now = System.nanoTime();
        // every 60 seconds send heartbeat
        if (now - lastHeartbeatTime > 60_000_000_000L) {
            sendHeartbeat();
            lastHeartbeatTime = now;
        }
    }

    /**
     * Check if the Player2 API is currently connected
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        return isConnected;
    }

    /**
     * Force a heartbeat check
     */
    public static void forceHeartbeat() {
        lastHeartbeatTime = 0; // Force immediate heartbeat
        injectIntoOnTick();
    }
}
