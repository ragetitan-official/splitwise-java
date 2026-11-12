package com.splitwise.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PercentageExpense extends Expense {

    private final Map<User, Double> percentages;

    public PercentageExpense(String description, double amount, User paidBy,
                             Map<User, Double> percentages) {
        super(description, amount, paidBy, SplitType.PERCENTAGE);
        validatePercentages(percentages);
        this.percentages = new HashMap<>(percentages);
    }

    private void validatePercentages(Map<User, Double> percentages) {
        if (percentages == null || percentages.isEmpty()) {
            throw new IllegalArgumentException("Percentages map cannot be null or empty.");
        }
        double total = percentages.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(total - 100.0) > 0.01) {
            throw new IllegalArgumentException(
                    "Percentages must sum to 100. Current sum: " + total);
        }
    }

    public Map<User, Double> getPercentages() {
        return new HashMap<>(percentages);
    }

    @Override
    public Map<User, Double> calculateSplit(List<User> members) {
        Map<User, Double> splits = new HashMap<>();
        for (Map.Entry<User, Double> entry : percentages.entrySet()) {
            splits.put(entry.getKey(), getAmount() * entry.getValue() / 100.0);
        }
        return splits;
    }
}
