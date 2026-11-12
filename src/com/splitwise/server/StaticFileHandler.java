package com.splitwise.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpHandler that serves static files from the 'web/' directory.
 * Maps file extensions to appropriate MIME types and serves index.html for the root path.
 */
public class StaticFileHandler implements HttpHandler {

    private final Path webRoot;
    private final Map<String, String> mimeTypes;

    public StaticFileHandler() {
        this.webRoot = Paths.get("web").toAbsolutePath();
        this.mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html; charset=UTF-8");
        mimeTypes.put("css", "text/css; charset=UTF-8");
        mimeTypes.put("js", "application/javascript; charset=UTF-8");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("ico", "image/x-icon");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("svg", "image/svg+xml");
        mimeTypes.put("json", "application/json; charset=UTF-8");
        mimeTypes.put("txt", "text/plain; charset=UTF-8");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // Serve index.html for root path
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Resolve the file path relative to web root
        Path filePath = webRoot.resolve(path.substring(1)).normalize();

        // Security check: ensure the resolved path is still within the web root
        if (!filePath.startsWith(webRoot)) {
            sendError(exchange, 403, "Forbidden");
            return;
        }

        // Check if file exists
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            sendError(exchange, 404, "Not Found");
            return;
        }

        // Determine MIME type from file extension
        String contentType = getMimeType(filePath.toString());

        // Read and send the file
        byte[] fileBytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, fileBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(fileBytes);
        }
    }

    /**
     * Determines the MIME type based on the file extension.
     */
    private String getMimeType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            String ext = fileName.substring(dotIndex + 1).toLowerCase();
            String mime = mimeTypes.get(ext);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    /**
     * Sends an error response with the given status code and message.
     */
    private void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] response = message.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
