package org.rundeck.core.auth.web;

import org.rundeck.core.auth.access.MissingParameter;
import org.rundeck.core.auth.access.ResIdResolver;

import java.util.*;

/**
 * Resolve ID param values
 */
public class WebParamsIdResolver
        implements ResIdResolver
{

    private final Map<String, String> paramNames;
    private final Map<Object, Object> paramMap;

    /**
     * @param paramNames parameter names for resource types
     * @param paramMap   parameter values
     */
    public WebParamsIdResolver(final Map<String, String> paramNames, final Map<Object, Object> paramMap) {
        this.paramNames = paramNames;
        this.paramMap = paramMap;
    }

    @Override
    public String idForType(final String type) throws MissingParameter {
        return idForTypeOptional(type)
                .orElseThrow(() -> new MissingParameter(paramNames.getOrDefault(type, type)));
    }

    @Override
    public Optional<String> idForTypeOptional(final String type) {
        String param = paramNames.getOrDefault(type, type);
        Object o = paramMap.get(param);
        if (o == null) {
            return Optional.empty();
        }

        return Optional.of(o.toString());
    }
}
