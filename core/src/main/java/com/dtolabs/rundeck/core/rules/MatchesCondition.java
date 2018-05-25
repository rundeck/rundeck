package com.dtolabs.rundeck.core.rules;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by greg on 5/4/16.
 */
public class MatchesCondition extends KeyValueEqualsCondition {
    final Pattern keyPattern;
    final Pattern valuePattern;

    public MatchesCondition(final String key, final boolean keyRegex, final String value, final boolean valueRegex) {
        super(key, value);
        if (keyRegex) {
            this.keyPattern = Pattern.compile(key);
        } else {
            this.keyPattern = null;
        }
        if (valueRegex) {
            this.valuePattern = Pattern.compile(value);
        } else {
            this.valuePattern = null;
        }

    }

    @Override
    public boolean test(final StateObj input) {
        if (null == keyPattern && null == valuePattern) {
            return super.test(input);
        }
        Map<String, String> state = input.getState();
        for (String key : state.keySet()) {
            if (match(key, keyPattern, getKey())
                && match(state.get(key), valuePattern, getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean match(final String key, final Pattern pattern, final String equalsString) {
        if (null != pattern) {
            return pattern.matcher(key).matches();
        } else {
            return equalsString.equals(key);
        }
    }

    @Override
    public String toString() {
        return "Matches{" +
               (null != keyPattern ? "~" : "") + getKey() +
               (null != valuePattern?"=~":"==")+ getValue() +
               "}";
    }
}
