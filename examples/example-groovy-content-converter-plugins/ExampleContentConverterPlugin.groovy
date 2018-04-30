import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import java.util.regex.Pattern
import java.util.regex.Matcher
import groovy.xml.MarkupBuilder

/**
* Example ContentConverterPlugin in groovy
*/
rundeckPlugin(ContentConverterPlugin) {
    // declare plugin metadata
	title = "Listify"
    description = "blah blah"
    version = "0.0.1"
    url = "http://rundeck.com"
    author = "© 2018, Rundeck, Inc."

    //note: currently configuration input, is not supported for ContentConverterPlugins

    /**
    * Define a conversion from a String datatype 'application/x-happy-data'
    * to a List datatype 'application/x-my-list-type'.
    */
    convert('application/x-happy-data', dataType(List,'application/x-my-list-type')){
        //split text into words
        // Note that without `toList()` the result would be an array,
        // and would not be converted by the second converter definition below
        data.split(' +').toList()
    }

    /**
    * Define a conversion from my List data type, to the default of String 'text/html' output,
    * by converting the List of strings into a HTML ordered list
    */
    convert(dataType(List,'application/x-my-list-type')){
        
        //if the log output contains a metadata entry named 'content-meta:css' we will
        // be able to retrieve it with metadata.css, otherwise we default to 'text-success'
        def css = metadata.css ?: 'text-success'

        // we could create the html by hand with strings...
        // """
        // <ol class='${css}'>
        //   <li>"""+
        //     data.join('</li>\n  <li>')+
        // '''
        //   </li>
        // </ol>
        // '''

        //...or instead use a MarkupBuilder to make sure we don't miss a <
        def writer = new StringWriter()
        new MarkupBuilder(writer).ol('class': css){
            data.each{d->
                li d
            }
        }

        //return the string
        writer.toString()
    }
}