package rundeck.controllers

import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.Resource
import rundeck.services.ResourceService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ResourceController {
    public static final String RES_META_RUNDECK_CONTENT_TYPE = 'Rundeck-content-type'
    public static final String RES_META_RUNDECK_CONTENT_SIZE = 'Rundeck-content-size'
    public static final String RES_META_RUNDECK_CONTENT_MASK = 'Rundeck-content-mask'
    public static final Map<String,String> RES_META_RUNDECK_OUTPUT = [
            (RES_META_RUNDECK_CONTENT_TYPE):"contentType",
            (RES_META_RUNDECK_CONTENT_SIZE):"contentLength",
    ]
    ResourceService resourceService

    private def jsonRenderResource(builder,Resource res){
        builder.with{
            path = res.path.toString()
            type = res.directory ? 'directory' : 'file'
            if (!res.directory) {
                if (res.contents.meta) {
                    def bd = delegate
                    def meta=[:]
                    RES_META_RUNDECK_OUTPUT.each{k,v->
                        if(res.contents.meta[k]){
                            meta[v]= res.contents.meta[k]
                        }
                    }
                    if(meta){
                        bd.meta=meta
                    }

                }
            }
        }
    }
    private def xmlRenderResource(builder,Resource res){
        builder.'resource'(path: res.path.toString(), type: res.directory ? 'directory' : 'file') {
            if (!res.directory) {
                def data = res.contents.meta
                delegate.'resource-meta' {
                    def bd = delegate
                    RES_META_RUNDECK_OUTPUT.each { k, v ->
                        if (res.contents.meta[k]) {
                            bd."${v}"(res.contents.meta[k])
                        }
                    }
                }
            }
        }
    }

    private def renderDirectory(HttpServletRequest request, HttpServletResponse response, Resource resource, Set<Resource> dirlist) {
        withFormat {
            json {
                render(contentType: 'application/json') {
                    delegate.'resources'=array{
                        def builder = delegate
                        dirlist.each{ diritem->
                            builder.element{
                                jsonRenderResource(delegate,diritem)
                            }
                        }
                    }
                }
            }
            xml {

                render {
                    delegate.'resource'(path: resource.path.toString(), type: resource.directory ? 'directory' : 'file') {
                        delegate.'contents'(count:dirlist.size()){
                            def builder=delegate
                            dirlist.each{diritem->
                                xmlRenderResource(builder,diritem)
                            }
                        }
                    }
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


    def apiPostResource() {
        String resourcePath = params.resourcePath
        def found = resourceService.hasResource(resourcePath)
        if (found) {
            response.status = 409
            return renderError("resource already exists: ${resourcePath}")
        }
        def map = [
                (RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (RES_META_RUNDECK_CONTENT_SIZE): request.contentLength,
        ]
        def resource = resourceService.putResource(resourcePath, map, request.inputStream)
        response.status=201
        renderResourceFile(request,response,resource)
    }


    def apiDeleteResource() {
        String resourcePath = params.resourcePath
        def deleted = resourceService.delResource(resourcePath)
        if(deleted){
            response.status=204
        }else{
            response.status=500
        }
    }
    def apiPutResource() {
        String resourcePath = params.resourcePath
        def found = resourceService.hasResource(resourcePath)
        if (!found) {
            response.status = 404
            return renderError("resource not found: ${resourcePath}")
        }
        def map = [(RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (RES_META_RUNDECK_CONTENT_SIZE): request.contentLength,]
        def resource = resourceService.putResource(resourcePath, map, request.inputStream)
        renderResourceFile(request,response,resource)
    }
    def apiGetResource() {
        String resourcePath = params.resourcePath
        def found = resourceService.hasResource(resourcePath)
        if(!found){
            response.status=404
            return renderError("resource not found: ${resourcePath}")
        }
        def resource = resourceService.getResource(resourcePath)
        if (resource.directory) {
            //list directory and render resources
            def dirlist = resourceService.listDir(resourcePath)
            return renderDirectory(request, response, resource,dirlist)
        } else {
            return renderResourceFile(request, response, resource)
        }
    }
}
