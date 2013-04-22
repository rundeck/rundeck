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
* BasePluggableProviderService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:02 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;

import java.lang.reflect.Constructor;


/**
 * FrameworkPluggableProviderService uses the Framework's plugin manager, and attempts to construct provider instances by
 * injecting the Framework instance as a Constructor parameter if possible.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class FrameworkPluggableProviderService<T> extends BasePluggableProviderService<T> {
    private Framework framework;

    protected FrameworkPluggableProviderService(final String name,
                                                final Framework framework,
                                                final Class<? extends T> implementationClass) {
        super(name, implementationClass);
        this.framework = framework;
    }

    public ServiceProviderLoader getPluginManager() {
        return framework.getPluginManager();
    }

    protected Framework getFramework() {
        return framework;
    }

    @Override
    protected boolean hasValidProviderSignature(Class clazz) {

        try {
            final Constructor method = clazz.getDeclaredConstructor(new Class[]{Framework.class});
            return null != method;
        } catch (NoSuchMethodException e) {
        }
        return super.hasValidProviderSignature(clazz);
    }

    @Override
    protected T createProviderInstanceFromType(Class<? extends T> execClass, String providerName) throws ProviderCreationException {
        try {
            final Constructor<? extends T> method = execClass.getDeclaredConstructor(new Class[]{Framework.class});
            return method.newInstance(framework);
        } catch (NoSuchMethodException e) {
            //ignore
        } catch (Exception e) {
            throw new ProviderCreationException("Unable to create provider instance: " + e.getMessage(), e, getName(),
                    providerName);
        }
        return super.createProviderInstanceFromType(execClass, providerName);
    }
}
