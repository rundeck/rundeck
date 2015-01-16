/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

/**
 * Facade for Authorization containing a subject
 */
public interface AuthContext {


    /**
     * Evaluate the authorization request and return if this request is valid. Make a single resource determination.
     * <br>
     * The method is loosely based on the XACML model for structuring requests and response.
     * <br>
     * These can be prefixed with a fully qualified namespace and ':'.
     * <br>
     * If anything goes wrong in evaluating the request, the result will be false.  No exception will be thrown.
     *
     * @param resource    The properties that identify the resource.
     * @param action      A set of actions that are being requested on the resource.
     * @param environment A set of environment properties (hostname, time of day, etc.)
     *
     * @return decision Return true if the subject's action on the object given the environment is authorized.
     */
    Decision evaluate(Map<String, String> resource, String action, Set<Attribute> environment);

    /**
     * Make a multiple resource determination.
     *
     * @param resources resource set
     * @param actions action set
     * @param environment environment
     *
     * @return decisions
     */
    Set<Decision> evaluate(Set<Map<String, String>> resources, Set<String> actions, Set<Attribute> environment);
}
