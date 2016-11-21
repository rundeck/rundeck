package com.dtolabs.rundeck.core.rules;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;

import java.util.Set;

/**
 * Basic rules engine
 */
public class BaseRuleEngine implements RuleEngine {
    private Set<Rule> ruleSet;

    public BaseRuleEngine(final Set<Rule> ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public Set<Rule> getRuleSet() {
        return ruleSet;
    }

    @Override
    public void addRule(final Rule rule) {
        ruleSet.add(rule);
    }

    /**
     * Evaluate each rule, if it applies, accrue the new state changes
     *
     * @param state input state
     *
     * @return accrued state changes from matching rules
     */
    @Override
    public StateObj evaluateRules(final StateObj state) {
        MutableStateObj dataState = States.mutable();
        ImmutableList<Optional<StateObj>> newStates =
                FluentIterable
                        .from(getRuleSet())
                        .filter(Rules.ruleApplies(state))
                        .transform(Rules.applyRule(state))
                        .toList();
        for (StateObj evaluate : Optional.presentInstances(newStates)) {
            dataState.updateState(evaluate);
        }
        return dataState;
    }

    @Override
    public String toString() {
        return "RuleEngine{" +
               "ruleSet=" + ruleSet +
               '}';
    }
}
