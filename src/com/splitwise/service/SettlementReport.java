package com.splitwise.service;

import com.splitwise.model.Group;
import com.splitwise.model.Settlement;

import java.util.List;

/**
 * Generates human-readable text and JSON reports for group settlements.
 */
public class SettlementReport implements Reportable {

    private final Group group;
    private final List<Settlement> settlements;

    public SettlementReport(Group group, List<Settlement> settlements) {
        this.group = group;
        this.settlements = settlements;
    }

    @Override
    public String generateTextReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Settlement Report ===\n");
        sb.append("Group: ").append(group.getName()).append("\n");
        sb.append("Members: ").append(group.getMembers().size()).append("\n");
        sb.append("Expenses: ").append(group.getExpenses().size()).append("\n");
        sb.append("------------------------\n");

        if (settlements.isEmpty()) {
            sb.append("No settlements needed. All balances are even.\n");
        } else {
            sb.append("Settlements (").append(settlements.size()).append(" transaction(s)):\n");
            for (int i = 0; i < settlements.size(); i++) {
                Settlement s = settlements.get(i);
                sb.append(String.format("  %d. %s pays %s: ₹%.2f%n",
                        i + 1,
                        s.getFromUser().getName(),
                        s.getToUser().getName(),
                        s.getAmount()));
            }
        }

        sb.append("========================\n");
        return sb.toString();
    }

    @Override
    public String generateJsonReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"group\": ").append(escapeJsonString(group.getName())).append(",\n");
        sb.append("  \"groupId\": ").append(escapeJsonString(group.getId())).append(",\n");
        sb.append("  \"memberCount\": ").append(group.getMembers().size()).append(",\n");
        sb.append("  \"expenseCount\": ").append(group.getExpenses().size()).append(",\n");
        sb.append("  \"settlementCount\": ").append(settlements.size()).append(",\n");
        sb.append("  \"settlements\": [\n");

        for (int i = 0; i < settlements.size(); i++) {
            Settlement s = settlements.get(i);
            sb.append("    {\n");
            sb.append("      \"from\": ").append(escapeJsonString(s.getFromUser().getName())).append(",\n");
            sb.append("      \"fromId\": ").append(escapeJsonString(s.getFromUser().getId())).append(",\n");
            sb.append("      \"to\": ").append(escapeJsonString(s.getToUser().getName())).append(",\n");
            sb.append("      \"toId\": ").append(escapeJsonString(s.getToUser().getId())).append(",\n");
            sb.append("      \"amount\": ").append(String.format("%.2f", s.getAmount())).append("\n");
            sb.append("    }");
            if (i < settlements.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Escapes a string value for safe inclusion in JSON output.
     */
    private String escapeJsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:   sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
