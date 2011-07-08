import java.io.Reader
import java.io.Writer

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpMethod
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.params.HttpClientParams
import org.apache.commons.httpclient.util.DateParseException
import org.apache.commons.httpclient.util.DateUtil
import org.codehaus.groovy.grails.web.json.JSONElement


class RemoteJSONUtil {
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
   static Object getRemoteJSON(String url, int timeout){
	   //attempt to get the URL JSON data
	   def stats=[:]
	   if(url.startsWith("http:") || url.startsWith("https:")){
		   final HttpClientParams params = new HttpClientParams()
		   params.setConnectionManagerTimeout(timeout*1000)
		   params.setSoTimeout(timeout*1000)
		   def HttpClient client= new HttpClient(params)
		   def URL urlo
		   def AuthScope authscope=null
		   def UsernamePasswordCredentials cred=null
		   boolean doauth=false
		   String cleanUrl = url.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
		   try{
			   urlo = new URL(url)
			   if(urlo.userInfo){
				   doauth = true
				   authscope = new AuthScope(urlo.host,urlo.port>0? urlo.port:urlo.defaultPort,AuthScope.ANY_REALM,"BASIC")
				   cred = new UsernamePasswordCredentials(urlo.userInfo)
				   url = new URL(urlo.protocol, urlo.host, urlo.port, urlo.file).toExternalForm()
			   }
		   }catch(MalformedURLException e){
			   throw new Exception("Failed to configure base URL for authentication: "+e.getMessage(),e)
		   }
		   if(doauth){
			   client.getParams().setAuthenticationPreemptive(true);
			   client.getState().setCredentials(authscope,cred)
		   }
		   def HttpMethod method = new GetMethod(url)
		   method.setFollowRedirects(true)
		   method.setRequestHeader("Accept","application/json")
		   stats.url = cleanUrl;
		   stats.startTime = System.currentTimeMillis();
		   def resultCode = client.executeMethod(method);
		   stats.httpStatusCode = resultCode
		   stats.httpStatusText = method.getStatusText()
		   stats.finishTime = System.currentTimeMillis()
		   stats.durationTime=stats.finishTime-stats.startTime
		   stats.contentLength = method.getResponseContentLength()
		   final header = method.getResponseHeader("Last-Modified")
		   if(null!=header){
			   try {
				   stats.lastModifiedDate= DateUtil.parseDate(header.getValue())
			   } catch (DateParseException e) {
			   }
		   }else{
			   stats.lastModifiedDate=""
			   stats.lastModifiedDateTime=""
		   }
		   try{
			   def reasonCode = method.getStatusText();
			   if(resultCode>=200 && resultCode<=300){
				   def expectedContentType="application/json"
				   def resultType=''
				   if (null != method.getResponseHeader("Content-Type")) {
					   resultType = method.getResponseHeader("Content-Type").getValue();
				   }
				   String type = resultType;
				   if (type.indexOf(";") > 0) {
					   type = type.substring(0, type.indexOf(";")).trim();
				   }

				   if (expectedContentType.equals(type)) {
					   final stream = method.getResponseBodyAsStream()
					   final writer = new StringWriter()
					   int len=copyToWriter(new BufferedReader(new InputStreamReader(stream, method.getResponseCharSet())),writer)
					   stream.close()
					   writer.flush()
					   final string = writer.toString()
					   def json=grails.converters.JSON.parse(string)
					   if(string){
						   stats.contentSHA1=string.encodeAsSHA1()
						   if(stats.contentLength<0){
							   stats.contentLength= len
						   }
					   }else{
						   stats.contentSHA1=""
					   }
					   return [json:json,stats:stats]
				   }else{
					   return [error:"Unexpected content type received: "+resultType,stats:stats]
				   }
			   }else{
				   stats.contentSHA1 = ""
				   return [error:"Server returned an error response: ${resultCode} ${reasonCode}",stats:stats]
			   }
		   } finally {
			   method.releaseConnection();
		   }
	   }else if (url.startsWith("file:")) {
		   def File srfile = new File(new URI(url))
		   final JSONElement parse = grails.converters.JSON.parse(new InputStreamReader(new FileInputStream(srfile)))
		   if(!parse ){
			   throw new Exception("JSON was empty")
		   }
		   return [json:parse,stats:stats]
	   } else {
		   throw new Exception("Unsupported protocol: " + url)
	   }
   }
   
   

   static int copyToWriter(Reader read, Writer writer){
	   char[] chars = new char[1024];
	   int len=0;
	   int size=read.read(chars,0,chars.length)
	   while(-1!=size){
		   len+=size;
		   writer.write(chars,0,size)
		   size = read.read(chars, 0, chars.length)
	   }
	   return len;
   }
}
