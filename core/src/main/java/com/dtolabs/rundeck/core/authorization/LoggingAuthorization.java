package com.dtolabs.rundeck.core.authorization;

import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

public class LoggingAuthorization
        implements Authorization
{
    private static Logger logger = Logger.getLogger(LoggingAuthorization.class);
    private Authorization authorization;

    public LoggingAuthorization(final Authorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public Decision evaluate(
            final Map<String, String> resource,
            final Subject subject,
            final String action,
            final Set<Attribute> environment
    )
    {
        Decision decision = getAuthorization().evaluate(resource, subject, action, environment);
        if (decision.isAuthorized()) {
            logger.info(MessageFormat.format("Evaluating {0} ({1}ms)", decision, decision.evaluationDuration()));
        } else {
            logger.warn(MessageFormat.format("Evaluating {0} ({1}ms)", decision, decision.evaluationDuration()));
        }
        return decision;
    }

    @Override
    public Set<Decision> evaluate(
            final Set<Map<String, String>> resources,
            final Subject subject,
            final Set<String> actions,
            final Set<Attribute> environment
    )
    {
        Set<Decision> decisions = getAuthorization().evaluate(resources, subject, actions, environment);
        boolean anyAuthorized = false;
        for (Decision decision : decisions) {
            if (decision.isAuthorized()) {
                anyAuthorized = true;
            }
        }
        for (Decision decision : decisions) {
            if (!anyAuthorized) {
                logger.warn(MessageFormat.format("Evaluating {0} ({1}ms)", decision, decision.evaluationDuration()));
            } else {
                logger.info(MessageFormat.format("Evaluating {0} ({1}ms)", decision, decision.evaluationDuration()));
            }
        }
        return decisions;
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
