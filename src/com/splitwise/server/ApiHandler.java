package com.splitwise.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.splitwise.model.*;
import com.splitwise.service.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HttpHandler for '/api/*' endpoints.
 * Handles all REST API routes for user, group, expense, and settlement management.
 * Parses JSON request bodies manually without any external library.
 */
public class ApiHandler implements HttpHandler {

    private final ExpenseManager manager;

    public ApiHandler() {
        this.manager = ExpenseManager.getInstance();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Add CORS headers
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            // Route the request
            if (path.equals("/api/users") || path.equals("/api/users/")) {
                handleUsers(exchange, method);
            } else if (path.matches("/api/groups/[^/]+/members/?")) {
                String groupId = extractGroupId(path);
                handleGroupMembers(exchange, method, groupId);
            } else if (path.matches("/api/groups/[^/]+/expenses/?")) {
                String groupId = extractGroupId(path);
                handleGroupExpenses(exchange, method, groupId);
            } else if (path.matches("/api/groups/[^/]+/settlements/?")) {
                String groupId = extractGroupId(path);
                handleGroupSettlements(exchange, method, groupId);
            } else if (path.matches("/api/groups/[^/]+/report/json/?")) {
                String groupId = extractGroupId(path);
                handleGroupReportJson(exchange, method, groupId);
            } else if (path.matches("/api/groups/[^/]+/report/?")) {
                String groupId = extractGroupId(path);
                handleGroupReport(exchange, method, groupId);
            } else if (path.matches("/api/groups/[^/]+/?")) {
                String groupId = extractGroupId(path);
                handleGroupById(exchange, method, groupId);
            } else if (path.equals("/api/groups") || path.equals("/api/groups/")) {
                handleGroups(exchange, method);
            } else {
                sendJsonError(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            sendJsonError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    //  USER ENDPOINTS
    // ──────────────────────────────────────────────

    private void handleUsers(HttpExchange exchange, String method) throws IOException {
        if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            Map<String, String> json = parseJsonObject(body);
            String name = json.get("name");
            String email = json.get("email");

            if (name == null || name.isEmpty()) {
                sendJsonError(exchange, 400, "Name is required");
                return;
            }
            if (email == null || email.isEmpty()) {
                sendJsonError(exchange, 400, "Email is required");
                return;
            }

            User user = manager.createUser(name, email);
            sendJsonResponse(exchange, 201, userToJson(user));
        } else if ("GET".equals(method)) {
            List<User> users = manager.getAllUsers();
            sendJsonResponse(exchange, 200, usersToJsonArray(users));
        } else {
            sendJsonError(exchange, 405, "Method not allowed");
        }
    }

    // ──────────────────────────────────────────────
    //  GROUP ENDPOINTS
    // ──────────────────────────────────────────────

    private void handleGroups(HttpExchange exchange, String method) throws IOException {
        if ("POST".equals(method)) {
            String body = readRequestBody(exchange);
            Map<String, String> json = parseJsonObject(body);
            String name = json.get("name");

            if (name == null || name.isEmpty()) {
                sendJsonError(exchange, 400, "Group name is required");
                return;
            }

            Group group = manager.createGroup(name);

            // Parse memberIds array if present
            List<String> memberIds = parseJsonArrayField(body, "memberIds");
            for (String memberId : memberIds) {
                try {
                    manager.addUserToGroup(group.getId(), memberId);
                } catch (IllegalArgumentException e) {
                    // Skip invalid member IDs silently
                }
            }

            // Re-fetch the group to get updated members
            group = manager.getGroup(group.getId());
            sendJsonResponse(exchange, 201, groupToJson(group));
        } else if ("GET".equals(method)) {
            List<Group> groups = manager.getAllGroups();
            sendJsonResponse(exchange, 200, groupsToJsonArray(groups));
        } else {
            sendJsonError(exchange, 405, "Method not allowed");
        }
    }

    private void handleGroupById(HttpExchange exchange, String method, String groupId) throws IOException {
        if ("GET".equals(method)) {
            Group group = manager.getGroup(groupId);
            if (group == null) {
                sendJsonError(exchange, 404, "Group not found");
                return;
            }
            sendJsonResponse(exchange, 200, groupDetailToJson(group));
        } else if ("DELETE".equals(method)) {
            Group group = manager.getGroup(groupId);
            if (group == null) {
                sendJsonError(exchange, 404, "Group not found");
                return;
            }
            manager.deleteGroup(groupId);
            sendJsonResponse(exchange, 200, "{\"message\":\"Group deleted successfully\"}");
        } else {
            sendJsonError(exchange, 405, "Method not allowed");
        }
    }

    // ──────────────────────────────────────────────
    //  GROUP MEMBERS
    // ──────────────────────────────────────────────

    private void handleGroupMembers(HttpExchange exchange, String method, String groupId) throws IOException {
        if (!"POST".equals(method)) {
            sendJsonError(exchange, 405, "Method not allowed");
            return;
        }

        Group group = manager.getGroup(groupId);
        if (group == null) {
            sendJsonError(exchange, 404, "Group not found");
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> json = parseJsonObject(body);
        String userId = json.get("userId");

        if (userId == null || userId.isEmpty()) {
            sendJsonError(exchange, 400, "userId is required");
            return;
        }

        try {
            manager.addUserToGroup(groupId, userId);
            group = manager.getGroup(groupId);
            sendJsonResponse(exchange, 200, groupDetailToJson(group));
        } catch (IllegalArgumentException e) {
            sendJsonError(exchange, 400, e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    //  GROUP EXPENSES
    // ──────────────────────────────────────────────

    private void handleGroupExpenses(HttpExchange exchange, String method, String groupId) throws IOException {
        if (!"POST".equals(method)) {
            sendJsonError(exchange, 405, "Method not allowed");
            return;
        }

        Group group = manager.getGroup(groupId);
        if (group == null) {
            sendJsonError(exchange, 404, "Group not found");
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> json = parseJsonObject(body);

        String description = json.get("description");
        String amountStr = json.get("amount");
        String paidById = json.get("paidById");
        String splitTypeStr = json.get("splitType");

        if (description == null || description.isEmpty()) {
            sendJsonError(exchange, 400, "Description is required");
            return;
        }
        if (amountStr == null || amountStr.isEmpty()) {
            sendJsonError(exchange, 400, "Amount is required");
            return;
        }
        if (paidById == null || paidById.isEmpty()) {
            sendJsonError(exchange, 400, "paidById is required");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            sendJsonError(exchange, 400, "Invalid amount format");
            return;
        }

        SplitType splitType = SplitType.EQUAL;
        if (splitTypeStr != null && !splitTypeStr.isEmpty()) {
            try {
                splitType = SplitType.valueOf(splitTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendJsonError(exchange, 400, "Invalid split type. Use EQUAL or PERCENTAGE");
                return;
            }
        }

        // Parse percentages if split type is PERCENTAGE
        Map<User, Double> percentages = new HashMap<>();
        if (splitType == SplitType.PERCENTAGE) {
            Map<String, String> percentageMap = parseJsonObjectField(body, "percentages");
            for (Map.Entry<String, String> entry : percentageMap.entrySet()) {
                User user = manager.getUser(entry.getKey());
                if (user != null) {
                    try {
                        percentages.put(user, Double.parseDouble(entry.getValue()));
                    } catch (NumberFormatException e) {
                        sendJsonError(exchange, 400, "Invalid percentage value for user: " + entry.getKey());
                        return;
                    }
                }
            }
        }

        try {
            Expense expense = manager.addExpense(groupId, description, amount, paidById, splitType, percentages);
            sendJsonResponse(exchange, 201, expenseToJson(expense));
        } catch (IllegalArgumentException e) {
            sendJsonError(exchange, 400, e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    //  GROUP SETTLEMENTS
    // ──────────────────────────────────────────────

    private void handleGroupSettlements(HttpExchange exchange, String method, String groupId) throws IOException {
        if (!"GET".equals(method)) {
            sendJsonError(exchange, 405, "Method not allowed");
            return;
        }

        Group group = manager.getGroup(groupId);
        if (group == null) {
            sendJsonError(exchange, 404, "Group not found");
            return;
        }

        List<Settlement> settlements = manager.getSettlements(groupId);
        sendJsonResponse(exchange, 200, settlementsToJsonArray(settlements));
    }

    // ──────────────────────────────────────────────
    //  GROUP REPORT
    // ──────────────────────────────────────────────

    private void handleGroupReport(HttpExchange exchange, String method, String groupId) throws IOException {
        if (!"GET".equals(method)) {
            sendJsonError(exchange, 405, "Method not allowed");
            return;
        }

        Group group = manager.getGroup(groupId);
        if (group == null) {
            sendJsonError(exchange, 404, "Group not found");
            return;
        }

        List<Settlement> settlements = manager.getSettlements(groupId);
        SettlementReport report = new SettlementReport(group, settlements);
        String textReport = report.generateTextReport();

        // Send as JSON with the text report embedded
        String jsonResponse = "{\"report\":" + escapeJsonString(textReport) + "}";
        sendJsonResponse(exchange, 200, jsonResponse);
    }

    private void handleGroupReportJson(HttpExchange exchange, String method, String groupId) throws IOException {
        if (!"GET".equals(method)) {
            sendJsonError(exchange, 405, "Method not allowed");
            return;
        }

        Group group = manager.getGroup(groupId);
        if (group == null) {
            sendJsonError(exchange, 404, "Group not found");
            return;
        }

        List<Settlement> settlements = manager.getSettlements(groupId);
        SettlementReport report = new SettlementReport(group, settlements);
        String jsonReport = report.generateJsonReport();

        sendJsonResponse(exchange, 200, jsonReport);
    }

    // ══════════════════════════════════════════════
    //  JSON PARSING HELPERS
    // ══════════════════════════════════════════════

    /**
     * Parses a flat JSON object string into a Map of key-value pairs.
     * Handles simple flat JSON like {"name":"John","email":"john@example.com"}.
     * Skips nested objects and arrays in values.
     */
    private Map<String, String> parseJsonObject(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        json = json.trim();
        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        // Split by commas, but skip commas inside nested braces/brackets/quotes
        List<String> pairs = splitTopLevel(json, ',');

        for (String pair : pairs) {
            int colonIndex = pair.indexOf(':');
            if (colonIndex < 0) continue;

            String key = pair.substring(0, colonIndex).trim();
            String value = pair.substring(colonIndex + 1).trim();

            // Strip quotes from key
            key = stripQuotes(key);

            // Skip nested objects and arrays for flat parsing
            if (value.startsWith("{") || value.startsWith("[")) {
                continue;
            }

            // Strip quotes from value
            value = stripQuotes(value);

            result.put(key, value);
        }

        return result;
    }

    /**
     * Parses a JSON array of strings.
     * Handles simple arrays like ["item1","item2"].
     */
    private List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return result;
        }

        json = json.trim();
        if (json.startsWith("[")) {
            json = json.substring(1);
        }
        if (json.endsWith("]")) {
            json = json.substring(0, json.length() - 1);
        }

        json = json.trim();
        if (json.isEmpty()) {
            return result;
        }

        String[] items = json.split(",");
        for (String item : items) {
            String stripped = stripQuotes(item.trim());
            if (!stripped.isEmpty()) {
                result.add(stripped);
            }
        }

        return result;
    }

    /**
     * Extracts and parses a named array field from a JSON body.
     * For example, extracts memberIds from {"name":"Group","memberIds":["id1","id2"]}.
     */
    private List<String> parseJsonArrayField(String json, String fieldName) {
        String searchKey = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return new ArrayList<>();
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) {
            return new ArrayList<>();
        }

        int bracketStart = json.indexOf('[', colonIndex);
        if (bracketStart < 0) {
            return new ArrayList<>();
        }

        // Find matching closing bracket
        int depth = 0;
        int bracketEnd = -1;
        for (int i = bracketStart; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') {
                depth--;
                if (depth == 0) {
                    bracketEnd = i;
                    break;
                }
            }
        }

        if (bracketEnd < 0) {
            return new ArrayList<>();
        }

        String arrayStr = json.substring(bracketStart, bracketEnd + 1);
        return parseJsonArray(arrayStr);
    }

    /**
     * Extracts and parses a named object field from a JSON body.
     * For example, extracts percentages from {"percentages":{"userId1":"50","userId2":"50"}}.
     */
    private Map<String, String> parseJsonObjectField(String json, String fieldName) {
        String searchKey = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex < 0) {
            return new HashMap<>();
        }

        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) {
            return new HashMap<>();
        }

        int braceStart = json.indexOf('{', colonIndex);
        if (braceStart < 0) {
            return new HashMap<>();
        }

        // Find matching closing brace
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0) {
                    braceEnd = i;
                    break;
                }
            }
        }

        if (braceEnd < 0) {
            return new HashMap<>();
        }

        String objStr = json.substring(braceStart, braceEnd + 1);
        return parseJsonObject(objStr);
    }

    /**
     * Splits a string by a delimiter, but only at the top level
     * (skipping delimiters inside nested braces, brackets, and quotes).
     */
    private List<String> splitTopLevel(String str, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        boolean escaped = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }

            if (!inQuotes) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                }

                if (c == delimiter && depth == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts;
    }

    /**
     * Strips surrounding double quotes from a string.
     */
    private String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    // ══════════════════════════════════════════════
    //  JSON SERIALIZATION HELPERS
    // ══════════════════════════════════════════════

    private String userToJson(User user) {
        return "{" +
                "\"id\":" + escapeJsonString(user.getId()) + "," +
                "\"name\":" + escapeJsonString(user.getName()) + "," +
                "\"email\":" + escapeJsonString(user.getEmail()) +
                "}";
    }

    private String usersToJsonArray(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(userToJson(users.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String groupToJson(Group group) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(escapeJsonString(group.getId())).append(",");
        sb.append("\"name\":").append(escapeJsonString(group.getName())).append(",");
        sb.append("\"memberCount\":").append(group.getMembers().size()).append(",");
        sb.append("\"expenseCount\":").append(group.getExpenses().size());
        sb.append("}");
        return sb.toString();
    }

    private String groupsToJsonArray(List<Group> groups) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(groupToJson(groups.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String groupDetailToJson(Group group) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(escapeJsonString(group.getId())).append(",");
        sb.append("\"name\":").append(escapeJsonString(group.getName())).append(",");

        // Members array
        sb.append("\"members\":[");
        List<User> members = group.getMembers();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(userToJson(members.get(i)));
        }
        sb.append("],");

        // Expenses array
        sb.append("\"expenses\":[");
        List<Expense> expenses = group.getExpenses();
        for (int i = 0; i < expenses.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(expenseToJson(expenses.get(i)));
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private String expenseToJson(Expense expense) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":").append(escapeJsonString(expense.getId())).append(",");
        sb.append("\"description\":").append(escapeJsonString(expense.getDescription())).append(",");
        sb.append("\"amount\":").append(expense.getAmount()).append(",");
        sb.append("\"paidBy\":").append(userToJson(expense.getPaidBy())).append(",");
        sb.append("\"splitType\":").append(escapeJsonString(expense.getSplitType().name())).append(",");
        sb.append("\"createdAt\":").append(escapeJsonString(expense.getCreatedAt().toString()));
        sb.append("}");
        return sb.toString();
    }

    private String settlementToJson(Settlement settlement) {
        return "{" +
                "\"from\":" + userToJson(settlement.getFromUser()) + "," +
                "\"to\":" + userToJson(settlement.getToUser()) + "," +
                "\"amount\":" + String.format("%.2f", settlement.getAmount()) +
                "}";
    }

    private String settlementsToJsonArray(List<Settlement> settlements) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < settlements.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(settlementToJson(settlements.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes a string for safe inclusion in a JSON value.
     */
    private String escapeJsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    // ══════════════════════════════════════════════
    //  HTTP HELPERS
    // ══════════════════════════════════════════════

    /**
     * Reads the full request body as a string.
     */
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Sends a JSON response with the given status code and body.
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonBody) throws IOException {
        byte[] response = jsonBody.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    /**
     * Sends a JSON error response.
     */
    private void sendJsonError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = "{\"error\":" + escapeJsonString(message) + "}";
        sendJsonResponse(exchange, statusCode, json);
    }

    /**
     * Extracts the group ID from a path like /api/groups/{id}/...
     */
    private String extractGroupId(String path) {
        String[] parts = path.split("/");
        // /api/groups/{id} -> parts = ["", "api", "groups", "{id}", ...]
        if (parts.length >= 4) {
            return parts[3];
        }
        return "";
    }
}
