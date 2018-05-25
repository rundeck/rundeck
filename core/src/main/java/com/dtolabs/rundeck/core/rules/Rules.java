package com.dtolabs.rundeck.core.rules;



import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * Create rules and a rule engine
 */
public class Rules {
    /**
     * @param ruleset initial rules
     *
     * @return new rule engine with the rules
     */
    public static RuleEngine createEngine(Set<Rule> ruleset) {
        return new BaseRuleEngine(ruleset);
    }

    /**
     * @return new rule engine with no rules
     */
    public static RuleEngine createEngine() {
        return createEngine(new HashSet<Rule>());
    }

    /**
     * Create a single state key equals value condition
     *
     * @param key   key
     * @param value value
     *
     * @return new condition
     */
    public static Condition equalsCondition(String key, String value) {
        return new KeyValueEqualsCondition(key, value);
    }

    /**
     * Create a condition when the given state is set
     *
     * @param state state
     *
     * @return new condition
     */
    public static Condition equalsCondition(final StateObj state) {
        return new Condition() {
            @Override
            public boolean test(final StateObj input) {
                return input.hasState(state);
            }

            @Override
            public String toString() {
                return "(State equals: " + state + ")";
            }
        };
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Set<Condition> conditionSet(final Condition... condition) {
        HashSet<Condition> conditions = new HashSet<>();
        conditions.addAll(Arrays.asList(condition));
        return conditions;
    }


    /**
     * Create a single match condition
     *
     * @param key        key name or regular expression
     * @param keyRegex   true if the key is a regular expression key match, false for an equals match
     * @param value      value value string
     * @param valueRegex true if the value is a regular expression match, false for an equals match
     *
     * @return new condition
     */
    public static Condition matchesCondition(String key, boolean keyRegex, String value, boolean valueRegex) {
        return new MatchesCondition(key, keyRegex, value, valueRegex);
    }

    /**
     * Create a rule: predicate(conditions) => new state(results)
     *
     * @param condition single condition
     * @param key       key
     * @param value     value
     *
     * @return rule
     */
    public static Rule conditionsRule(final Condition condition, String key, String value) {
        return conditionsRule(condition, States.state(key, value));
    }

    /**
     * Create a rule: predicate(conditions) => new state(results)
     *
     * @param condition single condition
     * @param result    result
     *
     * @return rule
     */
    public static Rule conditionsRule(final Condition condition, StateObj result) {
        HashSet<Condition> conditions = new HashSet<>();
        conditions.add(condition);
        return conditionsRule(conditions, result);
    }


    /**
     * Create a rule: predicate(conditions) => new state(results)
     *
     * @param conditions conditions
     * @param key        key
     * @param value      value
     *
     * @return rule
     */
    public static Rule conditionsRule(final Set<Condition> conditions, String key, String value) {
        HashMap<String, String> results = new HashMap<>();
        results.put(key, value);
        return conditionsRule(conditions, results);
    }

    /**
     * Create a rule: predicate(conditions) => new state(results)
     *
     * @param conditions conditions
     * @param results    results
     *
     * @return rule
     */
    public static Rule conditionsRule(final Set<Condition> conditions, final Map<String, String> results) {
        return conditionsRule(conditions, States.state(results));
    }

    /**
     * Create a rule: predicate(conditions) => new state(results)
     *
     * @param conditions conditions
     * @param results    results
     *
     * @return rule
     */
    public static Rule conditionsRule(final Set<Condition> conditions, StateObj results) {
        if (null == conditions) {
            throw new NullPointerException("conditions must not be null");
        }
        final StateObj newstate = States.state(results);
        return new Rule() {
            @Override
            public boolean test(final StateObj input) {
                return applyConditions(input, conditions, true);
            }

            @Override
            public StateObj evaluate(final StateObj stateObj) {
                if (test(stateObj)) {
                    return newstate;
                }
                return null;
            }

            @Override
            public String toString() {
                return "Rule: Conditions(" + conditions + ") => " + newstate;
            }
        };
    }

    public static boolean applyConditions(
            final StateObj state,
            final Set<Condition> runConditions,
            final boolean operationAnd
    )
    {
        boolean allCondition = operationAnd;
        for (Condition condition : runConditions) {
            boolean apply = condition.test(state);
            allCondition = operationAnd ? allCondition && apply : allCondition || apply;
        }
        return allCondition;
    }

    /**
     * Update the state by evaluating the rules, and applying state changes
     *
     * @param ruleEngine rule engine
     * @param state      state
     *
     * @return true if state was modified, false if no state change occured
     */
    public static boolean update(RuleEngine ruleEngine, MutableStateObj state) {
        StateObj newState = ruleEngine.evaluateRules(state);
        state.updateState(newState);
        return newState.getState().size() > 0;

    }

    public static java.util.function.Predicate<? super Rule> ruleApplies(final StateObj state) {
        return input -> input.test(state);
    }

    public static Function<? super Rule, Optional<StateObj>> applyRule(final StateObj state) {
        return input -> Optional.ofNullable(input.evaluate(state));
    }
}
