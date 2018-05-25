import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import java.util.regex.Pattern
import java.util.regex.Matcher

rundeckPlugin(LogFilterPlugin) {
	title = "Happy Filter"
    description = "replace :) with 😀"
    version = "0.0.1"
    url = "http://rundeck.com"
    author = "© 2018, Rundeck, Inc."
    date = "2018-04-03T17:52:09Z"

    configuration {
    	pattern = ':)'
    	replacement='😀'
    }
    handleEvent{
    	if(it.message.contains(configuration.pattern)){
    		it.message = it.message.replaceAll(Pattern.quote(configuration.pattern),Matcher.quoteReplacement(configuration.replacement))

    		it.addMetadata('content-data-type','application/x-happy-data')
    	}
    }
}