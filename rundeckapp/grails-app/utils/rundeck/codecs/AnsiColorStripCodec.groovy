package rundeck.codecs

/**
 * AnsiColorStripCodec strips ansi escape codes
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-05-15
 */
class AnsiColorStripCodec {
    def decode = { str ->
        str.replaceAll('\033[\\[%\\(]((\\d{1,2})?(;\\d{1,3})*)[mGHfABCDRsuhl]','')
    }
}
