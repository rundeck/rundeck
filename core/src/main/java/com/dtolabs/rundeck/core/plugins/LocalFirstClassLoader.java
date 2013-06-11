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
