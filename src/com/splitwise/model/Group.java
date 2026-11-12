package com.splitwise.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Group {

    private final String id;
    private final String name;
    private final List<User> members;
    private final List<Expense> expenses;

    public Group(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.members = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.members = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addMember(User user) {
        if (user != null && !members.contains(user)) {
            members.add(user);
        }
    }

    public void removeMember(User user) {
        members.remove(user);
    }

    public void addExpense(Expense expense) {
        if (expense != null) {
            expenses.add(expense);
        }
    }

    public List<User> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    @Override
    public String toString() {
        return "Group{id='" + id + "', name='" + name + "', members=" + members.size()
                + ", expenses=" + expenses.size() + "}";
    }
}
