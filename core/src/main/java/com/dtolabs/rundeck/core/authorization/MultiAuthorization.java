package com.dtolabs.rundeck.core.authorization;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Combines two Authorization implementations, and processes requests with both of them.
 */
public class MultiAuthorization implements Authorization {
    private Authorization auth1;
    private Authorization auth2;

    public MultiAuthorization(final Authorization auth1, final Authorization auth2) {
        this.auth1 = auth1;
        this.auth2 = auth2;
    }

    @Override
    public Decision evaluate(
            final Map<String, String> resource,
            final Subject subject,
            final String action,
            final Set<Attribute> environment
    )
    {
        Decision evaluate = auth1.evaluate(resource, subject, action, environment);
        if (evaluate.explain().getCode() == Explanation.Code.REJECTED_DENIED) {
            //fail fast
            return evaluate;
        }
        Decision evaluate2 = auth2.evaluate(resource, subject, action, environment);
        if (evaluate2.explain().getCode() == Explanation.Code.REJECTED_DENIED) {
            //fail fast
            return evaluate2;
        }
        if (evaluate.isAuthorized()) {
            return evaluate;
        }
        return evaluate2;
    }

    @Override
    public Set<Decision> evaluate(
            final Set<Map<String, String>> resources,
            final Subject subject,
            final Set<String> actions,
            final Set<Attribute> environment
    )
    {
        Set<Decision> evaluate = auth1.evaluate(resources, subject, actions, environment);
        HashMap<Map<String, String>, Map<String, Decision>> result1 = new HashMap<>();
        for (Decision decision : evaluate) {
            if (null == result1.get(decision.getResource())) {
                result1.put(decision.getResource(), new HashMap<String, Decision>());
            }
            Map<String, Decision> stringDecisionMap = result1.get(decision.getResource());
            stringDecisionMap.put(decision.getAction(), decision);
        }
        Set<Decision> evaluate2 = auth2.evaluate(resources, subject, actions, environment);
        Set<Decision> result = new HashSet<>();
        for (Decision decision2 : evaluate2) {
            Decision decision1 = result1.get(decision2.getResource()).get(decision2.getAction());
            //compare decision1 vs decision2
            if (decision1.explain().getCode() == Explanation.Code.REJECTED_DENIED) {
                result.add(decision1);
                continue;
            }
            if (decision2.explain().getCode() == Explanation.Code.REJECTED_DENIED) {
                result.add(decision2);
                continue;
            }
            if (decision1.isAuthorized()) {
                result.add(decision1);
                continue;
            }
            result.add(decision2);
        }
        return result;
    }
}
