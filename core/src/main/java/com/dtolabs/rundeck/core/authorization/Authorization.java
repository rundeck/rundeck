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

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

/**
 * Instances of classes that implement this interface  take context and command info
 * and lookup whether the user can execute the specified handler.
 * 
 * @author noahcampbell
 */
public interface Authorization {
    
    /**
     * Evaluate the authorization request and return if this request is valid.  
     * Make a single resource determination.  
     * 
     * The method is loosely based on the XACML model for structuring requests and response.  
     * 
     * These can be prefixed with a fully qualified namespace and ':'.
     * 
     * If anything goes wrong in evaluating the request, the result will be false.  No exception will
     * be thrown.
     * 
     * @param resource The properties that identify the resource.
     * @param subject The properties that represent the subject.
     * @param action A set of actions that are being requested on the resource.
     * @param environment A set of environment properties (hostname, time of day, etc.)
     * 
     * @return decision Return true if the subject's action on the object given the environment is authorized.
     */
    Decision evaluate(Map<String, String> resource, Subject subject, String action, 
            Set<Attribute> environment);
    
    /**
     * Make a multiple resource determination by evaluating each action for each resource.
     * 
     * @param resources resource set
     * @param subject subject
     * @param actions action set
     * @param environment environment
     * @return decisions for each resource+action pair
     */
    Set<Decision> evaluate(Set<Map<String, String>> resources, Subject subject, Set<String> actions, 
            Set<Attribute> environment);
}
