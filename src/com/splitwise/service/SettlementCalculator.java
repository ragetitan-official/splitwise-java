package com.splitwise.service;

import com.splitwise.model.Expense;
import com.splitwise.model.Group;
import com.splitwise.model.Settlement;
import com.splitwise.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates the minimum set of settlements needed to balance all debts
 * within a group. Uses a greedy algorithm that pairs the largest creditor
 * with the largest debtor in each iteration.
 */
public class SettlementCalculator {

    /**
     * Calculates settlements for all expenses in the given group.
     *
     * @param group the group whose expenses should be settled
     * @return a list of Settlement objects representing the minimum transactions
     */
    public List<Settlement> calculateSettlements(Group group) {
        // Step 1: Compute net balance for each user.
        //   Positive balance = net creditor (others owe them)
        //   Negative balance = net debtor (they owe others)
        Map<User, Double> balances = computeNetBalances(group);

        // Step 2: Greedy algorithm to minimise transactions
        return minimiseTransactions(balances);
    }

    /**
     * Computes each user's net balance across all expenses in the group.
     * For each expense, the payer is credited the full amount and each member
     * (including the payer) is debited their calculated split share.
     */
    private Map<User, Double> computeNetBalances(Group group) {
        Map<User, Double> balances = new HashMap<>();

        // Initialise all members to zero
        for (User member : group.getMembers()) {
            balances.put(member, 0.0);
        }

        for (Expense expense : group.getExpenses()) {
            User payer = expense.getPaidBy();

            // Credit the payer for the full amount
            balances.merge(payer, expense.getAmount(), Double::sum);

            // Debit each member their split share
            Map<User, Double> splits = expense.calculateSplit(group.getMembers());
            for (Map.Entry<User, Double> entry : splits.entrySet()) {
                balances.merge(entry.getKey(), -entry.getValue(), Double::sum);
            }
        }

        return balances;
    }

    /**
     * Greedy algorithm: repeatedly settle the largest debtor against the
     * largest creditor until all balances are zero (within rounding tolerance).
     */
    private List<Settlement> minimiseTransactions(Map<User, Double> balances) {
        List<Settlement> settlements = new ArrayList<>();

        // Build separate lists of debtors and creditors
        List<Map.Entry<User, Double>> debtors = new ArrayList<>();
        List<Map.Entry<User, Double>> creditors = new ArrayList<>();

        for (Map.Entry<User, Double> entry : balances.entrySet()) {
            double balance = entry.getValue();
            if (balance < -0.01) {
                debtors.add(entry);
            } else if (balance > 0.01) {
                creditors.add(entry);
            }
        }

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            // Find the largest debtor (most negative balance)
            Map.Entry<User, Double> maxDebtor = debtors.get(0);
            for (Map.Entry<User, Double> d : debtors) {
                if (d.getValue() < maxDebtor.getValue()) {
                    maxDebtor = d;
                }
            }

            // Find the largest creditor (most positive balance)
            Map.Entry<User, Double> maxCreditor = creditors.get(0);
            for (Map.Entry<User, Double> c : creditors) {
                if (c.getValue() > maxCreditor.getValue()) {
                    maxCreditor = c;
                }
            }

            // Settle the minimum of the two absolute values
            double debtAmount = -maxDebtor.getValue();
            double creditAmount = maxCreditor.getValue();
            double settleAmount = Math.min(debtAmount, creditAmount);
            settleAmount = Math.round(settleAmount * 100.0) / 100.0;

            settlements.add(new Settlement(maxDebtor.getKey(), maxCreditor.getKey(), settleAmount));

            // Update balances
            maxDebtor.setValue(maxDebtor.getValue() + settleAmount);
            maxCreditor.setValue(maxCreditor.getValue() - settleAmount);

            // Remove entries that are now settled (within tolerance)
            if (Math.abs(maxDebtor.getValue()) < 0.01) {
                debtors.remove(maxDebtor);
            }
            if (Math.abs(maxCreditor.getValue()) < 0.01) {
                creditors.remove(maxCreditor);
            }
        }

        return settlements;
    }
}
