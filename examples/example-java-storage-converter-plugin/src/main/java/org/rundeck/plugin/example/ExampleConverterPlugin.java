package org.rundeck.plugin.example;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.data.DataUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Example converter plugin that converts all resource content into base64-encoded form and back. When encoding the
 * data, it adds metadata it can read later.  When asked to decode, it first checks if the metadata indicates it was
 * encoded.
 */
@Plugin(service = ServiceNameConstants.StorageConverter, name = "example-b64-converter")
public class ExampleConverterPlugin implements StorageConverterPlugin {

    public static final String X_EXAMPLE_B64_CONVERTER_WAS_ENCODED = "x-example-b64-converter:is-b64-encoded";

    /**
     * Returns a {@link HasInputStream} as a lazy way to wrap an input stream with a base64 encode/decode stream
     *
     * @param hasResourceStream source
     * @param doEncode          true to encode
     *
     * @return lazy stream
     */
    private static HasInputStream wrap(final HasInputStream hasResourceStream, final boolean doEncode) {
        return new HasInputStream() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new Base64InputStream(hasResourceStream.getInputStream(), doEncode);
            }

            @Override
            public long writeContent(OutputStream outputStream) throws IOException {
                Base64OutputStream codec = new Base64OutputStream(outputStream, doEncode);
                try {
                    return hasResourceStream.writeContent(codec);
                } finally {
                    codec.flush();
                    codec.close();
                }
            }
        };
    }

    private HasInputStream decode(HasInputStream hasResourceStream) {
        return wrap(hasResourceStream, false);
    }

    private HasInputStream encode(HasInputStream hasResourceStream) {
        return wrap(hasResourceStream, true);
    }


    /**
     * Returns true if the metadata indicates the data was encoded
     *
     * @param resourceMetaBuilder metadata
     *
     * @return true if encoded was to true
     */
    static boolean wasEncoded(ResourceMetaBuilder resourceMetaBuilder) {
        return Boolean.parseBoolean(resourceMetaBuilder.getResourceMeta().get(X_EXAMPLE_B64_CONVERTER_WAS_ENCODED));
    }

    /**
     * Add base64-encoded=true metadata
     *
     * @param resourceMetaBuilder metadata
     */
    static void addMetadataWasEncoded(ResourceMetaBuilder resourceMetaBuilder) {
        resourceMetaBuilder.setMeta(X_EXAMPLE_B64_CONVERTER_WAS_ENCODED, Boolean.TRUE.toString());
    }


    /**
     * Reads stored data, so decodes a base64 stream if the metadata indicates it has been encoded
     *
     * @param path
     * @param resourceMetaBuilder
     * @param hasResourceStream
     *
     * @return
     */
    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, final HasInputStream
            hasResourceStream) {
        if (wasEncoded(resourceMetaBuilder)) {
            return decode(hasResourceStream);
        }
        //return null to indicate no change was performed on the data
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasResourceStream) {
        addMetadataWasEncoded(resourceMetaBuilder);
        return encode(hasResourceStream);
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasResourceStream) {
        addMetadataWasEncoded(resourceMetaBuilder);
        return encode(hasResourceStream);
    }
}
