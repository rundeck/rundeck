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
* ChainedProviderServiceImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 6:16 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;

import java.util.*;


/**
 * Concrete implementation of ChainedProviderService
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ChainedProviderServiceImpl<T> extends ChainedProviderService<T> {
    private String name;
    private List<ProviderService<T>> serviceList;

    public ChainedProviderServiceImpl(final String name,
                                      final ProviderService<T> primaryService,
                                      final ProviderService<T> secondaryService) {
        this.name = name;
        serviceList = new ArrayList<ProviderService<T>>();
        serviceList.add(primaryService);
        serviceList.add(secondaryService);
    }

    public ChainedProviderServiceImpl(String name, List<ProviderService<T>> serviceList) {
        this.name = name;
        this.serviceList = serviceList;
    }

    public String getName() {
        return name;
    }

    @Override
    protected List<ProviderService<T>> getServiceList() {
        return serviceList;
    }

    /**
     * Factory method
     * @return create a {@link ChainedProviderService}
     * @param <X> provider class
     * @param name name
     * @param primary first service
     * @param secondary secondary service
     */
    public static <X> ChainedProviderService<X> chain(final String name,
                                                      final ProviderService<X> primary,
                                                      final ProviderService<X> secondary) {

        return new ChainedProviderServiceImpl<X>(name, primary, secondary);
    }

    /**
     * Factory method
     * @return create a {@link ChainedProviderService}
     * @param <X> provider class
     * @param name name
     * @param services list of services
     */
    public static <X> ChainedProviderService<X> chain(final String name,
                                                      final List<ProviderService<X>> services) {

        return new ChainedProviderServiceImpl<X>(name, services);
    }
}
