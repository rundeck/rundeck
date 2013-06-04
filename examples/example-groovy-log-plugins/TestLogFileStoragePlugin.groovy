import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;
import java.util.zip.*

/**
 * This example is an log file storage plugin for Rundeck
 */
rundeckPlugin(LogFileStoragePlugin){
    def outputDir="/tmp"

    state { Map execution, Map configuration->
        def id = execution.execid
        //return state of storage given the id
        def tmpfile=new File(outputDir,"log-${id}.gz.tmp")
        def outfile=new File(outputDir,"log-${id}.gz")
        outfile.exists()? AVAILABLE : tmpfile.exists()? PENDING : NOT_FOUND
    }

    store { Map execution, Map configuration, InputStream source->
        def id = execution.execid
        //use gzip
        def outfile=new File(outputDir,"log-${id}.gz.tmp")
        source.withReader { reader ->
            outfile.withOutputStream { out ->
                def gzip=new OutputStreamWriter(new GZIPOutputStream(out))

                def line=reader.readLine()
                while(line!=null){
                    gzip.write(line+'\n')
                    line=reader.readLine()
                }
                gzip.flush()       
                gzip.close()
            }
            outfile.renameTo(new File(outputDir,"log-${id}.gz"))
        }
        source.close()
        true
    }

    retrieve {  Map execution, Map configuration, OutputStream out->
        def id = execution.execid

        def infile=new File(outputDir,"log-${id}.gz")
        infile.withInputStream { inpu ->
            def gzip=new BufferedReader(new InputStreamReader(new GZIPInputStream(inpu)))
            def writer=new BufferedWriter(new OutputStreamWriter(out))
            gzip.eachLine { line ->
                writer.write(line+'\n')
            }
            writer.flush()
        }
        true
    }
}