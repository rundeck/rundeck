package rundeck.services

import com.dtolabs.rundeck.plugins.scm.JobSerializer

/**
 * Created by greg on 4/28/15.
 */
class FromStringSerializer implements JobSerializer {
    Map<String,String> data

    FromStringSerializer(final Map<String, String> data) {
        this.data = data
    }

    @Override
    void serialize(final String format, final OutputStream outputStream) {
        def s = data[format]
        if(s==null){
            throw new IllegalArgumentException("Format not supported: " + format)
        }
        outputStream.write(s.getBytes("UTF-8"))
    }
}
