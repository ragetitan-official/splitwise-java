package com.splitwise.model;

public class Settlement {

    private final User fromUser;
    private final User toUser;
    private final double amount;

    public Settlement(User fromUser, User toUser, double amount) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
    }

    public User getFromUser() {
        return fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Settlement{" + fromUser.getName() + " owes " + toUser.getName()
                + " " + amount + "}";
    }
}
