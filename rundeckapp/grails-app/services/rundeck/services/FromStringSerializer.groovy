package rundeck.services

import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 4/28/15.
 */
class FromStringSerializer implements JobSerializer {
    Map<String,String> data

    @Override
    void serialize(final String format, final OutputStream outputStream) {
        def s = data[format]
        outputStream.write(s.getBytes("UTF-8"))
    }
}
