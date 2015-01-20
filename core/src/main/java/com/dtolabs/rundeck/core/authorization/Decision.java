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

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

public interface Decision {

    /**
     * Was the result from {@link Authorization#evaluate(java.util.Map, javax.security.auth.Subject, String, java.util.Set)}
     * successful or not.
     * 
     * @return the authorization decision is authorized if this method returns true.
     */
    boolean isAuthorized();
    
    /**
     * @return Reason why the Decision was granted or not granted.
     */
    Explanation explain();
    
    /**
     * Decision evaluation time.
     * 
     * @return evaluationTime The number of milliseconds it took to render this decision.
     */
    long evaluationDuration();
    
    /**
     * Return the time in seconds since the cache was last refreshed.
     * @return seconds
     */
// TODO:    long staleness();
    
    Map<String, String> getResource();
    
    String getAction();
    
    Set<Attribute> getEnvironment();
    
    Subject getSubject();
    
}
