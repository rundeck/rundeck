import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin;
import java.util.zip.*

/**
 * This example is an log file storage plugin for Rundeck
 */
rundeckPlugin(LogFileStoragePlugin){
    configuration{
        filebase defaultValue:"log-", required:true
        outputDir='/tmp'
        outputDir required:true, description: "Location of log files"
    }
    /**
     * Called to determine the file availability, return true to indicate it is available, 
     * false to indicate it is not available. An exception indicates an error.
     */
    available { Map execution, Map configuration->
        def id = execution.execid
        //return state of storage given the id
        def outfile=new File(configuration.outputDir,"${configuration.filebase}${id}.gz")
        outfile.exists()
    }

    /**
     * Called to store a log file, called with the execution data, configuration properties, and an InputStream.
     * Additionally `length` and `lastModified` properties are in the closure binding, providing the file length,
     * and last modification Date.
     * Return true to indicate success.
     */
    store { Map execution, Map configuration, InputStream source->
        def id = execution.execid
        //use gzip
        def outfile=new File(configuration.outputDir,"${configuration.filebase}${id}.gz.tmp")
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
            outfile.renameTo(new File(configuration.outputDir,"${configuration.filebase}${id}.gz"))
        }
        source.close()
        true
    }

    /**
     * Called to retrieve a log file, called with the execution data, configuration properties, and an OutputStream.
     * Return true to indicate success.
     */
    retrieve {  Map execution, Map configuration, OutputStream out->
        def id = execution.execid

        def infile=new File(configuration.outputDir,"${configuration.filebase}${id}.gz")
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