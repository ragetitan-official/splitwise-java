package com.splitwise.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * HTTP server for the SplitWise application.
 * Creates an HttpServer on port 8080, registers static file and API handlers.
 */
public class SplitWiseServer {

    private static final int PORT = 8080;
    private final HttpServer server;

    public SplitWiseServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", new ApiHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null); // default executor
    }

    /**
     * Starts the server and prints a banner.
     */
    public void start() {
        server.start();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║        SplitWise Expense Splitter        ║");
        System.out.println("║──────────────────────────────────────────║");
        System.out.println("║  Server running on http://localhost:" + PORT + "  ║");
        System.out.println("║  Press Ctrl+C to stop                    ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    /**
     * Stops the server.
     */
    public void stop() {
        server.stop(0);
        System.out.println("Server stopped.");
    }

    /**
     * Returns the port the server is running on.
     */
    public int getPort() {
        return PORT;
    }
}
