package com.splitwise.service;

import com.splitwise.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an expense amount according to the percentage specified for each member
 * in the params map. Percentages must sum to 100.
 */
public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public Map<User, Double> split(double amount, List<User> members, Map<User, Double> params) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Members list cannot be null or empty");
        }
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Percentage params must be provided for percentage split");
        }

        // Validate that every member has a percentage entry
        for (User member : members) {
            if (!params.containsKey(member)) {
                throw new IllegalArgumentException("Missing percentage for user: " + member.getName());
            }
        }

        // Validate that percentages sum to 100
        double totalPercentage = 0.0;
        for (User member : members) {
            totalPercentage += params.get(member);
        }
        if (Math.abs(totalPercentage - 100.0) > 0.01) {
            throw new IllegalArgumentException(
                    "Percentages must sum to 100, but got: " + totalPercentage);
        }

        Map<User, Double> shares = new HashMap<>();
        for (User member : members) {
            double percentage = params.get(member);
            double share = Math.round(amount * percentage / 100.0 * 100.0) / 100.0;
            shares.put(member, share);
        }

        return shares;
    }
}
