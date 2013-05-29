import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;
import java.util.zip.*

/**
 * This example is an log file storage plugin for Rundeck
 */
rundeckPlugin(LogFileStoragePlugin){
    def outputDir="/tmp"
    state { String id->
        //return state of storage given the id
        def tmpfile=new File(outputDir,"log-${id}.gz.tmp")
        def outfile=new File(outputDir,"log-${id}.gz")
        outfile.exists()? AVAILABLE : tmpfile.exists()? PENDING : NOT_FOUND
    }
    put { String id, InputStream source->
        //use gzip
        def outfile=new File(outputDir,"log-${id}.gz.tmp")
        source.withReader { reader ->
            outfile.withOutputStream { out ->
                def gzip=new OutputStreamWriter(new GZIPOutputStream(out))

                def line=reader.readLine()
                while(line!=null){
                    gzip.write(line)
                    line=reader.readLine()
                }
                gzip.flush()       
            }
            outfile.renameTo(outputDir,"log-${id}.gz")
        }
        source.close()
    }
    get {  String id, OutputStream out->

        def infile=new File(outputDir,"log-${id}.gz")
        infile.withInputStream { in ->
            def gzip=new BufferedReader(new GZIPInputStream(in))
            def writer=new BufferedWriter(out)
            gzip.eachLine { line ->
                writer.write(line)
            }
            writer.flush()
        }
    }
}