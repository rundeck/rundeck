package rundeck

import com.fasterxml.jackson.databind.ObjectMapper

class PluginMeta {

    static constraints = {
        jsonData(nullable: true, blank: true)
    }
    static mapping = {
        key column: 'data_key'
        jsonData(type: 'text')
    }
    Long id
    String key
    String project
    String jsonData
    Date dateCreated
    Date lastUpdated

    //ignore fake property 'configuration' and do not store it
    static transients = ['pluginData']

    public Map getPluginData() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    /**
     * store data under a key
     */
    public void storePluginData(String key, Object obj) {
        setPluginData(getPluginData().put(key, obj))
    }

    public void setPluginData(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }
}
