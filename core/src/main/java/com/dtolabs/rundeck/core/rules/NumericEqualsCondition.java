package com.dtolabs.rundeck.core.rules;


public class NumericEqualsCondition implements Condition {
    private String key;
    private String value;

    final double THRESHOLD = .00001;

    public NumericEqualsCondition(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean test(final StateObj input) {
        String anObject = input.getState().get(key);
        if (value == null) {
            return false;
        }
        Float fValue;
        String nValue = input.getState().get(value);
        if(nValue == null){
            fValue = NumericCondition.extractFloat(value);
        }else{
            fValue = NumericCondition.extractFloat(nValue);
        }
        Float fObject = NumericCondition.extractFloat(anObject);
        return (Math.abs(fValue - fObject) < THRESHOLD);
    }

    @Override
    public String toString() {
        return "(" + value + " == '" + key + '\'' + ")";
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
