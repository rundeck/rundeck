package com.dtolabs.rundeck.core.rules;

import com.dtolabs.rundeck.core.Constants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumericCondition {
    final static String DOUBLE_PATTERN = "^(-?[0-9]+\\.?[0-9]*).*";

    public static Float extractFloat(final String value){
        if(value == null){
            return 0f;
        }
        Pattern pattern = Pattern.compile(DOUBLE_PATTERN);
        Matcher matcher = pattern.matcher(value);
        if(matcher.matches()){
            String val = matcher.group(1);
            Float nValue = Float.parseFloat(val);
            return nValue;
        }else{
            return 0f;
        }
    }
}
