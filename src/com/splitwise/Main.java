package com.splitwise;

import com.splitwise.server.SplitWiseServer;

import java.io.IOException;

/**
 * Entry point for the SplitWise application.
 */
public class Main {

    public static void main(String[] args) {
        try {
            SplitWiseServer server = new SplitWiseServer();
            server.start();

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                server.stop();
            }));
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
