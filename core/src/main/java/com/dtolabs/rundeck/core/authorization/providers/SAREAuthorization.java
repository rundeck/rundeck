/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

/**
 * 
 */
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;
import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.Principal;
import java.util.*;

/**
 * Given a Subject, Action, Resource and Environment deliver an authorization decision.
 * 
 * @author noahcampbell
 *
 */
public class SAREAuthorization implements Authorization {
    
    private final static Logger logger = Logger.getLogger(SAREAuthorization.class);
    
    private final Policies policies;
    private final File baseDirectory;
    private long decisionsMade;
    
    /**
     * Create an authorization object that uses understands the .aclpolicy files.
     * 
     * @param directory The directory to ready *.aclpolicy from.
     * 
     * @throws IOException on io error
     * @throws PoliciesParseException on parse error
     */
    public SAREAuthorization(File directory) throws IOException, PoliciesParseException {
        policies = Policies.load(directory);
        baseDirectory = directory;
    }

    /**
     * Convenience constructor that looks in a predefine spot for policy files.
     * 
     * @throws IOException on io error
     * @throws PoliciesParseException on parse error
     */
    public SAREAuthorization() throws IOException, PoliciesParseException {
        this(new File("/etc/rundeck/security.d/"));
    }
    
    /**
     * 
     * @param resource resource
     * @param subject subject
     * @param action action
     * @param environment environment
     * @param contexts contexts
     * @return decision
     */
    private Decision internalEvaluate(Map<String, String> resource, Subject subject, String action,
                                      Set<Attribute> environment, List<AclContext> contexts) {
        long start = System.currentTimeMillis();
        if (contexts.size() < 1) {
            return authorize(false, "No context matches subject or environment", Code.REJECTED_NO_SUBJECT_OR_ENV_FOUND,
                    resource, subject, action, environment, System.currentTimeMillis() - start);
        }
        if(resource == null) {
            throw new IllegalArgumentException("Resource does not identify any resource because it's an empty resource property or null.");
        } else {
            for(Map.Entry<String, String> entry : resource.entrySet()) {
                if(entry.getKey() == null) {
                    throw new IllegalArgumentException("Resource definition cannot contain null property name.");
                }
                if(entry.getValue() == null) {
                    throw new IllegalArgumentException("Resource definition cannot contain null value.  Corresponding key: " + entry.getKey());
                }
            }
        }
        
        if(subject == null) throw new IllegalArgumentException("Invalid subject, subject is null.");
        if(action == null || action.length() <= 0) {
            return authorize(false, "No action provided.", Code.REJECTED_NO_ACTION_PROVIDED, resource, subject, action, environment, System.currentTimeMillis() - start);
        }
        // environment can be null.
        if(environment == null) {
            environment = Collections.emptySet();
        }
        
        this.decisionsMade++;
        

        ContextDecision contextDecision = null;
        ContextDecision lastDecision = null;

        //long contextIncludeStart = System.currentTimeMillis();
        boolean granted=false;
        boolean denied=false;
        for(AclContext ctx : contexts) {
            final ContextDecision includes = ctx.includes(resource, action);
            if (Code.REJECTED_DENIED == includes.getCode()) {
                contextDecision = includes;
                denied=true;
                return createAuthorize(false, contextDecision, resource, subject, action, environment,
                    System.currentTimeMillis() - start);
            }else if (includes.granted()) {
                contextDecision = includes;
                granted=true;
            }
            lastDecision = includes;
        }
        if(granted){
            return createAuthorize(true, contextDecision, resource, subject, action, environment,
                System.currentTimeMillis() - start);
        }

        if(lastDecision == null) {
            return authorize(false, "No resource or action matched.", 
                Code.REJECTED_NO_RESOURCE_OR_ACTION_MATCH, resource, subject, action, environment, System.currentTimeMillis() - start);
        } else {
            return createAuthorize(false, lastDecision, resource, subject, action, environment, System.currentTimeMillis() - start);
        }
    }
    
