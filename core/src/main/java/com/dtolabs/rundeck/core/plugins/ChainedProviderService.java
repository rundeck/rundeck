/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* ChainedProviderService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 4:34 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * ChainedProviderService attempts to load the provider from a primary service, and if that fails,
 * it attempts it from a secondary service.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class ChainedProviderService<T> implements ProviderService<T> {

    @Override
    public T providerOfType(final String providerName) throws ExecutionServiceException {
        T t = null;
        MissingProviderException caught = null;
        for (final ProviderService<T> service : getServiceList()) {
            try {
                t = service.providerOfType(providerName);
            } catch (MissingProviderException e) {
                //ignore and attempt to load from the secondary service
                caught = e;
            }
            if (null != t) {
                return t;
            }

        }
        if (null != caught) {
            throw caught;
        } else {
            throw new MissingProviderException("Provider not found", getName(), providerName);
        }
    }

    @Override
    public List<ProviderIdent> listProviders() {
        final HashSet<ProviderIdent> providers = new HashSet<ProviderIdent>();
        for (final ProviderService<T> service : getServiceList()) {
            providers.addAll(service.listProviders());
        }
        return new ArrayList<ProviderIdent>(providers);
    }

    protected abstract List<ProviderService<T>> getServiceList();
}
