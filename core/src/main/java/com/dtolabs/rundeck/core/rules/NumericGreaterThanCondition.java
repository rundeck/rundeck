package com.dtolabs.rundeck.core.rules;


public class NumericGreaterThanCondition implements Condition {
    private String key;
    private String value;

    public NumericGreaterThanCondition(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean test(final StateObj input) {
        String anObject = input.getState().get(key);
        if (value == null) {
            return false;
        }
        Float fValue = NumericCondition.extractFloat(value);
        Float fObject = NumericCondition.extractFloat(anObject);
        return fObject>fValue;
    }

    @Override
    public String toString() {
        return "(" + value + " > '" + key + '\'' + ")";
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
