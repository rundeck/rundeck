package com.dtolabs.rundeck.core.rules;

import java.util.Set;

/**
 * Engine for evaluating rules and generating a new state, see {@link Rules}
 */
public interface RuleEngine {
    /**
     * @return current rules
     */
    Set<Rule> getRuleSet();

    /**
     * Add a rule
     *
     * @param rule rule
     */
    void addRule(Rule rule);

    /**
     * Evaluate all current rules
     *
     * @param state input state
     *
     * @return generated output states
     */
    StateObj evaluateRules(StateObj state);
}
