package com.dtolabs.rundeck.core.authorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 7/30/15.
 */
public class ValidationSet implements Validation {
    private Map<String, List<String>> errors;
    private boolean valid;

    public ValidationSet() {
        valid = false;
        errors = new HashMap<>();
    }

    public void addError(String source, String message) {
        valid = false;
        if (null == errors.get(source)) {
            errors.put(source, new ArrayList<String>());
        }
        errors.get(source).add(message);
    }
    public void complete(){
        if(errors.size()<1){
            valid=true;
        }
    }

    @Override
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        return valid ? "Validation succeeded" : "Validation failed: " + errors;
    }
}
