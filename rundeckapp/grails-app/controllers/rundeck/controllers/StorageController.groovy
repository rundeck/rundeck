/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.controllers

import com.dtolabs.rundeck.app.support.StorageParams
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.storage.AuthStorageUsernameMeta
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageAuthorizationException
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.KeyStorageLayer
import grails.converters.JSON
import groovy.transform.CompileStatic
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.springframework.web.multipart.MultipartHttpServletRequest
import com.dtolabs.rundeck.app.api.ApiVersions
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService
import rundeck.services.StorageService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
class StorageController extends ControllerBase{
    public static final String RES_META_RUNDECK_CONTENT_MASK = 'Rundeck-content-mask'
    public static final List<String> RES_META_RUNDECK_OUTPUT = [
            StorageUtil.RES_META_RUNDECK_CONTENT_TYPE,
            StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH,
            StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME,
            StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME,
            AuthStorageUsernameMeta.RUNDECK_AUTH_CREATED_USERNAME,
            AuthStorageUsernameMeta.RUNDECK_AUTH_MODIFIED_USERNAME,
            KeyStorageLayer.RUNDECK_KEY_TYPE,
            KeyStorageLayer.RUNDECK_DATA_TYPE,
            RES_META_RUNDECK_CONTENT_MASK,
    ]
    /**
     * Metadata fields not included in masked resource output
     */
    public static final Set<String> RES_META_MASKED = [StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH]
    StorageService storageService
    FrameworkService frameworkService
    AuthContextProvider rundeckAuthContextProvider
    ConfigurationService configurationService

    static allowedMethods = [
            apiKeys: ['GET','POST','PUT','DELETE'],
            keyStorageAccess:['GET'],
            keyStorageDownload:['GET'],
            keyStorageUpload:['POST'],
            keyStorageDelete:['POST']
    ]

    private def pathUrl(path){
        def uriString = "/api/${ApiVersions.API_CURRENT_VERSION}/incubator/storage/$path"
        if ("${path}".startsWith('keys/') || path.toString() == 'keys') {
            uriString = "/api/${ApiVersions.API_CURRENT_VERSION}/storage/$path"
        }
        return createLink(absolute: true, uri: uriString)
    }

    private Map getMeta(Resource res){
        def meta_ = [:]
        if (!res.directory) {
            def meta1 = res.contents.meta
            if (meta1) {
                def masked = resourceContentsMasked(res)
                RES_META_RUNDECK_OUTPUT.each { k ->
                    if ((!masked || !(k in StorageController.RES_META_MASKED)) && meta1[k]) {
                        meta_[k] = meta1[k]
                    }
                }
            }
        }

        return meta_
    }

