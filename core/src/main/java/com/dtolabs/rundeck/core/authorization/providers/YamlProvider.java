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

package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Validation;
import com.dtolabs.rundeck.core.authorization.ValidationSet;
import com.dtolabs.rundeck.core.authorization.providers.yaml.model.ACLPolicyDoc;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by greg on 7/17/15.
 */
public class YamlProvider {
    private static PolicyCollectionFactory factory;
    public static final Class<?> DEFAULT_FACTORY =   YamlPolicyFactoryV2.class;
    public static final String FACTORY_CLASS_PROPERTY = YamlProvider.class.getName() + ".factoryClass";

    static {
        String prop = System.getProperty(FACTORY_CLASS_PROPERTY);
        Class<?> factoryClass = DEFAULT_FACTORY;
        if (null != prop) {
            try {
                factoryClass = Class.forName(prop);
                if (!PolicyCollectionFactory.class.isAssignableFrom(factoryClass)) {
                    throw new RuntimeException("Cannot use class " + prop + " as PolicyCollectionFactory");
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Constructor<?> declaredConstructor = factoryClass.getDeclaredConstructor();
            Object o = declaredConstructor.newInstance();
            factory = (PolicyCollectionFactory) o;
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException
                e) {
            throw new RuntimeException(e);
        }

    }

    static final FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".aclpolicy");
        }
    };

    public static Validation validate(final CacheableYamlSource source, final ValidationSet validation) {
        return validate(source, null, validation);
    }

    public static Validation validate(
        final CacheableYamlSource source,
        final Set<Attribute> forcedContext, final ValidationSet validation
    ) {
        return validate(Collections.singletonList(source), forcedContext, validation);
    }

    public static Validation validate(
        final Iterable<CacheableYamlSource> sources,
        final Set<Attribute> forcedContext, final ValidationSet validation
    ) {
        return validate(validation, sources, forcedContext);
    }

    public static Validation validate(final Iterable<CacheableYamlSource> sources, final ValidationSet validation) {
        return validate(sources, null, validation);
    }

    private static Validation validate(
            ValidationSet validation,
            final Iterable<CacheableYamlSource> sources,
            final Set<Attribute> forcedContext
    )
    {
        return getFactory().validate(validation, sources, forcedContext);
    }

    public static PolicyCollectionFactory getFactory() {
        return factory;
    }

    public static void setFactory(PolicyCollectionFactory factory) {
        YamlProvider.factory = factory;
    }

    /**
     * Load policies from a source
     *
     * @param source source
     *
     * @return policies
     *
     * @throws IOException
     */
    public static PolicyCollection policiesFromSource(final YamlSource source) throws IOException {
        return policiesFromSource(source, null, null);
    }

    /**
     * Load policies from a source
     *
     * @param source        source
     * @param forcedContext Context to require for all policies parsed
     *
     * @return policies
     *
     * @throws IOException
     */
    public static PolicyCollection policiesFromSource(
            final YamlSource source,
            final Set<Attribute> forcedContext,
            final ValidationSet validation
    )
            throws IOException
    {

        return factory.policiesFromSource(source, forcedContext, validation);
    }

    /**
     * Load policies from a source
     *
     * @param source        source
     * @param forcedContext Context to require for all policies parsed
     *
     * @return policies
     *
     * @throws IOException
     */
    public static PolicyCollection policiesFromSource(
            final YamlSource source,
            final Set<Attribute> forcedContext
    )
            throws IOException
    {
        return policiesFromSource(source, forcedContext, null);
    }

    /**
     * Load policies from a file
     *
     * @param source source
     *
     * @return policies
     *
     * @throws IOException
     */
    public static PolicyCollection policiesFromFile(final File source) throws IOException {
        return policiesFromSource(sourceFromFile(source, null));
    }

    public static CacheableYamlSource sourceFromFile(final File file, final ValidationSet validationSet) {
        return new CacheableYamlFileSource(file, validationSet);
    }

    public static Iterable<CacheableYamlSource> asSources(final File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("dir should be a directory");
        }
        return asSources(dir.listFiles(filenameFilter));
    }

    public static Iterable<CacheableYamlSource> asSources(final File[] files) {
        ArrayList<CacheableYamlSource> list = new ArrayList<>();
        if (null != files) {
            for (File file : files) {
                list.add(YamlProvider.sourceFromFile(file, null));
            }
        }
        return list;
    }

    /**
     * Source from a stream
     *
     * @param identity identity
     * @param content  yaml string
     * @param modified date the content was last modified, for caching purposes
     *
     * @return source
     */
    public static CacheableYamlSource sourceFromString(
        final String identity,
        final String content,
        final Date modified,
        final ValidationSet validation
    )
    {
        return new CacheableYamlSource() {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public Date getLastModified() {
                return modified;
            }

            @Override
            public String getIdentity() {
                return identity;
            }

            @Override
            public Iterable<ACLPolicyDoc> loadAll(final Yaml yaml) throws IOException {
                return YamlParsePolicy.documentIterable(yaml.loadAll(content).iterator(), validation, identity);
            }

            @Override
            public ValidationSet getValidationSet() {
                return validation;
            }

            @Override
            public void close() throws IOException {

            }
        };
    }

    /**
     * Source from a stream
     *
     * @param identity identity
     * @param stream   stream
     * @param modified date the content was last modified, for caching purposes
     *
     * @param validationSet
     * @return source
     */
    public static CacheableYamlSource sourceFromStream(
        final String identity,
        final InputStream stream,
        final Date modified,
        final ValidationSet validationSet
    )
    {
        return new CacheableYamlStreamSource(stream, identity, modified, validationSet);
    }

    public static SourceProvider getDirProvider(final File rootDir) {
        return new DirProvider(rootDir);
    }

    public static SourceProvider getFileProvider(final File singleFile) {
        return new FileProvider(singleFile);
    }

    private static class CacheableYamlFileSource implements CacheableYamlSource {
        private final File file;
        FileInputStream fileInputStream;
        private final ValidationSet validationSet;

        public CacheableYamlFileSource(final File file, final ValidationSet validationSet) {
            this.file = file;
            this.validationSet = validationSet;
        }

        @Override
        public Iterable<ACLPolicyDoc> loadAll(final Yaml yaml) throws IOException
        {
            if (null == fileInputStream) {
                fileInputStream = new FileInputStream(file);
            }
            return YamlParsePolicy.documentIterable(
                yaml.loadAll(fileInputStream).iterator(),
                validationSet,
                file.getName()
            );
        }

        @Override
        public String getIdentity() {
            return file.getAbsolutePath();
        }

        @Override
        public void close() throws IOException {
            if (null != fileInputStream) {
                fileInputStream.close();
            }
        }

        @Override
        public boolean isValid() {
            return file.exists();
        }

        @Override
        public Date getLastModified() {
            return new Date(file.lastModified());
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheableYamlFileSource that = (CacheableYamlFileSource) o;

            return file.equals(that.file);

        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public ValidationSet getValidationSet() {
            return validationSet;
        }
    }

    private static class CacheableYamlStreamSource implements CacheableYamlSource {
        private final InputStream   stream;
        private final String        identity;
        private final Date          modified;
        private final ValidationSet validationSet;

        public CacheableYamlStreamSource(
            final InputStream stream,
            final String identity,
            final Date modified,
            final ValidationSet validationSet
        ) {
            this.stream = stream;
            this.identity = identity;
            this.modified = modified;
            this.validationSet = validationSet;
        }

        @Override
        public Iterable<ACLPolicyDoc> loadAll(final Yaml yaml) throws IOException {
            return YamlParsePolicy.documentIterable(yaml.loadAll(stream).iterator(), validationSet, identity);
        }

        @Override
        public String getIdentity() {
            return identity;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }

        @Override
        public boolean isValid() {
            try {
                return null != stream && stream.available() > -1;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public Date getLastModified() {
            return modified;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final CacheableYamlStreamSource that = (CacheableYamlStreamSource) o;

            return identity.equals(that.identity);

        }

        @Override
        public int hashCode() {
            return identity.hashCode();
        }

        @Override
        public ValidationSet getValidationSet() {
            return validationSet;
        }
    }

    static class DirProvider implements SourceProvider {
        private File rootDir;

        public DirProvider(final File rootDir) {
            this.rootDir = rootDir;
        }

        long lastDirListCheckTime = 0;
        private File[] lastDirList;

        private File[] listDirFiles() {
            if (System.currentTimeMillis() - lastDirListCheckTime > PoliciesCache.DIR_LIST_CHECK_DELAY) {
                doListDir();
            }
            return lastDirList;
        }

        private void doListDir() {
            lastDirList = rootDir.listFiles(filenameFilter);
            lastDirListCheckTime = System.currentTimeMillis();
        }

        public Iterator<CacheableYamlSource> getSourceIterator() {
            return asSources(listDirFiles()).iterator();
        }
    }

    static class FileProvider implements SourceProvider {
        private File file;

        public FileProvider(final File file) {
            this.file = file;
        }

        @Override
        public Iterator<CacheableYamlSource> getSourceIterator() {
            return asSources(new File[]{file}).iterator();
        }
    }
}
