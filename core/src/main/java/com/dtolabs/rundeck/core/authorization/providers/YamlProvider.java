package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Date;

/**
 * Created by greg on 7/17/15.
 */
public class YamlProvider {
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
        public Iterable<Object> loadAll(final Yaml yaml) throws FileNotFoundException, IOException {
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
}
