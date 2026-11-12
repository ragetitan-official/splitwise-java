package com.splitwise.service;

import com.splitwise.model.User;

import java.util.List;
import java.util.Map;

/**
 * Strategy pattern interface for splitting an expense amount among members.
 * Different implementations provide different splitting algorithms.
 */
public interface SplitStrategy {

    /**
     * Splits the given amount among the provided members.
     *
     * @param amount  the total amount to split
     * @param members the list of users to split among
     * @param params  optional parameters for the split (e.g., percentage mappings);
     *                may be null or empty for strategies that don't require extra input
     * @return a map from each user to their share of the amount
     */
    Map<User, Double> split(double amount, List<User> members, Map<User, Double> params);
}
