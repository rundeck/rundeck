package com.dtolabs.rundeck.core.storage;

import org.rundeck.storage.api.HasInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * BaseStreamResource is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-03-28
 */
public class BaseStreamResource extends BaseResource {
    private HasInputStream stream;

    public BaseStreamResource(Map<String, String> meta, HasInputStream stream) {
        super(meta);
        this.stream = stream;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return stream.getInputStream();
    }

    @Override
    public long writeContent(OutputStream outputStream) throws IOException {
        return stream.writeContent(outputStream);
    }
}
