package com.dtolabs.rundeck.core.rules;


import java.util.Arrays;
import java.util.function.Predicate;
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

    @Override
    default Condition negate() {
        return Condition.with(t -> !test(t));
    }

    public static Condition not(final Condition condition) {
        return Condition.with(condition.negate());
    }

    public static Condition and(final Condition condition1, final Condition condition2) {
        return Condition.with(condition1.and(condition2));
    }

    public static Condition and(final Condition... conditions) {
        return Condition.with(andAll(conditions));
    }

    static Predicate<StateObj> andAll(final Condition[] conditions) {
        return andAll(Arrays.asList(conditions));
    }

    public static Condition and(final Iterable<Condition> conditions) {
        return Condition.with(andAll(conditions));
    }

    static Predicate<StateObj> andAll(final Iterable<Condition> conditions) {
        return state -> StreamSupport.stream(conditions.spliterator(), false).allMatch(pred -> pred.test(state));
    }

    static Predicate<StateObj> orAll(final Iterable<Condition> conditions) {
        return state -> StreamSupport.stream(conditions.spliterator(), false).anyMatch(pred -> pred.test(state));
    }

    public static Condition or(final Condition condition1, final Condition condition2) {
        return Condition.with(condition1.or(condition2));
    }

    public static Condition or(final Condition... conditions) {
        return Condition.with(orAll(Arrays.asList(conditions)));
    }

    public static Condition or(final Iterable<Condition> conditions) {
        return Condition.with(orAll(conditions));
    }
}
