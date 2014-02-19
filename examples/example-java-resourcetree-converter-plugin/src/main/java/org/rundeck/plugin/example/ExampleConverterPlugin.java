package org.rundeck.plugin.example;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.resourcetree.HasResourceStream;
import com.dtolabs.rundeck.core.resourcetree.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.resourcetree.ResourceConverterPlugin;
import org.apache.commons.codec.binary.Base64InputStream;
import us.vario.greg.lct.model.Path;

import java.io.IOException;
import java.io.InputStream;

/**
 * Example converter plugin that converts all resource content into base64-encoded form and back. When encoding the
 * data, it adds metadata it can read later.  When asked to decode, it first checks if the metadata indicates it was
 * encoded.
 */
@Plugin(service = ServiceNameConstants.ResourceConverter, name = "example-b64-converter")
public class ExampleConverterPlugin implements ResourceConverterPlugin {

    public static final String X_EXAMPLE_B64_CONVERTER_WAS_ENCODED = "x-example-b64-converter:is-b64-encoded";

    /**
     * Returns a {@link HasResourceStream} as a lazy way to wrap an input stream with a base64 encode/decode stream
     *
     * @param hasResourceStream source
     * @param doEncode          true to encode
     *
     * @return lazy stream
     */
    private static HasResourceStream wrap(final HasResourceStream hasResourceStream, final boolean doEncode) {
        return new HasResourceStream() {
            @Override
            public InputStream getInputStream() throws IOException {
                return new Base64InputStream(hasResourceStream.getInputStream(), doEncode);
            }
        };
    }

    private HasResourceStream decode(HasResourceStream hasResourceStream) {
        return wrap(hasResourceStream, false);
    }

    private HasResourceStream encode(HasResourceStream hasResourceStream) {
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
        resourceMetaBuilder.setMeta(X_EXAMPLE_B64_CONVERTER_WAS_ENCODED, Boolean.toString(true));
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
    public HasResourceStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, final HasResourceStream
            hasResourceStream) {
        if (wasEncoded(resourceMetaBuilder)) {
            return decode(hasResourceStream);
        }
        //return null to indicate no change was performed on the data
        return null;
    }

    @Override
    public HasResourceStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasResourceStream hasResourceStream) {
        addMetadataWasEncoded(resourceMetaBuilder);
        return encode(hasResourceStream);
    }

    @Override
    public HasResourceStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasResourceStream hasResourceStream) {
        addMetadataWasEncoded(resourceMetaBuilder);
        return encode(hasResourceStream);
    }
}
