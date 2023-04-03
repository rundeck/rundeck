package org.rundeck.app.data.options

import com.dtolabs.rundeck.core.http.ApacheHttpClient
import com.dtolabs.rundeck.core.http.HttpClient
import com.dtolabs.rundeck.core.options.RemoteJsonOptionRetriever
import com.dtolabs.rundeck.core.options.RemoteJsonResponse
import groovy.util.logging.Slf4j
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.utils.DateUtils
import org.grails.web.json.JSONElement

@Slf4j
class DefaultRemoteJsonOptionRetriever implements RemoteJsonOptionRetriever {
    @Override
    RemoteJsonResponse getRemoteJson(String url, int timeout, int contimeout, int retry=5, boolean disableRemoteOptionJsonCheck=false) {
        log.debug("getRemoteJSON: "+url+", timeout: "+timeout+", retry: "+retry)
        //attempt to get the URL JSON data
        def rjresponse = new RemoteJsonResponse()
        if(url.startsWith("http:") || url.startsWith("https:")){
            HttpClient<HttpResponse> client = new ApacheHttpClient()
            client.setFollowRedirects(true)
            client.setTimeout(timeout*1000)


            URL urlo
            String cleanUrl = url.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
            try{
                urlo = new URL(url)
                if(urlo.userInfo){
                    client.setUri(new URL(cleanUrl).toURI())
                    UsernamePasswordCredentials cred = new UsernamePasswordCredentials(urlo.userInfo)
                    client.setBasicAuthCredentials(cred.userName, cred.password)
                } else {
                    client.setUri(urlo.toURI())
                }
            }catch(MalformedURLException e){
                throw new Exception("Failed to configure base URL for authentication: "+e.getMessage(),e)
            }
            client.addHeader("Accept","application/json")
            response.stats.url = cleanUrl;
            response.stats.startTime = System.currentTimeMillis();
            def results = [:]
            client.execute { response ->
                int resultCode = response.statusLine.statusCode
                rjresponse.stats.httpStatusCode = response.statusLine.statusCode
                rjresponse.stats.httpStatusText = response.statusLine.reasonPhrase
                rjresponse.stats.finishTime = System.currentTimeMillis()
                rjresponse.stats.durationTime=rjresponse.stats.finishTime-rjresponse.stats.startTime
                rjresponse.stats.contentLength = response.getEntity().contentLength
                final header = response.getFirstHeader("Last-Modified")
                if(null!=header){
                    rjresponse.stats.lastModifiedDate= DateUtils.parseDate(header.getValue())
                }

                def reasonCode = response.statusLine.reasonPhrase
                if(resultCode>=200 && resultCode<=300){
                    def expectedContentType="application/json"
                    def resultType=''
                    if (null != response.getFirstHeader("Content-Type")) {
                        resultType = response.getFirstHeader("Content-Type").getValue();
                    }
                    String type = resultType;
                    if (type.indexOf(";") > 0) {
                        type = type.substring(0, type.indexOf(";")).trim();
                    }

                    boolean continueRendering=true

                    if(!disableRemoteOptionJsonCheck &&
                            !expectedContentType.equals(type)){
                        continueRendering=false
                    }

                    if (continueRendering) {
                        final stream = response.getEntity().content
                        final rawJson = stream.text
                        def json=grails.converters.JSON.parse(rawJson)
                        if(rawJson){
                            rjresponse.stats.contentSHA1=rawJson.encodeAsSHA1()
                            if(rjresponse.stats.contentLength<0){
                                rjresponse.stats.contentLength= rawJson.length()
                            }
                        }else{
                            rjresponse.stats.contentSHA1=null
                        }
                        rjresponse.json = json
                    }else{
                        rjresponse.error = "Unexpected content type received: "+resultType
                    }
                }else{
                    rjresponse.stats.contentSHA1 = null
                    rjresponse.error = "Server returned an error response: ${resultCode} ${reasonCode}"
                }
            }
            return rjresponse
        }else if (url.startsWith("file:")) {
            rjresponse.stats.url=url
            File srfile = new File(new URI(url))
            final stream= new FileInputStream(srfile)

            rjresponse.stats.startTime = System.currentTimeMillis();
            final rawJson = stream.text
            rjresponse.stats.finishTime = System.currentTimeMillis()
            rjresponse.stats.durationTime = rjresponse.stats.finishTime - rjresponse.stats.startTime
            final JSONElement parse = grails.converters.JSON.parse(rawJson)
            if (rawJson) {
                rjresponse.stats.contentSHA1 = rawJson.encodeAsSHA1()
            }else{
                rjresponse.stats.contentSHA1 = ""
            }
            rjresponse.stats.contentLength=srfile.length()
            rjresponse.stats.lastModifiedDate=new Date(srfile.lastModified())
            rjresponse.stats.lastModifiedDateTime=srfile.lastModified()
            rjresponse.json = parse
            return rjresponse
        } else {
            throw new Exception("Unsupported protocol: " + url)
        }
    }
}
