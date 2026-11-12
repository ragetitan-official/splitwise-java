package com.splitwise.service;

import com.splitwise.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits an expense amount equally among all members.
 * The params argument is ignored for equal splits.
 */
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public Map<User, Double> split(double amount, List<User> members, Map<User, Double> params) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("Members list cannot be null or empty");
        }

        Map<User, Double> shares = new HashMap<>();
        double share = Math.round(amount / members.size() * 100.0) / 100.0;

        // Distribute the rounded share to each member
        double distributed = 0.0;
        for (int i = 0; i < members.size(); i++) {
            if (i == members.size() - 1) {
                // Last member gets the remainder to avoid rounding drift
                double remainder = Math.round((amount - distributed) * 100.0) / 100.0;
                shares.put(members.get(i), remainder);
            } else {
                shares.put(members.get(i), share);
                distributed += share;
            }
        }

        return shares;
    }
}
