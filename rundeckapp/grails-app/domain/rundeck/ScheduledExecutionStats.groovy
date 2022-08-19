package rundeck

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper

class ScheduledExecutionStats {
    String content

    long _version = 0

    static belongsTo=[se:ScheduledExecution]
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
            Map<String, Object> newContentMap
            if (content != null) {
                newContentMap = objMapper.readValue(content, Map.class)
                obj.each { statKey, statVal ->
                    newContentMap.put(statKey, statVal)
                }
                content = objMapper.writeValueAsString(newContentMap)
            } else {
                content = objMapper.writeValueAsString(obj)
            }
        } else {
            content = null
        }
    }

}
