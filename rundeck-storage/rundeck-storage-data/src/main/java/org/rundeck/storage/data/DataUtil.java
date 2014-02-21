package org.rundeck.storage.data;

import org.rundeck.storage.api.ContentFactory;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.PathUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 12:28 PM
 */
public class DataUtil {
    public static DataContent dataWithText(String text) {
        return withText(text, contentFactory());
    }

    public static DataContent dataWithText(String text, Map<String, String> meta) {
        return withText(text, meta, contentFactory());
    }

    public static <T extends ContentMeta> T withText(String text, ContentFactory<T> factory) {
        return withText(text, null, factory);
    }

    /**
     * Returns a read-only FileMeta from the input source
     *
     * @param text text data
     * @param meta meta data
     *
     * @return
     */
    public static <T extends ContentMeta> T withText(String text, Map<String, String> meta, ContentFactory<T> factory) {
        return factory.create(PathUtil.lazyStream(new ByteArrayInputStream(text.getBytes())), meta);
    }

    public static DataContent dataWithBytes(byte[] data) {
        return withBytes(data, contentFactory());
    }

    public static <T extends ContentMeta> T withBytes(byte[] data, ContentFactory<T> factory) {
        return withBytes(data, null, factory);
    }

    /**
     * Returns a read-only FileMeta from the input source
     *
     * @param data byte[] data
     * @param meta meta data
     *
     * @return
     */
    public static <T extends ContentMeta> T withBytes(byte[] data, Map<String, String> meta,
            ContentFactory<T> factory) {
        return withStream(new ByteArrayInputStream(data), meta, factory);
    }

    public static <T extends ContentMeta> T withStream(InputStream source, ContentFactory<T> factory) {
        return withStream(source, null, factory);
    }

    /**
     * Returns a read-only FileMeta from the input source
     *
     * @param source data
     * @param meta   meta data
     *
     * @return
     */
    public static <T extends ContentMeta> T withStream(InputStream source, Map<String, String> meta,
            ContentFactory<T> factory) {
        return factory.create(PathUtil.lazyStream(source), meta);
    }

    private static class Factory implements ContentFactory<DataContent> {
        @Override
        public DataContent create(HasInputStream source, Map<String, String> meta) {
            return new DataContent(source, meta);
        }
    }

    /**
     * Base factory for DataContent implementation
     *
     * @return
     */
    public static ContentFactory<DataContent> contentFactory() {
        return new Factory();
    }
}
