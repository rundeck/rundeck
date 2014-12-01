package rundeck

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.storage.api.PathUtil


class Storage {
    String namespace
    String dir
    String name
    /**
     * json encoded metadata
     */
    String jsonData
    /**
     * Unique sha1 of namespace+dir+name to prevent duplicate
     */
    String pathSha
    byte[] data
    Date dateCreated
    Date lastUpdated
    static constraints = {
        namespace(nullable: true, blank: true, size: 0..255)
        jsonData(nullable: true, blank: true)
        data(nullable: true, maxSize: 52_428_800) /* 50MB */
        name(nullable: false, blank: false, maxSize: 1024)
        dir(nullable: true, blank: true, maxSize: 2048)
        pathSha(nullable: false, blank: false, size: 40..40, unique: true)
    }

    private void setupSha() {
        dir = dir ?: ''
        pathSha = ((namespace ?: '') + ':' + getPath()).encodeAsSHA1()
    }
    def beforeInsert() {
        setupSha()
    }
    def beforeUpdate() {
        setupSha()
    }
    def beforeValidate() {
        setupSha()
    }
    static mapping= {
        data(type: 'binary')
        dir(type: 'string')
        jsonData(type: 'text')
        name(type: 'string')
    }
    //ignore fake property 'storageMeta' and 'path' and do not store it
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
