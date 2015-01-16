package org.rundeck.storage.data;

import org.rundeck.storage.api.ContentFactory;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.PathUtil;

import java.io.*;
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
     * @param factory factory
     * @param <T> resource type
     *
     * @return content
     */
    public static <T extends ContentMeta> T withText(String text, Map<String, String> meta, ContentFactory<T> factory) {
        return factory.create(lazyStream(new ByteArrayInputStream(text.getBytes())), meta);
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
     * @param factory factory
     * @param <T> resource type
     *
     * @return content
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
     * @param factory factory
     * @param <T> resource type
     *
     * @return content
     */
    public static <T extends ContentMeta> T withStream(InputStream source, Map<String, String> meta,
            ContentFactory<T> factory) {
        return factory.create(lazyStream(source), meta);
    }

    private static class Factory implements ContentFactory<DataContent> {
        @Override
        public DataContent create(HasInputStream source, Map<String, String> meta) {
            return new DataContent(source, meta);
        }
    }

    /**
     * Lazy mechanism for stream loading
     *
     * @param data file
     *
     * @return lazy stream
     */
    public static HasInputStream lazyStream(final InputStream data) {
        return new HasInputStream() {
            @Override
            public InputStream getInputStream() throws IOException {
                return data;
            }

            @Override
            public long writeContent(OutputStream outputStream) throws IOException {
                return copyStream(data, outputStream);
            }
        };
    }


    /**
     * Base factory for DataContent implementation
     *
     * @return base factory
     */
    public static ContentFactory<DataContent> contentFactory() {
        return new Factory();
    }

    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        return copyStream(in, out, 10240);
    }

    public static long copyStream(InputStream in, OutputStream out, int bufsize) throws IOException {
        final byte[] buffer = new byte[bufsize];
        long tot = 0;
        int c;
        c = in.read(buffer);
        while (c >= 0) {
            if (c > 0) {
                out.write(buffer, 0, c);
                tot += c;
            }
            c = in.read(buffer);
        }
        return tot;
    }

    /**
     * Lazy mechanism for stream loading
     *
     * @param data file
     *
     * @return lazy stream
     */
    public static HasInputStream lazyFileStream(final File data) {
        return new HasInputStream() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new FileInputStream(data);
            }

            @Override
            public long writeContent(OutputStream outputStream) throws IOException {
                return copyStream(getInputStream(), outputStream);
            }
        };
    }
}
