package com.splitwise.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class Expense {

    private final String id;
    private final String description;
    private final double amount;
    private final User paidBy;
    private final SplitType splitType;
    private final LocalDateTime createdAt;

    protected Expense(String description, double amount, User paidBy, SplitType splitType) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.createdAt = LocalDateTime.now();
    }

    protected Expense(String id, String description, double amount, User paidBy,
                      SplitType splitType, LocalDateTime createdAt) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.createdAt = createdAt;
    }

    public abstract Map<User, Double> calculateSplit(List<User> members);

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Expense{id='" + id + "', description='" + description + "', amount=" + amount
                + ", paidBy=" + paidBy.getName() + ", splitType=" + splitType
                + ", createdAt=" + createdAt + "}";
    }
}
