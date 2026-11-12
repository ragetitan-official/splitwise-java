package com.splitwise.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EqualExpense extends Expense {

    public EqualExpense(String description, double amount, User paidBy) {
        super(description, amount, paidBy, SplitType.EQUAL);
    }

    @Override
    public Map<User, Double> calculateSplit(List<User> members) {
        Map<User, Double> splits = new HashMap<>();
        if (members == null || members.isEmpty()) {
            return splits;
        }
        double share = getAmount() / members.size();
        for (User member : members) {
            splits.put(member, share);
        }
        return splits;
    }
}
