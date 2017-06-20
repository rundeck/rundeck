package com.dtolabs.rundeck.core.rules;

import com.google.common.base.Predicate;

/**
 * A predicate of a state
 */
public interface Condition extends Predicate<StateObj> {
}
