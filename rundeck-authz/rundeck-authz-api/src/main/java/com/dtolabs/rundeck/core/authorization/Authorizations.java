package com.dtolabs.rundeck.core.authorization;

import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Authorizations {
    private Authorizations() {
    }

    /**
     * Append two authorizations
     *
     * @param auth
     * @param auth2
     */
    public static Authorization append(Authorization auth, Authorization auth2) {
        return new MultiAuth(auth, auth2);
    }

    static class MultiAuth
            implements Authorization
    {
        private final Authorization auth1;
        private final Authorization auth2;

        public MultiAuth(final Authorization auth1, final Authorization auth2) {
            this.auth1 = auth1;
            this.auth2 = auth2;
        }

        /**
         * @param code
         * @return ordinal of code, or -1 if REJECTED_DENIED
         */
        static int codeOrdinal(Explanation.Code code) {
            if (code == Explanation.Code.REJECTED_DENIED) {
                return -1;
            }
            return code.ordinal();
        }

        @Override
        public Decision evaluate(
                final Map<String, String> resource,
                final Subject subject,
                final String action,
                final Set<Attribute> environment
        )
        {
            Decision decision1 = auth1.evaluate(resource, subject, action, environment);
            Decision decision2 = auth2.evaluate(resource, subject, action, environment);
            return prioritize(decision1, decision2);
        }

        /**
         * Chooses between two decisions, based on explantion code priority
         *
         * @param decision1
         * @param decision2
         * @return decisions with highest priority
         */
        public Decision prioritize(final Decision decision1, final Decision decision2) {
            int d1 = codeOrdinal(decision1.explain().getCode());
            int d2 = codeOrdinal(decision2.explain().getCode());
            if (d1 < d2) {
                return decision1;
            } else if (d2 < d1) {
                return decision2;
            }
            return decision1;
        }

        @Override
        public Set<Decision> evaluate(
                final Set<Map<String, String>> resources,
                final Subject subject,
                final Set<String> actions,
                final Set<Attribute> environment
        )
        {
            Set<Decision> set1 = auth1.evaluate(resources, subject, actions, environment);
            Set<Decision> set2 = auth2.evaluate(resources, subject, actions, environment);
            Set<Decision> results = new HashSet<>();
            for (Decision d1 : set1) {
                boolean found = false;
                for (Decision d2 : set2) {
                    if (d1.getResource().equals(d2.getResource()) &&
                        d1.getAction().equals(d2.getAction())) {
                        results.add(prioritize(d2, d1));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    //!!
                }
            }
            return results;
        }
    }
}