    private def jsonRenderResource(Resource res, dirlist=[]){
        def json = [
            path: res.path.toString(),
            type: (res.directory ? 'directory' : 'file'),
            name: !res.directory ? res.path.name : null,
            url: pathUrl(res.path),
            meta: this.getMeta(res) ]

        if(dirlist){
            json.resources = dirlist.collect { diritem ->

                [
                        path: diritem.path.toString(),
                        type: (diritem.directory ? 'directory' : 'file'),
                        name: !diritem.directory ? diritem.path.name : null,
                        url: pathUrl(diritem.path),
                        meta: getMeta(diritem)
                ]
            }
        }
        return json
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
                def masked=resourceContentsMasked(res)
                def data = res.contents.meta
                delegate.'resource-meta' {
                    def bd = delegate
                    RES_META_RUNDECK_OUTPUT.each { k ->
                        if ((!masked || !(k in StorageController.RES_META_MASKED)) && res.contents.meta[k]) {
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
            def jsonClos = {
                render jsonRenderResource(resource,dirlist) as JSON
            }
            json  jsonClos
            if(isAllowXml()) {
                xml {
                    render(contentType: 'application/xml') {
                        xmlRenderResource(delegate, resource, dirlist)
                    }
                }
            }
            '*' jsonClos
        }
    }
    private boolean resourceContentsMasked(Resource resource){
        def meta = resource.contents?.meta
        def cmask = meta?.getAt(RES_META_RUNDECK_CONTENT_MASK)?.split(',') as Set
        return cmask?.contains('content')
    }
    private def renderResourceFile(HttpServletRequest request, HttpServletResponse response, Resource resource, boolean forceDownload=false) {
        def contents = resource.contents
        def meta = contents?.meta
        def resContentType= meta?.getAt(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE)

        def maskContent= resourceContentsMasked(resource)

        def askedForContent= forceDownload || resContentType && request.getHeader('Accept')?.contains(resContentType)
        def anyContent= response.format == 'all'

        if (askedForContent && maskContent) {
            //content is masked, issue 403
            response.status = 403
            return renderError("unauthorized")
        }
        if((askedForContent || anyContent) && !maskContent) {
            response.contentType=resContentType
            if(forceDownload){
                def filename= resource.path.name
                response.setHeader("Content-Disposition", "attachment; filename=\"${filename}\"")
            }

            def baos = new ByteArrayOutputStream()
            try{
                def len=contents.writeContent(baos)
                writeOutputStream(baos)
            }catch (IOException e){
                //problem reading storage contents
                log.error("Failed reading storage content: "+e.message,e)
                response.status=HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                appendOutput(response, "Failed reading storage content: "+e.message)
            }

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
                render jsonRenderResource(resource) as JSON
        }
    }

    @CompileStatic
    private void writeOutputStream(ByteArrayOutputStream out) {
        out.writeTo(response.outputStream)
        response.outputStream.flush()
    }

    private Object renderError(String message) {
        def jsonResponseclosure= {
            render(contentType: "application/json") {
                delegate.error message
            }
        }
        if(!(response.format in ['json','xml'])){
            return jsonResponseclosure.call()
        }
        withFormat {
            json(jsonResponseclosure)
            if(isAllowXml()) {
                xml {
                    render(contentType: "application/xml") {
                        delegate.'error'(message)
                    }
                }
            }
            '*' jsonResponseclosure
        }
    }

    /**
     * non-api action wrapper for apiKeys method
     */
    public def keyStorageAccess(StorageParams storageParams){
        if (!storageParams.resourcePath ) {
            storageParams.resourcePath = "/keys${storageParams.relativePath ? ('/' + storageParams.relativePath) : ''}"
        }
        getResource(storageParams)
    }
    /**
     * non-api action wrapper for apiKeys method
     */
    public def keyStorageDownload(StorageParams storageParams){

        Boolean downloadenabled = configurationService.getBoolean("gui.keystorage.downloadenabled", true)
        if(!downloadenabled){
            response.status=403
            return renderError("download is not enabled")
        }
        if (!storageParams.resourcePath ) {
            storageParams.resourcePath = "/keys${storageParams.relativePath ? ('/' + storageParams.relativePath) : ''}"
        }
        getResource(storageParams,true)
    }
    /**
     * non-api action wrapper for apiKeys method
     *
     */
    def keyStorageUpload(StorageParams storageParams){
        if(!requestHasValidToken()) return

        if (storageParams.hasErrors()) {
            flash.errors=storageParams.errors
            return redirect(controller: 'menu',action: 'storage',params: [project:params.project])
        }
        if (!storageParams.resourcePath ) {
            storageParams.resourcePath = "/keys${storageParams.relativePath ? ('/'+storageParams.relativePath): ''}"
        }
        AuthContext authContext = getAuthContextForPath(session.subject, storageParams.resourcePath)
        def resourcePath = storageParams.resourcePath

        def contentType= null

        if (storageParams.uploadKeyType == 'public') {
            contentType = KeyStorageLayer.PUBLIC_KEY_MIME_TYPE
        } else if (storageParams.uploadKeyType == 'private') {
            contentType = KeyStorageLayer.PRIVATE_KEY_MIME_TYPE
        } else if (storageParams.uploadKeyType == 'password') {
            contentType = KeyStorageLayer.PASSWORD_MIME_TYPE
        } else {
            //invalid
            flash.errorCode = 'api.error.parameter.invalid'
            flash.errorArgs = [storageParams.uploadKeyType, 'uploadKeyType']
            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        }
        if( !(storageParams.inputType in ['file', 'text'])){
            flash.errorCode = 'api.error.parameter.not.inList'
            flash.errorArgs = [storageParams.inputType, 'inputType']
            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        }

        def filename = storageParams.fileName

        if (!filename) {
            flash.errorCode = 'api.error.upload.missing'
            flash.errorArgs = ['fileName']
            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        }

        def uploadText = params.uploadText
        if(storageParams.uploadKeyType in ['public', 'private']){
            if(!uploadText) {
                flash.errorCode = 'api.error.parameter.required'
                flash.errorArgs = ['uploadText']

                return redirect(controller: 'menu', action: 'storage', params: [project: params.project])
            }
        } else if(storageParams.uploadKeyType == 'password'){  // Password input type is always text
            //store a password
            if (!params.uploadPassword) {
                //invalid
                flash.errorCode = 'api.error.parameter.required'
                flash.errorArgs = ['uploadPassword']

                return redirect(controller: 'menu', action: 'storage', params: [project: params.project])
            }
            uploadText = params.uploadPassword
        }

        resourcePath = PathUtil.cleanPath(resourcePath + '/' + filename)

        def newparams = new StorageParams(resourcePath: resourcePath)
        newparams.validate()

        if(newparams.hasErrors()){
            flash.errors=newparams.errors
            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        }

        boolean dontOverwrite = params.dontOverwrite in [true, 'true']

        def hasResource = storageService.hasResource(authContext, resourcePath)

        if (dontOverwrite && hasResource) {
            flash.error = g.message(code: 'api.error.item.alreadyexists',
                    args: ['Storage file', resourcePath])

            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        } else if (!hasResource && storageService.hasPath(authContext, resourcePath)) {
            flash.error = g.message(code: 'api.error.item.alreadyexists',
                    args: ['Storage directory path', resourcePath])

            return redirect(controller: 'menu', action: 'storage',
                    params: [project: params.project])
        }

        def inputBytes = uploadText.bytes

        Map<String, String> map = [
                (StorageUtil.RES_META_RUNDECK_CONTENT_TYPE): contentType,
                (StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH): inputBytes.length.toString() //if the value of content length is not cast to a string here,
                // Groovy allows the value into the map as an int or long
                // which will cause a type cast exception later if the contentLength
                // is accessed later in the storage chain
        ]
        try {
            def inputStream = new ByteArrayInputStream(inputBytes)

            if(hasResource){
                storageService.updateResource(authContext, resourcePath, map, inputStream)
            }else{
                storageService.createResource(authContext, resourcePath, map, inputStream)
            }
            return redirect(controller: 'menu', action: 'storage', params: [resourcePath:resourcePath]+(params.project?[project:params.project]:[:]))
        } catch (StorageAuthorizationException e) {
            log.error("Unauthorized: resource ${resourcePath}: ${e.message}")
            flash.errorCode = 'api.error.item.unauthorized'
            flash.errorArgs = [e.event.toString(), 'Path', e.path.toString()]
            return redirect(controller: 'menu', action: 'storage',params: [resourcePath:resourcePath]+(params.project?[project:params.project]:[:]))
        } catch (StorageException e) {
            log.error("Error creating resource ${resourcePath}: ${e.message}")
            log.debug("Error creating resource ${resourcePath}", e)
            flash.error= e.message
            return redirect(controller: 'menu', action: 'storage',params: [resourcePath:resourcePath]+(params.project?[project:params.project]:[:]))
        }
    }
    /**
     * non-api action wrapper for deleteResource method
     */
    public def keyStorageDelete(StorageParams storageParams) {
        if (!storageParams.resourcePath ) {
            storageParams.resourcePath = "/keys${storageParams.relativePath ? ('/' + storageParams.relativePath) : ''}"
        }
        def valid = false
        withForm {
            valid = true
            g.refreshFormTokensHeader()
        }.invalidToken {
            def message = g.message(code: 'request.error.invalidtoken.message')
            log.error(message)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'request.error.invalidtoken.message'
            ])
        }
        if (!valid) {
            return
        }
        deleteResource(storageParams)
    }
    /**
     * Handle resource requests to the /ssh-key path
     * @return
     */
    def apiKeys(StorageParams storageParams) {
        if(!apiService.requireApi(request,response)){
            return
        }
        storageParams.resourcePath = "/keys/${storageParams.resourcePath?:''}"
        storageParams.validate()
        switch (request.method) {
            case 'POST':
                apiPostResource(storageParams)
                break
            case 'PUT':
                apiPutResource(storageParams)
                break
            case 'GET':
                apiGetResource(storageParams)
                break
            case 'DELETE':
                apiDeleteResource(storageParams)
                break
        }
    }

