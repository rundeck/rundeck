package com.dtolabs.rundeck.core.authorization;

import java.util.List;
import java.util.Map;

/**
 * Created by greg on 7/30/15.
 */
public interface Validation {
    public boolean isValid();

    public Map<String, List<String>> getErrors();
}
