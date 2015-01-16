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
* AdapterService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:33 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.utils.Converter;

import java.util.*;


/**
 * AdapterService adapts one service provider type to another.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class AdapterService<S,T> implements ProviderService<T> {
    private ProviderService<S> sourceService;
    private Converter<S,T> converter;

    public AdapterService(final ProviderService<S> sourceService, final Converter<S, T> converter) {
        this.sourceService = sourceService;
        this.converter = converter;
    }

    @Override
    public T providerOfType(final String providerName)
        throws ExecutionServiceException {
        return converter.convert(sourceService.providerOfType(providerName));
    }

    @Override
    public List<ProviderIdent> listProviders() {
        return sourceService.listProviders();
    }

    @Override
    public String getName() {
        return sourceService.getName();
    }

    public ProviderService<S> getSourceService() {
        return sourceService;
    }

    public Converter<S, T> getConverter() {
        return converter;
    }

    /**
     * @return Create an AdapterService given a source service and a Converter.
     * @param converter converter
     * @param sourceService source
     * @param <X> provider type
     * @param <Y> destination type
     */
    public static <X,Y> AdapterService<X,Y> adaptFor(final ProviderService<X> sourceService,
                                                     final Converter<X, Y> converter) {
        return new AdapterService<X, Y>(sourceService, converter);
    }
}