    /**
     * 
     */
    public Decision evaluate(Map<String, String> resource, Subject subject, 
            String action, Set<Attribute> environment) {
        return evaluate(resource, subject, action, environment, policies.narrowContext(subject, environment));
    }
    /**
     * Return the evaluation decision for the resource, subject, action, environment and contexts
     */
    private Decision evaluate(Map<String, String> resource, Subject subject,
                             String action, Set<Attribute> environment, List<AclContext>contexts) {

        Decision decision = internalEvaluate(resource, subject, action, environment, contexts);
        StringBuilder sb = new StringBuilder();
        sb.append("Evaluating ").append(decision).append(" (").append(decision.evaluationDuration()).append("ms)");
        logger.info(sb.toString());

        return decision;
    }

    public Set<Decision> evaluate(Set<Map<String, String>> resources, Subject subject, Set<String> actions,
            Set<Attribute> environment) {
        
        Set<Decision> decisions = new HashSet<Decision>();
        long duration=0;
        List<AclContext> aclContexts = policies.narrowContext(subject, environment);
        for(Map<String, String> resource: resources) {
            for(String action: actions) {
                final Decision decision = evaluate(resource, subject, action, environment, aclContexts);
                duration += decision.evaluationDuration();
                decisions.add(decision);
            }
        }
        return decisions;
    }
    
    
    
    private static Decision authorize(final boolean authorized, final String reason, 
            final Code reasonId, final Map<String, String> resource, final Subject subject, 
            final String action, final Set<Attribute> environment, final long evaluationTime) {
        return createAuthorize(authorized, new Explanation() {
                    
                    public Code getCode() {
                        return reasonId;
                    }
                    
                    public void describe(PrintStream out) {
                        out.println(toString());
                    }
                    
                    public String toString() {
                        return "\t" + reason + " => " + reasonId;
                    }
                }, resource, subject, action, environment, evaluationTime);
    }
    
    private static Decision createAuthorize(final boolean authorized, final Explanation explanation, 
            final Map<String, String> resource, final Subject subject, 
            final String action, final Set<Attribute> environment, final long evaluationTime) {
        
        return new Decision(){
            private String representation;
            public boolean isAuthorized() { return authorized; }
            public Map<String, String> getResource() { return resource;  }
            public String getAction() { return action; }
            public Set<Attribute> getEnvironment() { return environment; }
            public Subject getSubject() { return subject; }
            public String toString() {
                if(representation == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Decision for: ");
                    builder.append("res<");
                    Iterator<Map.Entry<String,String>> riter = resource.entrySet().iterator();
                    while(riter.hasNext()) {
                        Map.Entry<String, String> s = riter.next();
                        builder.append(s.getKey()).append(':').append(s.getValue());
                        if(riter.hasNext()) {
                            builder.append(", ");
                        }
                    }
                    
                    builder.append("> subject<");
                    Iterator<Principal> iter = subject.getPrincipals().iterator();
                    while(iter.hasNext()) {
                        Principal principal = iter.next();
                        builder.append(principal.getClass().getSimpleName());
                        builder.append(':');
                        builder.append(principal.getName());
                        if(iter.hasNext()) {
                            builder.append(' ');
                        }
                    }
                    
                    builder.append("> action<");
                    builder.append(action);
                    
                    builder.append("> env<");
                    Iterator<Attribute> eiter = environment.iterator();
                    while(eiter.hasNext()) {
                        Attribute a = eiter.next();
                        builder.append(a);
                        if(eiter.hasNext()) {
                            builder.append(", ");
                        }
                    }
                    builder.append(">");
                    builder.append(": authorized: ");
                    builder.append(isAuthorized());
                    builder.append(": ");
                    builder.append(explanation.toString());

                    this.representation = builder.toString();
                }
                return this.representation;
            }
            public Explanation explain() {
                return explanation;
            }
            public long evaluationDuration() {
                return evaluationTime;
            }
        };
    }  

    @Override
    public String toString() {
        return getClass().getName() + " (" + this.policies.count() + ") [" + this.baseDirectory.toString() + "]";
    }

    /**
     * This WILL be refactored.
     * @return role list
     */
    @Deprecated
    public List<String> hackMeSomeRoles() {
        return policies.listAllRoles();
    }

}
