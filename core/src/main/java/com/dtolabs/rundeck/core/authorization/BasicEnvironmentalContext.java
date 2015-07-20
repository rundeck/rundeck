package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Created by greg on 7/17/15.
 */
public class BasicEnvironmentalContext implements EnvironmentalContext {
    String key;
    String value;
    Pattern valuePattern;
    URI keyUri;

    private BasicEnvironmentalContext(final String key, final String value, final Pattern valuePattern) {
        this.key = key;
        this.value = value;
        this.valuePattern = valuePattern;
        keyUri = URI.create(
                EnvironmentalContext.URI_BASE + key
        );
    }

    public static BasicEnvironmentalContext patternContextFor(String key, String value) {
        final Pattern pattern;
        try {
            return new BasicEnvironmentalContext(key, value, Pattern.compile(value));
        } catch (PatternSyntaxException e) {
        }
        return new BasicEnvironmentalContext(key, value, null);
    }

    public static BasicEnvironmentalContext staticContextFor(String key, String value) {
        return new BasicEnvironmentalContext(key, value, null);
    }

    @Override
    public boolean matches(final Set<Attribute> environment) {
        if (environment.size() != 1) {
            return false;
        }
        Attribute next = environment.iterator().next();

        if (next.getProperty().equals(keyUri)) {
            if (value.equals(next.getValue())) {
                return true;
            } else if (null != valuePattern && valuePattern.matcher(next.getValue()).matches()) {
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
