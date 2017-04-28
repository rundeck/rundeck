package com.dtolabs.rundeck.core.rules;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Basic rules engine
 */
public class BaseRuleEngine implements RuleEngine {
    private Set<Rule> ruleSet;

    public BaseRuleEngine(final Set<Rule> ruleSet) {
        this.ruleSet = new HashSet<>(ruleSet);
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
        getRuleSet().stream()
                    //filter rules that apply given the state
                    .filter(i -> i.test(state))
                    //evaluate the applicable rules
                    .map(input -> Optional.ofNullable(input.evaluate(state)))
                    //exclude empty results
                    .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    //update the state with each result
                    .forEach(dataState::updateState);
        return dataState;
    }

    @Override
    public String toString() {
        return "RuleEngine{" +
               "ruleSet=" + ruleSet +
               '}';
    }
}
