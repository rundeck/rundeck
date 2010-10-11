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

package com.dtolabs.rundeck.core.authentication;

import com.dtolabs.rundeck.core.common.Framework;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.dtolabs.rundeck.core.CoreException;
import org.apache.log4j.Category;


/**
 * Factory class for instances of {@link INodeAuthResolutionStrategy}
 */
public class NodeAuthResolutionStrategyFactory {
    public static final Category logger = Category.getInstance(NodeAuthResolutionStrategyFactory.class);


    public static INodeAuthResolutionStrategy create(String classname, Framework framework) {
        return createNodeAuthResolutionStrategy(classname,framework);
    }


    private static INodeAuthResolutionStrategy createNodeAuthResolutionStrategy(final String classname, final Framework framework) {
        if (null==classname || "".equals(classname)) {
            throw new IllegalArgumentException("A null or empty java class name was specified for the framework.nodeauthentication.classname.");
        }

        logger.debug("NodeAuthResolutionStrategyFactory using node authentication class: '" + classname+"'");
        final INodeAuthResolutionStrategy strategy;
        try {
            final Class cls = Class.forName(classname);
            final Method method = cls.getDeclaredMethod("create", new Class[]{Framework.class});
            strategy = (INodeAuthResolutionStrategy) method.invoke(null, new Object[]{framework});
        } catch (ClassNotFoundException e) {
            throw new CoreException("error instantiating node authentication class: '" + classname+"'", e);
        } catch (IllegalAccessException e) {
            throw new CoreException("error instantiating node authentication class: '" + classname+"'", e);
        } catch (NoSuchMethodException e) {
            throw new CoreException("error instantiating node authentication class: '" + classname +"'", e);
        } catch (InvocationTargetException e) {
            throw new CoreException("error instantiating node authentication class: '" + classname +"'", e);
        }

        return strategy;

    }

}
