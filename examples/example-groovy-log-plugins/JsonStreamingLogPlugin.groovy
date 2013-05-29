import com.dtolabs.rundeck.plugins.logging.StreamingLogPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.*;

/**
 * This example is an example Json format streaming log plugin for Rundeck
 */
rundeckPlugin(StreamingLogPlugin){
    def outputDir="/tmp"
    def fileForExecution={ execution->
        new File(outputDir,"plugin-log-${execution.id}.json")
    }
    reader {
        open { Map execution, long offset ->
            //execution = [id: Long, user: String, jobName: String, dateStarted: Date]
            def file=fileForExecution(execution)
            
            //return map of context data for your plugin to reuse later,
            //or return a closure which will perform stream opening asynchronously
            return {
                def mapper=new ObjectMapper()
                JsonParser jp = mapper.getJsonFactory().createJsonParser(file)
                while (jp.nextToken() != JsonToken.START_ARRAY) { }
                def next=0
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
                [file:file,input:in,iterator:mapping,next:next,json:jp]
            }
        }
        next { context->
            if (context.iterator.hasNextValue()) {
               Map value = context.iterator.nextValue();
               context.next++
               return value
            }else{
                return null
            }
        }
        close{ context->
            //perform any close action
            context.json.close()
        }
    }
    writer {
        open { Map execution ->
            //execution = [id: Long, user: String, jobName: String, dateStarted: Date]
            def file=fileForExecution(execution)
            def out=new FileOutputStream(file)
            out<<'{ \'entries\': [\n'
            //return context data for your plugin to reuse later
            [file:file,out:out,json:new ObjectMapper(),count:0]
        }
        onEvent('log'){ Map context, Date time, LogLevel level, Map meta, String message->
            if(context.wasWritten){
                context.out <<','    
            }
            context.out<< context.json.writeValueAsString(meta+[time:time,level:level,message:message])
            context.wasWritten=true
            context.count++
        }
        close { context->
            //close file output stream
            out<<'],'
            out<<context.json.writeValueAsString([total:context.count])
            out<<'}\n'
            context.out.close()
        }
    }
    
}