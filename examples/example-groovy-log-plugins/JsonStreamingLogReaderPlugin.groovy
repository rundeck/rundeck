import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;

/**
 * This example is an example Json format streaming log reader plugin for Rundeck
 */
rundeckPlugin(StreamingLogReaderPlugin){
    //define some utility and constant data
    configuration{
        outputDir="/tmp"
        outputDir required:true, description: "Location of log files"
    }
    //the file for storing the log data given execution data
    def fileForExecution={ execution,config->
        new File(config.outputDir,"plugin-log-${execution.execid}.json")
    }
    //the file for storing the log data given execution data
    def indexfileForExecution={ execution,config->
        new File(config.outputDir,"plugin-log-${execution.execid}.json.index")
    }
    /**
     * The 'info' closure is called to retrieve some metadata about the stream, 
     * such as whether it is available to read, totalSize of the content, and last
     *  modification time
     * 
     * It should return a Map containing these two entries:
     *  `ready` : a boolean indicating whether 'open' will work
     * `lastModified`: Long (unix epoch) or Date indicating last modification of the log
     * `totalSize`: Long indicating total size of the log, it doesn't have to indicate bytes,
     *     merely a measurement of total data size
     */
    info {Map execution, Map configuration->
        def file=fileForExecution(execution,configuration)
        def idx=indexfileForExecution(execution,configuration)
        def data=[:]
        if(idx.exists()){
            def mapper=new ObjectMapper()
            data=mapper.readValue(idx.text,Map.class)
        }

        //return map containing metadata about the stream
        // it SHOULD contain these two elements:
        [
            lastModified: file.lastModified(),
            totalSize: data?.total?:file.length(),
            ready: file.exists()
        ]
    }
    /**
     * The `open` closure is called to begin reading events from the stream.
     * It is passed the execution data, the plugin configuration, and an offset.
     * It should return a Map containing any context to store between calls.
     */
    open { Map execution, Map configuration, long offset ->
        //execution = [id: Long, user: String, jobName: String, dateStarted: Date]
        def file=fileForExecution(execution,configuration)
        
        def mapper=new ObjectMapper()
        JsonParser jp = mapper.getJsonFactory().createJsonParser(file)
        while (jp.nextToken() != JsonToken.START_ARRAY) { }
        long next=0
        if(offset>0){
            while(next<offset ){
                //mapping.nextValue();
                def tok=jp.nextToken()
                while (tok != JsonToken.END_OBJECT && tok!=JsonToken.END_ARRAY) {
                    tok=jp.nextToken()
                }
                if(tok==JsonToken.END_ARRAY){
                    break
                }
                next++
            }
        }
        MappingIterator<Map> mapping = mapper.readValues(jp, Map.class);
        //return map of context data for your plugin to reuse later,
        [
            file:file,iterator:mapping,next:next,json:jp,
        ]
    }
    /**
     * Next is called to produce the next event, it should return a Map
     * containing: [event: (event data), offset: (next offset), complete: (true/false)].  
     * The event data can be a LogEvent, or a Map containing:
     * [
     * message: (String),
     * loglevel: (String or LogLevel),
     * datetime: (long or Date),
     * eventType: (String),
     * metadata: (Map),
     * ]
     * `complete` should be true if no more events will ever be available.
     */
    next { context->
        try{
            Map value=null
            while(!value && context.iterator.hasNextValue()){
                value = context.iterator.nextValue()
            }
            if(!context.iterator.hasNextValue()){
                complete()
            }
            if(value){
                context.next++
                def metakeys=value.keySet().findAll{it==~/^meta_.*$/}
                def meta=[:]
                metakeys.each{k->
                    def key=k.replaceAll(/^meta_/,'')
                    meta[key]=value.remove(k)
                }
                value.meta=meta
                //return a map containing event: (Map) and offset: (Long)
                return [event:value,offset: context.next]
            }
        }catch (JsonParseException e){
            //indicates json format was incomplete.  return a null event 
            
        }catch (JsonMappingException e){
            //indicates json format was incomplete.  return a null event 
            
        }
        return [event:null,offset:context.next]
    }
    /**
     * Close is called to finish the read stream
     */
    close{ context->
        //perform any close action
        context.json.close()
    }
    
}