    @Post(uri='/storage/keys/{path}')
    @Operation(
        method = "POST",
        summary = "Create Keys",
        description = '''
Specify the type of key via the `Content-type` header:

* `application/octet-stream` specifies a **private key**
* `application/pgp-keys` specifies a **public key**
* `application/x-rundeck-data-password` specifies a **password**

Authorization required: `create` for the `key` resource.
 
Authorization under the key path `project/{project}` can be granted at the project context.
''',
        tags = ['key storage'],
        parameters = @Parameter(
            name = 'path',
            in = ParameterIn.PATH,
            description = "Path and Key file name. Can be a directory path such as `subdir/` or include a filename `subdir/file.password`",
            schema=@Schema(type='string'),
            allowReserved = true
        ),
        requestBody = @RequestBody(
            description='Private key, public key, or password content',
            required=true,
            content=[
                @Content(
                    mediaType='application/octet-stream',
                    schema=@Schema(type='string'),
                    examples=@ExampleObject('''...private key...''')
                ),
                @Content(
                    mediaType='application/pgp-keys',
                    schema=@Schema(type='string'),
                    examples=@ExampleObject('''...public key...''')
                ),
                @Content(
                    mediaType='application/x-rundeck-data-password',
                    schema=@Schema(type='string'),
                    examples=@ExampleObject('''password-value''')
                )
            ]
        ),
        responses = [
            @ApiResponse(
                responseCode = "201",
                description = "Created",
                content = [
                    @Content(
                        mediaType = MediaType.APPLICATION_JSON,
                        schema = @Schema(type = 'object'),
                        examples = [
                            @ExampleObject(value='''
{
  "meta": {
    "Rundeck-key-type": "public",
    "Rundeck-content-size": "393",
    "Rundeck-content-type": "application/pgp-keys"
  },
  "url": "http://rundeckhost/api/11/storage/keys/test1.pub",
  "name": "test1.pub",
  "type": "file",
  "path": "keys/test1.pub"
}''',name='key-metadata',summary='Key Metadata Result'),
                            ]
                    ),
                    @Content(
                        mediaType = 'application/pgp-keys',
                        schema=@Schema(type='string'),
                        examples = @ExampleObject(
                            name='public-key',
                            summary='Public Key contents',
                            value='''...Public Key Contents...'''
                        )
                    )
                ]
            ),
            @ApiResponse(responseCode='403',description='Unauthorized'),
            @ApiResponse(responseCode='409',description='Conflict: the specified file or path already exists')
        ]
    )
    def apiPostResource(@Parameter(hidden = true) StorageParams storageParams) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        return postResource(storageParams)
    }

    private def postResource(StorageParams storageParams) {
        AuthContext authContext = getAuthContextForPath(session.subject, storageParams.resourcePath)
        String resourcePath = storageParams.resourcePath
        storageParams.requireRoot('/keys/')
        if (storageParams.hasErrors()) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.invalid.request',
                    args  : [storageParams.errors.allErrors.collect { g.message(error: it) }.join(",")]
            ]
            )
        }
        //require path is longer than "/keys/"
        if (storageService.hasResource(authContext, resourcePath)) {
            response.status = 409
            return renderError("resource already exists: ${resourcePath}")
        } else if (storageService.hasPath(authContext, resourcePath)) {
            response.status = 409
            return renderError("directory already exists: ${resourcePath}")
        }
        Map<String,String> map = [
                (StorageUtil.RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH): Integer.toString(request.contentLength),
        ] + (request.resourcePostMeta?:[:])
        try{
            def resource = storageService.createResource(authContext,resourcePath, map, request.inputStream)
            response.status=201
            renderResourceFile(request,response,resource)
        } catch (StorageAuthorizationException e) {
            log.error("Unauthorized: resource ${resourcePath}: ${e.message}")
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [e.event.toString(), 'Path', e.path.toString()]
            ])
        } catch (StorageException e) {
            log.error("Error creating resource ${resourcePath}: ${e.message}")
            log.debug("Error creating resource ${resourcePath}", e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }
    }


    @Delete(uri = '/storage/keys/{path}')
    @Operation(
        method = "DELETE",
        summary = "Delete A Key",
        description = '''Deletes the file if it exists and returns `204` response.

Authorization required: `delete` for the `key` resource.

Authorization under the key path `project/{project}` can be granted at the project context.
''',
        tags = ['key storage'],
        parameters = @Parameter(
            name = 'path',
            in = ParameterIn.PATH,
            description = "Path and Key file name. Can be a directory path such as `subdir/` or include a filename `subdir/file.password`",
            schema=@Schema(type='string'),
            allowReserved = true,
            required = true
        ),
        responses = [
            @ApiResponse(responseCode='204',description='Deleted'),
            @ApiResponse(responseCode='403',description='Unauthorized'),
            @ApiResponse(responseCode='404',description='The file does not exist')
        ]
    )
    def apiDeleteResource(@Parameter(hidden = true) StorageParams storageParams) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        return deleteResource(storageParams)
    }

    private def deleteResource(StorageParams storageParams) {
        AuthContext authContext = getAuthContextForPath(session.subject, storageParams.resourcePath)
        String resourcePath = storageParams.resourcePath
        storageParams.requireRoot('/keys/')
        if (storageParams.hasErrors()) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.invalid.request',
                    args  : [storageParams.errors.allErrors.collect { g.message(error: it) }.join(",")]
            ]
            )
        }
        if(!storageService.hasResource(authContext, resourcePath)) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist',
                    args: ['Resource', resourcePath]
            ])
        }
        try{
            def deleted = storageService.delResource(authContext, resourcePath)
            if(deleted){
                render(status: HttpServletResponse.SC_NO_CONTENT)
            }else{
                apiService.renderErrorFormat(response, [
                        status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        message: "Resource was not deleted: ${resourcePath}"
                ])
            }
        } catch (StorageAuthorizationException e) {
            log.error("Unauthorized: resource ${resourcePath}: ${e.message}")
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [e.event.toString(), 'Path', e.path.toString()]
            ])
        } catch (StorageException e) {
            log.error("Error deleting resource ${resourcePath}: ${e.message}")
            log.debug("Error deleting resource ${resourcePath}", e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }
    }

    @Put(uri='/storage/keys/{path}')
    @Operation(
        method = "PUT",
        summary = "Modify A Key",
        description = '''
Specify the type of key via the `Content-type` header:

* `application/octet-stream` specifies a **private key**
* `application/pgp-keys` specifies a **public key**
* `application/x-rundeck-data-password` specifies a **password**

Authorization required: `update` for the `key` resource.

Authorization under the key path `project/{project}` can be granted at the project context.
''',
        tags = ['key storage'],
        parameters = @Parameter(
            name = 'path',
            in = ParameterIn.PATH,
            description = "Path and Key file name. Can be a directory path such as `subdir/` or include a filename `subdir/file.password`",
            schema=@Schema(type='string'),
            allowReserved = true,
            required = true
        ),
        requestBody = @RequestBody(ref = '#/paths/~1storage~1keys~1%7Bpath%7D/post/requestBody'),
        responses = [
            @ApiResponse(ref = '#/paths/~1storage~1keys~1%7Bpath%7D/get/responses/200'),
            @ApiResponse(responseCode='403',description='Unauthorized'),
            @ApiResponse(responseCode='404',description='The file does not exist')
        ]
    )
    def apiPutResource(@Parameter(hidden = true) StorageParams storageParams) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        return putResource(storageParams)
    }

    private def putResource(StorageParams storageParams) {
        AuthContext authContext = getAuthContextForPath(session.subject, storageParams.resourcePath)
        String resourcePath = storageParams.resourcePath
        storageParams.requireRoot('/keys/')
        if (storageParams.hasErrors()) {
            return apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : 'api.error.invalid.request',
                    args  : [storageParams.errors.allErrors.collect { g.message(error: it) }.join(",")]
            ]
            )
        }
        def found = storageService.hasResource(authContext, resourcePath)
        if (!found) {
            response.status = 404
            return renderError("resource not found: ${resourcePath}")
        }
        Map<String, String> map = [
                (StorageUtil.RES_META_RUNDECK_CONTENT_TYPE): request.contentType,
                (StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH): Integer.toString(request.contentLength),
        ] + (request.resourcePostMeta ?: [:])
        try {
            def resource = storageService.updateResource(authContext,resourcePath, map, request.inputStream)
            return renderResourceFile(request,response,resource)
        } catch (StorageAuthorizationException e) {
            log.error("Unauthorized: resource ${resourcePath}: ${e.message}")
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [e.event.toString(), 'Path', e.path.toString()]
            ])
        } catch (StorageException e) {
            log.error("Error putting resource ${resourcePath}: ${e.message}")
            log.debug("Error putting resource ${resourcePath}", e)
            apiService.renderErrorFormat(response,[
                    status:HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message:e.message
            ])
        }
    }

    @Get(uri = '/storage/keys/{path}')
    @Operation(
        method = "GET",
        summary = "List and Get Keys and Key Metadata",
        description = '''
Lists resources at the specified PATH if it is a directory.

Otherwise if it is a file, return the metadata about the stored file if JSON response is requested.

Provides the content for **public key** files if the `Accept` request header matches `*/*` or `application/pgp-keys`.

Returns `403` if content is requested from other Key file types.

Authorization required: `read` for the `key` resource

Authorization under the key path `project/{project}` can be granted at the project context.
''',
        tags = ['key storage'],
        parameters = @Parameter(
            name = 'path',
            in = ParameterIn.PATH,
            description = "Path and Key file name. Can be a directory path such as `subdir/` or include a filename `subdir/file.password`",
            schema=@Schema(type='string'),
            allowReserved = true,
            allowEmptyValue = true
        ),
        responses = [
            @ApiResponse(
            responseCode = "200",
            description = "Key Metadata",
            content = [
                @Content(
                mediaType = MediaType.APPLICATION_JSON,
                schema = @Schema(type = 'object'),
                examples = [
                    @ExampleObject(value='''
{
  "meta": {
    "Rundeck-key-type": "public",
    "Rundeck-content-size": "393",
    "Rundeck-content-type": "application/pgp-keys"
  },
  "url": "http://rundeckhost/api/11/storage/keys/test1.pub",
  "name": "test1.pub",
  "type": "file",
  "path": "keys/test1.pub"
}''',name='key-metadata',summary='Key Metadata Result'),
                    @ExampleObject(value='''
{
  "resources": [
    {
      "meta": {
        "Rundeck-key-type": "private",
        "Rundeck-content-mask": "content",
        "Rundeck-content-size": "1679",
        "Rundeck-content-type": "application/octet-stream"
      },
      "url": "http://rundeckhost/api/11/storage/keys/test1.pem",
      "name": "test1.pem",
      "type": "file",
      "path": "keys/test1.pem"
    },
    {
      "url": "http://rundeckhost/api/11/storage/keys/subdir",
      "type": "directory",
      "path": "keys/subdir"
    },
    {
      "meta": {
        "Rundeck-key-type": "public",
        "Rundeck-content-size": "640198",
        "Rundeck-content-type": "application/pgp-keys"
      },
      "url": "http://rundeckhost/api/11/storage/keys/monkey1.pub",
      "name": "monkey1.pub",
      "type": "file",
      "path": "keys/monkey1.pub"
    },
    {
      "meta": {
        "Rundeck-key-type": "public",
        "Rundeck-content-size": "393",
        "Rundeck-content-type": "application/pgp-keys"
      },
      "url": "http://rundeckhost/api/11/storage/keys/test1.pub",
      "name": "test1.pub",
      "type": "file",
      "path": "keys/test1.pub"
    }
  ],
  "url": "http://rundeckhost/api/11/storage/keys",
  "type": "directory",
  "path": "keys"
}''', name='list-keys',summary='List Directory')]
            ),
            @Content(
                mediaType = 'application/pgp-keys',
                schema=@Schema(type='string'),
                examples = @ExampleObject(
                    name='public-key',
                    summary='Public Key contents',
                    value='''...Public Key Contents...'''
                )
            )
            ]
        ),
        @ApiResponse(responseCode='403',description='Unauthorized')
        ]
    )
    def apiGetResource(@Parameter(hidden = true) StorageParams storageParams) {
        if (!apiService.requireApi(request, response)) {
            return
        }
        return getResource(storageParams)
    }

    private AuthContext getAuthContextForPath(def subject, String path){
        def project=storageService.getProjectPath(path)
        if(project) {
            return rundeckAuthContextProvider.getAuthContextForSubjectAndProject(subject, project)
        } else {
            return rundeckAuthContextProvider.getAuthContextForSubject(subject)
        }
    }

    private def getResource(StorageParams storageParams,boolean forceDownload=false) {
        if (storageParams.hasErrors()) {
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.invalid.request',
                    args: [storageParams.errors.allErrors.collect { g.message(error: it) }.join(",")]
            ])
        }
        AuthContext authContext = getAuthContextForPath(session.subject, storageParams.resourcePath)
        String resourcePath = storageParams.resourcePath
        def found = storageService.hasPath(authContext, resourcePath)
        if(!found){
            response.status=404
            return renderError("resource not found: ${resourcePath}")
        }
        try{
            def resource = storageService.getResource(authContext, resourcePath)
            if (resource.directory) {
                //list directory and render resources
                def dirlist = storageService.listDir(authContext, resourcePath)
                return renderDirectory(request, response, resource,dirlist)
            } else {
                return renderResourceFile(request, response, resource, forceDownload)
            }
        } catch (StorageAuthorizationException e) {
            log.error("Unauthorized: resource ${resourcePath}: ${e.message}")
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_FORBIDDEN,
                    code: 'api.error.item.unauthorized',
                    args: [e.event.toString(), 'Path', e.path.toString()]
            ])
        }catch (StorageException e) {
            log.error("Error reading resource ${resourcePath}: ${e.message}")
            log.debug("Error reading resource ${resourcePath}",e)
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: e.message
            ])
        }catch(Exception e){
            log.error("Error reading resource ${resourcePath}: ${e.message}")
            apiService.renderErrorFormat(response, [
                    status: HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    message: "Error: ${e.getMessage()}",
                    meta: "Error: ${e.message}"
            ])
        }
    }
}
