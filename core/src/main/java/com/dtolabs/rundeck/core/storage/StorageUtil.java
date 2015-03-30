package com.dtolabs.rundeck.core.storage;


import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.utils.Streams;
import org.rundeck.storage.api.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Provides utility methods for use by the storage layer, or implementing plugins.
 */
public class StorageUtil {
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
     * @return a factory for ResourceMeta
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
     * @return Construct a resource
     *
     * @param stream stream
     * @param meta metadata
     */
    public static ResourceMeta withStream(final HasInputStream stream, final Map<String, String> meta) {
        return new BaseStreamResource(meta,stream);
    }
    public static ResourceMeta withStream(final InputStream stream, final Map<String, String> meta) {
        return new BaseResource(meta) {
            @Override
            public long writeContent(OutputStream out) throws IOException {
                return Streams.copyStream(stream, out);
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return stream;
            }
        };
    }

    /**
     * Delete all resources and subdirectories of the given resource path
     * @param tree tree
     * @param path path
     * @return true if all resources were deleted successfully.
     */
    public static boolean deletePathRecursive(Tree<ResourceMeta> tree, Path path){
        if(tree.hasResource(path)) {
            //delete just this resource
            return tree.deleteResource(path);
        }else if (tree.hasDirectory(path)) {
            //list resources and delete
            Set<Resource<ResourceMeta>> resources = tree.listDirectory(path);
            boolean failed=false;
            for (Resource<ResourceMeta> resource : resources) {
                if(resource.isDirectory()){
                    if(!deletePathRecursive(tree,resource.getPath())){
                        failed=true;
                    }
                }else {
                    if(!tree.deleteResource(resource.getPath())){
                        failed=true;
                    }
                }
            }
            return !failed;
        }else{
            return true;
        }
    }

    /**
     * Coerce a Tree of ResourceMeta into A StorageTree
     * @param impl the tree
     * @return a StorageTree
     */
    public static StorageTree asStorageTree(Tree<ResourceMeta> impl) {
        return new StorageTreeImpl(impl);
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

    /**
     * Create a StorageTree using authorization context and authorizing tree
     * @param context auth context
     * @param authStorage authorizing storage tree
     * @param <S> context type
     * @return StorageTree for the authorization context
     */
    public static <S> StorageTree resolvedTree(S context, ExtTree<S, ResourceMeta> authStorage) {
        return ResolvedExtTree.with(context, authStorage);
    }
}
