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
* ProviderIdent.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 4:00 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.PairImpl;

/**
 * ProviderIdent is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ProviderIdent extends PairImpl<String, String> {
    /**
     * Constructor
     * @param service service name
     * @param providerName providername
     */
    ProviderIdent(final String service, final String providerName) {
        super(service, providerName);
    }

    public String getService() {
        return getFirst();
    }

    public String getProviderName() {
        return getSecond();
    }

    @Override
    public String toString() {
        return "Provider(" + getService() + "," + getProviderName() + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderIdent)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
