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
package com.dtolabs.rundeck.core.authentication;

import java.io.Serializable;
import java.security.Principal;

/**
 * @author noahcampbell
 *
 */
public class Username implements Principal,Serializable {
    private static final long serialVersionUID = 1L;
    public Username(String username) {
        super();
        this.username = username;
    }

    private final String username;
    
    /**
     * @see java.security.Principal#getName()
     */
    public String getName() {
        return username;
    }
    
    @Override
    public String toString() {
        return "RUNDECK Username: " + this.username;
    }

}
