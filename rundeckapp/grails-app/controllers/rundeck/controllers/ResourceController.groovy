package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import rundeck.filters.ApiRequestFilters
import rundeck.services.ApiService
import rundeck.services.FrameworkService
import rundeck.services.ResourceService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ResourceController {
    public static final String RES_META_RUNDECK_CONTENT_TYPE = 'Rundeck-content-type'
    public static final String RES_META_RUNDECK_CONTENT_SIZE = 'Rundeck-content-size'
    public static final String RES_META_RUNDECK_CONTENT_MASK = 'Rundeck-content-mask'
    public static final String RES_META_RUNDECK_SSHKEY_TYPE = 'Rundeck-ssh-key-type'
    public static final Map<String,String> RES_META_RUNDECK_OUTPUT = [
            (RES_META_RUNDECK_CONTENT_TYPE):"contentType",
            (RES_META_RUNDECK_CONTENT_SIZE):"contentLength",
            (RES_META_RUNDECK_CONTENT_MASK): RES_META_RUNDECK_CONTENT_MASK,
            (RES_META_RUNDECK_SSHKEY_TYPE): RES_META_RUNDECK_SSHKEY_TYPE
    ]
    ResourceService resourceService
    ApiService apiService
    FrameworkService frameworkService
    static allowedMethods = [
            sshKey: ['GET','POST','PUT','DELETE']
    ]

    private def pathUrl(path){
        def uriString = "/api/${ApiRequestFilters.API_CURRENT_VERSION}/incubator/storage/$path"
        if ("${path}".startsWith('ssh-key/') || path.toString() == 'ssh-key') {
            uriString = "/api/${ApiRequestFilters.API_CURRENT_VERSION}/storage/$path"
        }
        return createLink(absolute: true, uri: uriString)
    }
    private def jsonRenderResource(builder,Resource res, dirlist=[]){
        builder.with{
            path = res.path.toString()
            type = res.directory ? 'directory' : 'file'
            if(!res.directory){
                name = res.path.name
            }
            url = pathUrl(res.path)
            if (!res.directory) {
                if (res.contents.meta) {
                    def bd = delegate
                    def meta=[:]
                    RES_META_RUNDECK_OUTPUT.each{k,v->
                        if(res.contents.meta[k]){
                            meta[k]= res.contents.meta[k]
                        }
                    }
                    if(meta){
                        bd.meta=meta
                    }

                }
            }
            if(dirlist){
                delegate.'resources' = array {
                    def builder2 = delegate
                    dirlist.each { diritem ->
                        builder2.element {
                            jsonRenderResource(delegate, diritem,[])
                        }
                    }
                }
            }
        }
    }
    private def xmlRenderResource(builder,Resource res,dirlist=[]){
        def map=[path: res.path.toString(),
                type: res.directory ? 'directory' : 'file',
                url: pathUrl(res.path)]
        if(!res.directory){
            map.name= res.path.name
        }
        builder.'resource'(map) {
            if (!res.directory) {
                def data = res.contents.meta
                delegate.'resource-meta' {
                    def bd = delegate
                    RES_META_RUNDECK_OUTPUT.each { k, v ->
                        if (res.contents.meta[k]) {
                            bd."${k}"(res.contents.meta[k])
                        }
                    }
                }
            }else if (dirlist){
                delegate.'contents'(count: dirlist.size()) {
                    def builder2 = delegate
                    dirlist.each { diritem ->
                        xmlRenderResource(builder2, diritem,[])
                    }
                }
            }
        }
    }

    private def renderDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource,
                                Set<Resource<ResourceMeta>> dirlist) {
        withFormat {
            json {
                render(contentType: 'application/json') {
                    jsonRenderResource(delegate, resource,dirlist)
                }
            }
            xml {
                render {
                    xmlRenderResource(delegate, resource, dirlist)
                }
            }
        }
    }
    private def renderResourceFile(HttpServletRequest request, HttpServletResponse response, Resource resource) {
        def resContentType=resource.contents?.meta?.getAt(RES_META_RUNDECK_CONTENT_TYPE)
        def cmask=resource.contents?.meta?.getAt(RES_META_RUNDECK_CONTENT_MASK)?.split(',') as Set
        //
        def maskContent=cmask?.contains('content')

        def askedForContent= resContentType && request.getHeader('Accept').contains(resContentType)
        def anyContent= response.format == 'all'

        if (askedForContent && maskContent) {
            //content is masked, issue 403
            response.status = 403
            return renderError("unauthorized")
        }
        if(!resource.directory && (askedForContent || anyContent) && !maskContent) {
            response.contentType=resContentType
//            response.outputStream<<resource.contents?.meta?.toString()
            Streams.copy(resource.contents.readContent(),response.outputStream,true)
            return
        }

        //render API resource file data
        switch (response.format){
            case 'xml':
                render(contentType: 'application/xml') {
                    xmlRenderResource(delegate, resource)
                }
                break;
            case 'json':
                ///fallthrough json response by default
            default:
                render(contentType: 'application/json') {
                    jsonRenderResource(delegate, resource)
                }
        }
    }

    private Object renderError(String message) {
        def jsonResponseclosure= {
            render(contentType: "application/json") {
                delegate.'error' = message
            }
        }
        if(!(response.format in ['json','xml'])){
            return jsonResponseclosure.call()
        }
        withFormat {
            json(jsonResponseclosure)
            xml {
                render(contentType: "application/xml") {
                    delegate.'error'(message)
                }
            }
        }
    }

    /**
     * Handle resource requests to the /ssh-key path
     * @return
     */
    def apiSshKey() {
        params.resourcePath = "/ssh-key/${params.resourcePath}"
        switch (request.method) {
            case 'POST':
                apiPostResource()
                break
            case 'PUT':
                apiPutResource()
                break
            case 'GET':
                apiGetResource()
                break
            case 'DELETE':
                apiDeleteResource()
                break
        }
    }

    def apiPostResource() {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        String resourcePath = params.resourcePath
        if (resourceService.hasResource(authContext, resourcePath)) {
            response.status = 409
            return renderError("resource already exists: ${resourcePath}")
        }else if(resourceService.hasPath(authContext, resourcePath)){
            response.status = 409
            return renderError("directory already exists: ${resourcePath}")
        }
        Map<String,String> map = [
                (RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (RES_META_RUNDECK_CONTENT_SIZE): Integer.toString(request.contentLength),
        ] + (request.resourcePostMeta?:[:])
        try{
            def resource = resourceService.putResource(authContext,resourcePath, map, request.inputStream)
            response.status=201
            renderResourceFile(request,response,resource)
        } catch (StorageException e) {
            log.error("Error creating resource ${resourcePath}: ${e.message}")
            log.debug("Error creating resource ${resourcePath}", e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }
    }


    def apiDeleteResource() {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        String resourcePath = params.resourcePath
        if(!resourceService.hasResource(authContext, resourcePath)) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args: ['Resource', resourcePath]
            ])
        }
        try{
            def deleted = resourceService.delResource(authContext, resourcePath)
            if(deleted){
                response.status=204
            }else{
                response.status=500
            }
        } catch (StorageException e) {
            log.error("Error deleting resource ${resourcePath}: ${e.message}")
            log.debug("Error deleting resource ${resourcePath}", e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }
    }
    def apiPutResource() {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        String resourcePath = params.resourcePath
        def found = resourceService.hasResource(authContext, resourcePath)
        if (!found) {
            response.status = 404
            return renderError("resource not found: ${resourcePath}")
        }
        Map<String, String> map = [
                (RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (RES_META_RUNDECK_CONTENT_SIZE): Integer.toString(request.contentLength),
        ] + (request.resourcePostMeta ?: [:])
        try {
            def resource = resourceService.putResource(authContext,resourcePath, map, request.inputStream)
            return renderResourceFile(request,response,resource)
        } catch (StorageException e) {
            log.error("Error putting resource ${resourcePath}: ${e.message}")
            log.debug("Error putting resource ${resourcePath}", e)
            apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message:e.message
            ])
        }
    }
    def apiGetResource() {
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        String resourcePath = params.resourcePath
        def found = resourceService.hasPath(authContext, resourcePath)
        if(!found){
            response.status=404
            return renderError("resource not found: ${resourcePath}")
        }
        try{
            def resource = resourceService.getResource(authContext, resourcePath)
            if (resource.directory) {
                //list directory and render resources
                def dirlist = resourceService.listDir(authContext, resourcePath)
                return renderDirectory(request, response, resource,dirlist)
            } else {
                return renderResourceFile(request, response, resource)
            }
        } catch (StorageException e) {
            log.error("Error reading resource ${resourcePath}: ${e.message}")
            log.debug("Error reading resource ${resourcePath}",e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }
    }
}
