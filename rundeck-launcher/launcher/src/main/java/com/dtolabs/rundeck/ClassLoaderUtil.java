/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* RunClassLoader.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Oct 4, 2010 9:51:09 AM
*/
package com.dtolabs.rundeck;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassLoaderUtil can create a ClassLoader for a set of jars and/or library dirs containing jars.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ClassLoaderUtil {
    private ArrayList<File> libdirs;
    private ArrayList<File> jars;

    public ClassLoaderUtil(final File libdir) {
        libdirs = new ArrayList<File>();
        jars = new ArrayList<File>();
        libdirs.add(libdir);
        DEBUG("lib dirs: " + libdirs);
    }

    public void addJarFile(final File jarfile) {
        jars.add(jarfile);
    }

    public void addLibDir(final File libdir) {
        libdirs.add(libdir);
    }

    public static Method findMain(final Class cls) throws NoSuchMethodException {
        return cls.getDeclaredMethod("main", String[].class);
    }

    /**
     * get classloader based on the list of directories using {@link java.net.URLClassLoader}
     *
     * @param parent parent classloader
     *
     * @return ClassLoader
     *
     * @throws java.net.MalformedURLException If a bad dir name causes a malformed url
     */
    public ClassLoader getClassLoader(final ClassLoader parent) throws MalformedURLException {
        return getClassLoader(parent, libdirs, jars);
    }

    /**
     * get classloader based on the list of directories using {@link java.net.URLClassLoader}
     *
     * @param dirs List of dirs that contain jar files
     *
     * @return ClassLoader
     *
     * @throws java.net.MalformedURLException If a bad dir name causes a malformed url
     */
    private static ClassLoader getClassLoader(final ClassLoader parent, final List<File> dirs,
                                              final List<File> jars) throws MalformedURLException {

        final List<URL> jarUrlList = new ArrayList<URL>(); // contains a list of URL values

        addDirJars(dirs, jarUrlList);
        addJars(jars, jarUrlList);

        DEBUG("jar list: " + jarUrlList);

        return URLClassLoader.newInstance(jarUrlList.toArray(new URL[jarUrlList.size()]), parent);
    }

    private static void addJars(final List<File> jars, final List<URL> jarUrlList) throws MalformedURLException {
        for (final File jar : jars) {
            jarUrlList.add(jar.toURI().toURL());
        }
    }

    private static void addDirJars(final List<File> dirs, final List<URL> jarUrlList) throws MalformedURLException {
        for (final File jarDir : dirs) {
            final List<URL> jarUrls = listJarUrls(jarDir);
            jarUrlList.addAll(jarUrls);
        }
    }

    /**
     * return an array of URL's for each jar within given dirUrl and its classloader
     *
     * @param jarDir A directory containing jar files
     *
     * @return a list of URLS referencing jar files
     */
    private static List<URL> listJarUrls(final File jarDir) throws MalformedURLException {
        if (!jarDir.isDirectory()) {
            throw new IllegalArgumentException("path: " + jarDir.getAbsolutePath() + " is not a directory");
        }

        final List<URL> jarUrlList = new ArrayList<URL>();
        final String[] dirList = jarDir.list(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".jar");
            }
        });
        for (final String dirEntry : dirList) {
            DEBUG("listJarUrls(), adding jar: " + dirEntry);
            final URL jarUrl = new File(jarDir, dirEntry).toURI().toURL();
            jarUrlList.add(jarUrl);
        }

        return jarUrlList;
    }

    private static boolean isDebug = Boolean.getBoolean("rundeck.classloader.debug");

    private static void DEBUG(final String msg) {
        if (isDebug) {
            System.err.println("ClassLoaderUtil: " + msg);
        }
    }
}
