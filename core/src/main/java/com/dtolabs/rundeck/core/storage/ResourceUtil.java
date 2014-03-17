package com.dtolabs.rundeck.core.storage;


import org.rundeck.storage.api.ContentFactory;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Tree;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides utility methods for use by the storage layer, or implementing plugins.
 */
public class ResourceUtil  {
    /**
     * Metadata key for the content-type
     */
    public static final String RES_META_RUNDECK_CONTENT_TYPE = "Rundeck-content-type";
    /**
     * Metadata key for the content size
     */
    public static final String RES_META_RUNDECK_CONTENT_LENGTH = "Rundeck-content-size";
    /**
     * Metadata key for the creation time
     */
    public static final String RES_META_RUNDECK_CONTENT_CREATION_TIME = "Rundeck-content-creation-time";
    /**
     * Metadata key for the modification time
     */
    public static final String RES_META_RUNDECK_CONTENT_MODIFY_TIME = "Rundeck-content-modify-time";
    /**
     * Date format for stored date/time
     */
    public static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final ThreadLocal<DateFormat> w3cDateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            SimpleDateFormat fmt = new SimpleDateFormat(ISO_8601_FORMAT, Locale.US);
            fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            return fmt;
        }
    };

    /**
     * Return a factory for ResourceMeta
     * @return
     */
    public static ContentFactory<ResourceMeta> factory(){
        return new ContentFactory<ResourceMeta>() {
            @Override
            public ResourceMeta create(HasInputStream hasInputStream, Map<String, String> metadata) {
                return withStream(hasInputStream, metadata);
            }
        };
    }

    /**
     * Create a new builder
     *
     * @return builder
     */
    public static ResourceMetaBuilder create() {
        return create(null);
    }

    /**
     * Create a new builder with a set of metadata
     *
     * @param meta original metadata
     *
     * @return builder
     */
    public static ResourceMetaBuilder create(Map<String, String> meta) {
        ResourceMetaBuilder mutableRundeckResourceMeta = new ResourceMetaBuilder(meta);
        return mutableRundeckResourceMeta;
    }

    /**
     * Construct a resource with a stream of data
     *
     * @param stream stream
     * @param meta   metadata
     *
     * @return new resource
     */
    public static ResourceMeta withStream(final InputStream stream, final HasResourceMeta meta) {
        return withStream(stream, meta.getResourceMeta());
    }

    /**
     * Construct a resource
     *
     * @param stream
     * @param meta
     *
     * @return
     */
    public static ResourceMeta withStream(final HasInputStream stream, final Map<String, String> meta) {
        return new BaseResource(meta) {
            @Override
            public InputStream readContent() throws IOException {
                return stream.getInputStream();
            }
        };
    }
    public static ResourceMeta withStream(final InputStream stream, final Map<String, String> meta) {
        return new BaseResource(meta) {
            @Override
            public InputStream readContent() throws IOException {
                return stream;
            }
        };
    }

    static ResourceMeta wrap(final ContentMeta contentMeta) {
        if (contentMeta instanceof ResourceMeta) {
            return (ResourceMeta) contentMeta;
        }
        return new BaseResource(contentMeta.getMeta()) {
            @Override
            public InputStream readContent() throws IOException {
                return contentMeta.readContent();
            }
        };
    }

    /**
     * Coerce a Tree of ResourceMeta into A ResourceTree
     * @param impl the tree
     * @return a ResourceTree
     */
    public static ResourceTree asResourceTree(Tree<ResourceMeta> impl) {
        return new ResourceTreeImpl(impl);
    }

    static long parseLong(String s, long defval) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }
        return defval;
    }

    static Date parseDate(String s, Date defval) {
        try {
            return w3cDateFormat.get().parse(s);
        } catch (ParseException ignored) {
        }
        return defval;
    }

    public static String formatDate(Date time) {
        return w3cDateFormat.get().format(time);
    }
}
