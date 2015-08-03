package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Validation;
import com.dtolabs.rundeck.core.authorization.ValidationSet;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

/**
 * Created by greg on 7/17/15.
 */
public class YamlProvider {
    static final FilenameFilter filenameFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".aclpolicy");
        }
    };

    public static Validation validate(final CacheableYamlSource source) {
        return validate(source, null);
    }

    public static Validation validate(final CacheableYamlSource source, final Set<Attribute> forcedContext) {
        return validate(Collections.singletonList(source), forcedContext);
    }

    public static Validation validate(final Iterable<CacheableYamlSource> sources, final Set<Attribute> forcedContext) {
        return validate(new ValidationSet(), sources, forcedContext);
    }

    public static Validation validate(final Iterable<CacheableYamlSource> sources) {
        return validate(sources, null);
    }

    private static Validation validate(
            ValidationSet validation,
            final Iterable<CacheableYamlSource> sources,
            final Set<Attribute> forcedContext
    )
    {
        for (CacheableYamlSource source : sources) {
            try {
                YamlPolicyCollection yamlPolicyCollection = YamlProvider.policiesFromSource(
                        source,
                        forcedContext,
                        validation
                );
            } catch (Exception e1) {
                validation.addError(source.getIdentity(), e1.getMessage());
            }
        }
        validation.complete();
        return validation;
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
    public static YamlPolicyCollection policiesFromSource(final YamlSource source) throws IOException {
        return new YamlPolicyCollection(source);
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
    public static YamlPolicyCollection policiesFromSource(final YamlSource source, final Set<Attribute> forcedContext)
            throws IOException
    {
        return new YamlPolicyCollection(source, forcedContext, null);
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
    public static YamlPolicyCollection policiesFromSource(
            final YamlSource source,
            final Set<Attribute> forcedContext,
            ValidationSet validation
    )
            throws IOException
    {
        return new YamlPolicyCollection(source, forcedContext, validation);
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
    public static YamlPolicyCollection policiesFromFile(final File source) throws IOException {
        return new YamlPolicyCollection(sourceFromFile(source));
    }

    public static CacheableYamlSource sourceFromFile(final File file) {
        return new CacheableYamlFileSource(file);
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
                list.add(YamlProvider.sourceFromFile(file));
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
            final Date modified
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
            public Iterable<Object> loadAll(final Yaml yaml) throws IOException {
                return yaml.loadAll(content);
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
     * @return source
     */
    public static CacheableYamlSource sourceFromStream(
            final String identity,
            final InputStream stream,
            final Date modified
    )
    {
        return new CacheableYamlStreamSource(stream, identity, modified);
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

        public CacheableYamlFileSource(final File file) {
            this.file = file;
        }

        @Override
        public Iterable<Object> loadAll(final Yaml yaml) throws IOException
        {
            if (null == fileInputStream) {
                fileInputStream = new FileInputStream(file);
            }
            return yaml.loadAll(fileInputStream);
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
    }

    private static class CacheableYamlStreamSource implements CacheableYamlSource {
        private final InputStream stream;
        private final String identity;
        private final Date modified;

        public CacheableYamlStreamSource(final InputStream stream, final String identity, final Date modified) {
            this.stream = stream;
            this.identity = identity;
            this.modified = modified;
        }

        @Override
        public Iterable<Object> loadAll(final Yaml yaml) throws IOException {
            return yaml.loadAll(stream);
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
