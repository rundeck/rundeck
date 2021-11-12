package org.rundeck.core.auth.access;

import java.util.Collections;
import java.util.List;

public class MissingParameter
        extends Exception
{
    private final List<String> parameters;

    public MissingParameter(String parameter) {
        this(Collections.singletonList(parameter));
    }

    public MissingParameter(final List<String> parameters) {
        super("Required parameters were missing: " + String.join(", ", parameters));
        this.parameters = parameters;
    }

    public List<String> getParameters() {
        return parameters;
    }
}