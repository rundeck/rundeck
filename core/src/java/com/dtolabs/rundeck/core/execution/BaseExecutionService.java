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

/*
* BaseExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 12:15:15 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;

import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * BaseExecutionService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
abstract class BaseExecutionService implements ExecutionService {
    final HashMap<Class<? extends ExecutionItem>, Class<? extends Executor>> executorClasses;
    final HashMap<Class<?extends ExecutionItem>, Executor> executors;
    protected Framework framework;

    BaseExecutionService(final Map<Class<? extends ExecutionItem>, Class<? extends Executor>> mapping,
                         final HashMap<Class<?extends ExecutionItem>, Executor> executors,
                         final Framework framework) {
        this.executorClasses = new HashMap<Class<? extends ExecutionItem>, Class<? extends Executor>>(mapping);
        this.executors= executors;
        this.framework = framework;
    }

    /**
     * Return an executor instance for the given item.
     *
     * @param item ExecutorItem
     *
     * @return Executor
     *
     * @throws ExecutionException if an error occurs
     */
    public Executor executorForItem(final ExecutionItem item) throws ExecutionException {
        final Class<? extends ExecutionItem> aClass = item.getClass();
        return executorForItemClass(aClass);
    }

    /**
     * Return an executor for the given class
     * @param aClass ExecutionItem subclass
     * @return Executor
     * @throws ExecutionException if an error occurs
     */
    public Executor executorForItemClass(final Class<? extends ExecutionItem> aClass) throws ExecutionException {
        //find implementation class of ExecutionItem
        Class foundClz=null;
        Executor foundImpl=null;
        for (final Class clz : executors.keySet()) {
            if (aClass.equals(clz) || clz.isAssignableFrom(aClass)) {
                return executors.get(clz);
            }
        }
        for(final Class clz: executorClasses.keySet()) {
            if (aClass.equals(clz) || clz.isAssignableFrom(aClass)) {
                foundClz=clz;
            }
        }

        if (null!= foundClz && null != executorClasses.get(foundClz)) {
            final Class<? extends Executor> execClass = executorClasses.get(foundClz);
            try {
                final Constructor method = execClass.getDeclaredConstructor(new Class[]{});
                final Executor executor = (Executor) method.newInstance();
                executors.put(aClass, executor);
                return executor;
            } catch (NoSuchMethodException e) {
                throw new ExecutionException(
                    "Unable to create Executor for the ExecutioItem's class: " + aClass.getName(), e);
            } catch (InvocationTargetException e) {
                throw new ExecutionException(
                    "Unable to create Executor for the ExecutioItem's class: " + aClass.getName(), e);
            } catch (IllegalAccessException e) {
                throw new ExecutionException(
                    "Unable to create Executor for the ExecutioItem's class: " + aClass.getName(), e);
            } catch (InstantiationException e) {
                throw new ExecutionException(
                    "Unable to create Executor for the ExecutioItem's class: " + aClass.getName(), e);
            }
        }else {
            throw new ExecutionException(
                "Unable to create Executor for the ExecutioItem's class: " + aClass.getName()
                + ": No Executor class registered for this item type");

        }
    }

    /**
     * Register an implementation of Executor that can execute the given ExecutionItem class
     *
     * @param itemClass     ExecutionItem class
     * @param executorClass Executor class
     */
    public void registerExecutor(final Class<? extends ExecutionItem> itemClass,
                                 final Class<? extends Executor> executorClass) {
        executorClasses.put(itemClass, executorClass);
    }

}
