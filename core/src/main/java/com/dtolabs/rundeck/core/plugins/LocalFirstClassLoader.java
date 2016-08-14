/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
 */

package com.dtolabs.rundeck.core.plugins;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Loads classes from the local URLs first, before delegating to the parent.
 */
public class LocalFirstClassLoader extends URLClassLoader {
    public LocalFirstClassLoader(URL[] urls, ClassLoader classLoader) {
        super(urls, classLoader);
    }

    public LocalFirstClassLoader(URL[] urls) {
        super(urls);
    }

    public LocalFirstClassLoader(URL[] urls, ClassLoader classLoader, URLStreamHandlerFactory urlStreamHandlerFactory) {
        super(urls, classLoader, urlStreamHandlerFactory);
    }

    public static LocalFirstClassLoader newInstance(URL[] urls, ClassLoader classLoader) {
        return new LocalFirstClassLoader(urls, classLoader);
    }
    @Override
    public Class<?> loadClass(String s) throws ClassNotFoundException {
        return loadClass(s, false);
    }

    @Override
    protected synchronized Class<?> loadClass(String s, boolean b) throws ClassNotFoundException {
        Class c = findLoadedClass(s);
        if (c == null) {
            try {
                c = findClass(s);
            } catch (ClassNotFoundException e) {

            }
        }
        if (c == null) {
            c = getParent().loadClass(s);
        }
        if (b) {
            resolveClass(c);
        }
        return c;
    }

    @Override
    public URL getResource(String s) {
        URL resource = findResource(s);
        if(null==resource) {
            return getParent().getResource(s);
        }
        return resource;
    }
}
