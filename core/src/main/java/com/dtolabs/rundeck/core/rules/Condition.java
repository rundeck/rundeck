package com.dtolabs.rundeck.core.rules;


import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A predicate of a state
 */
public interface Condition extends Predicate<StateObj> {
    static Condition with(Predicate<StateObj> pred) {
        return new Condition() {
            @Override
            public boolean test(final StateObj input) {
                return pred.test(input);
            }

            @Override
            public String toString() {
                return pred.toString();
            }
        };
    }

    /**
     * Create a condition from a predicate with a human readable description, used by
     * {@link Object#toString()} (e.g. in "step did not run" log messages) instead of the
     * predicate's own, typically unreadable, lambda text.
     *
     * @param pred        predicate
     * @param description supplies the description, evaluated lazily
     * @return condition
     */
    static Condition with(final Predicate<StateObj> pred, final Supplier<String> description) {
        return new Condition() {
            @Override
            public boolean test(final StateObj input) {
                return pred.test(input);
            }

            @Override
            public String toString() {
                return description.get();
            }
        };
    }

    /**
     * Describe a group of conditions joined by an operator, e.g. "(a AND b)".
     *
     * @param conditions conditions
     * @param operator   joining operator
     * @return description
     */
    static String describe(final Iterable<Condition> conditions, final String operator) {
        return StreamSupport.stream(conditions.spliterator(), false)
                            .map(String::valueOf)
                            .collect(Collectors.joining(" " + operator + " ", "(", ")"));
    }

    @Override
    default Condition negate() {
        return Condition.with(t -> !test(t), () -> "NOT " + this);
    }

    public static Condition not(final Condition condition) {
        return Condition.with(condition.negate(), () -> "NOT " + condition);
    }

    public static Condition and(final Condition condition1, final Condition condition2) {
        return Condition.with(condition1.and(condition2), () -> describe(Arrays.asList(condition1, condition2), "AND"));
    }

    public static Condition and(final Condition... conditions) {
        return Condition.with(andAll(conditions), () -> describe(Arrays.asList(conditions), "AND"));
    }

    static Predicate<StateObj> andAll(final Condition[] conditions) {
        return andAll(Arrays.asList(conditions));
    }

    public static Condition and(final Iterable<Condition> conditions) {
        return Condition.with(andAll(conditions), () -> describe(conditions, "AND"));
    }

    static Predicate<StateObj> andAll(final Iterable<Condition> conditions) {
        return state -> StreamSupport.stream(conditions.spliterator(), false).allMatch(pred -> pred.test(state));
    }

    static Predicate<StateObj> orAll(final Iterable<Condition> conditions) {
        return state -> StreamSupport.stream(conditions.spliterator(), false).anyMatch(pred -> pred.test(state));
    }

    public static Condition or(final Condition condition1, final Condition condition2) {
        return Condition.with(condition1.or(condition2), () -> describe(Arrays.asList(condition1, condition2), "OR"));
    }

    public static Condition or(final Condition... conditions) {
        return Condition.with(orAll(Arrays.asList(conditions)), () -> describe(Arrays.asList(conditions), "OR"));
    }

    public static Condition or(final Iterable<Condition> conditions) {
        return Condition.with(orAll(conditions), () -> describe(conditions, "OR"));
    }
}
