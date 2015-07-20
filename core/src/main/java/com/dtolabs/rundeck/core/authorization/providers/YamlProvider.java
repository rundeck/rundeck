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
     * @param source source
     * @return policies
     * @throws IOException
     */
    public static YamlPolicyCollection policiesFromSource(final YamlSource source) throws IOException {
        return new YamlPolicyCollection(source);
    }
    /**
     * Load policies from a file
     * @param source source
     * @return policies
     * @throws IOException
     */
    public static YamlPolicyCollection policiesFromFile(final File source) throws IOException {
        return new YamlPolicyCollection(sourceFromFile(source));
    }
    public static CacheableYamlSource sourceFromFile(final File file) {
        return new CacheableYamlSource() {
            FileInputStream fileInputStream;
            @Override
            public Iterable<Object> loadAll(final Yaml yaml) throws IOException
            {
                if(null==fileInputStream){
                    fileInputStream=new FileInputStream(file);
                }
                return yaml.loadAll(fileInputStream);
            }

            @Override
            public String getIdentity() {
                return file.getAbsolutePath();
            }

            @Override
            public void close() throws IOException {
                if(null!=fileInputStream){
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
        };
    }

    /**
     * Source from a stream
     * @param identity identity
     * @param stream input stream
     * @return source
     */
    public static CacheableYamlSource sourceFromStream(final String identity,final InputStream stream) {
        final Date modified = new Date();
        return sourceFromStream(identity, stream, modified);
    }

    /**
     * Source from a stream
     * @param identity identity
     * @param stream stream
     * @param modified date the content was last modified, for caching purposes
     * @return source
     */
    public static CacheableYamlSource sourceFromStream(
            final String identity,
            final InputStream stream,
            final Date modified
    )
    {
        return new CacheableYamlSource() {
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
        };
    }
}
