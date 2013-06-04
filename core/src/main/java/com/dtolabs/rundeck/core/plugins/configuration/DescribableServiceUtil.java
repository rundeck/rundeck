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
* DescribableServiceUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 6:06 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.plugins.ProviderIdent;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.*;


/**
 * DescribableServiceUtil provides utility methods for use by {@link DescribableService} implementations.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DescribableServiceUtil {

    public static <T> List<Description> listDescriptions(final ProviderService<T> service) {
        return listDescriptions(service, true);
    }

    public static <T> List<Description> listDescriptions(final ProviderService<T> service,
                                                         final boolean includeFieldProperties) {
        final ArrayList<Description> list = new ArrayList<Description>();
        for (final ProviderIdent providerIdent : service.listProviders()) {
            try {
                final T providerForType = service.providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    final Describable desc = (Describable) providerForType;
                    final Description description = desc.getDescription();
                    if (null != description) {
                        list.add(description);
                    }
                } else if (PluginAdapterUtility.canBuildDescription(providerForType)) {
                    list.add(PluginAdapterUtility.buildDescription(providerForType, DescriptionBuilder.builder(),
                            includeFieldProperties));
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }

    public static <T> List<ProviderIdent> listDescribableProviders(final ProviderService<T> service) {
        final ArrayList<ProviderIdent> list = new ArrayList<ProviderIdent>();
        for (final ProviderIdent providerIdent : service.listProviders()) {
            try {
                final T providerForType = service.providerOfType(providerIdent.getProviderName());
                if (providerForType instanceof Describable) {
                    final Describable desc = (Describable) providerForType;
                    final Description description = desc.getDescription();
                    if (null != description) {
                        list.add(providerIdent);
                    }
                }else if(PluginAdapterUtility.canBuildDescription(providerForType)){
                    list.add(providerIdent);
                }
            } catch (ExecutionServiceException e) {
                e.printStackTrace();
            }

        }
        return list;
    }
}
