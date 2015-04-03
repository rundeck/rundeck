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

import java.io.PrintStream;

public interface Explanation { 
    
    public enum Code {
        GRANTED, 
        GRANTED_NO_AUTHORIZATION_ATTEMPTED, 
        GRANTED_ACTIONS_AND_COMMANDS_MATCHED, 
        REJECTED, 
        REJECTED_NO_ACTION_PROVIDED, 
        REJECTED_NO_SUBJECT_OR_ENV_FOUND, 
        REJECTED_NO_RESOURCE_OR_ACTION_MATCH, 
        REJECTED_NO_ACTIONS_DECLARED, 
        REJECTED_ACTIONS_DECLARED_EMPTY, 
        REJECTED_NO_ACTIONS_MATCHED, 
        REJECTED_CONTEXT_EVALUATION_ERROR, 
        REJECTED_COMMAND_NOT_MATCHED, 
        REJECTED_NO_RESOURCE_PROPERTY_PROVIDED, 
        REJECTED_RESOURCE_PROPERTY_NOT_MATCHED, 
        REJECTED_NO_RULES_DECLARED, 
        REJECTED_NO_DESCRIPTION_PROVIDED, 
        REJECTED_NO_RESOURCE_TYPE,
        REJECTED_INVALID_FOR_SECTION,
        REJECTED_DENIED,
    };
    
    public Code getCode();
    
    public void describe(PrintStream out);
}