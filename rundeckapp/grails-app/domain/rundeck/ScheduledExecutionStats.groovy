package rundeck

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.execution.RdJobStats

class ScheduledExecutionStats implements RdJobStats {
    String content
    String jobUuid

    long _version = 0

//    static belongsTo=[se:ScheduledExecution]
    static transients = ['contentMap']

    static mapping = {
        version false
        _version column: 'version'
        content type: 'text'
    }

    public Map getContentMap() {
        if (null != content) {
            final ObjectMapper objMapper = new ObjectMapper()
            try{
                return objMapper.readValue(content, Map.class)
            }catch (JsonParseException e){
                return null
            }
        } else {
            return null
        }

    }


    public void setContentMap(Map obj) {
        if (null != obj) {
            final ObjectMapper objMapper = new ObjectMapper()
            content = objMapper.writeValueAsString(obj)
        } else {
            content = null
        }
    }

    Long getVersion(){
        _version
    }

}
