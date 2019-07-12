package com.dtolabs.rundeck.core.rules;

/**
 * Evaluates true if the state has the key and exact value
 */
public class KeyValueEqualsCondition implements Condition {
    private String key;
    private String value;

    public KeyValueEqualsCondition(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean test(final StateObj input) {
        String anObject = input.getState().get(key);
        if (value == null) {
            return anObject == null;
        }
        String nValue = input.getState().get(value);
        if(nValue == null){
            return value.equals(anObject);
        }else{
            return nValue.equals(anObject);
        }
    }

    @Override
    public String toString() {
        return "(" + key + " == '" + value + '\'' + ")";
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
