import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;

/**
 * This example is a minimal streaming log writer plugin for Rundeck
 */
rundeckPlugin(StreamingLogWriterPlugin){
    configuration{
        outputDir="/tmp"
        outputDir required:true, description: "Location of log files"
    }
    /**
     * The "open" closure is called to open the stream for writing events.
     * It is passed two map arguments, the execution data, and the plugin configuration data.
     *
     * It should return a Map containing the stream context, which will be passed back for later
     * calls to the "addEvent" closure.
     */
    open { Map execution, Map config ->
        //in this example we open a file output stream to store data in JSON format.
        def outputDir=new File(config.outputDir)
        if(!outputDir.isDirectory() && !outputDir.mkdirs()){
            throw new RuntimeException("Couldn't create outputdir: ${config.outputDir}")
        }
        def file=new File(config.outputDir,"plugin-log-${execution.execid}.json")
        
        def out=new BufferedOutputStream(new FileOutputStream(file))

        //write some prefix data
        out<<'{ "entries": [\n'
        out.flush()

        //return context map for the plugin to reuse later
        [file:file,out:out,json:new ObjectMapper(),count:0,execution:execution]
    }

    /**
     * "addEvent" closure is called to append a new event to the stream.  
     * It is passed the Map of stream context created in the "open" closure, and a LogEvent.
     * 
     */
    addEvent { Map context, LogEvent event->
        if(context.wasWritten){
            context.out <<','    
        }
        def data=[datetime:event.datetime.time,loglevel:event.loglevel.toString(),message:event.message,eventType:event.eventType]
        event.metadata?.each{k,v->
            data['meta_'+k]=event.metadata[k]
        }
        context.out<< context.json.writeValueAsString(data)
        context.out.flush()
        context.wasWritten=true
        context.count++
    }
    /**
     * "close" closure is called to end writing to the stream.
     *
     * In this example we don't declare any arguments, but an implicit 'context' variable is available with the stream
     * context data.
     */
    close { 

        //close JSON format, and include a total count of entries
        context.out<<']\n'
        // context.out<<', "total":'
        // context.out<<context.json.writeValueAsString(context.count)
        context.out<<'}\n'

        //close file output stream
        context.out.flush()
        context.out.close()

        //write index info about the json data
        def file=new File(configuration.outputDir,"plugin-log-${context.execution.execid}.json.index")
        file.withWriter{w->
            w<< context.json.writeValueAsString([total:context.count,execution:context.execution])
        }
    }
}