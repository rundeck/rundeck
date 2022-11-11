package org.rundeck.app.data.util

import com.dtolabs.rundeck.core.http.ApacheHttpClient
import com.dtolabs.rundeck.core.http.HttpClient
import groovy.util.logging.Slf4j
import org.apache.http.HttpResponse
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.utils.DateUtils
import org.grails.web.json.JSONElement

@Slf4j
class RemoteJsonUtil {
    /**
     * Make a remote URL request and return the parsed JSON data and statistics for http requests in a map.
     * if an error occurs, a map with a single 'error' entry will be returned.
     * the stats data contains:
     *
     * url: requested url
     * startTime: start time epoch ms
     * httpStatusCode: http status code (int)
     * httpStatusText: http status text
     * finishTime: finish time epoch ms
     * durationTime: duration time in ms
     * contentLength: response content length bytes (long)
     * lastModifiedDate: Last-Modified header (Date)
     * contentSHA1: SHA1 hash of the content
     *
     * @param url URL to request
     * @param timeout request timeout in seconds
     * @return Map of data, [json: parsed json or null, stats: stats data, error: error message]
     *
     */
    static Object getRemoteJSON(String url, int timeout, int contimeout, int retry=5,boolean disableRemoteOptionJsonCheck=false){
        log.debug("getRemoteJSON: "+url+", timeout: "+timeout+", retry: "+retry)
        //attempt to get the URL JSON data
        def stats=[:]
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
            stats.url = cleanUrl;
            stats.startTime = System.currentTimeMillis();
            def results = [:]
            client.execute { response ->
                int resultCode = response.statusLine.statusCode
                stats.httpStatusCode = response.statusLine.statusCode
                stats.httpStatusText = response.statusLine.reasonPhrase
                stats.finishTime = System.currentTimeMillis()
                stats.durationTime=stats.finishTime-stats.startTime
                stats.contentLength = response.getEntity().contentLength
                final header = response.getFirstHeader("Last-Modified")
                if(null!=header){
                    stats.lastModifiedDate= DateUtils.parseDate(header.getValue())
                }else{
                    stats.lastModifiedDate=""
                    stats.lastModifiedDateTime=""
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
                            stats.contentSHA1=rawJson.encodeAsSHA1()
                            if(stats.contentLength<0){
                                stats.contentLength= rawJson.length()
                            }
                        }else{
                            stats.contentSHA1=""
                        }
                        results = [json:json,stats:stats]
                    }else{
                        results = [error:"Unexpected content type received: "+resultType,stats:stats]
                    }
                }else{
                    stats.contentSHA1 = ""
                    results = [error:"Server returned an error response: ${resultCode} ${reasonCode}",stats:stats]
                }
            }
            return results
        }else if (url.startsWith("file:")) {
            stats.url=url
            File srfile = new File(new URI(url))
            final stream= new FileInputStream(srfile)

            stats.startTime = System.currentTimeMillis();
            final rawJson = stream.text
            stats.finishTime = System.currentTimeMillis()
            stats.durationTime = stats.finishTime - stats.startTime
            final JSONElement parse = grails.converters.JSON.parse(rawJson)
            if (rawJson) {
                stats.contentSHA1 = rawJson.encodeAsSHA1()
            }else{
                stats.contentSHA1 = ""
            }
            stats.contentLength=srfile.length()
            stats.lastModifiedDate=new Date(srfile.lastModified())
            stats.lastModifiedDateTime=srfile.lastModified()
            return [json:parse,stats:stats]
        } else {
            throw new Exception("Unsupported protocol: " + url)
        }
    }

}
