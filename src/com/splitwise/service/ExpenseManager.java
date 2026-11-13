package com.splitwise.service;

import com.splitwise.model.*;

import java.util.*;

/**
 * Singleton service that manages users, groups, expenses, and settlements.
 * This is the central façade that API handlers call.
 */
public class ExpenseManager {

    // Singleton instance
    private static ExpenseManager instance;

    private final Map<String, User> users;
    private final Map<String, Group> groups;
    private final SettlementCalculator settlementCalculator;

    // Private constructor enforces the singleton pattern
    private ExpenseManager() {
        this.users = new HashMap<>();
        this.groups = new HashMap<>();
        this.settlementCalculator = new SettlementCalculator();
    }

    /**
     * Returns the single shared instance of ExpenseManager.
     *
     * @return the ExpenseManager singleton
     */
    public static synchronized ExpenseManager getInstance() {
        if (instance == null) {
            instance = new ExpenseManager();
        }
        return instance;
    }

    // ── User management ─────────────────────────────────────────────

    /**
     * Creates and registers a new user with auto-generated UUID.
     */
    public User createUser(String name) {
        String id = UUID.randomUUID().toString();
        User user = new User(id, name);
        users.put(id, user);
        return user;
    }

    /**
     * Retrieves a user by id.
     */
    public User getUser(String id) {
        return users.get(id);
    }

    /**
     * Returns all registered users.
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Deletes a user by id.
     */
    public User deleteUser(String id) {
        return users.remove(id);
    }

    // ── Group management ────────────────────────────────────────────

    /**
     * Creates and registers a new group with auto-generated UUID.
     */
    public Group createGroup(String name) {
        String id = UUID.randomUUID().toString();
        Group group = new Group(id, name);
        groups.put(id, group);
        return group;
    }

    /**
     * Creates and registers a new group with specific ID.
     */
    public Group createGroup(String id, String name) {
        if (groups.containsKey(id)) {
            throw new IllegalArgumentException("Group with id '" + id + "' already exists");
        }
        Group group = new Group(id, name);
        groups.put(id, group);
        return group;
    }

    /**
     * Retrieves a group by id.
     */
    public Group getGroup(String id) {
        return groups.get(id);
    }

    /**
     * Returns all registered groups.
     */
    public List<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    /**
     * Deletes a group by id.
     */
    public Group deleteGroup(String id) {
        return groups.remove(id);
    }

    // ── Group member & expense operations ───────────────────────────

    /**
     * Adds an existing user to an existing group.
     */
    public void addUserToGroup(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        group.addMember(user);
    }

    /**
     * Adds an expense to a group directly.
     */
    public void addExpense(String groupId, Expense expense) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        group.addExpense(expense);
    }

    /**
     * Overload to create and add an expense from parameters.
     */
    public Expense addExpense(String groupId, String description, double amount, String paidById,
                              SplitType splitType, Map<User, Double> percentages) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        User paidBy = users.get(paidById);
        if (paidBy == null) {
            throw new IllegalArgumentException("Payer user not found: " + paidById);
        }

        Expense expense;
        if (splitType == SplitType.PERCENTAGE) {
            expense = new PercentageExpense(description, amount, paidBy, percentages);
        } else {
            expense = new EqualExpense(description, amount, paidBy);
        }

        group.addExpense(expense);
        return expense;
    }

    // ── Settlements ─────────────────────────────────────────────────

    /**
     * Calculates the optimal settlements for a group.
     */
    public List<Settlement> getSettlements(String groupId) {
        Group group = groups.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        return settlementCalculator.calculateSettlements(group);
    }
}
