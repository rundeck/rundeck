package rundeck

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.storage.api.PathUtil


class Storage {
    String dir
    String name
    String jsonData
    byte[] data
    Date dateCreated
    Date lastUpdated
    static constraints = {
        jsonData(nullable: true, blank: true)
        data(nullable: true)
        name(nullable: false, blank: false, unique: 'dir')
        dir(nullable: false, blank: true)
    }
    static mapping= {
        data(type: 'binary', sqlType: "longblob")
        name(type: 'text')
        dir(type: 'text')
        jsonData(type: 'text')
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['storageMeta','path']

    public String getPath() {
        return (dir?(dir+'/'):'')+name
    }
    public void setPath(String path){
        def path1 = PathUtil.asPath(path)
        def parent = PathUtil.parentPath(path1)
        dir= parent?parent.path:''
        name=path1.name
    }

    public Map getStorageMeta() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    public void setStorageMeta(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }

}
