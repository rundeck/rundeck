/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* BasAuthorization.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/7/11 9:34 AM
* 
*/
package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.common.Framework;
import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import java.io.File;
import java.io.PrintStream;
import java.security.Principal;
import java.util.*;

/**
 * BasAuthorization is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseAuthorization implements Authorization, LegacyAuthorization {
    final Framework framework;
    final File baseDir;
    final Explanation explanation = new Explanation() {

        public String toString() {
            return "\t" + getDescription() + " => " + getCode();
        }

        public void describe(final PrintStream out) {
            out.println(toString());
        }

        public Code getCode() {
            return getResultCode();
        }
    };

    protected abstract Logger getLogger();
    protected abstract String getDescription();

    protected abstract Explanation.Code getResultCode();

    protected abstract boolean isAuthorized();

    final class BaseAuthorizationDecision implements Decision {

        Map<String, String> resource;
        String action;
        Set<Attribute> environment;
        Subject subject;

        BaseAuthorizationDecision(final Map<String, String> resource, final String action,
                                  final Set<Attribute> environment,
                                  final Subject subject) {
            this.resource = resource;
            this.action = action;
            this.environment = environment;
            this.subject = subject;
        }

        public boolean isAuthorized() {
            return BaseAuthorization.this.isAuthorized();
        }


        public Map<String, String> getResource() {
            return resource;
        }


        public String getAction() {
            return action;
        }


        public Set<Attribute> getEnvironment() {
            return environment;
        }


        public Subject getSubject() {
            return subject;
        }

        public Explanation explain() {
            return explanation;
        }

        public long evaluationDuration() {
            return 0;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("Decision for: ");
            builder.append("res<");
            final Iterator<Map.Entry<String, String>> riter = resource.entrySet().iterator();
            while (riter.hasNext()) {
                final Map.Entry<String, String> s = riter.next();
                builder.append(s.getKey()).append(':').append(s.getValue());
                if (riter.hasNext()) {
                    builder.append(", ");
                }
            }

            builder.append("> subject<");
            final Iterator<Principal> iter = subject.getPrincipals().iterator();
            while (iter.hasNext()) {
                final Principal principal = iter.next();
                builder.append(principal.getClass().getSimpleName());
                builder.append(':');
                builder.append(principal.getName());
                if (iter.hasNext()) {
                    builder.append(' ');
                }
            }

            builder.append("> action<");
            builder.append(action);

            builder.append("> env<");
            final Iterator<Attribute> eiter = environment.iterator();
            while (eiter.hasNext()) {
                final Attribute a = eiter.next();
                builder.append(a);
                if (eiter.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            builder.append(": authorized: ");
            builder.append(isAuthorized());
            builder.append(": ");
            builder.append(explanation.toString());

            return builder.toString();
        }

    }

    /**
     * Default constructor
     *
     * @param framework
     * @param aclBaseDir
     */
    public BaseAuthorization(final Framework framework, final File aclBaseDir) {
        this.framework = framework;
        this.baseDir = aclBaseDir;
    }


    public String[] getMatchedRoles() {
        return new String[0];
    }


    public String listMatchedRoles() {
        return "";
    }


    public boolean authorizeScript(final String user, final String project, final String adhocScript) throws
        AuthorizationException {
        return isAuthorized();
    }


    public Decision evaluate(final Map<String, String> resource, final Subject subject,
                             final String action, final Set<Attribute> environment) {
        final BaseAuthorizationDecision decision = new BaseAuthorizationDecision(resource, action,
            environment, subject);
        final StringBuilder sb = new StringBuilder();
        sb.append("Evaluating ").append(decision).append(" (").append(decision.evaluationDuration()).append("ms)")
            .append(':');

        sb.append(decision.explain().toString());

        getLogger().info(sb.toString());
        return decision;
    }


    public Set<Decision> evaluate(final Set<Map<String, String>> resources, final Subject subject,
                                  final Set<String> actions, final Set<Attribute> environment) {

        final Set<Decision> decisions = new HashSet<Decision>();
        for (final Map<String, String> resource : resources) {
            for (final String action : actions) {
                decisions.add(new BaseAuthorizationDecision(resource, action, environment, subject));
            }
        }
        for (final Decision decision : decisions) {

            final StringBuilder sb = new StringBuilder();
            sb.append("Evaluating ").append(decision).append(" (").append(decision.evaluationDuration()).append("ms)")
                .append(':');

            sb.append(decision.explain().toString());

            getLogger().info(sb.toString());
        }

        return decisions;
    }
}
