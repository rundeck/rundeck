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

package com.dtolabs.rundeck.core.authorization;


import com.dtolabs.rundeck.core.common.Framework;

import javax.security.auth.Subject;
import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides trivial implementation of {@link Authorization} interface.
 * 
 * @author alexh
 */
public class NoAuthorization implements Authorization, LegacyAuthorization {
    final Framework framework;
    final File baseDir;

    /**
     * Factory method returning an instance implementing the {@link Authorization} interface.
     *
     * @param framework  Framework instance
     * @param aclBasedir Directory where the ACLs reside.
     * @return
     */
    public static Authorization create(final Framework framework, final File aclBasedir) {
        return new NoAuthorization(framework, aclBasedir);
    }

    /**
     * Default constructor
     *
     * @param framework
     * @param aclBaseDir
     */
    public NoAuthorization(final Framework framework, final File aclBaseDir) {
        this.framework = framework;
        this.baseDir = aclBaseDir;
    }
    
    
    public String[] getMatchedRoles() {
        return new String[0];
    }

    
    public String listMatchedRoles() {
        return "";
    }

    
    public boolean authorizeScript(String user, String project, String adhocScript) throws AuthorizationException {
        return true;
    }

    
    public Decision evaluate(final Map<String, String> resource, final Subject subject, 
            final String action, final Set<Attribute> environment) {
        return new Decision() {

            
            public boolean isAuthorized() {
                return true;
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
                return new Explanation() {

                    public Code getCode() {
                        return Code.GRANTED_NO_AUTHORIZATION_ATTEMPTED;
                    }
                    
                    public void describe(PrintStream out) {
                        out.println("No authorization attempted.");
                    }
                };
            }};
    }

    
    public Set<Decision> evaluate(Set<Map<String, String>> resources, final Subject subject,
            Set<String> actions, final Set<Attribute> environment) {
        
        Set<Decision> decisions = new HashSet<Decision>();
        for(final Map<String, String> resource : resources) {
            for(final String action : actions) {
                decisions.add(new Decision() {

            
            public boolean isAuthorized() {
                return true;
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
                return new Explanation() {
                    
                    public void describe(PrintStream out) {
                        out.println("No authorization attempted.");
                    }

                    public Code getCode() {
                        return Code.GRANTED_NO_AUTHORIZATION_ATTEMPTED;
                    }
                };
            }});
            }
        }
        return decisions;
    }
}
