package rundeck.codecs

/**
 * encode all &lt; and &gt; chars with HTML entity equivalents.
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-05-15
 */
class HTMLElementCodec {
    def encode={str->
        str.replaceAll('<',"&lt;").replaceAll('>',"&gt;")
    }
}
