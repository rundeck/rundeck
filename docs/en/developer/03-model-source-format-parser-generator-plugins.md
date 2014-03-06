% Model Format Parser and Generator Plugin
% Greg Schueler, Alex Honor
% November 20, 2010

## About
Resource format Parser and Generator providers are used to serialize a set of
Node resources into a textual format for transport or storage.



Each Parser and Generator must declare the set of filename extensions 
(such as "xml" or "json") that it supports, as well as the set of 
MIME types that it supports (such as "text/xml" or "application/json".)  
This lets other services retrieve the appropriate
parser or generator when all that is known about the source or destination 
of serialized data is a filename or a MIME type.

## Java Plugin Type

### ResourceFormatParser

For Parsers, your provider class must implement the `com.dtolabs.rundeck.core.resources.format.ResourceFormatParser` interface:

~~~~~ {.java}
public interface ResourceFormatParser {
    /**
     * Return the list of file extensions that this format parser can parse.
     */
    public Set<String> getFileExtensions();
 
    /**
     * Return the list of MIME types that this format parser can parse. 
     * This may include wildcards such as "*&#47;xml".
     */
    public Set<String> getMIMETypes();
 
    /**
     * Parse a file
     */
    public INodeSet parseDocument(File file) throws ResourceFormatParserException;
 
    /**
     * Parse an input stream
     */
    public INodeSet parseDocument(InputStream input) throws ResourceFormatParserException;
}
~~~~~~~~

### ResourceFormatGenerator

For Generators, your provider class must implement the `com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator` interface:

~~~~~ {.java}
public interface ResourceFormatGenerator {
 
    /**
     * Return the list of file extensions that this format generator can generate
     */
    public Set<String> getFileExtensions();
 
    /**
     * Return the list of MIME types that this format generator can generate. 
     * If more than one are returned, then the first value will be used by 
     * default if necessary.
     */
    public List<String> getMIMETypes();
 
    /**
     * generate formatted output
     */
    public void generateDocument(INodeSet nodeset, OutputStream stream) 
                   throws ResourceFormatGeneratorException,IOException;
}
~~~~~~~~

More information is available in the Javadoc.

