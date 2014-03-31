package org.rundeck.storage.data;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.HasInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Basic implementation of ContentMeta
 */
public class DataContent implements ContentMeta {
    InputStream stream;
    HasInputStream lazyStream;
    Map<String, String> meta;

    DataContent(InputStream stream, Map<String, String> meta) {
        this.stream = stream;
        this.meta = meta;
    }

    public DataContent(HasInputStream lazyStream, Map<String, String> meta) {
        this.lazyStream = lazyStream;
        this.meta = meta;
    }

    @Override
    public Map<String, String> getMeta() {
        return meta;
    }


    @Override
    public long writeContent(OutputStream out) throws IOException {
        if (null != stream) {
            return DataUtil.copyStream(stream, out);
        } else if (null != lazyStream) {
            return lazyStream.writeContent(out);
        }
        return -1;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (null != stream) {
            return stream;
        } else if (null != lazyStream) {
            return lazyStream.getInputStream();
        }
        return null;
    }
}
